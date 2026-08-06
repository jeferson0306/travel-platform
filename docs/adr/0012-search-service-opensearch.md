# 0012 — search-service: OpenSearch as its only datastore

- Status: Accepted
- Date: 2026-07-23

## Context

Travelers need to search/autocomplete flights and hotels faster and more
flexibly than flight-service's/hotel-service's own exact-match MongoDB
queries allow. This is a distinct bounded concern - "make inventory
discoverable" - so it gets its own service (`search-service`, ROADMAP M13),
continuing the same one-service-per-bounded-context shape as every other
service in this platform.

Two questions shaped the design, both departures from precedent set by
every other service so far:

1. **Does this service need MongoDB?** ADR
   [0003](0003-use-mongodb-as-primary-database.md) established MongoDB as
   this platform's primary database. But `search-service` owns no source
   data of its own - every document it holds is a rebuildable projection of
   flight-service's/hotel-service's own MongoDB collections. Introducing
   MongoDB here would mean a second datastore with nothing independently
   valuable in it, purely to satisfy a platform-wide default that does not
   actually apply to a pure read-model service. OpenSearch is both the
   query engine this service needs (full-text, prefix, relevance) and,
   pragmatically, a perfectly adequate document store for data this
   disposable - so it is the _only_ store, not a second one alongside Mongo.
2. **How does search-service learn about flights and hotels?** Choreography,
   continuing the established event-driven style (ADR
   [0004](0004-use-kafka-for-event-driven-communication.md), ADR
   [0010](0010-payment-saga.md)): `flight-service` and `hotel-service` each
   gain their first domain event (`FlightCreated`/`HotelCreated`), raised by
   their existing `create()` factory and published through the exact same
   generic outbox relay `booking-service`/`payment-service` already use (no
   new publish code path - the relay's topic is always the event's
   `eventType`). `search-service` consumes both.

## Decision

- No Quarkus extension for OpenSearch exists at this platform's Quarkus
  version, so the official `opensearch-java` client is used directly and
  wired by hand (an `OpenSearchClient` CDI producer) - the same situation
  already accepted for AWS S3 (ADR
  [0008](0008-use-localstack-and-terraform-for-aws-resources.md)).
- Indices: `flights` and `hotels`, with explicit keyword/date/number
  mappings created on startup if missing (`SearchIndexBootstrap`) - the same
  "own the schema" discipline every other service's MongoDB document mapper
  already follows. `retry_tasks`/`dead_letters` (below) are left to
  OpenSearch's default dynamic mapping - operational bookkeeping, never
  queried by field type.
- **Idempotency is free here.** Indexing by `flightId`/`hotelId` (the
  document `_id`) is an upsert - a duplicate Kafka delivery just overwrites
  the same document with identical content. Every other consumer in this
  platform needs a `processed_bookings`-style claim collection because its
  write is not naturally idempotent (a double-authorize would double-charge,
  a double-decrement would over-reserve); a double-index of the same
  content is a no-op. `FlightCreatedConsumer`/`HotelCreatedConsumer` have no
  claim collection as a direct consequence, not an oversight.
- **Failure handling** keeps the exact shape used everywhere else (ADR
  0004's M10 addendum): a poison message is logged and dropped, a technical
  failure is recorded and retried with exponential backoff, exhausted
  retries move to a dead-letter store and publish to a DLQ topic
  (`flight-created.search-indexer.dlq`, `hotel-created.search-indexer.dlq`).
  The only difference is the retry/DLQ bookkeeping lives in `retry_tasks`/
  `dead_letters` **OpenSearch indices** instead of MongoDB collections,
  since this service has no MongoDB to begin with. `RetryRelay` dispatches
  on the task's `eventType` field exactly like `payment-service`'s
  `RetryRelay` dispatches on `action`.
- **Search is fully public** - no traveler needs an account to search, and
  this service holds no write endpoint at all (indexing only ever happens
  via the Kafka consumers), so there is nothing to protect with RBAC (ADR
  [0006](0006-rbac-roles.md)). It is the first service with no
  authentication of any kind - no `quarkus-smallrye-jwt`, no
  `publicKey.pem`.
- **Autocomplete is a case-insensitive prefix (wildcard-free) query against
  keyword fields**, not an edge-ngram analyzer - `nameLower`/`cityLower`
  storage-only fields (lowercased at index time, never exposed on the
  domain type) make this cheap without a custom analyzer. `AirportCode` is
  already uppercase-normalized by flight-service, so flight autocomplete
  needs no equivalent lowercasing. Sufficient for this dataset's scale;
  revisit with a proper analyzer (typo tolerance, relevance ranking) if
  that ever becomes a real requirement.
- **Tests use Testcontainers, not Quarkus Dev Services** - OpenSearch has no
  Dev Services integration in this Quarkus version, unlike MongoDB/Kafka
  elsewhere. `OpenSearchTestResource` (a
  `QuarkusTestResourceLifecycleManager`) starts a real OpenSearch node per
  test run via the official `org.opensearch:opensearch-testcontainers`
  module.

## Rejected alternative: "geo search"

ROADMAP originally described this milestone as "OpenSearch-backed
autocomplete and geo search." True geo search (radius/distance queries)
needs coordinates on the underlying data; `hotel-service`'s `City` is a
freeform string with no latitude/longitude (documented on `City` itself as
"no reference geo/city data source exists yet"). Building geo search
against data that has no geo data would mean fabricating coordinates -
worse than not building it. Descoped to city-text search, the same
trusted-input-simplification pattern already applied to `amount`
(ADR 0010) and `travelerEmail` (ADR
[0011](0011-notification-service.md)) - revisit once `City` (or a
dedicated `Location` service) carries real coordinates.

## Consequences

- `search-service`'s JaCoCo coverage floor is set lower than every other
  service's (0.30 vs. the 0.45 baseline - docs/development/testing.md): a
  disproportionate share of this service's code is thin OpenSearch
  client-wiring glue (index mapping, CDI producers) already exercised
  indirectly by the integration tests, not meaningfully unit-testable in
  isolation. Measured, not arbitrary, same philosophy as the platform-wide
  gate itself.
- A flight/hotel is only ever indexed once, at creation - `Flight`/`Hotel`
  have no update lifecycle yet (both aggregates' own class comments already
  say so), so there is no `flight-updated`/`hotel-updated` event to
  consume. `availableSeats`/`availableRooms` in the search index can go
  stale relative to the live inventory count as bookings consume seats/
  rooms - acceptable for a search/discovery result (the authoritative
  check still happens at booking time against flight-service/hotel-service
  directly), but worth flagging: this is eventual consistency by design,
  not an oversight.

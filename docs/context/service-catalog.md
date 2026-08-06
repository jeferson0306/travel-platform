# Service catalog (AI-context layer, ROADMAP M18)

One section per service, same shape every time, so an agent can jump to
exactly what it needs. Facts only - derived from each service's code and
`application.yml`; verify against the code before relying on details for a
change.

Every backend service except `gateway` shares the hexagonal layout
`api / application / domain / infrastructure` (enforced by ArchUnit tests)
and the shared conventions in [conventions.md](conventions.md): canonical
error shape, structured JSON request logs, health/metrics endpoints,
Mongo-backed retry queue + per-consumer-group RabbitMQ DLQ for consumers
(except search-service, which keeps that bookkeeping in OpenSearch).

## identity-service (8081)

- **Owns**: users (email, bcrypt password hash, fullName, role, active).
- **API**: `POST /api/v1/auth/register` (creates `USER`-role account),
  `POST /api/v1/auth/login` (returns RS256-signed JWT access token).
- **Events**: none published, none consumed.
- **Security note**: holds the platform's only JWT private key; every
  other service verifies with the public key (ADR 0006 addendum). Roles:
  USER < MANAGER < ADMIN < SUPER_ADMIN, plus SUPPORT. No self-service
  role elevation exists.
- **Perf note**: bcrypt makes registration/login the platform's CPU
  bottleneck under load (measured, ADR 0015/0016).

## booking-service (8082)

- **Owns**: bookings (`PENDING` → `CONFIRMED` | `CANCELLED`), the saga's
  central aggregate.
- **API**: `POST /api/v1/bookings` (flat body: travelerEmail, itemType
  FLIGHT|HOTEL, itemId, quantity, amount, currency, optional itemSummary),
  `GET /api/v1/bookings` (caller's own bookings), `POST
/api/v1/bookings/{id}/cancel`. Every endpoint requires a valid JWT
  (`@Authenticated`); travelerId is always the JWT subject, never client
  input - a caller can only create/list/cancel their own bookings.
  amount/currency/travelerEmail remain trusted client input (a separate,
  still-open gap - no authoritative pricing/identity lookup yet).
  `itemSummary` is likewise trusted, optional client input: a short
  human-readable description (e.g. "Lisbon -> Sao Paulo, TP123, TAP Air
  Portugal") the frontend already has from the search result the user
  clicked - added so "My bookings"/the confirmation page can show what was
  actually booked instead of a bare `itemId`. No synchronous lookup to
  flight-service/hotel-service exists or is planned (ADR 0004 forbids
  sync service-to-service calls).
- **Publishes**: `booking-created`, `booking-cancelled`,
  `booking-confirmed` - all via the transactional outbox (ADR 0007).
- **Consumes**: `payment-authorized` / `payment-failed` (group
  `booking-payment-outcome`) - confirms or compensates.
- **Extra**: best-effort JSON receipt to S3 `booking-receipts`
  (LocalStack locally; not transactional with the booking write - ADR
  0009).

## flight-service (8083) / hotel-service (8084)

Twins, differing only in the inventory noun (seats vs rooms).

- **Owns**: flight/hotel inventory documents. Beyond the original
  route/price/inventory fields, both now carry richer, informational-only
  fields for search-result display: flights add `airline`, `airlineCode`,
  `flightNumber` (all required on create), `cabinClass` (optional
  freeform string), `stops` (int, defaults 0); hotels add `address`
  (optional), `starRating` (1-5, defaults to 3 if omitted), `amenities`
  (list of string, defaults empty), `description` (optional),
  `reviewScore`/`reviewCount` (optional). None of this is looked up
  synchronously from elsewhere - see `docs/events/flight-events.md` and
  `hotel-events.md` for the full field list and the documented gap where
  `search-service`'s own projection does not yet carry these fields.
- **API**: `POST /api/v1/flights|hotels` (MANAGER/ADMIN/SUPER_ADMIN),
  `GET` search - public.
- **Publishes**: `flight-created` / `hotel-created` (own outbox).
- **Consumes**: `booking-created` (groups `flight-inventory` /
  `hotel-inventory`) - atomically decrements inventory when `itemType`
  matches; idempotent via a `processed_bookings` claim collection.

## payment-service (8085)

- **Owns**: payments (`AUTHORIZED` | `FAILED` | `REFUNDED`), simulated
  gateway.
- **API**: `GET /api/v1/payments/{bookingId}` (SUPPORT/ADMIN/SUPER_ADMIN).
- **Publishes**: `payment-authorized`, `payment-failed`,
  `payment-refunded`.
- **Consumes**: `booking-created` (authorize) and `booking-cancelled`
  (refund), group `payment-processor`; claims bookingIds for idempotency.

## notification-service (8086)

- **Owns**: notifications (`SENT`), simulated email gateway. The saga's
  terminal step - deliberately no domain events and no outbox (ADR 0011).
- **API**: `GET /api/v1/notifications/{bookingId}`
  (SUPPORT/ADMIN/SUPER_ADMIN).
- **Consumes**: `booking-confirmed`, group `notification-processor`.

## search-service (8087)

- **Owns**: nothing canonical - a rebuildable OpenSearch projection of
  flight/hotel inventory (ADR 0012). The only service with no MongoDB.
- **API** (fully public): `GET /api/v1/search/flights`
  (`origin`+`destination`, or `q=` prefix autocomplete),
  `GET /api/v1/search/hotels` (`city`, or `q=`).
- **Consumes**: `flight-created`, `hotel-created`, group `search-indexer`.
  Idempotency is free (index-by-id is an upsert); retry/DLQ bookkeeping
  lives in OpenSearch indices (`retry_tasks`/`dead_letters`).
- **Resilience**: `@Timeout`/`@Retry` on the three read methods only
  (ADR 0014).

## assistant-service (8088)

- **Owns**: nothing - stateless, read-only. No MongoDB, no RabbitMQ. Its one
  synchronous external dependency is Ollama (local LLM runtime, ADR 0018),
  the third such dependency in this platform after gateway->backends and
  search-service->OpenSearch (ADR 0014) - same `@Timeout`/`@Retry` pattern.
- **API**: `POST /api/v1/assistant/ask` (`{"question": "..."}` ->
  `{"answer", "sourcesUsed"}`), gated to MANAGER/ADMIN/SUPER_ADMIN/SUPPORT
  - an internal engineering tool, not traveler-facing.
- **How it answers**: "stuffs" the entire `docs/context`/`docs/prompts`
  corpus (bundled into the JAR at build time, not read from disk at
  runtime) into the LLM system prompt on every call - no retrieval/vector
  search step, the corpus is small enough not to need one (ADR 0018).
- **Model**: `llama3:latest` by default (`OLLAMA_MODEL`), `num_ctx=8192`
  explicitly set - Ollama's own default (2048) silently truncated this
  corpus and caused real hallucination in testing before this was fixed.

## gateway (8080)

- **Owns**: nothing - stateless except Redis rate-limit counters. Not
  hexagonal (no domain to protect, ADR 0013).
- **Behavior**: forwards method/headers/query/body verbatim to the backend
  owning the first path segment after `/api/v1/` (upstreams configurable
  via `GATEWAY_UPSTREAM_<SEGMENT>` env vars). Before every proxy:
  Redis-backed fixed-window rate limit (default 120 req/min per client
  IP → 429; override `GATEWAY_RATE_LIMIT_REQUESTS_PER_MINUTE`) and a JWT
  fast-fail check (present-but-invalid token → 401; missing token passes
  through - authorization stays in each backend).
- **Resilience** (ADR 0014): one SmallRye `Guard` per backend segment
  (independent timeout/circuit-breaker/bulkhead), retry only on the
  idempotent GET/HEAD path; `CircuitBreakerOpenException` → 503, FT
  timeout → 504, other connect failures → 502.

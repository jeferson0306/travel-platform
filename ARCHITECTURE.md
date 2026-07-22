# Architecture

This document describes the system's shape and the reasoning behind it. For
the reasoning behind any single choice, see the corresponding ADR in
[docs/adr](docs/adr) — this file describes the _current_ state, ADRs describe
_why_ it got that way and what was rejected.

## Guiding principles

1. **A service exists because it owns a bounded context, not because it is
   convenient to split.** Every service boundary below maps to a business
   capability with its own data and lifecycle.
2. **Framework at the edges, domain in the center.** Business rules never
   depend on Quarkus, MongoDB, or Kafka types — those are adapters. See
   "Package structure" below.
3. **Every cross-service interaction is either a synchronous REST call to a
   narrow, versioned API, or an asynchronous domain event.** There is no
   shared database between services.
4. **Nothing is "eventually consistent" by accident.** Where consistency
   matters (payments, bookings), the pattern used (outbox, saga, idempotency
   key) is explicit and documented.

## Service map

| Service                | Owns                                       | Talks to                                                        |
| ---------------------- | ------------------------------------------ | --------------------------------------------------------------- |
| `identity-service`     | Users, credentials, sessions, RBAC         | issues JWTs consumed by all services                            |
| `booking-service`      | Reservation lifecycle                      | flight/hotel-service (availability), Kafka (`booking-*` events) |
| `payment-service`      | Payment authorization/capture, idempotency | booking-service (saga), Kafka (`payment-*` events)              |
| `flight-service`       | Flight inventory & pricing                 | search-service (indexing)                                       |
| `hotel-service`        | Hotel inventory & pricing                  | search-service (indexing)                                       |
| `currency-service`     | FX rates, multi-currency conversion        | consumed by booking/payment                                     |
| `notification-service` | Email/SMS/push delivery                    | Kafka (`notification-created`, `email-requested`)               |
| `search-service`       | Autocomplete, fuzzy & geo search           | OpenSearch, consumes flight/hotel events                        |
| `gateway`              | Routing, auth enforcement, rate limiting   | fronts every service above                                      |

Full container-level detail: [docs/c4](docs/c4).

## Package structure (per backend service)

Every Quarkus service follows the same internal layering (hexagonal /
ports-and-adapters), regardless of its domain:

```
com.travelplatform.<service>/
├── domain/            # Entities, value objects, domain events, business rules.
│                       # No framework imports.
├── application/        # Use cases / application services. Orchestrates domain
│                       # objects, defines ports (interfaces) infrastructure implements.
├── infrastructure/      # Adapters: MongoDB repositories, Kafka producers/consumers,
│                       # REST clients to other services, S3/SES clients.
├── api/                # Inbound adapters: REST resources, DTOs, mappers (MapStruct).
├── configuration/       # Wiring, MicroProfile Config, security config.
├── messaging/          # Kafka topic definitions, event schemas, consumer wiring.
└── shared/             # Cross-cutting kernel shared within the service only.
```

`domain` never imports from `infrastructure` or `api`. Dependencies point
inward, enforced by an ArchUnit test in every service's test suite (see
[docs/development](docs/development)).

## Data ownership

MongoDB collections are never shared across services. Each service's data is
private to it; other services only see it through that service's API or
through the events it publishes. See ADR
[0003](docs/adr/0003-use-mongodb-as-primary-database.md).

## Event-driven backbone

Kafka is the integration backbone for anything that does not need an
immediate synchronous answer. Topics, producers, and consumers are documented
as they are introduced in [docs/asyncapi](docs/asyncapi) and
[docs/events](docs/events). See ADR
[0004](docs/adr/0004-use-kafka-for-event-driven-communication.md).

## Resilience

Every outbound call (service-to-service REST, database, cache, external API)
is wrapped with Quarkus Fault Tolerance (timeout, retry, circuit breaker,
bulkhead) as it is implemented — documented per-service as it lands, with the
patterns catalogued in [docs/runbooks](docs/runbooks).

## Observability contract

Every request, regardless of which service handles it, produces a structured
JSON log line carrying: `timestamp`, `traceId`, `spanId`, `correlationId`,
`requestId`, `userId` (when authenticated), `method`, `uri`, `status`,
`durationMs`. This is a contract, not a convention — it is enforced by a
shared logging adapter every service uses (introduced in Phase 1).

## Status

This document reflects the target architecture. As of the current milestone
(see [ROADMAP.md](ROADMAP.md)), `identity-service` (Phase 1), `booking-service`
(Phase 2, M8), `flight-service` and `hotel-service` (Phase 2, M9) are
implemented; `payment-service`, `currency-service`, `notification-service`,
`search-service` and `gateway` are still planned. `flight-service` and
`hotel-service` also consume `booking-created` (M10) - the platform's first
real cross-service event-driven integration, not just publish-and-forget.

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
   depend on Quarkus, MongoDB, or RabbitMQ types — those are adapters. See
   "Package structure" below.
3. **Every cross-service interaction is either a synchronous REST call to a
   narrow, versioned API, or an asynchronous domain event.** There is no
   shared database between services.
4. **Nothing is "eventually consistent" by accident.** Where consistency
   matters (payments, bookings), the pattern used (outbox, saga, idempotency
   key) is explicit and documented.

## Service map

| Service                | Owns                                       | Talks to                                                                                     |
| ---------------------- | ------------------------------------------ | -------------------------------------------------------------------------------------------- |
| `identity-service`     | Users, credentials, sessions, RBAC         | issues JWTs consumed by all services                                                         |
| `booking-service`      | Reservation lifecycle                      | flight/hotel-service (availability), RabbitMQ (`booking-*` events)                           |
| `payment-service`      | Payment authorization/capture, idempotency | booking-service (saga), RabbitMQ (`payment-*` events)                                        |
| `flight-service`       | Flight inventory & pricing                 | search-service (indexing)                                                                    |
| `hotel-service`        | Hotel inventory & pricing                  | search-service (indexing)                                                                    |
| `notification-service` | Email/SMS/push delivery                    | RabbitMQ (consumes `booking-confirmed`)                                                      |
| `search-service`       | Autocomplete & route/city search           | OpenSearch (its only store - ADR 0012), RabbitMQ (consumes `flight-created`/`hotel-created`) |
| `assistant-service`    | Engineering Q&A grounded in docs/context   | Ollama, local LLM runtime (ADR 0018)                                                         |
| `gateway`              | Routing, JWT fast-fail, rate limiting      | fronts every service above (ADR 0013), Redis (rate limit counters)                           |

A `currency-service` was sketched in early planning (FX rates, multi-currency
conversion) but never built - `Money` value objects in `booking-service` and
`payment-service` carry a currency code today, single-currency, with no
conversion step. Descoped rather than forgotten: no milestone ever targeted
it, and nothing downstream depends on it existing.

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
├── infrastructure/      # Adapters: MongoDB repositories, RabbitMQ producers/consumers,
│                       # REST clients to other services, S3/SES clients.
├── api/                # Inbound adapters: REST resources, DTOs, mappers (MapStruct).
├── configuration/       # Wiring, MicroProfile Config, security config.
├── messaging/          # RabbitMQ exchange/queue definitions, event schemas, consumer wiring.
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

RabbitMQ is the integration backbone for anything that does not need an
immediate synchronous answer. Topics, producers, and consumers are documented
as they are introduced in [docs/asyncapi](docs/asyncapi) and
[docs/events](docs/events). See ADR
[0004](docs/adr/0004-use-kafka-for-event-driven-communication.md).

## AWS resources

Where a need is naturally AWS-shaped rather than message-shaped (durable
object storage, later transactional email/config/secrets), resources are
provisioned with Terraform against LocalStack locally and a real AWS
account otherwise - see ADR
[0008](docs/adr/0008-use-localstack-and-terraform-for-aws-resources.md).
First resource: an S3 bucket (`booking-receipts`) that `booking-service`
writes a JSON booking confirmation to - ADR
[0009](docs/adr/0009-booking-receipts-in-s3.md).

## Resilience

Applied where a genuine synchronous external dependency exists, not
uniformly across every service - most services have none (M15, ADR 0014).
`gateway` wraps every proxied call to a backend in a SmallRye Fault
Tolerance `Guard` (timeout, circuit breaker, bulkhead; retry only for
idempotent GET/HEAD), one independent `Guard` per backend so one struggling
service can't trip the breaker for the rest. `search-service` wraps its
OpenSearch read queries in `@Timeout`/`@Retry`. Every other service's
resilience story for its RabbitMQ consumers is the Mongo/OpenSearch-backed
retry queue + DLQ pattern from M10 (ADR 0004's addendum), not Fault
Tolerance annotations - the two mechanisms would conflict if stacked.
Operational playbooks for these failure modes live in
[docs/runbooks/load-and-chaos-results.md](docs/runbooks/load-and-chaos-results.md)
(M16, ADR 0015), written against real scenarios a Toxiproxy chaos
experiment actually exercised against the running platform - not
speculatively: injecting latency in front of `flight-service` was
observed to open its `Guard`, fail fast, stay fully isolated from every
other backend, and recover automatically once the fault cleared.

## Observability contract

Every request, regardless of which service handles it, produces a structured
JSON log line carrying: `timestamp`, `traceId`, `spanId`, `correlationId`,
`requestId`, `userId` (when authenticated), `method`, `uri`, `status`,
`durationMs`. This is a contract, not a convention — it is enforced by a
shared logging adapter every service uses (introduced in Phase 1).

## Status

This document reflects the target architecture. As of the current milestone
(see [ROADMAP.md](ROADMAP.md)), `identity-service` (Phase 1), `booking-service`
(Phase 2, M8), `flight-service`, `hotel-service` (Phase 2, M9),
`payment-service` (Phase 3, M11), `notification-service` (Phase 3, M12),
`search-service` (Phase 3, M13) and `gateway` (Phase 3, M14, closing the
phase) are implemented; `currency-service` was descoped (see "Service map"
above), not deferred.
`flight-service` and `hotel-service` consume `booking-created` (M10) - the
platform's first real cross-service event-driven integration, not just
publish-and-forget. `payment-service` and `booking-service` extend that into
a full choreography saga (M11, ADR 0010): booking-created triggers payment
authorization, whose outcome confirms or compensates (cancels) the booking.
`notification-service` (M12, ADR 0011) is the saga's terminal step: it
consumes `booking-confirmed` and sends a confirmation email - the platform's
first consumer with no domain events/outbox of its own, since nothing
downstream reacts to "a notification was sent." `search-service` (M13, ADR 0012) is the platform's first service with no MongoDB at all - OpenSearch is
both its query engine and its only store, since it owns no source data of
its own (every document is a rebuildable projection of flight-service's/
hotel-service's own data). It is also the first fully public service (no
authentication anywhere) and the first where a duplicate RabbitMQ delivery
needs no idempotency claim collection, since indexing by id is a natural
upsert. `gateway` (M14, ADR 0013) fronts every service above it: routing by
path prefix, a JWT fast-fail check (signature/expiry only - authorization
stays exclusively in each backend, defense in depth), and Redis-backed rate
limiting. It is the only container reachable from outside the platform
boundary besides the SPA build artifacts. Phase 4 (resilience & scale)
opened with M15 (ADR 0014): fault tolerance applied only to `gateway`'s
and `search-service`'s genuine synchronous dependencies, not invented
elsewhere. M16 (ADR 0015) then validated that fault tolerance against the
real running stack: a docker-compose `apps` profile brings up all 8
services together, k6 load-tests the public search path and the full
booking saga through `gateway`, and a Toxiproxy chaos experiment confirms
`gateway`'s per-backend circuit breaker isolates a struggling
`flight-service` from the rest of the platform and recovers on its own.
M17 (ADR 0016) closes the phase with Kubernetes manifests
(`infrastructure/kubernetes/`, Kustomize base + local overlay): per-service
Deployments/Services/HPAs with probes on the M6 health endpoints, deployed
and verified end-to-end on a real local kind cluster - including the full
booking saga completing in-cluster and HPA scaling observed under genuine
CPU load, not just configured. Phase 5 (AI engineering layer) opened with
M18 (ADR 0017: curated `docs/context`/`docs/prompts` + root `AGENTS.md`)
and continued with M19 (ADR 0018): `assistant-service`, a ninth backend
service answering engineering questions grounded in that same
docs/context corpus, backed by a local Ollama runtime rather than a paid
hosted LLM API - consistent with this platform's local-first posture.
Phase 6 (production polish) closes with M20 (ADR 0019): a minimal React
frontend covering the essential booking flow, verified end-to-end against
the real stack, and a public backend deployment scoped to the six
services the choreography saga actually needs - `search-service` and
`assistant-service` (and the OpenSearch/Ollama dependencies behind them)
stay local-only, since neither fits a genuinely free hosting tier.

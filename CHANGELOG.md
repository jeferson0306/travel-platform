# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses milestone-based versioning as defined in
[ROADMAP.md](ROADMAP.md).

## [Unreleased]

### Added

- Repository scaffolding: branching model, contribution guidelines, security
  policy, ADR process, base local infrastructure (MongoDB, Redis, Kafka,
  LocalStack).
- Automatic formatting (Prettier, Spotless/google-java-format) enforced at
  commit time and re-checked in CI.
- `identity-service`: user registration and login, hexagonal package
  structure, JWT issuance, unit/integration/ArchUnit tests (M4).
- Cross-service error-handling, audit-logging, RBAC-role, rate-limiting and
  webhook-idempotency pattern docs; `identity-service` adopts the canonical
  error shape and RBAC roles.
- `identity-service`: structured JSON request logs (correlationId,
  requestId, userId, method, uri, status, durationMs), health/readiness/
  liveness endpoints, Prometheus metrics (M6).
- CI pipeline (`ci.yml`): test suite, secret scan (gitleaks), dependency
  scan (Trivy), Dockerfile lint (hadolint), Docker image build and scan,
  fail-fast ordered (M7). `identity-service`'s Dockerfile.jvm base image
  fixed to a Java 25 runtime (the generated default targeted Java 21).
- `booking-service`: create/cancel a booking, transactional outbox pattern
  (MongoDB multi-document transaction + scheduled relay), `booking-created`/
  `booking-cancelled` published to Kafka (M8, ADR 0007). Local MongoDB is
  now a single-node replica set (required for transactions); Kafka topics
  are created by `infrastructure/kafka/create-topics.sh`.
- `flight-service` and `hotel-service`: inventory create/search, first
  services to actually verify JWTs issued by `identity-service` (M9). Token
  signing moved from an HS256 shared secret to RS256 with identity-service
  holding the only private key (ADR 0006 addendum) - the shared-secret
  approach's verification path was found unreliable in the installed
  SmallRye JWT version. `ci.yml`'s per-service matrix now covers all four
  services.
- `booking-service`'s `BookingReference` is now structured (`itemType`,
  `itemId`, `quantity`) instead of a freeform string, now that
  flight-service/hotel-service exist to point at. `flight-service` and
  `hotel-service` each consume `booking-created` (own consumer group) and
  atomically decrement inventory - idempotent (`processed_bookings`), with
  a Mongo-backed retry queue and exponential backoff, moving to a
  dead-letter Kafka topic per consumer group after too many attempts (M10,
  ADR 0004 addendum).
- AWS resources are now provisioned with Terraform against LocalStack
  (`infrastructure/terraform`), starting with a `booking-receipts` S3
  bucket. `booking-service` writes a JSON receipt to it (best-effort, not
  transactional with the booking write) on booking creation (ADR 0008,
  ADR 0009).
- `payment-service`: choreography saga with `booking-service` (M11, ADR
  0010). `booking-created` (now carrying `amount`) triggers a simulated
  payment authorization; the outcome (`payment-authorized`/`payment-failed`)
  confirms or compensates (cancels) the booking - the cancel path reuses
  `CancelBookingUseCase` directly. `booking-cancelled` triggers a refund.
  Idempotent throughout: payment-service claims bookingIds like the M10
  inventory consumers, while booking-service relies on
  `Booking.confirm()`/`cancel()` already rejecting a repeat call. Same
  Mongo-backed retry/DLQ pattern as M10. `ci.yml`'s matrix now covers all
  five services.
- Test pyramid close-out (M5), applied across all five services: filled
  gaps found by an audit of every domain exception, REST error status and
  consumer failure path (inactive-user login, malformed/poison Kafka
  payloads, cross-item-type events ignored, refunding an already-refunded
  payment, `ConfirmBookingService` had no test at all). New `RetryRelay`
  tests per service drive the scheduler directly (package-visible `relay()`
  called synchronously) to deterministically cover backoff math and the
  exhausted-retries dead-letter transition - the least-covered path before
  this. Fixed a real (if rare) concurrency bug this surfaced: the
  background `@Scheduled` trigger and a direct `relay()` call could race
  on the same overdue task and crash on a duplicate dead-letter insert;
  fixed by making that write an upsert. Every test now carries a JUnit 5
  `@DisplayName`; `mvn test` streams readable pass/fail output straight to
  the console (`reportFormat=plain`, `useFile=false`). JaCoCo coverage gate
  (0.45 line-coverage floor, measured not arbitrary) added to the parent
  POM, bound to `mvn test` itself. Mutation testing (PIT) is configured but
  currently blocked by an upstream Java 25 bytecode incompatibility - see
  docs/development/testing.md.
- `notification-service`: sixth microservice, the saga's terminal step
  (M12, ADR 0011). `booking-service`'s `Booking.confirm()` now raises
  `BookingConfirmed` (carrying the traveler's email, added as trusted
  client input on `CreateBookingRequest` like `amount` before it),
  published to Kafka for free via the existing generic outbox relay.
  `notification-service` consumes `booking-confirmed` (own consumer group
  `notification-processor`), sends a booking confirmation email (simulated
  gateway) and stores a `Notification` record, exposed read-only via
  `GET /api/v1/notifications/{bookingId}` (support/admin roles). Unlike
  every other aggregate in this platform, `Notification` raises no domain
  events and has no transactional outbox - nothing downstream reacts to "a
  notification was sent." Same Mongo-backed idempotency/retry/DLQ shape as
  M10/M11. `ci.yml`'s matrix now covers all six services.
- `search-service`: seventh microservice, and the first with no MongoDB at
  all (M13, ADR 0012). `flight-service` and `hotel-service` each gain their
  first domain event (`FlightCreated`/`HotelCreated`, raised by their
  existing `create()` factory, published for free via the same generic
  outbox relay every other service already uses). `search-service` consumes
  both and indexes into OpenSearch - its only datastore, since every
  document it holds is a rebuildable projection of flight-service's/
  hotel-service's own data, not something worth a second Mongo instance for.
  Exposes public `GET /api/v1/search/flights` (route or `q=` prefix
  autocomplete) and `GET /api/v1/search/hotels` (city or `q=` prefix
  autocomplete) - no authentication anywhere, the platform's first fully
  public service. Idempotency is free here: indexing by id is an upsert, so
  duplicate Kafka deliveries need no claim collection, unlike every other
  consumer in this platform. Retry/DLQ bookkeeping (`retry_tasks`/
  `dead_letters`) lives in OpenSearch indices instead of MongoDB
  collections, same shape otherwise. Tests run against a real OpenSearch via
  Testcontainers (`opensearch-testcontainers`), since this Quarkus version
  has no Dev Services support for it. `docker-compose.yml` gains an
  `opensearch` service; `ci.yml`'s matrix now covers all seven services.
- `gateway`: eighth microservice, closing Phase 3 (M14, ADR 0013). Single
  entry point fronting every backend service: a generic reverse-proxy route
  (`quarkus-reactive-routes` + Vert.x `WebClient`) forwards
  method/headers/query/body verbatim to whichever service owns a path's
  first segment after `/api/v1/`, no path rewriting. Redis-backed
  fixed-window rate limiting (429 once exceeded) and a JWT fast-fail check
  (401 for a present-but-invalid token; a missing token passes through
  untouched) run before every proxy - both cheap protections against wasted
  upstream work, not a duplicate authorization layer: every backend still
  independently enforces its own `@RolesAllowed` rules exactly as before.
  CORS is native Quarkus config, no custom code. No MongoDB, no Kafka - the
  gateway is stateless except for Redis rate-limit counters. Unlike every
  other service, has no hexagonal domain/application/infrastructure split
  (nothing here is a business domain to protect from framework leakage).
  Tests stand in for real backends with a stub HTTP server, exercising
  forwarding correctness, status passthrough, rate limiting and JWT
  rejection over real HTTP calls into a running instance. `ci.yml`'s matrix
  now covers all eight services.
- Fault tolerance (M15, ADR 0014), scoped to the two places a genuine
  synchronous external dependency actually exists - an audit found no
  synchronous inter-service REST calls anywhere else in the platform, since
  it ended up fully event-driven (ADR 0004). `gateway`'s
  `UpstreamProxyClient` now builds one SmallRye Fault Tolerance `Guard`
  (the programmatic API, not annotations - a single annotated method would
  share one circuit breaker across all seven backends) per backend segment,
  giving each an independent timeout/circuit-breaker/bulkhead; only the
  idempotent (GET/HEAD) path retries, since retrying a POST/PUT/PATCH/DELETE
  could duplicate a side effect on the backend. `ProxyRoutes` maps
  `CircuitBreakerOpenException`/`TimeoutException` to 503/504 instead of a
  blanket 502. `search-service`'s three OpenSearch read methods gained
  `@Timeout`/`@Retry` - not `index()`, which already has its own durable
  retry/DLQ mechanism (ADR 0012) that stacking SmallRye retry on top of
  would be redundant with. New `StubUpstreamResource` failure-injection
  (configurable delay, to trigger a real timeout) and a dedicated
  closed-port test resource (to trip the circuit breaker deterministically)
  cover the new gateway behavior end to end.
- Load & chaos testing (M16, ADR 0015): docker-compose gains an `apps`
  profile wiring all 8 backend services together on one network with the
  container names `gateway`'s upstream config already expects, and a
  `chaos` profile adding Toxiproxy - both layered on the existing infra
  services without touching the everyday `quarkus:dev` workflow. New
  `Makefile` targets (`apps-build`/`apps-up`/`apps-down`/`apps-logs`/
  `apps-ps`). k6 load scripts (`testing/load/k6/`) run against the real
  stack via the official Docker image, no local install: `search-load.js`
  (public read-only search, 20 VUs) and `booking-saga-load.js` (the full
  register → login → create-booking journey, driving the whole
  choreography saga end to end). Toxiproxy chaos experiment
  (`testing/chaos/flight-service-latency.sh`) injects latency in front of
  `flight-service` and confirms, against the real running gateway, that
  M15's per-backend circuit breaker opens, fails fast, stays fully
  isolated to `flight-service` (hotels/auth unaffected throughout), and
  recovers automatically once the fault is removed. All results are
  measured, not estimated, and written up in
  `docs/runbooks/load-and-chaos-results.md`, including a real (not
  hypothetical) finding about the gateway's per-IP rate limiter penalizing
  many real clients that share one source IP.
- Kubernetes manifests (M17, ADR 0016), closing Phase 4:
  `infrastructure/kubernetes/` with a Kustomize base (Deployment + Service
  - HPA per backend service, single-replica Mongo/Kafka/Redis/OpenSearch/
    LocalStack with PVCs, one-shot init Jobs replacing docker-compose's
    mongodb-init/kafka-init containers, reusing the same
    `create-topics.sh` topic list) and a `local` overlay (gateway as
    NodePort 30080 for kind's port mapping, machine-specific resource
    trims). Readiness/liveness probes split onto `/health/ready` /
    `/health/live` (built in M6 for exactly this). Deployed for real to a
    local kind cluster and verified end-to-end through the gateway: full
    register → login → create-flight → book flow, saga completion confirmed
    in Mongo (CONFIRMED/AUTHORIZED/SENT), flight indexed into search via
    in-cluster Kafka, and HPA observed organically scaling booking-service
    and payment-service 1→2 replicas under real CPU load. Three genuine
    Kubernetes-specific bugs were found only by running it and are fixed in
    the manifests: auto-injected `<SERVICE>_PORT` env vars crashing
    search-service (`enableServiceLinks: false` everywhere), the single-node
    KRaft broker unable to reach its own controller through a ClusterIP
    Service (headless + `publishNotReadyAddresses`), and docker-compose's
    JVM-spawning Kafka healthcheck being too expensive as a liveness probe
    (plain tcpSocket instead). Verification log:
    `docs/runbooks/kubernetes-verification.md`. No production Java code
    changed.
- Repository AI-context layer (M18, ADR 0017), opening Phase 5:
  `docs/context/` populated with four curated fact files
  (`platform-overview.md`, `service-catalog.md`, `event-catalog.md`,
  `conventions.md`) covering all 8 services, the booking saga, the full
  API surface, every Kafka topic and its publishers/consumers, and the
  rules any change must follow - hand-written and cited back to their
  authoritative source in the code, not generated dumps or embeddings.
  `docs/prompts/` gained five fill-in task templates (milestone workflow,
  new microservice, new endpoint, new Kafka consumer, new ADR), each
  encoding this platform's mandatory shape for that task and naming a
  reference implementation to copy from. New root `AGENTS.md` entry point
  routing to both, front-loading the golden rules (Git Flow with
  confirmation-gated merges, the `NNNN - Sentence.` commit format,
  events-only integration). No production code changed.

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

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

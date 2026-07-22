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

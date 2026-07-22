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

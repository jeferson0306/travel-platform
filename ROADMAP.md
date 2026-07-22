# Roadmap

This roadmap is public and updated as work progresses. Each milestone is
tracked as a GitHub Milestone with its own issues. Status: `Planned` →
`In Progress` → `Done`.

## Phase 0 — Foundation

- [x] **M1 — Repository scaffolding**: branching model, contribution/security
      policy, issue & PR templates, CODEOWNERS, base ADRs.
- [ ] **M2 — Local infrastructure**: Docker Compose (MongoDB, Redis, Kafka,
      LocalStack) reproducible with a single command.
- [ ] **M3 — Architecture baseline**: C4 context & container diagrams,
      `ARCHITECTURE.md`, `ENGINEERING.md`, service boundaries defined.

## Phase 1 — Walking skeleton (`identity-service`)

- [x] **M4 — Identity domain core**: user registration, authentication, JWT
      issuance, hexagonal package structure.
- [x] **M5 — Test pyramid close-out, all five services**: gap-filled unit,
      integration and architecture tests (error paths, RBAC, retry/DLQ
      backoff and dead-lettering, idempotency), JUnit 5 `@DisplayName`
      throughout, JaCoCo coverage gate in the parent POM. Scoped to all
      five services, not just identity-service, once there were five to
      cover consistently. Mutation testing (PIT) is configured but
      currently blocked by an upstream Java 25 bytecode incompatibility -
      see docs/development/testing.md.
- [x] **M6 — Observability baseline**: structured JSON logs with
      trace/span/correlation IDs, health checks, metrics exposed.
- [x] **M7 — CI pipeline v1**: lint, test, security scan, Docker build,
      running end-to-end on every PR. Coverage gate deliberately deferred
      to the M5 close-out (see M5) so it isn't re-tuned on every change
      while the test suite is still actively growing.

## Phase 2 — Core domains

- [x] **M8 — booking-service**: reservation lifecycle, outbox pattern, first
      Kafka events (`booking-created`, `booking-cancelled`).
- [x] **M9 — flight-service & hotel-service**: inventory/search domain
      models, consuming shared identity/auth (JWT issued by
      `identity-service`, RS256-verified - role-protected inventory
      creation, public search).
- [x] **M10 — Event-driven integration**: `flight-service` and
      `hotel-service` consume `booking-created` (own consumer groups) and
      decrement inventory - idempotent, with a Mongo-backed retry queue and
      a dead-letter Kafka topic per consumer group once retries are
      exhausted (ADR 0004 addendum).

## Phase 3 — Cross-cutting services

- [x] **M11 — payment-service**: choreography saga with booking-service -
      booking-created triggers payment authorization (simulated gateway),
      whose outcome confirms or compensates (cancels) the booking;
      booking-cancelled triggers a refund. Idempotent throughout (ADR 0010).
- [x] **M12 — notification-service**: consumes `booking-confirmed` (own
      consumer group), sends a booking confirmation email (simulated
      gateway, drop-in seam for SES/LocalStack later) and stores a
      `Notification` record. Terminal consumer - no domain events/outbox of
      its own. Idempotent, with the same Mongo-backed retry queue/DLQ shape
      as M10/M11 (ADR 0011).
- [x] **M13 — search-service**: `flight-service`/`hotel-service` publish
      their first domain events (`flight-created`/`hotel-created`, via the
      existing generic outbox relay); `search-service` consumes both and
      indexes into OpenSearch - its only datastore, no MongoDB (ADR 0012).
      Public route/city search plus prefix autocomplete. True geo search
      descoped - `City` has no coordinates yet.
- [ ] **M14 — API Gateway**: routing, JWT validation, rate limiting, CORS.

## Phase 4 — Resilience & scale

- [ ] **M15 — Fault tolerance**: circuit breakers, retries, timeouts,
      bulkheads across all services.
- [ ] **M16 — Load & chaos testing**: k6/Gatling load profiles, Toxiproxy
      fault injection, documented failure playbooks.
- [ ] **M17 — Kubernetes manifests**: deployments, HPA, probes, resource
      requests/limits (documented as portable target, run locally via kind/
      minikube).

## Phase 5 — AI engineering layer

- [ ] **M18 — Repository AI-context layer**: structured `docs/context` and
      `docs/prompts` so the codebase is consumable by AI agents.
- [ ] **M19 — Engineering assistants**: docs assistant, code assistant,
      architecture assistant, deployed as their own service(s).

## Phase 6 — Production polish

- [ ] **M20 — Public-facing polish**: README diagrams/screenshots, deployed
      demo (frontend on Vercel, backend on a free-tier host), final
      CHANGELOG pass.

Milestones are deliberately small — each should be shippable and reviewable
in a single pull request or a short stack of PRs.

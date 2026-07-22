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
- [ ] **M5 — Test pyramid on identity-service**: unit, integration
      (Testcontainers), architecture tests (ArchUnit) done as part of M4;
      still open: mutation testing (PIT) and a JaCoCo coverage gate in CI.
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
- [ ] **M10 — Event-driven integration**: consumer groups, retry topics,
      dead-letter queues across the services built so far.

## Phase 3 — Cross-cutting services

- [ ] **M11 — payment-service**: idempotency keys, saga coordination with
      booking-service.
- [ ] **M12 — notification-service**: email templates via SES (LocalStack),
      Kafka-driven delivery.
- [ ] **M13 — search-service**: OpenSearch-backed autocomplete and geo
      search.
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

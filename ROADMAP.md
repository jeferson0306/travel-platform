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
- [x] **M14 — API Gateway**: single entry point fronting all seven backend
      services, path-prefix routing (no rewriting), JWT fast-fail
      (signature/expiry only - authorization stays exclusively per-backend,
      ADR 0013), Redis-backed fixed-window rate limiting, native Quarkus
      CORS. Closes Phase 3.

## Phase 4 — Resilience & scale

- [x] **M15 — Fault tolerance**: applied only where a real synchronous
      dependency exists - `gateway`→backend (per-backend circuit
      breaker/timeout/bulkhead via the programmatic SmallRye `Guard` API,
      retry only for idempotent GET/HEAD) and `search-service`→OpenSearch
      read queries. Every other service already had its fault-tolerance
      story since M10 (Mongo-backed retry/DLQ on Kafka consumers) and
      needed nothing new (ADR 0014).
- [x] **M16 — Load & chaos testing**: full 8-service stack wired via a
      docker-compose `apps` profile, k6 load profiles (public search,
      full booking saga) run against it, Toxiproxy fault injection
      validating M15's per-backend circuit breaker (isolated failure,
      automatic recovery), all documented from real measured results,
      not estimates (ADR 0015).
- [x] **M17 — Kubernetes manifests**: Kustomize base (Deployments,
      Services, HPAs, readiness/liveness probes on `/health/ready` and
      `/health/live`, measured resource requests/limits) + a local overlay,
      actually deployed and verified end-to-end on a real `kind` cluster -
      full smoke test through the gateway including a complete booking
      saga, and HPA observed genuinely scaling under real CPU load. Three
      real Kubernetes-specific bugs found and fixed by running it
      (service-link env-var injection, KRaft self-hairpin, probe cost) -
      ADR 0016. Closes Phase 4.

## Phase 5 — AI engineering layer

- [x] **M18 — Repository AI-context layer**: `docs/context` populated with
      four curated fact files (platform overview, service catalog, event
      catalog, conventions), `docs/prompts` with five fill-in task
      templates encoding the platform's mandatory shapes and reference
      implementations, and a root `AGENTS.md` entry point - curated
      Markdown over generated dumps, same accuracy contract as the ADRs
      (ADR 0017).
- [x] **M19 — Engineering assistant**: `assistant-service` (ninth backend
      service, port 8088), hexagonal like every other service, answering
      questions about this platform grounded in the whole docs/context
      corpus ("stuff everything" RAG - no vector DB needed at this size).
      Backed by a local Ollama runtime (free, no paid API - matches this
      platform's local-first posture), gated behind the gateway like
      every other segment. Real end-to-end test against the actual model
      found and fixed a genuine hallucination bug (Ollama's default
      context window silently truncated the corpus) before the answers
      were verified correct and cited (ADR 0018). Scoped from three
      assistants (docs/code/architecture) down to one, and Kubernetes
      wiring is manifests-only (not live-deployed, unlike M17) - both
      explicit trade-offs, not omissions.

## Phase 6 — Production polish

- [ ] **M20 — Public-facing polish**: README rewritten with real screenshots
      (register, search, booking confirmation) from a locally seeded run,
      minimal companion frontend (register/login/search/book) built and
      verified against the real stack, ADR 0019 scoping the public backend
      deployment to the essential booking flow (Kafka/saga included,
      OpenSearch/Ollama excluded - neither fits a free tier), plus a public
      `/status` dashboard polling every service's real health endpoint
      live from the browser. **Frontend deployed** (Vercel). **Backend
      deployment in progress**: Railway's trial expired before go-live, so
      hosting is moving to Render + MongoDB Atlas - see ADR 0019's
      2026-08-05 addendum and [docs/deploy/render.md](docs/deploy/render.md)
      for the reopened Kafka-hosting trade-off and the exact steps, not
      yet executed.

Milestones are deliberately small — each should be shippable and reviewable
in a single pull request or a short stack of PRs.

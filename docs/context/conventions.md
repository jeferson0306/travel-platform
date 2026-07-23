# Conventions (AI-context layer, ROADMAP M18)

The rules every change to this repo must follow. An agent producing code
or docs that violates any of these is producing a wrong answer, however
plausible it looks.

## Architecture

- Hexagonal per service: `api / application / domain / infrastructure`,
  enforced by ArchUnit tests. Domain has zero framework imports.
  (`gateway` is the sole exception - no domain, ADR 0013.)
- Cross-service communication is Kafka events only - never add a
  synchronous REST call between services (ADR 0004; audited in ADR 0014).
- New aggregates publish via the transactional outbox (ADR 0007); new
  consumers follow the idempotency + retry-queue + DLQ shape
  ([event-catalog.md](event-catalog.md)).
- Every design decision of consequence gets an ADR in `docs/adr/`
  (sequentially numbered, Context/Decision/Consequences), grounded in
  what was actually built/measured - never speculative benchmarks.

## API

- Paths: `/api/v1/<resource>` - the first segment after `/api/v1/` must
  match the owning service's gateway routing segment.
- Canonical error shape everywhere:
  `{"error": "<CODE>", "message": "...", "details": [...] | null}`
  (see docs/architecture/error-handling.md). Never leak exception
  class names/messages to clients.
- AuthZ lives in each backend (`@RolesAllowed`), never only in the
  gateway (its JWT check is fast-fail, not authorization). Roles:
  USER, MANAGER, ADMIN, SUPER_ADMIN, SUPPORT (ADR 0006).
- Health (`/health/ready`, `/health/live`) and Prometheus metrics are
  mandatory on every service (M6) - Kubernetes probes depend on them.

## Observability

Every request produces one structured JSON log line: `timestamp`,
`traceId`, `spanId`, `correlationId`, `requestId`, `userId` (when
authenticated), `method`, `uri`, `status`, `durationMs` - via the shared
logging adapter, not per-service ad-hoc logging.

## Code & language

- All code (names, comments, endpoints) in English. UI text via i18n
  (PT/EN/ES), never hard-coded.
- Formatting is automated and non-negotiable: Spotless/google-java-format
  (Java) and Prettier (YAML/MD/JS), applied by the pre-commit hook and
  re-checked in CI. Never hand-format against them.
- Tests: JUnit 5 with `@DisplayName` on every test; integration tests
  use Testcontainers/Dev Services; JaCoCo line-coverage floor 0.45 is
  bound to `mvn test`. New consumer logic must cover the retry/DLQ path
  (drive `RetryRelay.relay()` directly - see M5's pattern).

## Git workflow

- Git Flow: `feature/JLJS-XXXX` → PR → `develop`; periodic release PR
  `develop` → `main`. Never commit to `develop`/`main` directly.
- Commit messages: `NNNN - Text starting with a capital, ending with
punctuation.` where NNNN is the JLJS task number from the branch (e.g.
  `0017 - Add Kubernetes manifests.`). No Co-Authored-By lines.
  (Format adopted at M17; older commits predate it.)
- Every milestone PR updates ROADMAP.md (checkbox + summary), CHANGELOG.md
  (one detailed bullet), and ARCHITECTURE.md when the architecture story
  changed; new services join `ci.yml`'s matrix, docker-compose's `apps`
  profile, and `infrastructure/kubernetes/`.
- Merges to `develop`/`main` require explicit user confirmation - agents
  open PRs and stop.

## Environment quirks (this machine)

Host-side port remaps in docker-compose only (containers unaffected):
Redis on 6380, Mongo on 27018, flight-service on 18083 - collisions with
unrelated local projects (ADR 0015). Terraform is not installed here; the
S3 bucket is provisioned with `aws --endpoint-url=http://localhost:4566`
against LocalStack instead.

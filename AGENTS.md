# AGENTS.md

Entry point for AI agents working on this repository (ROADMAP M18, ADR
0017). Human contributors: start at [CONTRIBUTING.md](CONTRIBUTING.md)
instead - the rules are the same, this file just front-loads them for
agents.

## Read first

1. [docs/context/platform-overview.md](docs/context/platform-overview.md)
   - what this platform is, all 8 services, the saga, the API surface.
2. [docs/context/conventions.md](docs/context/conventions.md) - the rules
   every change must follow. Non-negotiable.
3. For the specific task: [docs/context/service-catalog.md](docs/context/service-catalog.md),
   [docs/context/event-catalog.md](docs/context/event-catalog.md), and the
   matching template in [docs/prompts](docs/prompts).

## Golden rules (details in conventions.md)

- **Git Flow**: work on `feature/JLJS-XXXX`, PR into `develop`. Never
  commit to `develop`/`main` directly. Never merge without explicit user
  confirmation.
- **Commits**: `NNNN - Capitalized sentence.` (NNNN = the branch's JLJS
  number). No Co-Authored-By lines.
- **No synchronous service-to-service REST** - Kafka events only (ADR
  0004). Publish via the outbox; consume idempotently with retry/DLQ.
- **Honest docs**: ADRs and runbooks state only what was actually built,
  run, and measured. If you didn't run it, don't claim it.
- **Everything in English**; formatting is automated (Spotless/Prettier
  via pre-commit hook) - don't fight it.
- **Verify before claiming done**: `mvn -f backend/pom.xml test` when Java
  changed; the docs/context files are part of the change when any fact
  they state changed.

## Layout

- `backend/` - 8 Quarkus services (hexagonal, ArchUnit-enforced) + parent
  POM.
- `infrastructure/` - docker-compose (+`apps`/`chaos` profiles), Kafka
  topic script, Kubernetes (Kustomize base + local overlay), Terraform.
- `testing/` - k6 load scripts, Toxiproxy chaos script.
- `docs/` - ADRs (0001+), event payloads, runbooks (real results only),
  context + prompts (this layer).
- `ai/` - engineering assistants (Phase 5 M19, not yet built).

# 0005 — Monorepo for backend, frontend, and infrastructure

- Status: Accepted
- Date: 2026-07-22

## Context

The project consists of multiple backend services, a frontend application,
infrastructure-as-code, and documentation. These could live in separate
repositories per service (polyrepo) or together in one repository
(monorepo).

This is currently a single-maintainer project where cross-cutting changes
(an event schema change touching a producer and two consumers, an ADR that
affects three services, a documentation pass) are common, and where
reviewers (recruiters, tech leads) benefit from seeing the whole system's
history and structure in one place rather than piecing it together across
repositories.

## Decision

Use a **single monorepo** (`travel-platform`) containing `backend/`
(one directory per service, each an independent Maven module),
`frontend/`, `infrastructure/`, `docs/`, and `ai/`.

Each backend service still builds, tests, and ships as an independent
artifact/container — the monorepo is an organizational choice for source
control and review, not a shared-runtime coupling. CI is structured so a
change to one service's directory only triggers that service's pipeline
where practical (documented in [docs/development](../development) once the
CI is built).

Rejected alternative:

- **Polyrepo (one repository per service)** — mirrors how larger
  organizations with dedicated per-team ownership often operate, and is
  documented here as the natural next step if this project ever needed
  independent per-service access control or release cadences. For a
  single-maintainer showcase project, it would fragment history and make the
  system harder to evaluate as a whole, which works against this project's
  goal.

## Consequences

- Simpler local development: one `git clone`, one `make up`.
- Cross-service refactors (e.g. an event contract change) can be reviewed as
  a single, coherent PR instead of coordinated across repositories.
- Requires discipline to keep services genuinely decoupled at runtime (no
  shared database, no in-process calls between services) even though they
  share a repository — enforced by the rules in
  [ARCHITECTURE.md](../../ARCHITECTURE.md), not by repository boundaries.
- If this project ever needed different access control per service, this
  decision would need to be revisited (noted here rather than assumed away).

# 0017 — Repository AI-context layer: curated facts + task templates, not generated dumps

- Status: Accepted
- Date: 2026-07-23

## Context

ROADMAP's M18 (opening Phase 5) calls for "structured `docs/context` and
`docs/prompts` so the codebase is consumable by AI agents." Both
directories have existed as placeholder READMEs since Phase 1,
deliberately left empty until all 8 services existed to describe - writing
an AI-context layer for a platform that didn't exist yet would have
violated this repo's own honesty rule.

By M18 the platform is complete through Phase 4 (16 prior ADRs, 8
services, full-stack compose, load/chaos results, Kubernetes manifests) -
there is now a large, real body of fact to make consumable.

## Decision

### Curated, human-readable Markdown - not generated indexes or embeddings

The context layer is four hand-written files
(`platform-overview.md`, `service-catalog.md`, `event-catalog.md`,
`conventions.md`) with a fixed structure per concern, rather than
generated API dumps, an embedding store, or per-file summaries. Reasons:

- What an agent actually gets wrong on this repo isn't "where is class X"
  (search tools already solve that) - it's platform-level facts and rules
  no single file states: who consumes which topic, why consumers must not
  use `@Retry`, that the gateway does not authorize, that merges need
  explicit confirmation. Those are exactly what a curated layer captures
  and generated artifacts don't.
- Generated content rots silently and voluminously; four short curated
  files are cheap to keep honest, and the same accuracy contract that
  governs ADRs applies: describing something the code doesn't do is a
  bug, and updating these files is part of any milestone that changes a
  fact they state (stated in `docs/context/README.md`).
- The same files serve new human engineers unchanged - no separate
  "AI docs" fork to maintain.

Each file cites the code location that is authoritative for its facts
(e.g. `create-topics.sh` for topics), so an agent can verify rather than
trust.

### Prompts as fill-in task templates encoding conventions + reference implementations

`docs/prompts/` holds five templates for this repo's recurring tasks
(milestone workflow, new microservice, new endpoint, new Kafka consumer,
new ADR). Each encodes two things a generic agent lacks: the platform's
mandatory shape for that task (idempotency + retry/DLQ for consumers,
hexagonal placement for endpoints, the full wiring checklist for a new
service - CI matrix, compose profile, Kubernetes manifests, gateway
upstream, topic script and its k8s ConfigMap copy), and _which existing
service to copy the pattern from_ - reference implementations beat
abstract instructions for keeping output consistent with the codebase.

### `AGENTS.md` at the repo root as the entry point

The emerging cross-tool convention for agent instructions (analogous to a
CLAUDE.md, but tool-neutral). It front-loads the golden rules (Git Flow
with confirmation-gated merges, `NNNN - Sentence.` commit format,
events-only integration, honest-docs rule) and routes to `docs/context` /
`docs/prompts` in reading order. `CONTRIBUTING.md` remains the human entry
point; the rules are the same.

### What was deliberately not built here

- No assistant services - that's M19's scope (`ai/` stays a placeholder).
- No mechanical enforcement that context files stay current (e.g. a CI
  check diffing them against code) - the accuracy contract is procedural
  for now; automation can come later if drift actually happens.

## Consequences

- Any agent (this repo's future M19 assistants, or a general coding
  agent) starts from `AGENTS.md` and gets the platform's real rules and
  cross-service facts in ~4 short reads, instead of re-deriving them from
  16 ADRs and 8 codebases per session.
- Two placeholder READMEs (`docs/context`, `docs/prompts`) were replaced
  with real indexes; `docs/events`' stale "Planned: search-service"
  consumer note is superseded by `event-catalog.md`'s current table.
- M19 (engineering assistants) now has its grounding corpus: the
  assistants can be built to read exactly these files.

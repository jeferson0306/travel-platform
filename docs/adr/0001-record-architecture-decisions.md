# 0001 — Record architecture decisions

- Status: Accepted
- Date: 2026-07-22

## Context

Significant technical decisions (framework choices, service boundaries, data
storage, integration patterns) need a durable record of *why* they were made,
not just what was chosen. Without this, decisions get silently re-litigated
or reversed without anyone remembering the trade-off that motivated them —
especially in a project that spans many months and many independent working
sessions.

## Decision

We will use Architecture Decision Records (ADRs), one Markdown file per
decision, numbered sequentially in `docs/adr/`, following this template:

```
# NNNN — <short title>

- Status: Proposed | Accepted | Deprecated | Superseded by NNNN
- Date: YYYY-MM-DD

## Context
<the problem, forces, constraints>

## Decision
<what was decided>

## Consequences
<what becomes easier or harder as a result, including trade-offs accepted>
```

Any change that introduces a new dependency, a new service boundary, or
deviates from an established pattern requires an ADR before (or alongside)
the implementation PR, per [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Consequences

- Decisions are traceable and reviewable independent of the code that
  implements them.
- Reversing a decision later is cheap to justify — supersede the old ADR
  instead of silently diverging from it.
- Adds a small amount of process overhead per significant decision; this is
  accepted as worthwhile given the project's goal of demonstrating
  engineering judgment, not just output.

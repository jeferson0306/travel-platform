# Context (AI-context layer, ROADMAP M18)

Structured, factual reference material about this platform, written to be
consumable by AI agents and new engineers alike. Populated in M18 (ADR
0017), after all 8 services existed to describe - nothing here is
aspirational.

Read in this order:

1. [platform-overview.md](platform-overview.md) - what the platform is,
   the services table, the saga, the full API surface, how to run it.
2. [service-catalog.md](service-catalog.md) - per-service detail:
   ownership, endpoints, events published/consumed, quirks.
3. [event-catalog.md](event-catalog.md) - every RabbitMQ event type,
   publisher/consumer cross-reference, DLQ naming, publishing rules.
4. [conventions.md](conventions.md) - the rules any change must follow
   (architecture, API, git workflow, commit format, testing gates).

Reusable task templates that build on these files live in
[docs/prompts](../prompts); the agent entry point is the repo-root
[AGENTS.md](../../AGENTS.md).

Accuracy contract: these files describe the code as it exists. When a
milestone changes something they cover, updating them is part of that
milestone's documentation work - a stale statement here is a bug.

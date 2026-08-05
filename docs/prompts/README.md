# Prompts (AI-context layer, ROADMAP M18)

Reusable prompt templates for this repo's recurring engineering tasks.
Each one encodes the platform's conventions (docs/context/conventions.md)
and names the reference implementation to copy from, so any AI agent -
this repo's future assistants (Phase 5 M19, see [ai/](../../ai)) or a
general coding agent - produces platform-consistent output instead of
generic boilerplate.

Templates:

- [milestone-workflow.md](milestone-workflow.md) - run a ROADMAP milestone
  end to end (branch → build → verify → document → PR → stop).
- [new-microservice.md](new-microservice.md) - scaffold a ninth-plus
  service with all mandatory wiring (CI, compose, Kubernetes, gateway,
  topics, docs).
- [new-endpoint.md](new-endpoint.md) - add a REST endpoint (hexagonal
  placement, authz, error shape, tests).
- [new-rabbitmq-consumer.md](new-rabbitmq-consumer.md) - add an event consumer
  (idempotency, retry/DLQ shape, topic wiring).
- [new-adr.md](new-adr.md) - write an ADR in the house style.

Usage: fill the `[brackets]`, paste the whole template as the task prompt.
Keep templates updated when the convention they encode changes - same
accuracy contract as [docs/context](../context).

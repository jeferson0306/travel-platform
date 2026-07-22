# 0004 — Use Kafka for event-driven communication

- Status: Accepted
- Date: 2026-07-22

## Context

Services need to react to things that happen elsewhere in the system
(`booking-created` triggers a payment request and a notification;
`payment-approved` confirms a booking; `miles-earned` updates loyalty
balance) without every service calling every other service synchronously,
which would create a fragile web of direct dependencies and cascading
failure.

Candidates considered: direct synchronous REST calls for everything, a
lightweight broker (RabbitMQ, SQS via LocalStack), or Kafka.

## Decision

Use **Apache Kafka** as the event backbone for all asynchronous, fan-out, or
eventually-consistent communication between services. Synchronous REST is
still used where an immediate answer is required (e.g. "is this hotel room
available right now").

Patterns adopted:

- **Consumer groups** per service, so scaling a consumer horizontally is
  free.
- **Retry topics + dead-letter queue** per consumer, so a poison message
  cannot block a partition indefinitely, and failures are inspectable
  instead of silently dropped or infinitely retried in place.
- **Outbox pattern** for services that must atomically update their own
  MongoDB state and publish an event (e.g. `booking-service` writing a
  booking and emitting `booking-created` must not do one without the other).
- Topics and event schemas are documented in [docs/asyncapi](../asyncapi) as
  they are introduced, keyed to the service that owns/publishes them.

Rejected alternatives:

- **REST-only, no broker** — would work for a much smaller system, but
  couples services' availability to each other and offers no natural retry/
  DLQ/backpressure story; also forgoes demonstrating event-driven design,
  which is explicitly a goal of this project.
- **RabbitMQ** — simpler to operate for small volumes, but Kafka's log-based
  model, consumer groups, and replay capability better fit "notification
  service reprocesses last hour of events after an outage"-style scenarios
  this project wants to demonstrate handling.
- **SQS/SNS via LocalStack** — used, but for point-to-point AWS-style
  integration (see ADR for LocalStack usage), not as the primary
  cross-service event backbone.

## Consequences

- Adds real operational complexity (broker to run, topics/partitions to
  design, schema evolution to manage) — accepted because handling that
  complexity well is the point.
- Every event-driven flow needs an explicit failure story (what happens on
  poison message, what happens on consumer downtime) documented per topic in
  [docs/events](../events), not left implicit.
- Schema Registry / Avro is deferred until a concrete need for schema
  evolution guarantees appears; JSON Schema is the default until then, noted
  per topic in the AsyncAPI spec.

## Addendum (M10) — Mongo-backed retry, not a literal second Kafka topic

The first real consumers (`flight-service` and `hotel-service`, each in their
own consumer group, decrementing inventory on `booking-created`) surfaced a
gap in "retry topics + dead-letter queue per consumer" as originally
written: Kafka has no built-in delayed redelivery. Publishing a failed
message straight back onto a literal retry topic means a consumer either
busy-loops re-reading it immediately (no backoff) or must itself implement
delay logic - at which point the "retry topic" is not doing any of the
actual work.

Implemented instead: a failed message (transient Mongo error, or a business
failure such as insufficient inventory) is recorded in that service's own
`retry_tasks` MongoDB collection with a `nextAttemptAt`, and a `@Scheduled`
relay (mirroring `booking-service`'s `OutboxRelay` - see ADR 0007) retries
it with exponential backoff. The original Kafka message is acknowledged
immediately either way, so a poison message never blocks the partition -
the ADR's actual goal. Only once a task exhausts its retry budget does it
become externally visible on Kafka: it is moved to a `dead_letters`
collection and a summary is published to a **DLQ topic named after the
topic and consumer group** (`booking-created.<group>.dlq`, e.g.
`booking-created.flight-inventory.dlq`), so on-call tooling can alert on it
without querying MongoDB directly.

A malformed (unparseable) message is not retried at all - retrying can
never fix a parsing failure, so it is logged and dropped immediately rather
than occupying a retry slot.

This keeps the two behaviors ADR 0004 actually cares about (partition never
blocks; failures are inspectable, not silently dropped) while being honest
that the redelivery mechanism is Mongo-backed, not Kafka-native.

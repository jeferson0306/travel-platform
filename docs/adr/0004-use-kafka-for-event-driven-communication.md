# 0004 — Use Kafka for event-driven communication

- Status: Superseded by the 2026-08-05 addendum (RabbitMQ) - kept as the original record, not
  rewritten, per this project's own rule that every ADR reflects what was actually decided and
  measured at the time.
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

## Addendum (2026-08-05) — Migrated to RabbitMQ; the rejection above was wrong for this project

This ADR originally rejected RabbitMQ in favor of Kafka's "log-based model,
consumer groups, and replay capability" for scenarios like "notification
service reprocesses last hour of events after an outage." In the ~2 weeks
since, that capability was never used - no consumer has ever replayed
history, and the actual redelivery mechanism that shipped (M10's addendum,
above) is Mongo-backed retry with exponential backoff, not Kafka's log
replay at all. The one thing this project actually needed from a broker -
fan-out delivery to independent consumers, a queue per failure mode - is
exactly what RabbitMQ provides.

**What forced the reconsideration**: hosting, not architecture. Railway
(chosen in ADR 0019 specifically to keep running Kafka after Render was
found to have no free managed Kafka tier) was a paid plan the user decided
not to keep paying for. Rather than degrade the demo to a Kafka-less,
partially-working saga on a free host, the honest fix was to admit the
original broker choice - not just the original hosting choice - was
over-engineered for what this project actually does with it, and migrate
to a broker with a genuinely free, generally-available managed tier
(CloudAMQP) that fits the actual usage pattern.

**What changed:**

- **Topology**: each publishing service owns one RabbitMQ **topic
  exchange** named after its old default Kafka topic prefix (e.g.
  `booking-events`), and every event type it emits (`booking-created`,
  `booking-cancelled`, ...) is a **routing key** on that exchange, not a
  separate topic. Each consumer declares its own durable **queue** bound to
  the exchange with the routing key(s) it wants - the direct analog of "one
  consumer group per Kafka topic," expressed as exchange+routing-key+queue
  instead of topic+partition+consumer-group. Queue names keep the exact
  same `<eventType>.<old-consumer-group-name>` convention Kafka's
  consumer-group naming already used (e.g.
  `booking-created.flight-inventory`), so every existing doc, log line, and
  mental model referencing that name is still accurate.
- **DLQ topics → DLQ queues**: the M10 addendum's app-level "publish a
  give-up summary" DLQ mechanism needed zero behavior change - it was
  already Mongo-backed retry logic that only ever _published a plain
  message to a fixed destination_ on giving up, never anything Kafka-native
  (no consumer group, no partition, no replay). Each `<...>.dlq` Kafka
  topic became an equivalently-named RabbitMQ queue, pre-declared by
  `infrastructure/rabbitmq/declare-dlq-queues.sh` (mirrors the old
  `kafka-init`/`create-topics.sh` pattern) since nothing in this app
  consumes them - they exist purely for external visibility, same as before.
- **Code changes were minimal**: of the six services touched, only the
  four `OutboxRelay` classes (`booking-service`, `flight-service`,
  `hotel-service`, `payment-service`) needed a Java change at all - they
  used `OutgoingKafkaRecordMetadata.withTopic(eventType)` to route each
  outbox event dynamically; the RabbitMQ equivalent is
  `OutgoingRabbitMQMetadata.withRoutingKey(eventType)`, a one-line swap.
  Every consumer, every DLQ-publishing `RetryRelay`, and the entire test
  suite (`%test` already used `smallrye-in-memory` for every channel,
  never a real broker) needed **no code change** - only
  `application.yml`'s connector config changed, per the same channel names.
- **Local infra**: `infrastructure/docker/docker-compose.yml`'s `kafka`/
  `kafka-init` services became `rabbitmq`/`rabbitmq-init`
  (`rabbitmq:4-management-alpine`, management UI on :15672).

**Verified, not assumed**: after the migration, a real booking was created
against the full local stack (all 8 services + gateway, real RabbitMQ, no
mocks) - `booking-created` flowed through the `booking-events` exchange to
`flight-service`'s bound queue and decremented seat inventory by exactly
the booked quantity (127 → 125 for a quantity-2 booking), `payment-service`
authorized it, `booking-service` confirmed it (`status: CONFIRMED` in
Mongo), and `notification-service` sent the confirmation email - the whole
choreography saga, unmodified in behavior, running on RabbitMQ instead of
Kafka.

See [docs/deploy/render.md](../deploy/render.md) and this project's
[render.yaml](../../render.yaml) for how this unblocks the Render
deployment ADR 0019's addendum was waiting on - CloudAMQP's free tier
closes the gap that made Render's own free tier insufficient the first
time this was evaluated.

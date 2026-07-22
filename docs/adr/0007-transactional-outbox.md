# 0007 — Transactional outbox implementation

- Status: Accepted
- Date: 2026-07-22

## Context

`booking-service` must atomically (a) persist a booking's state change and
(b) guarantee a corresponding domain event (`booking-created`,
`booking-cancelled`) eventually reaches Kafka. Writing to MongoDB and then
publishing to Kafka as two separate steps has no atomicity guarantee: a
crash between the two leaves the booking persisted with no event ever
published, silently breaking every downstream consumer (payment,
notification, search).

MongoDB supports multi-document ACID transactions, but only when the
deployment is a replica set - not on a standalone `mongod`. This forces two
decisions: how the outbox is written atomically with the aggregate, and how
it gets from MongoDB to Kafka.

## Decision

**Local MongoDB runs as a single-node replica set** (`infrastructure/docker/docker-compose.yml`),
not standalone, specifically so transactions are available locally the same
way they are in a real MongoDB Atlas deployment (Atlas is always a replica
set or sharded cluster). Quarkus Dev Services already provisions its
ephemeral test/dev Mongo as a replica set automatically, so this only
affects the docker-compose-only workflow.

**Outbox write is embedded in the repository adapter, not a separate
port.** `MongoBookingRepository.save(Booking)` opens a MongoDB
`ClientSession`, and within one transaction: upserts the booking document
and inserts one outbox document per domain event pulled from the aggregate
(`booking.pullDomainEvents()`), then commits. The application layer
(`CreateBookingService`, `CancelBookingService`) only calls
`bookingRepository.save(booking)` - it does not know an outbox exists.

This was chosen over a separate `OutboxPort` interface: atomicity requires
both writes to share one transaction, and a port boundary between "save
aggregate" and "save outbox event" would either leak the transaction/session
across the boundary (defeating the point of the port) or lose atomicity
entirely. Embedding it in the one adapter that already owns the Mongo
session is the honest representation of that constraint, not a shortcut.

**Relay, not dual-write, moves events to Kafka.** A `@Scheduled` poller
(`OutboxRelay`) reads unpublished outbox documents, publishes each to its
Kafka topic, and marks it published (or deletes it) only after a successful
send. If the process crashes between commit and publish, the event is still
in the outbox and gets published on the next poll - at-least-once delivery,
which is why every consumer this platform builds must be idempotent (see
[docs/business-rules/webhooks-idempotency.md](../business-rules/webhooks-idempotency.md)
for the same principle applied to inbound webhooks).

## Consequences

- Every future service that needs to publish events reliably follows this
  same shape: replica-set MongoDB, outbox write inside the repository's
  transaction, a relay polling loop. Documented here once instead of
  re-derived per service.
- The relay introduces publish latency bounded by its poll interval, not
  the transaction itself - acceptable for this platform's use cases (no
  sub-second delivery requirement exists yet).
- Consumers must be idempotent (at-least-once delivery). This is called out
  explicitly rather than assumed, since it changes how every consumer is
  written.
- No Debezium/CDC-based outbox relay is used - a polling `@Scheduled` job is
  simpler to run locally and sufficient at this platform's scale. Revisit
  if publish latency or poll-induced load ever becomes a real constraint.

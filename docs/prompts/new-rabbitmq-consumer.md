# Prompt: add a RabbitMQ consumer to a service

Use this template when a service needs to react to an existing platform
event. Fill the brackets, paste the whole thing to the agent.

---

Add a RabbitMQ consumer to `[service]` for the `[routing-key]` event on
the `[exchange-name]` exchange, following this platform's mandatory
consumer shape (docs/context/event-catalog.md, "Publishing and consuming
rules"; reference implementations: flight-service's booking-created
consumer for the claim-collection style, search-service's indexer for the
natural-upsert style):

- Queue name: `[routing-key].[consumer-name]` (matches the old
  Kafka-consumer-group naming convention - one purpose-named queue per
  service, bound to `[exchange-name]` with `routing-keys: [routing-key]`).
- Idempotency: [claim collection `processed_[x]` | natural upsert -
  justify which].
- Failure handling: the existing Mongo-backed retry queue + exponential
  backoff pattern (`RetryRelay`), dead-lettering to
  `[routing-key].[consumer-name].dlq` after exhaustion. Do NOT add
  SmallRye `@Retry` to the consumer - the two retry mechanisms conflict
  (ADR 0014).
- Add `[routing-key].[consumer-name].dlq` to
  `infrastructure/rabbitmq/declare-dlq-queues.sh` AND to the ConfigMap
  copy in `infrastructure/kubernetes/base/infra/rabbitmq.yaml`.
- Update `docs/events/[topic-family]-events.md`'s consumer list and
  `docs/context/event-catalog.md`'s table.
- Tests: happy path, duplicate delivery (idempotency), malformed payload,
  and the retry→DLQ transition driven via a synchronous `relay()` call
  (M5's `RetryRelay` test pattern), all with `@DisplayName`.
- Business logic goes in `application`/`domain`; the consumer in
  `infrastructure/messaging` only deserializes, claims, and delegates.

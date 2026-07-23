# Prompt: add a Kafka consumer to a service

Use this template when a service needs to react to an existing platform
event. Fill the brackets, paste the whole thing to the agent.

---

Add a Kafka consumer to `[service]` for the `[topic]` event, following
this platform's mandatory consumer shape (docs/context/event-catalog.md,
"Publishing and consuming rules"; reference implementations:
flight-service's booking-created consumer for the claim-collection style,
search-service's indexer for the natural-upsert style):

- Consumer group: `[group-name]` (one purpose-named group per service).
- Idempotency: [claim collection `processed_[x]` | natural upsert -
  justify which].
- Failure handling: the existing Mongo-backed retry queue + exponential
  backoff pattern (`RetryRelay`), dead-lettering to
  `[topic].[group-name].dlq` after exhaustion. Do NOT add SmallRye
  `@Retry` to the consumer - the two retry mechanisms conflict (ADR 0014).
- Add `[topic].[group-name].dlq` to `infrastructure/kafka/create-topics.sh`
  AND to the ConfigMap copy in
  `infrastructure/kubernetes/base/infra/kafka.yaml`.
- Update `docs/events/[topic-family]-events.md`'s consumer list and
  `docs/context/event-catalog.md`'s table.
- Tests: happy path, duplicate delivery (idempotency), malformed payload,
  and the retry→DLQ transition driven via a synchronous `relay()` call
  (M5's `RetryRelay` test pattern), all with `@DisplayName`.
- Business logic goes in `application`/`domain`; the consumer in
  `infrastructure/messaging` only deserializes, claims, and delegates.

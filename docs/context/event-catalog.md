# Event catalog (AI-context layer, ROADMAP M18)

Every Kafka topic in the platform. The authoritative list is
[infrastructure/kafka/create-topics.sh](../../infrastructure/kafka/create-topics.sh)
(auto-topic-creation is disabled on the broker, so a topic exists only if
that script creates it); payload field tables live in
[docs/events](../events). This file is the cross-reference an agent needs
to answer "who publishes/consumes what."

All business topics: 3 partitions, replication factor 1 (single local
broker). DLQ naming convention: `<topic>.<consumer-group>.dlq` - one DLQ
per consumer group, created only once a consumer for the topic exists
(ADR 0004 + M10 addendum).

| Topic                | Published by    | Consumed by (group)                                                                                           |
| -------------------- | --------------- | ------------------------------------------------------------------------------------------------------------- |
| `booking-created`    | booking-service | flight-service (`flight-inventory`), hotel-service (`hotel-inventory`), payment-service (`payment-processor`) |
| `booking-cancelled`  | booking-service | payment-service (`payment-processor`) - refund path                                                           |
| `booking-confirmed`  | booking-service | notification-service (`notification-processor`)                                                               |
| `payment-authorized` | payment-service | booking-service (`booking-payment-outcome`) - confirm                                                         |
| `payment-failed`     | payment-service | booking-service (`booking-payment-outcome`) - compensate/cancel                                               |
| `payment-refunded`   | payment-service | nobody yet (published for completeness, ADR 0010)                                                             |
| `flight-created`     | flight-service  | search-service (`search-indexer`)                                                                             |
| `hotel-created`      | hotel-service   | search-service (`search-indexer`)                                                                             |

DLQ topics (one per consumer-group above):
`booking-created.flight-inventory.dlq`,
`booking-created.hotel-inventory.dlq`,
`booking-created.payment-processor.dlq`,
`booking-cancelled.payment-processor.dlq`,
`payment-authorized.booking-payment-outcome.dlq`,
`payment-failed.booking-payment-outcome.dlq`,
`booking-confirmed.notification-processor.dlq`,
`flight-created.search-indexer.dlq`,
`hotel-created.search-indexer.dlq`.

## Publishing and consuming rules

- Publishing is always via the generic transactional-outbox relay (ADR 0007) - domain aggregates raise events, a scheduled relay publishes
  them; never publish directly from a request handler.
  (Exception: notification-service publishes nothing at all - ADR 0011.)
- Consuming is always idempotent: a claim collection
  (`processed_bookings`-style) for consumers with side effects, or a
  natural upsert (search-service). Failures go to a Mongo-backed retry
  queue with exponential backoff, then the consumer group's DLQ topic
  after exhaustion - never an in-process SmallRye `@Retry` on a consumer
  (the two retry mechanisms would race, ADR 0014).
- Adding a topic means adding it to `create-topics.sh` (plus the DLQ once
  a consumer exists), the Kubernetes ConfigMap copy of that script in
  `infrastructure/kubernetes/base/infra/kafka.yaml`, and a payload doc in
  `docs/events/`.

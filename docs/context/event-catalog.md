# Event catalog (AI-context layer, ROADMAP M18)

Every RabbitMQ event type in the platform (migrated from Kafka - ADR
0004's 2026-08-05 addendum). Each publishing service owns one topic
exchange (named in the table below); each event type is a routing key on
that exchange, and each consumer declares its own durable queue bound to
it. DLQ queues are pre-declared by
[infrastructure/rabbitmq/declare-dlq-queues.sh](../../infrastructure/rabbitmq/declare-dlq-queues.sh)
(auto-declaration is off for those specifically, since nothing in-app
consumes them); every other queue self-declares from its owning service's
`application.yml`. Payload field tables live in [docs/events](../events).
This file is the cross-reference an agent needs to answer "who
publishes/consumes what."

DLQ naming convention unchanged from the Kafka era:
`<routing-key>.<consumer-name>.dlq` - one DLQ per consumer, created only
once a consumer for that event type exists (ADR 0004 + M10 addendum).

| Routing key (exchange)                  | Published by    | Consumed by (queue)                                                                                                                                           |
| --------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `booking-created` (`booking-events`)    | booking-service | flight-service (`booking-created.flight-inventory`), hotel-service (`booking-created.hotel-inventory`), payment-service (`booking-created.payment-processor`) |
| `booking-cancelled` (`booking-events`)  | booking-service | payment-service (`booking-cancelled.payment-processor`) - refund path                                                                                         |
| `booking-confirmed` (`booking-events`)  | booking-service | notification-service (`booking-confirmed.notification-processor`)                                                                                             |
| `payment-authorized` (`payment-events`) | payment-service | booking-service (`payment-authorized.booking-payment-outcome`) - confirm                                                                                      |
| `payment-failed` (`payment-events`)     | payment-service | booking-service (`payment-failed.booking-payment-outcome`) - compensate/cancel                                                                                |
| `payment-refunded` (`payment-events`)   | payment-service | nobody yet (published for completeness, ADR 0010)                                                                                                             |
| `flight-created` (`flight-events`)      | flight-service  | search-service (`flight-created.search-indexer`)                                                                                                              |
| `hotel-created` (`hotel-events`)        | hotel-service   | search-service (`hotel-created.search-indexer`)                                                                                                               |

DLQ queues (one per consumer above):
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
  queue with exponential backoff, then the consumer's DLQ queue after
  exhaustion - never an in-process SmallRye `@Retry` on a consumer (the
  two retry mechanisms would race, ADR 0014).
- Adding an event type means adding its routing key to the publisher's
  `application.yml` outgoing channel and the consumer's incoming channel
  (with a matching `queue.name`), plus its DLQ queue name to
  `declare-dlq-queues.sh` (and the Kubernetes ConfigMap copy of that
  script in `infrastructure/kubernetes/base/infra/rabbitmq.yaml`) once a
  consumer exists, and a payload doc in `docs/events/`.

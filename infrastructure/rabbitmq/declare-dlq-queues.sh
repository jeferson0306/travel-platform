#!/bin/sh
# Pre-declares the DLQ (dead-letter, visibility-only) queues, idempotently.
# Run automatically by the `rabbitmq-init` service in docker-compose after the
# broker reports healthy.
#
# Every other queue and exchange in this platform's topology self-declares from
# the owning service's own mp.messaging.incoming/outgoing config (queue.declare
# and exchange.declare both default to true in the SmallRye RabbitMQ connector) -
# but these DLQ queues have no in-app consumer (RetryRelay classes only ever
# publish to them, for external visibility/alerting, see
# docs/adr/0004-use-kafka-for-event-driven-communication.md), so nothing would
# otherwise create them before a service tries to publish to one. Declared via
# the management HTTP API (curl) rather than rabbitmqadmin - no extra binary to
# fetch, and this image already has curl.
set -eu

API="http://rabbitmq:15672/api/queues/%2f"
AUTH="guest:guest"

declare_queue() {
  name="$1"
  curl -sf -u "$AUTH" -X PUT "$API/$name" \
    -H "content-type: application/json" \
    -d '{"durable": true}' \
    -o /dev/null
  echo "Declared queue: $name"
}

# booking-created consumers' DLQs (flight-inventory, hotel-inventory, payment-processor)
declare_queue "booking-created.flight-inventory.dlq"
declare_queue "booking-created.hotel-inventory.dlq"
declare_queue "booking-created.payment-processor.dlq"

# booking-cancelled consumer's DLQ (payment-processor)
declare_queue "booking-cancelled.payment-processor.dlq"

# payment-authorized/payment-failed consumer's DLQ (booking-payment-outcome)
declare_queue "payment-authorized.booking-payment-outcome.dlq"
declare_queue "payment-failed.booking-payment-outcome.dlq"

# booking-confirmed consumer's DLQ (notification-processor)
declare_queue "booking-confirmed.notification-processor.dlq"

# flight-created/hotel-created consumer's DLQ (search-indexer)
declare_queue "flight-created.search-indexer.dlq"
declare_queue "hotel-created.search-indexer.dlq"

echo "All DLQ queues declared."

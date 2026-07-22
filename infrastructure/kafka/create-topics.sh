#!/usr/bin/env bash
# Creates the topics this platform's services publish to, idempotently.
# Run automatically by the `kafka-init` service in docker-compose after the
# broker reports healthy. Auto-topic-creation is deliberately disabled on
# the broker (KAFKA_AUTO_CREATE_TOPICS_ENABLE=false) so every topic that
# exists is one this script - and therefore this file - accounts for.
#
# Retry/DLQ topics (per docs/adr/0004-use-kafka-for-event-driven-communication.md)
# are added here once a consumer actually exists for a topic - there is no
# point creating a DLQ for a topic nothing consumes yet.
set -euo pipefail

BOOTSTRAP_SERVER="kafka:19092"
TOPICS_BIN="/opt/kafka/bin/kafka-topics.sh"

create_topic() {
  local topic="$1"
  "$TOPICS_BIN" --bootstrap-server "$BOOTSTRAP_SERVER" \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1
}

# booking-service (see ROADMAP M8)
create_topic "booking-created"
create_topic "booking-cancelled"

# flight-service / hotel-service consumer groups on booking-created (ROADMAP M10) - DLQ per
# consumer group, see docs/adr/0004-use-kafka-for-event-driven-communication.md's M10 addendum.
create_topic "booking-created.flight-inventory.dlq"
create_topic "booking-created.hotel-inventory.dlq"

# payment-service (see ROADMAP M11, docs/adr/0010-payment-saga.md)
create_topic "payment-authorized"
create_topic "payment-failed"
create_topic "payment-refunded"

# payment-service's consumer group on booking-created/booking-cancelled, and booking-service's
# consumer group on payment-authorized/payment-failed - same DLQ-per-consumer-group pattern as M10.
create_topic "booking-created.payment-processor.dlq"
create_topic "booking-cancelled.payment-processor.dlq"
create_topic "payment-authorized.booking-payment-outcome.dlq"
create_topic "payment-failed.booking-payment-outcome.dlq"

echo "Topics ready:"
"$TOPICS_BIN" --bootstrap-server "$BOOTSTRAP_SERVER" --list

# Booking events

## `booking-created`

Published by `booking-service` when a booking is created (`POST /api/v1/bookings`).

| Field        | Type                        | Meaning                                    |
| ------------ | --------------------------- | ------------------------------------------ |
| `bookingId`  | object (`BookingId`)        | The new booking's id                       |
| `travelerId` | object (`TravelerId`)       | Owning traveler (identity-service user id) |
| `reference`  | object (`BookingReference`) | What is being booked - see below           |
| `amount`     | object (`Money`)            | What the booking costs (ROADMAP M11)       |
| `occurredOn` | timestamp                   | When the booking was created               |

`reference` is structured (ROADMAP M10, once flight-service/hotel-service
existed to point at):

| Field      | Type                | Meaning                                  |
| ---------- | ------------------- | ---------------------------------------- |
| `itemType` | `FLIGHT` \| `HOTEL` | Which inventory service owns the item    |
| `itemId`   | string (UUID)       | The flight or hotel id                   |
| `quantity` | int                 | Seats (flight) or rooms (hotel) reserved |

Consumers:

- `flight-service` (consumer group `flight-inventory`) - decrements
  `availableSeats` when `itemType` is `FLIGHT`.
- `hotel-service` (consumer group `hotel-inventory`) - decrements
  `availableRooms` when `itemType` is `HOTEL`.
- `payment-service` (consumer group `payment-processor`) - authorizes
  payment for `amount`, publishing `payment-authorized`/`payment-failed`
  (ROADMAP M11, see
  [docs/adr/0010-payment-saga.md](../adr/0010-payment-saga.md) and
  [docs/events/payment-events.md](payment-events.md)).
- Planned: `search-service` (once it exists).

## `booking-confirmed`

Published by `booking-service` when a booking transitions `PENDING` →
`CONFIRMED` - i.e. when its `payment-authorized` consumer calls
`Booking.confirm()` (see the saga in
[docs/adr/0010-payment-saga.md](../adr/0010-payment-saga.md)). Raised by the
same generic outbox mechanism as `booking-created`/`booking-cancelled` - no
separate publish code path.

| Field           | Type                  | Meaning                                      |
| --------------- | --------------------- | -------------------------------------------- |
| `bookingId`     | object (`BookingId`)  | The confirmed booking's id                   |
| `travelerId`    | object (`TravelerId`) | Owning traveler                              |
| `travelerEmail` | object (`Email`)      | Where to send the confirmation (ROADMAP M12) |
| `occurredOn`    | timestamp             | When the booking was confirmed               |

`travelerEmail` is trusted client input on `CreateBookingRequest`, carried
through to this event - same simplification already noted for `amount`
(ROADMAP M11). Revisit once the Gateway (M14) or an identity lookup lands.

Consumer: `notification-service` (consumer group `notification-processor`)

- sends a booking confirmation email and stores a `Notification` record
  (ROADMAP M12, see
  [docs/adr/0011-notification-service.md](../adr/0011-notification-service.md)).

## `booking-cancelled`

Published by `booking-service` when a booking is cancelled
(`POST /api/v1/bookings/{id}/cancel`).

| Field        | Type                  | Meaning                        |
| ------------ | --------------------- | ------------------------------ |
| `bookingId`  | object (`BookingId`)  | The cancelled booking's id     |
| `travelerId` | object (`TravelerId`) | Owning traveler                |
| `occurredOn` | timestamp             | When the cancellation happened |

Consumer: `payment-service` (consumer group `payment-processor`) - refunds
the booking's payment if one was authorized, publishing `payment-refunded`
(ROADMAP M11). Restoring the flight/hotel inventory a cancelled booking had
reserved is still out of scope - no consumer for it yet.

## Delivery guarantees

At-least-once. Both events are written to the transactional outbox in the
same MongoDB transaction as the booking write, then relayed to Kafka - see
[docs/adr/0007-transactional-outbox.md](../adr/0007-transactional-outbox.md).
A consumer may see the same event more than once and must be idempotent.

## Failure story

`flight-service` and `hotel-service` are the first real consumers of
`booking-created` (ROADMAP M10). Each:

- **Idempotency**: claims a `bookingId` in a `processed_bookings` collection
  (unique `_id`) before mutating inventory - a duplicate delivery is a
  silent no-op.
- **Transient/business failure** (Mongo error, or insufficient
  seats/rooms): recorded in a `retry_tasks` collection and retried with
  exponential backoff by a scheduled relay, rather than nacking the Kafka
  message - see the ADR 0004 M10 addendum for why this is Mongo-backed
  rather than a literal second Kafka topic.
- **Malformed message**: logged and dropped immediately - not retried,
  since retrying can never fix a parsing failure.
- **Exhausted retries**: moved to a `dead_letters` collection and published
  to a per-consumer-group DLQ topic (`booking-created.flight-inventory.dlq`,
  `booking-created.hotel-inventory.dlq`) for external visibility.

`payment-service`'s consumers for both `booking-created` and
`booking-cancelled` (own consumer group `payment-processor`, ROADMAP M11)
follow the exact same idempotency/retry/DLQ shape described above, against
their own `booking-created.payment-processor.dlq`/
`booking-cancelled.payment-processor.dlq` topics (see
[docs/events/payment-events.md](payment-events.md) and
[docs/adr/0010-payment-saga.md](../adr/0010-payment-saga.md)).

`notification-service`'s `booking-confirmed` consumer (own consumer group
`notification-processor`, ROADMAP M12) follows the same shape too, against
its own `booking-confirmed.notification-processor.dlq` topic (see
[docs/adr/0011-notification-service.md](../adr/0011-notification-service.md)).

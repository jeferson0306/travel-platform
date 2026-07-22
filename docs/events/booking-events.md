# Booking events

## `booking-created`

Published by `booking-service` when a booking is created (`POST /api/v1/bookings`).

| Field        | Type                        | Meaning                                    |
| ------------ | --------------------------- | ------------------------------------------ |
| `bookingId`  | object (`BookingId`)        | The new booking's id                       |
| `travelerId` | object (`TravelerId`)       | Owning traveler (identity-service user id) |
| `reference`  | object (`BookingReference`) | What is being booked - see below           |
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
- Planned: `notification-service` (confirmation email), `search-service`
  (once it exists).

## `booking-cancelled`

Published by `booking-service` when a booking is cancelled
(`POST /api/v1/bookings/{id}/cancel`).

| Field        | Type                  | Meaning                        |
| ------------ | --------------------- | ------------------------------ |
| `bookingId`  | object (`BookingId`)  | The cancelled booking's id     |
| `travelerId` | object (`TravelerId`) | Owning traveler                |
| `occurredOn` | timestamp             | When the cancellation happened |

No consumer exists yet. Planned consumer: `payment-service` (refund, once
it exists - M11). Restoring the flight/hotel inventory a cancelled booking
had reserved is deliberately out of scope until then - it needs the same
idempotency and retry story as the decrement path below, and there is no
consumer to build it against yet.

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

`booking-cancelled` has no consumer yet, so there is nothing to retry or
dead-letter for it.

# Booking events

## `booking-created`

Published by `booking-service` when a booking is created (`POST /api/v1/bookings`).

| Field        | Type                        | Meaning                                                               |
| ------------ | --------------------------- | --------------------------------------------------------------------- |
| `bookingId`  | object (`BookingId`)        | The new booking's id                                                  |
| `travelerId` | object (`TravelerId`)       | Owning traveler (identity-service user id)                            |
| `reference`  | object (`BookingReference`) | What is being booked (freeform until flight/hotel-service exist - M9) |
| `occurredOn` | timestamp                   | When the booking was created                                          |

No consumer exists yet. Planned consumers: `notification-service`
(confirmation email), `search-service` (nothing to index yet - depends on
flight/hotel-service).

## `booking-cancelled`

Published by `booking-service` when a booking is cancelled
(`POST /api/v1/bookings/{id}/cancel`).

| Field        | Type                  | Meaning                        |
| ------------ | --------------------- | ------------------------------ |
| `bookingId`  | object (`BookingId`)  | The cancelled booking's id     |
| `travelerId` | object (`TravelerId`) | Owning traveler                |
| `occurredOn` | timestamp             | When the cancellation happened |

No consumer exists yet. Planned consumer: `payment-service` (refund, once
it exists - M11).

## Delivery guarantees

At-least-once. Both events are written to the transactional outbox in the
same MongoDB transaction as the booking write, then relayed to Kafka - see
[docs/adr/0007-transactional-outbox.md](../adr/0007-transactional-outbox.md).
A consumer may see the same event more than once and must be idempotent.

## Failure story

Not yet defined - no consumer exists yet, so there is nothing to retry or
dead-letter. Per ADR 0004, retry topics and a DLQ are added when the first
real consumer is built (ROADMAP M9/M10), against that consumer's actual
failure modes rather than speculatively now.

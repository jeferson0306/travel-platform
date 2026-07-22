# Payment events

See [docs/adr/0010-payment-saga.md](../adr/0010-payment-saga.md) for the saga these events are
part of.

## `payment-authorized`

Published by `payment-service` when payment for a booking is authorized.

| Field        | Type                 | Meaning                         |
| ------------ | -------------------- | ------------------------------- |
| `paymentId`  | object (`PaymentId`) | The payment's id                |
| `bookingId`  | object (`BookingId`) | The booking this payment is for |
| `amount`     | object (`Money`)     | Amount authorized               |
| `occurredOn` | timestamp            | When the authorization happened |

Consumer: `booking-service` (consumer group `booking-payment-outcome`) -
confirms the booking (`PENDING` → `CONFIRMED`).

## `payment-failed`

Published by `payment-service` when payment for a booking is declined.

| Field        | Type                 | Meaning                         |
| ------------ | -------------------- | ------------------------------- |
| `paymentId`  | object (`PaymentId`) | The payment's id                |
| `bookingId`  | object (`BookingId`) | The booking this payment is for |
| `amount`     | object (`Money`)     | Amount that was attempted       |
| `reason`     | string               | Why the gateway declined it     |
| `occurredOn` | timestamp            | When the decline happened       |

Consumer: `booking-service` (consumer group `booking-payment-outcome`) -
cancels the booking (compensating transaction, reusing `CancelBookingUseCase`).

## `payment-refunded`

Published by `payment-service` when an authorized payment is refunded (in
reaction to `booking-cancelled` - see docs/events/booking-events.md).

| Field        | Type                 | Meaning                         |
| ------------ | -------------------- | ------------------------------- |
| `paymentId`  | object (`PaymentId`) | The payment's id                |
| `bookingId`  | object (`BookingId`) | The booking this payment is for |
| `amount`     | object (`Money`)     | Amount refunded                 |
| `occurredOn` | timestamp            | When the refund happened        |

No consumer exists yet - a refund-confirmation notification is a candidate
for a future milestone, not scoped to any so far.

## Delivery guarantees

At-least-once, same transactional-outbox mechanism as booking-service (ADR 0007) - `payment-service` writes its own `payments` + `outbox` collections in
one MongoDB transaction, relayed to Kafka by its own `OutboxRelay`.

## Failure story

`booking-service`'s `payment-authorized`/`payment-failed` consumers (own
consumer group `booking-payment-outcome`):

- **Idempotency**: no separate claim collection needed -
  `Booking.confirm()`/`Booking.cancel()` already reject a repeat call
  (`BookingAlreadyConfirmedException`/`BookingAlreadyCancelledException`),
  which the consumer treats as a no-op.
- **Technical failure**: recorded in a `retry_tasks` collection, retried
  with exponential backoff, moved to a dead-letter Kafka topic
  (`payment-authorized.booking-payment-outcome.dlq`,
  `payment-failed.booking-payment-outcome.dlq`) after too many attempts -
  same Mongo-backed pattern as M10 (see the ADR 0004 M10 addendum).
- **Malformed message**: logged and dropped immediately.

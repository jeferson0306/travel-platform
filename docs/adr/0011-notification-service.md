# 0011 — notification-service as a terminal, event-driven consumer

- Status: Accepted
- Date: 2026-07-22

## Context

A traveler whose booking is confirmed (`PENDING` → `CONFIRMED`, the last step
of the payment saga - ADR 0010) should get an email saying so. This is a new
bounded concern - "deliver a notification" - distinct from booking,
inventory, or payment, so it gets its own service (`notification-service`,
ROADMAP M12), continuing the same one-service-per-bounded-context shape as
every other service in this platform.

Two questions shaped the design:

1. **Which event triggers the email?** `booking-created` fires as soon as a
   booking is placed, before payment is known to succeed - emailing at that
   point could tell a traveler their booking is confirmed when it might be
   about to fail and get auto-cancelled by the saga. `booking-confirmed` (new
   - raised by `Booking.confirm()`, see `booking-service`'s
     `BookingConfirmed` domain event) only fires once payment has actually been
     authorized, so that is the trigger.
2. **Does this service need an outbox/domain events of its own?** Every other
   aggregate in this platform (`Booking`, `Payment`) raises domain events
   because something downstream reacts to them. Nothing reacts to "a
   notification was sent" - this service is a terminal consumer, the end of
   every chain it participates in.

## Decision

- `booking-service` adds `travelerEmail` to `CreateBookingRequest` (trusted
  client input, same simplification already applied to `amount` - ADR
  0010's Consequences section, revisit once the Gateway/M14 or an identity
  lookup lands), carries it onto `Booking`, and includes it on the new
  `BookingConfirmed` domain event. No new publish code path: `Booking.confirm()`
  raises the event and the existing generic outbox relay (topic = the
  event's `eventType`, `"booking-confirmed"`) picks it up for free, exactly
  like `booking-created`/`booking-cancelled` already do.
- `notification-service` consumes `booking-confirmed` (own consumer group
  `notification-processor`), sends the email via an `EmailGateway` port
  (simulated for now - `infrastructure.gateway.SimulatedEmailGateway`, a
  drop-in seam for a real provider like SES later, mirroring
  `payment-service`'s `SimulatedPaymentGateway`), and persists a
  `Notification` record.
- `Notification` is a plain aggregate with **no domain events and no
  transactional outbox** - a deliberate deviation from every other aggregate
  in this platform. `MongoNotificationRepository.save()` is a single
  `replaceOne`, not a two-collection transaction. This is not an oversight;
  it is the direct consequence of there being no downstream consumer for
  "a notification was sent."
- A read-only `GET /api/v1/notifications/{bookingId}` endpoint exists for
  support/ops (`SUPPORT`/`ADMIN`/`SUPER_ADMIN` roles - ADR 0006), so a
  traveler-support agent can confirm whether an email actually went out.
  Notifications are never created via this API, only by the Kafka consumer.

## Idempotency and failure handling

Same shape as every other consumer in this platform (ADR 0004's M10
addendum, reused unchanged through M10/M11): a `processed_bookings`
claim collection for idempotency, a Mongo-backed `retry_tasks`
collection with exponential backoff for transient failures, and a
`dead_letters` collection + `booking-confirmed.notification-processor.dlq`
Kafka topic once retries are exhausted. Unlike `payment-service`'s
`RetryRelay` (which dispatches between two action types - authorize vs.
refund), `notification-service`'s `RetryRelay` only ever retries one thing
(sending the confirmation email), so there is no action-field dispatch.

## Consequences

- A booking placed but never confirmed (payment fails, or the traveler
  cancels first) never triggers an email - correct: there is nothing to
  confirm.
- The simulated email gateway always succeeds, so the retry/DLQ path is only
  exercised via directly-seeded `retry_tasks` in tests (see
  `RetryRelayTest`), not through an induced gateway failure - the same
  limitation this platform accepts elsewhere for simulated adapters
  (`SimulatedPaymentGateway` always approves too).
- `payment-failed`'s `reason` (ADR 0010's Consequences section flagged this
  as unaddressed) is still not surfaced to the traveler - this milestone
  only covers the confirmation path, not a "why was my booking cancelled"
  notification. A candidate for a future milestone, not scoped here.

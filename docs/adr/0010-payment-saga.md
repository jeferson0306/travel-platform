# 0010 — Payment as a choreography saga with booking-service

- Status: Accepted
- Date: 2026-07-22

## Context

A booking is only really "done" once it is paid for. Charging a traveler and
confirming their booking are two separate services' responsibilities
(booking-service owns reservation lifecycle, a new `payment-service` owns
charging), so this can never be a single local transaction - it needs a
saga: a sequence of local transactions coordinated through events, with a
compensating action if a later step fails.

Two saga styles were considered:

- **Orchestration** - a central coordinator (e.g. inside booking-service, or
  a dedicated saga-orchestrator service) explicitly calls each participant
  and decides the next step.
- **Choreography** - each service reacts to the previous service's event and
  publishes its own; there is no central coordinator.

## Decision

Use **choreography**, continuing the same event-driven style already
established for inventory (ADR 0004, ROADMAP M10):

1. `booking-service` creates a booking (`PENDING`), publishing
   `booking-created` (already existed - M8/M10 added the `amount` it now
   also carries, since payment-service needs it).
2. `payment-service` consumes `booking-created` (own consumer group
   `payment-processor`), authorizes payment via `PaymentGateway`
   (simulated - see `infrastructure.gateway.SimulatedPaymentGateway`, a
   drop-in seam for a real provider like Stripe later), and publishes
   `payment-authorized` or `payment-failed`.
3. `booking-service` consumes both (own consumer group
   `booking-payment-outcome`): `payment-authorized` confirms the booking
   (`PENDING` → `CONFIRMED`); `payment-failed` cancels it - the compensating
   transaction, reusing the exact same `CancelBookingUseCase` the public
   cancel endpoint uses, not a separate code path.
4. When a booking is later cancelled (`booking-cancelled`, either by the
   traveler or the compensating step above), `payment-service` consumes it
   and refunds an authorized payment, publishing `payment-refunded`. No
   consumer for `payment-refunded` exists yet - noted in
   [docs/events/payment-events.md](../events/payment-events.md).

Rejected alternative - **orchestration**: would centralize the saga's logic
in one place (easier to read as a single flow) at the cost of a new
coordinator component/responsibility and, more importantly, inconsistency
with M10's inventory consumers, which are already choreography. Introducing
a second coordination style for the same category of problem (a
cross-service reaction to a domain event) would make the codebase harder to
reason about, not easier - not justified here.

## Idempotency

Kafka's at-least-once delivery means every consumer below must tolerate a
duplicate message:

- `payment-service`'s `booking-created`/`booking-cancelled` consumers claim
  the bookingId in a `processed_bookings`/`processed_cancellations`
  collection before acting - the same pattern M10's inventory consumers use
  (a payment is not naturally idempotent the way a domain-guarded status
  transition is; re-authorizing would double-charge).
- `booking-service`'s `payment-authorized`/`payment-failed` consumers need
  no separate claim collection: `Booking.confirm()`/`Booking.cancel()`
  already throw `BookingAlreadyConfirmedException`/
  `BookingAlreadyCancelledException` on a repeat call, which the consumer
  treats as a normal idempotent no-op. The domain model's own invariant
  _is_ the idempotency guard here.

## Failure handling

Same Mongo-backed retry queue + exponential backoff + DLQ-per-consumer-group
pattern as M10 - see the ADR 0004 M10 addendum for why this is Mongo-backed
rather than a literal second Kafka topic. Not re-explained per service; see
[docs/events/payment-events.md](../events/payment-events.md) and
[docs/events/booking-events.md](../events/booking-events.md) for the
per-topic specifics.

## Consequences

- `booking-service`'s create-booking API now requires `amount`/`currency` -
  still trusted client input for now (same simplification already noted on
  `CreateBookingRequest`), not looked up authoritatively from
  flight-service/hotel-service. Revisit once the Gateway (M14) or a pricing
  lookup lands - charging the wrong amount is a real bug once this is a real
  payment provider instead of a simulation.
- `payment-service`'s gateway is simulated (always approves) - the point of
  this milestone is the saga/idempotency/outbox mechanics, not a real
  charge. Swapping in a real provider is a single adapter behind
  `PaymentGateway`, changing nothing else in this service.
- A traveler-facing "why was my booking cancelled" experience (i.e.
  surfacing the payment failure reason) is not built yet - `payment-failed`
  carries a `reason`, but nothing reads it beyond the log line in
  `PaymentFailedConsumer`. Revisit once notification-service (M12) exists.

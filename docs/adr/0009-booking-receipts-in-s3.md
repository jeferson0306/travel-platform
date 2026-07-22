# 0009 — Booking receipts stored in S3

- Status: Accepted
- Date: 2026-07-22

## Context

A booking confirmation is naturally a durable, retrievable artifact (a
receipt) rather than a query against live booking state - the kind of
thing a real travel platform emails, links from a "my bookings" page, or
hands to support. Object storage (S3) is a better fit for this than
another MongoDB collection: it is where the traveler-facing document
representation belongs, separate from `booking-service`'s own write model.

## Decision

`booking-service` writes a JSON receipt to the `booking-receipts` S3
bucket (provisioned by Terraform against LocalStack - see
[docs/adr/0008](0008-use-localstack-and-terraform-for-aws-resources.md))
at key `receipts/<bookingId>.json`, immediately after a booking is
successfully created.

- **Best-effort, not transactional.** The write happens after
  `BookingRepository.save()` has already committed. S3 cannot join the
  same MongoDB transaction the booking write and outbox event use (ADR
  0007), so there is no way to make this atomic with the booking write
  without a saga - not justified for a convenience artifact that can
  always be regenerated from the booking record, which remains the
  system of record. If the S3 write fails, it is logged and the booking
  creation still succeeds; it is not retried by this ADR's scope (compare
  to the Mongo-backed retry queue used for the M10 inventory consumers,
  which exists because a lost inventory decrement is a correctness bug -
  a missing receipt is not).
- **Not the transactional outbox.** The outbox (ADR 0007) exists to
  atomically pair a MongoDB write with a Kafka publish for other services
  to consume. A receipt is a leaf artifact nothing consumes as an event -
  routing it through the outbox would misuse the pattern for a case it
  was not designed for.
- **Content**: `bookingId`, `travelerId`, `itemType`, `itemId`,
  `quantity`, `createdAt` - the same facts already on `BookingCreated`
  (ADR 0004/M10), serialized as a standalone JSON document rather than a
  rendered PDF; a rendering step can be layered on top later without
  changing where the source data comes from.

## Consequences

- `booking-service` gains an AWS SDK dependency and an S3 output port/
  adapter, configured to hit LocalStack in dev/test and real AWS (default
  credential chain, no endpoint override) otherwise.
- A receipt can silently fail to be written (logged, not retried) -
  acceptable because the booking itself is unaffected and the receipt is
  reconstructable from the booking record; revisit if a stronger guarantee
  is ever needed (e.g. move to a scheduled relay like the outbox's).

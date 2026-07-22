# Webhook idempotency

Applies to any inbound webhook this platform receives (payment provider
callbacks, future third-party integrations). No service implements this yet
(payment-service lands in Phase 3) - this document exists so the pattern is
decided before the first webhook handler is written, not improvised then.

## Core principles

1. **Always verify the signature** before touching the payload. Reject
   unsigned or badly-signed requests immediately, with no partial
   processing.
2. **Verify against the raw request body**, not a parsed/re-serialized one -
   signatures are computed over exact bytes.
3. **Idempotency** - store the provider's event id and skip any event
   already processed. A provider retries; the same event id must never be
   applied twice.
4. **Return 2xx fast, process async** - acknowledge receipt immediately (via
   an outbox/Kafka message to the owning service), then do the real work.
   A slow synchronous handler risks the provider's retry/backoff kicking in
   and causing duplicate deliveries.
5. **Compare signatures with a constant-time comparison** to avoid timing
   attacks (`MessageDigest.isEqual` in Java, not `String.equals`).

## Minimum persistence shape

```
processed_webhook_events
  id             (internal id)
  provider       ("payment-provider", ...)
  provider_event_id   (unique per provider)
  event_type
  processed_at
```

A unique index on `(provider, provider_event_id)` is what actually enforces
idempotency - the application check is a fast path, the index is the
guarantee.

## Checklist (apply when a webhook handler is first built)

- [ ] Raw body available to the handler before any body-parsing middleware
      runs.
- [ ] Signature verified with a constant-time comparison.
- [ ] Idempotency table/index in place before the first real event is
      accepted.
- [ ] Response sent before any downstream async processing.
- [ ] Webhook signing secret is environment-specific and never committed
      (see [SECURITY.md](../../SECURITY.md)).

# Audit logging

Sensitive actions - anything that changes who can do what, or that changes
money, must leave a durable, queryable trail independent of the application
log stream.

## When to write an audit entry

- Role or permission changes.
- Password reset / credential changes.
- Payment authorization, capture, refund.
- Booking cancellation.
- Any administrative override (support acting on a user's behalf).

Regular reads, and writes that are already fully described by a domain
event (e.g. `booking-created`), do not need a separate audit entry - the
event stream already carries that trail. Audit logging is for the cases
where "who did this, and what changed" is not otherwise reconstructable.

## Minimum fields

| Field       | Meaning                                                 |
| ----------- | ------------------------------------------------------- |
| `id`        | Unique identifier for the audit entry                   |
| `actorId`   | Who performed the action (user or service identity)     |
| `targetId`  | What was acted on                                       |
| `action`    | What happened, e.g. `ROLE_CHANGED`, `BOOKING_CANCELLED` |
| `oldValue`  | State before the change (only the changed fields)       |
| `newValue`  | State after the change                                  |
| `ipAddress` | Origin of the request                                   |
| `userAgent` | Client that made the request                            |
| `createdAt` | When it happened                                        |

## Rules

- Audit entries are append-only - never updated, never deleted (not even by
  soft delete).
- Never store a raw secret (password, token, card number) in `oldValue` /
  `newValue` - if the changed field is sensitive, record that it changed,
  not its value.
- Written synchronously, in the same transaction/operation as the change it
  records - an audit entry that can silently fail to write defeats the
  purpose.
- Each service that performs sensitive actions owns its own audit
  collection; audit entries are not an event a consumer reconstructs -
  they are written directly by the service performing the action.

This complements, but does not replace, the request-level structured
logging described in the
[observability contract](../../ARCHITECTURE.md#observability-contract):
request logs answer "what happened technically," audit logs answer "who
did what, to what, with what before/after state."

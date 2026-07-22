# Error handling contract

Every service in this platform returns errors in the same shape, over the
same small set of machine-readable codes, so a client (or another service)
never has to special-case a given service's error format.

## Response shape

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Human-readable summary, safe to display",
  "details": ["email: must be a well-formed email address"]
}
```

- `error` - machine-readable, `SCREAMING_SNAKE_CASE`. Never invent a new one
  without adding it to the table below.
- `message` - optional, human-readable, safe to show a user. Never a raw
  exception message or stack trace.
- `details` - optional list, used for validation failures (one entry per
  violated constraint).

## Error codes

| Code                  | HTTP | When                                              |
| --------------------- | ---- | ------------------------------------------------- |
| `VALIDATION_ERROR`    | 400  | Request body failed bean validation               |
| `INVALID_CREDENTIALS` | 401  | Wrong email/password at login                     |
| `NO_TOKEN`            | 401  | Missing Bearer token on a protected endpoint      |
| `INVALID_TOKEN`       | 401  | JWT expired, malformed, or failed signature check |
| `FORBIDDEN`           | 403  | Authenticated but lacks the required role         |
| `NOT_FOUND`           | 404  | Resource does not exist                           |
| `CONFLICT`            | 409  | Duplicate value against a unique constraint       |
| `RATE_LIMITED`        | 429  | Too many requests                                 |
| `INTERNAL_ERROR`      | 500  | Unexpected server error                           |

This table is the single source of truth. A service-specific domain
exception maps to one of these codes via a JAX-RS `ExceptionMapper` - it
never invents its own code or status.

## Rules

- Never leak a stack trace or raw exception message to the client.
- Unknown/unexpected exceptions always fall back to `INTERNAL_ERROR` (500)
  with a generic message; the real detail goes to the structured logs (see
  [observability contract](../../ARCHITECTURE.md#observability-contract)),
  correlatable by `traceId`.
- `NO_TOKEN` / `INVALID_TOKEN` / `FORBIDDEN` apply once a service starts
  validating JWTs issued by `identity-service` (gateway and any service with
  protected endpoints) - see
  [docs/adr/0006-rbac-roles.md](../adr/0006-rbac-roles.md).

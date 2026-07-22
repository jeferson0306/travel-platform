# Rate limiting

Not implemented yet - lands with the API Gateway (ROADMAP M14). Decided now
so the Gateway is built against a settled approach rather than one invented
under deadline.

## Rules

- Rate limiting state lives in Redis, never in process memory - every
  service instance must see the same counters, and an in-memory limiter is
  meaningless the moment there is more than one replica.
- The Gateway is the enforcement point for general per-client limits
  (`RATE_LIMITED`, 429 - see
  [error-handling.md](error-handling.md)). Individual services may add a
  stricter limit on a specific sensitive endpoint (e.g. login, password
  reset) in addition to the Gateway's default.
- Return rate-limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`,
  `Retry-After`) so a well-behaved client can back off without guessing.
- Limits are per authenticated identity when available, falling back to per
  IP for anonymous requests (registration, login).

## Sensitive endpoints requiring a stricter limit regardless of the Gateway default

- `POST /api/v1/auth/login` - brute-force protection.
- `POST /api/v1/auth/register` - abuse/spam protection.
- Any password-reset or MFA endpoint, once they exist.

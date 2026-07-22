# 0006 — Standard RBAC roles across services

- Status: Accepted
- Date: 2026-07-22

## Context

Multiple services will eventually need to distinguish what an authenticated
user is allowed to do: a traveler managing their own bookings versus support
staff looking up any booking versus an operator managing platform
configuration. Without a shared role vocabulary, each service would invent
its own, making the JWT `groups` claim meaningless across service
boundaries and turning authorization review into a service-by-service
guessing game.

## Decision

Define one shared set of roles, owned by `identity-service` and carried in
the JWT `groups` claim so every downstream service authorizes against the
same vocabulary:

- `USER` - default role for every registered traveler.
- `SUPPORT` - read access across users' bookings for customer support.
- `MANAGER` - manages inventory/pricing within a domain service.
- `ADMIN` - platform administration within a service.
- `SUPER_ADMIN` - cross-service administration.

Rules:

- Role checks happen server-side, in the service that owns the resource.
  Never trust a client-supplied role, and never treat UI visibility as
  authorization.
- Changing a user's role is itself a sensitive action and must be audit
  logged (see [docs/business-rules/audit-logging.md](../business-rules/audit-logging.md)).
- A user has exactly one role at a time in this phase. Fine-grained
  per-resource permissions (ABAC) are deferred until a concrete use case
  needs them - documented here rather than speculatively built.
- New roles are added to this ADR (superseding it) before any service
  starts checking for them - the list is a contract, not a per-service
  convenience.

## Consequences

- `identity-service` is the only place a role is assigned or changed.
- Every other service treats the JWT `groups` claim as the source of truth
  and never queries `identity-service` synchronously just to check a role.
- The `FORBIDDEN` (403) error code in
  [docs/architecture/error-handling.md](../architecture/error-handling.md)
  is raised whenever an authenticated request's role doesn't satisfy an
  endpoint's requirement.

## Addendum (M9) — RS256, not a shared HMAC secret

Signing was originally HS256 with a secret shared via `JWT_SECRET` across
every service (matching the local dev default already in `.env.example`).
Implementing verification in `flight-service` (the first consumer) surfaced
two problems with that: architecturally, every verifying service would need
the same signing secret, meaning any of them leaking it lets that service
forge tokens for every role - a much larger blast radius than an
architecture where only `identity-service` can ever sign. Practically, the
installed SmallRye JWT version's HS256 verification path
(`smallrye.jwt.verify.secretkey`) never actually resolved a request-time
verification key from that config in testing - it consistently failed with
"Verification key is unresolvable", with no combination of the documented
config properties observed to fix it.

Switched to RS256: `identity-service` signs with an RSA private key
(`smallrye.jwt.sign.key.location=privateKey.pem`); every verifying service
only needs the public key (`mp.jwt.verify.publickey.location=publicKey.pem`).
The keypair committed under each service's `src/main/resources` is a
dev/test fixture (same status as the HS256 default it replaces) - a real
deployment mounts its own key material via the same properties. Only
`identity-service`'s copy of `privateKey.pem` matters for security; the
public key is not sensitive by design, and a consuming service's test
suite may hold a copy of the private key purely to mint synthetic tokens
without running identity-service - it has no way to reach a real key
because it's the same test fixture, not a production secret.

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

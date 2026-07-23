# Prompt: add an HTTP endpoint to a service

Use this template when a service needs a new REST endpoint.

---

Add `[METHOD] /api/v1/[resource][/subpath]` to `[service]`, following
docs/context/conventions.md:

- Resource class in `api/resource`, delegating to a use-case port in
  `application/port/in` - no business logic in the resource.
- AuthZ via `@RolesAllowed({...})` on the endpoint itself (roles: USER,
  MANAGER, ADMIN, SUPER_ADMIN, SUPPORT - docs/adr/0006). The gateway does
  NOT authorize; do not rely on it.
- Request DTO with Bean Validation annotations; errors surface in the
  canonical shape `{"error","message","details"}` via the existing
  exception mappers - never a raw exception message.
- If the endpoint creates/changes an aggregate that others react to, the
  aggregate raises a domain event published via the existing outbox -
  never publish to Kafka directly from the request path (ADR 0007).
- If this is a new first-path-segment, register it in gateway's
  `RoutingTable`/upstream config and its `%prod` URL.
- Tests over real HTTP (`@QuarkusTest` + RestAssured): success, validation
  failure, authz rejection (401/403), each with `@DisplayName`.
- Update docs/context/platform-overview.md's API surface list and
  docs/context/service-catalog.md's section for the service.

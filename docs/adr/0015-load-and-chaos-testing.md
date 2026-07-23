# 0015 — Load & chaos testing: k6 + Toxiproxy, measured against the real stack

- Status: Accepted
- Date: 2026-07-23

## Context

ROADMAP's M16 calls for "k6/Gatling load profiles, Toxiproxy fault
injection, documented failure playbooks." Consistent with this project's
documentation ethos throughout M1-M15 (every prior ADR/CHANGELOG entry
describes what was actually built and, where applicable, actually
observed - not projected), M16 was scoped as: bring up the entire 8-service
platform for real via Docker Compose, run genuine load against it, inject
a genuine fault, and document only numbers actually measured on this run.
No synthetic/estimated benchmark numbers appear anywhere in
[docs/runbooks/load-and-chaos-results.md](../runbooks/load-and-chaos-results.md).

Two prerequisites didn't exist yet at the start of M16:

1. Nothing ran all 8 backend services together. Each service had its own
   `quarkus:dev` inner loop and its own integration tests against
   Testcontainers, but nothing wired `gateway` to real containerized
   `identity-service`/`booking-service`/`flight-service`/`hotel-service`/
   `payment-service`/`notification-service`/`search-service` instances on
   one Docker network with the container names `gateway`'s hardcoded
   `%prod` upstream URLs expect.
2. No load-generation or fault-injection tooling was present at all.

## Decision

### Full-stack Compose profile, not a separate compose file

`infrastructure/docker/docker-compose.yml` gained an `apps` profile
covering all 8 backend services, layered on top of the existing
infra-only services (Mongo, Kafka, Redis, OpenSearch, LocalStack) which
stay profile-less so `make up` (the everyday `quarkus:dev` workflow) is
completely unaffected. A single compose file was kept instead of a
second `docker-compose.apps.yml`, because the backend services need to
resolve the same Mongo/Kafka/Redis/LocalStack containers by the same
DNS names the infra file already defines - splitting the file would mean
keeping two network definitions in sync for no benefit.

A third `chaos` profile holds only `toxiproxy` (`shopify/toxiproxy:2.1.4`

- the newest tag Shopify actually publishes; 2.9.x does not exist).
  Kept in its own profile, and never referenced by any service's default
  upstream config, so `toxiproxy` existing at all has zero effect unless a
  chaos experiment explicitly starts it and explicitly repoints a
  container at it (see below) - it cannot accidentally sit in the request
  path of a normal `apps-up` run.

Three host-side port collisions surfaced against other, unrelated local
projects on the machine used to build this (an already-running
`health-facility-manager` Redis container on 6379, a native `mongod` on
27017, a `beautyos-mongo-express` container on 8083) - all three were
remapped on the **host side only** (6380, 27018, 18083); every
container-to-container reference still uses the original default ports
over the Compose network, so no application config changed.

### k6 over Gatling, run via Docker, not installed locally

ROADMAP listed both as options. k6 was chosen: JS test scripts fit this
polyglot-but-mostly-scripty repo better than Gatling's Java/Scala DSL,
and `grafana/k6`'s official Docker image means `testing/load/k6/*.js` run
with zero local install (`docker run --rm -i --network travel-platform
grafana/k6 run - < script.js`), matching every other tool in this
project's local workflow (Mongo, Kafka, LocalStack are all
containerized too - nothing requires a host install beyond Docker,
Maven, and a JDK).

Two scripts, not one, because they exercise genuinely different paths:

- `search-load.js` - public, unauthenticated, read-only (route/city
  search, autocomplete). Cheap to ramp hard.
- `booking-saga-load.js` - the full write path (register → login →
  create booking), which is what actually drives the choreography saga
  end-to-end (booking → payment → booking confirmation →
  notification, ADR 0010/0011). Kept to a handful of VUs deliberately:
  bcrypt hashing in `identity-service` (ADR 0006) is intentionally slow,
  so this script's bottleneck is registration, not booking-service
  itself - conflating the two would have muddied what the numbers mean.

### Toxiproxy for the chaos scenario, latency only, one backend only

`flight-service` was chosen as the target because it is a plain
idempotent GET path behind `gateway`, making before/after behavior easy
to interpret. A `latency` toxic (3000ms, exceeding the 2000ms idempotent
timeout from ADR 0014) was chosen over a `timeout`/connection-reset
toxic because it more realistically models the "backend is alive but
degraded" case `Guard`'s per-backend circuit breaker exists for, versus
a clean, instant "backend is down" case that a naive health check would
also catch.

`gateway` was repointed at the Toxiproxy-fronted `flight-service` by
recreating the `gateway` container with a single environment variable
override (`GATEWAY_UPSTREAM_FLIGHT=http://toxiproxy:18999`) - no code or
config file changes, relying on the same Quarkus env-var-to-config-
property mapping used throughout this platform (e.g.
`BOOKING_AWS_S3_ENDPOINT_OVERRIDE` in `booking-service`). The experiment
is captured as a reproducible script,
[testing/chaos/flight-service-latency.sh](../../testing/chaos/flight-service-latency.sh),
with a `trap cleanup EXIT` that always restores `gateway`'s normal
docker-compose wiring, even if the script fails partway - a chaos script
that can itself leave the system in a broken state defeats its own
purpose.

### RBAC gap worked around, not fixed, for load-test seeding

Both k6 scripts need pre-existing inventory (a flight, a hotel) to
search/book against, which requires a `MANAGER`-role user (ADR 0006);
public registration only grants `USER`, and no endpoint exists to
self-elevate. Rather than adding a role-elevation endpoint (a real
feature with real access-control implications, out of scope for a
testing milestone) or a backdoor config flag, the one `MANAGER` user
needed for seeding was created by a single documented, one-off MongoDB
document mutation, explicitly labeled in
[testing/load/README.md](../../testing/load/README.md) as
"load-test-only, never a pattern for anything user-facing." The
inventory itself (the flight/hotel documents) was then created through
the real `POST /api/v1/flights` API using that user's real JWT, so
`flight-created`/`hotel-created` Kafka events still flow into
`search-service` exactly as they would for a real request - only the
role grant was shortcut, not the inventory creation.

## Consequences

- The platform now has a genuine, repeatable way to bring up all 8
  services together and to inject a real fault - useful for any future
  milestone that needs the same, not just M16.
- Real measurements surfaced a real, pre-existing limitation
  (`gateway`'s per-IP rate limiter, ADR 0013, penalizes many real users
  sharing one NAT/IP identically to one abusive client) that unit/
  integration tests could never have caught, because they don't generate
  concurrent multi-client traffic. Documented as a known limitation in
  [docs/runbooks/load-and-chaos-results.md](../runbooks/load-and-chaos-results.md),
  not fixed here - changing M14's rate-limiting key (e.g. to prefer the
  JWT `sub` claim) is a deliberate design change with its own trade-offs
  and belongs in its own change, not folded into a testing milestone.
- The `apps`/`chaos` Compose profiles and `Makefile` targets
  (`apps-build`/`apps-up`/`apps-down`/`apps-logs`/`apps-ps`) are now
  permanent, reusable infrastructure, not one-off scratch work - any
  future milestone needing the full stack up can reuse them unchanged.
- No production code changed in this milestone - M16 is testing and
  documentation only, so the risk of regression is confined to
  `infrastructure/docker/docker-compose.yml`, the `Makefile`, and
  `.env.example`.

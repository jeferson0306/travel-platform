# 0019 — Public demo deployment: essential-flow scope, Railway + MongoDB Atlas

- Status: Accepted (deployment execution in progress - see Consequences)
- Date: 2026-07-23

## Context

ROADMAP's M20 calls for "a deployed demo (frontend on Vercel, backend on a
free-tier host)." At the point this milestone started, the platform was
nine backend services, MongoDB, Kafka, Redis, OpenSearch, LocalStack, and
Ollama - the full stack from M16-M19. No frontend existed at all; the
platform had only ever been driven via curl/k6/browser-devtools against
the gateway directly.

## Decision

### Frontend: minimal React/Vite SPA, not the full "Padrão" stack

A small companion frontend (`frontend/`) covering exactly the essential
flow: register, log in, search flights/hotels, create a booking, view a
static confirmation. No i18n framework, no state-management library, no
component library - four pages don't need them, and the "no premature
abstractions" rule this codebase follows throughout applies here too.
Talks to the backend exclusively through the gateway, matching every
constraint already established for real clients.

One real platform gap surfaced immediately: there is no
`GET /api/v1/bookings/{id}` endpoint (noted as a known gap since M15's
manual testing, ADR 0015). The confirmation page cannot poll booking
status - it says so honestly instead of faking a status check, and
explains that the real choreography saga still runs asynchronously behind
it regardless of what the demo UI can observe.

### Backend hosting: essential-flow subset only, not the full stack

The full stack does not fit a genuinely free hosting tier. Kafka and
OpenSearch are the two blockers: Kafka needs a persistent stateful TCP
service (not supported by Render's or most other providers' free web-only
tiers), and OpenSearch needs meaningful RAM no free tier grants. Rather
than fake or skip the parts that make this platform interesting (the
choreography saga), the deployed demo excludes `search-service` and
`assistant-service` (and therefore OpenSearch and Ollama) and keeps the
full saga: `identity`, `flight`, `hotel`, `booking`, `payment`,
`notification` services, `gateway`, Kafka, and Redis - the same nine
minus the two that specifically need OpenSearch/Ollama, chosen precisely
because they're the two least central to "does the booking flow actually
work end to end."

**Hosting decision trail, documented rather than silently resolved:**
Render.com (genuinely free, no card) was evaluated first but rejected -
its free tier is HTTP web services only, with no free Kafka or Redis
equivalent, and this platform's core value (the async saga) depends on
real Kafka. Oracle Cloud's "Always Free" tier (a real free-forever VM
that could run `docker-compose.yml` unmodified) was the next candidate,
rejected due to known account-verification and capacity-provisioning
friction that would have blocked autonomous setup. Railway was the final
choice - already integrated into this session's tooling (CLI + skill),
but its free trial had expired, requiring the user to add a paid plan
($5/month) to provision new resources. The user chose to pay for Railway
over accepting a functionally reduced (Kafka-less) demo on a free host -
a deliberate trade-off in favor of deploying the real architecture, not
a simplified stand-in for it.

### Database: MongoDB Atlas free tier (M0), not a Railway-managed Mongo

The user already had an Atlas account and cluster; reusing it avoided
provisioning a database inside Railway's paid resource budget for
something a genuinely free tier already covers well.

### Multi-stage build required for Railway specifically

Every other Dockerfile.jvm in this repo (`backend/*/src/main/docker/`)
expects a pre-built `target/quarkus-app/` - the actual `mvn package` step
runs separately, in CI or `make apps-build`, before `docker build`.
Railway's Docker builder only runs `docker build` with no separate build
step, so a new `backend/Dockerfile.railway` was added: a multi-stage build
that runs Maven itself. Its build context has to be the whole `backend/`
tree, not just the target service's own directory - Maven's multi-module
reactor requires every module declared in `backend/pom.xml`'s `<modules>`
to physically exist on disk even when building a single module with
`-pl <service> -am`, so a per-service isolated build context (the
"isolated monorepo" pattern) doesn't work here; the "shared monorepo"
pattern (full repo context, service selection via a build arg) does.

### API docs: each service's Swagger UI exposed directly, not only via the gateway

The gateway proxies only `/api/v1/...` paths, unchanged (ADR 0013) - it does
not rewrite or aggregate `/q/swagger-ui`/`/q/openapi`, which every service
exposes individually (`quarkus-smallrye-openapi`, present since M6). Locally
this is a non-issue since docker-compose publishes every service's own port.
For the public demo, each of the six essential-flow Railway services gets
its own public domain (in addition to the gateway's), specifically so
`/q/swagger-ui` stays reachable per service - chosen over building a
gateway-side aggregator (more code, for a documentation surface only) or
hiding the docs entirely (a portfolio demo should let a visitor see the API
contracts directly, not just call them through the SPA).

## Findings

Preparing local screenshots for this milestone's README pass (seeding
real data through the full stack) surfaced two genuine bugs, both fixed
in place rather than worked around:

1. **`ollama`'s docker-compose healthcheck always failed**: it ran
   `wget`, but the `ollama/ollama` image ships neither `wget` nor `curl` -
   only the `ollama` binary itself (confirmed directly:
   `command -v wget curl` both fail inside the container). The container
   was healthy the entire time; only the healthcheck command was wrong.
   Fixed to `ollama list`, which exercises the real local server through
   Ollama's own client instead of assuming a tool that was never there.
   The Kubernetes manifest (`infrastructure/kubernetes/base/infra/ollama.yaml`)
   was unaffected - it already used a plain `tcpSocket` probe.
2. **`gateway` silently detached from the Docker network** after several
   `docker compose up` calls targeting partial service subsets in the
   same session - `docker network inspect` showed every other container
   attached except `gateway`, causing `UnknownHostException: redis` even
   though the `redis` container was healthy and reachable from every
   other service. Recreating the container (`docker rm` + `compose up`)
   resolved it; root cause is presumed to be state left over from an
   earlier interrupted `up` invocation in this same long-running local
   session, not a manifest defect.

## Addendum (2026-08-05): Railway trial expired, moving to Render

Railway's trial expired before backend deployment was completed. Rather
than pay to reactivate it, hosting moves to Render - which this ADR
already evaluated and rejected above, for a real reason that still
applies: **Render's free web-service tier has no free Kafka or Redis**,
and this platform's core value is the async choreography saga.

This addendum does not silently reverse that trade-off. Two honest paths
forward, neither chosen yet:

1. **Kafka-less/simplified demo on Render** - deploy `identity`, `flight`,
   `hotel`, `booking`, `gateway` without Kafka. The outbox pattern means
   booking creation still succeeds and persists correctly (the DB write
   and the Kafka publish are already decoupled, ADR 0008); only the
   downstream effects - inventory decrement, confirmation email - would
   silently not fire until Kafka exists. Cheapest, fastest, but visibly
   incomplete: `notification-service` and `payment-service` would sit
   idle.
2. **Render + an external managed Kafka** - a free tier like Upstash
   Kafka (serverless, reachable over the public internet, unlike a
   self-hosted broker) would let Render's web services keep publishing
   and consuming exactly as they do locally, no code changes, just a new
   `KAFKA_BOOTSTRAP_SERVERS`/SASL config. Preserves the real architecture
   this ADR originally paid Railway to keep; not yet evaluated for
   Render's outbound-connection limits or Upstash's free-tier throughput
   ceiling.

`render.yaml` (repo root) is prepared for path 1 today - `identity` and
`gateway` only, since neither touches Kafka - with the remaining services
commented out pending one of the two decisions above. Not yet deployed;
kept local until the user chooses a path. See
[docs/deploy/render.md](../deploy/render.md).

## Consequences

- `backend/Dockerfile.railway` is a permanent addition, reusable for any
  future Railway (or similar single-`docker build`-step platform)
  deployment of any service in this repo, not just the six in this
  milestone's scope.
- The public demo's booking flow is the platform's real architecture,
  unreduced - the choreography saga, retry/DLQ machinery, and rate
  limiting all behave exactly as documented in earlier ADRs. Only search
  (OpenSearch-backed) and the engineering assistant (Ollama-backed) are
  absent from the public deployment; both remain fully verified locally
  (ADR 0012-0013 for search, ADR 0018 for the assistant) and via
  Kubernetes manifests (ADR 0016-0017).
- As of this ADR being written, backend deployment to Railway is in
  progress, pending the user's plan activation -
  [docs/runbooks/public-demo-verification.md](../runbooks/public-demo-verification.md)
  lists every public URL this deployment will produce (frontend, gateway,
  per-service Swagger) and the golden-path test to run against them, with
  placeholders to fill in once the domains exist, following the same
  "measured, not speculative" rule every prior ADR in this project has
  held to.

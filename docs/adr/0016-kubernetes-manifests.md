# 0016 — Kubernetes manifests: Kustomize, deployed and verified against a real local cluster

- Status: Accepted
- Date: 2026-07-23

## Context

ROADMAP's M17 calls for "deployments, HPA, probes, resource requests/limits
(documented as portable target, run locally via kind/minikube)." Consistent
with every prior milestone in this project, "documented as portable target"
was not read as license to write YAML nobody runs: the manifests below were
actually applied to a real local `kind` cluster, and every design decision
recorded here - including three genuine bugs - was found by that process,
not guessed at ahead of time.

The full platform (8 backend services + MongoDB, Kafka, Redis, OpenSearch,
LocalStack) had never run under Kubernetes before this milestone; only
docker-compose (M16, ADR 0015).

## Decision

### Kustomize, plain manifests, no Helm

A `base/` of plain Deployment/Service/HPA/PVC manifests plus one `overlays/
local/` for kind-specific concerns (see below) - not a Helm chart. Nothing
here needs templating logic (no conditionals, no values passed at install
time beyond what an overlay already covers), and Kustomize's patch model
maps directly onto "same manifests everywhere, small deltas for the local
environment," which is exactly this milestone's actual need. Helm would add
a templating layer with nothing to template.

### Namespace, Service naming - reusing the docker-compose design directly

One `travel-platform` namespace for everything (no infra/apps split - there
is no multi-tenancy concern here to justify the extra `.svc.cluster.local`
plumbing). Every Service is named to match the container name every
service's `%prod` config already hardcodes (`mongodb`, `kafka`, `redis`,
`opensearch`, `localstack`, `identity-service`, ... `gateway`, from ADR
0002-0013) - exactly the same trick M16's docker-compose file used via
`container_name`. Zero application code or config changed to run under
Kubernetes; only infrastructure manifests were added.

### Probes: `/health/live` and `/health/ready`, not `/health`

Every backend service already exposes both via `quarkus-smallrye-health`
(confirmed in each service's own test suite, e.g.
`ObservabilityEndpointsTest`) - built for exactly this (M6). `readinessProbe`
uses `/health/ready`, `livenessProbe` uses `/health/live`, instead of both
pointing at the combined `/health` docker-compose's healthcheck used - that
combined endpoint conflates two different failure modes Kubernetes
otherwise treats differently (readiness failure removes a pod from load
balancing; liveness failure restarts the container).

### Resource requests/limits - base vs. local overlay

`base/` targets a real cluster and is intentionally generous. `overlays/
local/resource-patch.yaml` exists because base's total request sum didn't
fit this specific machine's Docker Desktop VM (first at ~4GiB, see
"Findings" below) - trimming those numbers _inside base_ would have made
the "real" numbers dishonest, so the overlay carries the local-only
adjustment instead, each one commented with what was actually observed
(idle memory measured via `docker stats` against M16's docker-compose
stack, then corrected further after real OOMKills on this run - see
below). A cluster with more headroom should apply `base/` directly.

### HPA - CPU-only, 70% target, 1-5 replicas

One `HorizontalPodAutoscaler` per backend service (not gateway-only),
targeting 70% CPU utilization, `minReplicas: 1`/`maxReplicas: 5` - a
starting point, not load-tested numbers (M16's load tests measured request
latency, not container CPU under sustained load). Needs
`metrics-server` to function at all - not installed by default on `kind`,
installed manually
(`kubectl apply -f .../metrics-server/.../components.yaml`) with
`--kubelet-insecure-tls` added via patch, because kind's kubelet serves a
self-signed cert metrics-server doesn't trust by default. This is a
`kind`-specific step, not part of the manifests themselves (a real cluster
typically already runs a metrics pipeline).

## Findings (all from actually running this on `kind`)

Three real bugs surfaced only by deploying to Kubernetes, none of which
docker-compose's model could have caught, because none of them are
Kubernetes-manifest mistakes in the "wrong YAML" sense - they're
Kubernetes-specific _runtime_ behavior that docker-compose has no
equivalent of at all.

1. **`search-service` crashed on every start**: `Failed to load config
value of type int for: search.opensearch.port`. Root cause: Kubernetes
   auto-injects `<SERVICE>_HOST`/`<SERVICE>_PORT` environment variables
   into every pod for every Service in the namespace (legacy Docker-links
   compatibility) - and this platform's own `opensearch` Service produced
   `OPENSEARCH_PORT=tcp://10.x.x.x:9200`, colliding with and overriding
   `search-service`'s own expected `OPENSEARCH_PORT=9200` (Quarkus's
   relaxed env-var-to-config-property binding maps both to
   `search.opensearch.port`). Fixed by setting `enableServiceLinks: false`
   on every app Deployment's pod spec (not just search-service's) - DNS
   (the Service name itself) is how every service already finds every
   other service, so these injected vars are pure risk with zero upside
   anywhere in this platform.
2. **`kafka` crash-looped indefinitely, `RegistrationResponseHandler:
channel manager timed out`, then `UnknownHostException: kafka`**. Root
   cause, in two parts: (a) `KAFKA_CONTROLLER_QUORUM_VOTERS` is set to
   `kafka:19093` (from docker-compose, unchanged) - a single-node KRaft
   broker heartbeats its own controller through that address, meaning the
   pod connects to _itself_ via the Service. With a normal `ClusterIP`
   Service, that's a self-connection through kube-proxy's hairpin NAT,
   which failed outright on this `kind`/kindnet setup. (b) Switching the
   Service to headless (`clusterIP: None`) alone wasn't enough:
   headless Services only publish DNS records for endpoints that are
   already _Ready_, but the controller-registration heartbeat that needs
   `kafka` to resolve happens _before_ the broker can pass its own
   readiness probe - a chicken-and-egg deadlock. Fixed by adding
   `publishNotReadyAddresses: true` on top of `clusterIP: None`, safe here
   specifically because this Service has exactly one possible backend
   (itself) and nothing else depends on `kafka` only resolving once
   traffic is safe to send.
3. **`kafka`'s own liveness probe (reused from docker-compose's
   healthcheck, `kafka-broker-api-versions.sh`) contributed to the above
   crash-loop**: that command starts a whole extra short-lived JVM per
   check. Tolerable as an occasional Docker healthcheck; actively harmful
   as a Kubernetes probe on a memory/CPU-constrained node already running
   12 other pods, where the probe JVM competed for the exact resources it
   was checking, slowed itself down, and got the container killed by its
   own liveness probe as a result. Replaced with a plain `tcpSocket: {
port: 19092 }` check - carries none of that cost.

## Consequences

- This machine's Docker Desktop VM needed raising from ~4GiB to 8GiB to run
  all 13 workloads together reliably - even after every fix above,
  `opensearch` and `localstack` were still getting `OOMKilled` at the
  overlay's first-pass memory limits under real concurrent contention
  (832Mi and 160Mi respectively), corrected to 1280Mi and 512Mi
  after direct observation. This was a deliberate, user-approved change to
  local Docker Desktop settings (`~/Library/Group
Containers/group.com.docker/settings-store.json`, `MemoryMiB`), not
  silently assumed - see [docs/runbooks/kubernetes-verification.md](../runbooks/kubernetes-verification.md)
  for the exact before/after numbers and the full verification log.
- The gateway is exposed via a fixed `NodePort` (30080) in the local
  overlay, forwarded to the host's 8080 by `kind-cluster.yaml`'s
  `extraPortMappings` - `http://localhost:8080` behaves identically to
  M16's docker-compose stack from a client's point of view, so the same
  manual smoke-test steps from that milestone's runbook apply unchanged.
- HPA was confirmed working, not just configured: `booking-service` and
  `payment-service` were observed scaling from 1 to 2 replicas organically
  during the end-to-end smoke test, triggered by real CPU load
  (bcrypt hashing during registration, concurrent saga processing) crossing
  the 70% target - see the runbook for the exact `kubectl get hpa` output.
- No production Java code changed in this milestone - same as M16, the
  risk surface is confined to `infrastructure/kubernetes/` and the new
  `Makefile`/`kind-cluster.yaml` scaffolding.

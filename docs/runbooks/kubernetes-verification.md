# Kubernetes deployment verification (ROADMAP M17)

Real observed results from deploying the full platform to a local `kind`
cluster on 2026-07-23 - the verification run behind
[docs/adr/0016-kubernetes-manifests.md](../adr/0016-kubernetes-manifests.md).
Environment: Apple Silicon Mac (24GB), Docker Desktop VM raised from 4GiB
to 8GiB for this run (see "Environment sizing" below), kind v0.32.0,
Kubernetes v1.36.1, kubectl v1.30.2.

## How to reproduce

```bash
# 1. Build the 8 backend images (same images docker-compose uses)
make apps-build
docker compose -f infrastructure/docker/docker-compose.yml --profile apps build

# 2. Create the cluster (forwards host 8080 -> gateway's NodePort 30080)
kind create cluster --config infrastructure/kubernetes/kind-cluster.yaml

# 3. Load the images into the kind node
for img in identity-service booking-service flight-service hotel-service \
           payment-service notification-service search-service gateway; do
  kind load docker-image "travel-platform-${img}:latest" --name travel-platform
done

# 4. Apply everything (base + local overlay)
kubectl apply -k infrastructure/kubernetes/overlays/local

# 5. metrics-server (kind-specific - HPAs are inert without it)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl -n kube-system patch deployment metrics-server --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

# 6. Wait for convergence, then smoke-test through http://localhost:8080
kubectl -n travel-platform get pods -w
```

## Final verified state

All 13 workloads Ready (both init Jobs `Complete`):

- Infra: `mongodb`, `kafka`, `redis`, `opensearch`, `localstack` - all 1/1.
- Apps: `identity-service`, `booking-service`, `flight-service`,
  `hotel-service`, `payment-service`, `notification-service`,
  `search-service`, `gateway` - all 1/1, readiness/liveness probes green
  against `/health/ready` / `/health/live`.

## End-to-end smoke test (all through the gateway at `http://localhost:8080`)

Every step below was actually executed against the running cluster, in
order, and returned the result shown:

1. `GET /health` → `200`.
2. `POST /api/v1/auth/register` → `201`, userId returned (gateway →
   identity-service → MongoDB, all in-cluster).
3. Public search before any inventory existed: `GET
/api/v1/search/flights?origin=LIS&destination=GRU` → `200 []` (gateway →
   search-service → OpenSearch).
4. Seed user elevated to `MANAGER` directly in Mongo (`kubectl exec` into
   the mongodb pod - same documented load-test-only shortcut as M16, see
   testing/load/README.md), re-login, `POST /api/v1/flights` → `201`
   (gateway → flight-service, JWT verified against identity-service's
   RS256 public key exactly as in every other environment).
5. Second (plain `USER`) account registered, logged in, `POST
/api/v1/bookings` → `201 {"bookingId": ...}`.
6. Saga completion verified in MongoDB a few seconds later: booking
   `CONFIRMED`, payment `AUTHORIZED`, notification `SENT` - the full
   choreography (booking → payment → booking confirmation → notification,
   via the in-cluster Kafka) completed with no intervention.
7. Search after seeding: the created flight came back from `GET
/api/v1/search/flights?origin=LIS&destination=GRU` - proving
   `flight-created` flowed through Kafka into search-service's OpenSearch
   index inside the cluster.

## HPA observed actually scaling (not just configured)

During the smoke test, `kubectl get hpa` showed real autoscaling activity -
`booking-service` at `cpu: 102%/70%` scaled 1→2 replicas and
`payment-service` at `40%/70%` had already scaled to 2 during saga
processing, while quiet services stayed at 1 (e.g. `flight-service`
`32%/70%`, `gateway` `8%/70%`). Registration's bcrypt hashing and
concurrent saga work is genuinely CPU-heavy enough to trip a 70% target on
these small requests - consistent with M16's finding that identity-service
registration is the platform's CPU bottleneck.

## Genuine issues found by this run (fixed in the manifests)

Details and root-cause analysis in ADR 0016; summary:

1. `enableServiceLinks: false` on every app pod - Kubernetes's auto-injected
   `OPENSEARCH_PORT=tcp://...` Service var overrode search-service's real
   `OPENSEARCH_PORT` config and crashed it on startup.
2. Kafka's Service is headless + `publishNotReadyAddresses: true` - a
   single-node KRaft broker heartbeating its own controller through its own
   Service address failed via kube-proxy hairpin NAT, and then via
   readiness-gated DNS, before both were fixed.
3. Kafka's probes are plain `tcpSocket` checks - docker-compose's
   `kafka-broker-api-versions.sh` healthcheck spawns a JVM per check, which
   under node-level resource contention got the broker killed by its own
   liveness probe.

## Environment sizing

- With Docker Desktop's default-ish 4GiB VM, the base manifests' requests
  (~3.7GiB) didn't fit alongside kube-system: `kafka` sat `Pending`
  (`Insufficient memory`) and the node thrashed. Raised to 8GiB
  (user-approved) - everything converged afterwards.
- Two OOMKill corrections were still needed at the overlay level even
  with 8GiB: `opensearch` (832Mi → 1280Mi limit; Lucene's off-heap memory
  needs real room beyond the 512m JVM heap cap) and `localstack`
  (160Mi → 512Mi limit; its API server needs far more than its idle
  footprint once it starts serving).

## Teardown

```bash
kind delete cluster --name travel-platform
```

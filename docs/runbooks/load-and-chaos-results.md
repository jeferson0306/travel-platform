# Load & chaos testing results (ROADMAP M16)

Real numbers from running `testing/load/k6/*.js` and
`testing/chaos/flight-service-latency.sh` against the full stack
(`make apps-build && make apps-up`) on a single-machine Docker Desktop setup
(Apple Silicon, arm64) - not a production-grade environment, but every
number below was actually measured, not estimated. See
[docs/adr/0015-load-and-chaos-testing.md](../adr/0015-load-and-chaos-testing.md)
for the tooling choices and scope.

## Load: public search (`search-load.js`)

20 VUs ramping over 60s, 3 requests/iteration (route search, city search,
autocomplete) - all through `gateway`.

| Run                                                                 | `gateway.rate-limit` | Result                                                                                                                                                                                                                                                            |
| ------------------------------------------------------------------- | -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Default (120 req/min per client IP)                                 | 120/min              | **91.15% failed with 429** after the first ~120 requests. All 20 k6 VUs share one source IP (the k6 container), so a burst of concurrent VUs from a single IP/NAT trips the limit almost immediately - a genuine, useful finding, not a bug: see "Finding" below. |
| Rate limit raised (`GATEWAY_RATE_LIMIT_REQUESTS_PER_MINUTE=100000`) | ~unlimited           | **100% success**, `p(95)=10ms`, avg 4.59ms, sustained ~44 req/s (throttled by the script's own `sleep(1)`, not backend capacity).                                                                                                                                 |

**Finding**: a plain per-client-IP fixed-window limit (ADR 0013) is
appropriate for protecting the platform from a single abusive client, but
a request confirmed this by direct measurement: a burst of exactly 120
requests from one IP succeeds, request 121 onward gets 429 within the
same one-minute window (reproduced directly with a 150-request curl loop:
requests 1-120 → 200, requests 121-150 → 429). Real deployments behind a
corporate NAT/shared IP would see the same effect from many distinct real
users - worth revisiting with a per-authenticated-subject limit (using the
JWT `sub` claim when present, IP otherwise) if this ever runs somewhere
that matters. Documented as a known, measured limitation, not fixed here -
out of scope for M16, which is about measuring and documenting, not
redesigning M14's rate limiter.

## Load: full booking saga (`booking-saga-load.js`)

5 VUs ramping over 40s; each iteration registers a brand-new user, logs
in, and creates a booking - driving the entire choreography saga
(booking-service → payment-service → booking-service →
notification-service, all via Kafka).

| Run                                                                                  | Result                                                                                                                                                                                                                                                                                                                                                                                                 |
| ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Default rate limit, buggy first version of the script (no backoff on a failed check) | Runaway loop: **328,123 requests in 40s** (~8,200 req/s) once the rate limit started rejecting registrations, because the script skipped its `sleep(1)` on failure. **The gateway did not crash or degrade** under this accidental stress test - `http_req_duration` stayed low (avg 311µs) throughout. Fixed in the script (always sleeps, even on failure) before it was used for the numbers below. |
| Rate limit raised, fixed script                                                      | **132 complete iterations, 100% success.** `createBooking` `p(95)=12.58ms`. Verified directly in MongoDB afterward: of the bookings created across this run and an earlier manual run, **172/172 reached `CONFIRMED`, 172/172 payments `AUTHORIZED`, 172/172 notifications `SENT`** - full saga completion, zero stuck states, zero data loss.                                                         |

**Finding**: the choreography saga (ADR 0010/0011) completes reliably
under concurrent load with no manual reconciliation needed - the
outbox/Kafka/retry-queue machinery built in M8-M12 held up under genuine
concurrent writes, not just the single-request integration tests each
service's own test suite already covered.

## Chaos: flight-service latency via Toxiproxy (`flight-service-latency.sh`)

Toxiproxy inserted between `gateway` and `flight-service` only (`gateway`'s
`GATEWAY_UPSTREAM_FLIGHT` env var repointed at the proxy); every other
backend reached directly, unaffected.

1. **Baseline**: `GET /api/v1/search/flights` → `200`, 176ms.
2. **3000ms latency injected** (exceeds `forwardIdempotent`'s 2000ms
   timeout, ADR 0014):
   - Request 1: `504` after **6.21s** - the initial call times out, both
     configured retries also time out against the still-slow proxy
     (2000ms × 3 attempts + retry delay ≈ the observed 6.21s).
   - Requests 2-8: `503` in **23ms-330ms** each - the circuit is now open
     (`requestVolumeThreshold=4`, `failureRatio=0.5` tripped by the
     failures accumulated across request 1's three internal attempts plus
     request 2), so every subsequent call fails immediately without even
     attempting a connection.
3. **Per-backend isolation, verified directly, not assumed**: while
   `flights` search kept returning `503`, `GET /api/v1/search/hotels` and
   `POST /api/v1/auth/login` both returned `200` throughout - a single
   backend's circuit opening had zero effect on any other backend, exactly
   as ADR 0014 claims (each backend gets its own independent `Guard`).
4. **Toxic removed**, waited out the 5s recovery delay: next request
   half-opens the circuit, the trial call succeeds (flight-service was
   never actually down, just slow), and the circuit closes - three
   consecutive requests immediately after recovery all returned `200` in
   18-38ms.

**Finding**: M15's design decision to build one `Guard` per backend
segment (ADR 0014), instead of one shared circuit breaker for all
non-idempotent/idempotent forwards, is not just a theoretical improvement -
this experiment demonstrates it directly. An equivalent experiment against
the single-shared-breaker design this platform explicitly rejected would
have shown `hotels` and `auth` also failing while `flights` recovered,
which did not happen here.

## Known limitations of this M16 pass

- Single-machine Docker Desktop, not a representative multi-node/cloud
  environment - throughput numbers are a lower bound on what dedicated
  infrastructure would sustain, not a capacity plan.
- Terraform was not installed on the machine these results were measured
  on; the `booking-receipts` S3 bucket was provisioned with a one-off
  `aws s3 mb`/`put-bucket-versioning` call against LocalStack instead
  (equivalent to what `infrastructure/terraform/environments/local` would
  apply) - see `testing/load/README.md`.
- Chaos testing covered one backend (`flight-service`) and one toxic
  (latency). Connection-cut/reset toxics, and chaos scenarios against
  Mongo/Kafka/Redis/OpenSearch directly, are natural follow-ups but were
  not run for this milestone.

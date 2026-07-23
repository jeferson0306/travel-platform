# 0014 — Fault tolerance: only where a real synchronous dependency exists

- Status: Accepted
- Date: 2026-07-23

## Context

ROADMAP's M15 was originally scoped as "circuit breakers, retries, timeouts,
bulkheads across all services," written under the assumption that
synchronous inter-service calls (e.g. `booking-service` checking
availability against `flight-service`/`hotel-service` directly) would exist
by this point. They do not: this platform ended up fully event-driven for
every cross-service interaction (ADR
[0004](0004-use-kafka-for-event-driven-communication.md)), and every Kafka
consumer already has its own Mongo/OpenSearch-backed retry queue and DLQ
(ADR 0004's M10 addendum, reused unchanged through M11-M13). That mechanism
already **is** this platform's fault tolerance story for the async paths -
adding SmallRye Fault Tolerance retries on top of it would be redundant at
best, actively conflicting at worst (two independent retry loops racing
each other).

An audit of the whole codebase for `RestClient`/`WebClient`/`HttpClient`
usage turned up exactly two genuine synchronous external-call sites:

1. `gateway` → each backend (the reverse proxy itself).
2. `search-service` → OpenSearch (its query engine and only store, ADR
   0012).

M15's real scope is applying fault tolerance to these two, honestly, rather
than inventing synchronous call sites elsewhere just to have somewhere to
put the annotations. Every other service gets nothing new here - not
because they were skipped, but because they have no synchronous dependency
to protect.

## Decision

### `gateway`

- `quarkus-smallrye-fault-tolerance`, used via the **programmatic** `Guard`
  API (`io.smallrye.faulttolerance.api.Guard`), not the declarative
  `@Timeout`/`@CircuitBreaker`/`@Bulkhead`/`@Retry` annotations. This was
  not the first attempt - annotations apply per _method_, not per method
  _argument_. A single `forward(segment, ...)` method annotated once would
  share one circuit breaker across all seven backends: a single struggling
  backend (say, `payment-service`) would trip the breaker for bookings,
  hotels, search, everything else too, which defeats the entire point of a
  circuit breaker (isolating one failure from the rest of the system).
  `UpstreamProxyClient` instead builds one `Guard` per backend segment
  (`Map<String, Guard>`, keyed by the same path segment `RoutingTable`
  already resolves), giving every backend fully independent
  timeout/circuit-breaker/bulkhead state.
- Split into `forwardIdempotent`/`forwardNonIdempotent` (per backend, so
  really two `Guard`s per segment): retrying a GET a struggling backend
  already received is harmless, but retrying a POST/PUT/PATCH/DELETE risks
  duplicating a side effect (creating two bookings, charging twice). Only
  the idempotent path configures a retry (2 attempts, 100ms delay).
  Timeout differs too - 2s for reads, 5s for writes, illustrative defaults,
  not load-tested numbers.
- The programmatic `Guard` API is synchronous (`Callable`/`Supplier`, or
  `CompletionStage` for async - not Mutiny `Uni`), so `ProxyRoutes`'
  `@Route` runs with `type = BLOCKING` (dispatched to a worker thread) and
  `UpstreamProxyClient` blocks on the underlying Mutiny `WebClient` call
  (`.await().indefinitely()`) inside the guarded `Callable`. A deliberate
  trade-off: fully reactive end-to-end would need bridging `Guard`'s
  `CompletionStage` support with Mutiny `Uni`, adding real complexity for a
  gateway whose primary job (routing + fault tolerance) is not raw
  throughput-critical at this project's scale.
- `ProxyRoutes` maps the resulting exception to the matching HTTP status
  instead of a blanket 502 for everything: `CircuitBreakerOpenException` →
  503, FT `TimeoutException` → 504, anything else (connection refused, DNS
  failure, ...) → 502. A fixed message per case, never
  `failure.getMessage()`, so an internal exception's class name/detail
  never leaks to an API client.

### `search-service`

- `@Timeout`/`@Retry` (plain annotations - no per-argument sharing concern
  here, since these methods only ever talk to the one OpenSearch cluster
  this service owns) on the three **read** methods only
  (`searchByRoute`, `autocomplete` x2). Not on `index()`: that write path
  already has its own durable, Mongo-independent retry/DLQ mechanism
  (`RetryRelay`/`retry_tasks` - ADR 0012); stacking SmallRye's in-process
  retry on top would be redundant. Reads are pure and side-effect-free, so
  retrying them is always safe - no idempotent/non-idempotent split needed
  the way `gateway` needs one.

## Consequences

- No fault-tolerance code was added to `identity-service`, `booking-service`,
  `flight-service`, `hotel-service`, `payment-service`, or
  `notification-service` - they have zero synchronous external
  dependencies (MongoDB access isn't a candidate: the driver has its own
  connection pooling/retry semantics, and this isn't the kind of "remote
  service call" circuit breakers exist for). Their resilience story for
  Kafka consumption was already built in M10 and reused since.
- Testing `gateway`'s fault tolerance needed genuine failure injection, not
  just HTTP status codes returned by a healthy stub: `StubUpstreamResource`
  gained delay simulation (a `X-Stub-Slow-Until-Attempt`/`X-Stub-Delay-Ms`
  header pair) to force a real `TimeoutException`, and a second test
  resource (`ClosedPortUpstreamResource`, remapping only the `hotels`
  segment to an unreachable port) drives the circuit breaker open
  deterministically without waiting out a timeout. Because each backend now
  has an independent `Guard`, these two resources' failure injection never
  interferes with the tests exercising the (separate, healthy) `bookings`
  segment - a direct, testable confirmation that the per-backend isolation
  works, not just an assertion about it.
- `search-service`'s existing test suite already covers these methods'
  happy path; no new tests were added specifically for the fault-tolerance
  annotations there, since triggering a genuine OpenSearch failure
  deterministically (as opposed to gateway's simple closed-port/delay
  tricks) would need fault injection this milestone's scope did not
  justify building.

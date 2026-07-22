# Testing (ROADMAP M5 close-out)

M5 was deliberately left open until the rest of the platform stabilized -
tightening coverage/mutation gates while services were still being built
would have meant re-tuning them on every feature. This is that close-out,
applied uniformly across all five services.

## Test pyramid, per service

- **Domain unit tests** (`domain/**/*Test.java`) - pure, no Quarkus context.
  Every aggregate invariant, value-object validation rule, and domain
  exception has a test.
- **Application unit tests** (`application/usecase/**/*Test.java`) - Mockito
  mocks for output ports, `@ExtendWith(MockitoExtension.class)`. Every
  branch a use case can take (success, not-found, already-in-that-state,
  idempotent no-op) is covered, not just the happy path.
- **Integration tests** (`@QuarkusTest`) - real MongoDB (Quarkus Dev
  Services/Testcontainers) and, where the service has consumers, the
  in-memory Kafka connector (`smallrye-in-memory`, see each service's
  `%test` profile) instead of a real broker. Cover the full HTTP
  request/response cycle including every documented error status
  (400/401/403/404/409), and the full consumer failure story: idempotent
  duplicate delivery, a malformed/unparseable payload (dropped, not
  retried), a business/technical failure (Mongo-backed retry queue), and -
  for `RetryRelay` specifically - the backoff math and the exhausted-retries
  dead-letter transition.
- **Architecture tests** (`architecture/HexagonalArchitectureTest.java`,
  ArchUnit) - the domain never depends on a framework type, dependencies
  only point inward.

### `RetryRelay` tests: driving the scheduler directly

`RetryRelay.relay()` is package-visible on purpose. Its test class (same
package) injects the bean and calls `relay()` directly instead of waiting
on its real 10s `@Scheduled` trigger - this makes the backoff-math and
dead-letter assertions deterministic instead of timing-dependent. Seed a
`retry_tasks` document with `nextAttemptAt` in the past, call `relay()`,
assert the outcome (cleared / backed off / dead-lettered).

One real bug this pattern caught: the background `@Scheduled` trigger and a
test's direct `relay()` call can race on the same overdue task and both try
to write the same dead-letter document. Fixed by making that write an
upsert instead of an insert (see each `RetryRelay.deadLetter()`) - cheap
concurrency hardening that a purely timing-based test would never have
surfaced.

## Naming and console output

Every test class and method carries a JUnit 5 `@DisplayName` - a plain
English sentence, not the camelCase method name. Combined with
`maven-surefire-plugin`'s `reportFormat=plain`/`useFile=false` (parent POM),
`mvn test` streams a readable account of what ran and passed straight to
the console instead of only to XML report files:

```
[INFO] Running BookingCreatedConsumer (flight-inventory)
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in BookingCreatedConsumer (flight-inventory)
[INFO] com.travelplatform.flight...BookingCreatedConsumerTest.decrementsAvailableSeatsOnBookingCreated -- Time elapsed: 0.08 s
```

## Coverage gate (JaCoCo)

`jacoco-maven-plugin` (parent POM `pluginManagement`) instruments every
`mvn test` run and enforces a **0.45 line-coverage floor at BUNDLE
(whole-service) level**. Both `report` and `check` are bound to the `test`
phase, not `verify` - `mvn test` is the command every workflow in this repo
already runs, so the gate needs no extra step to remember.

0.45 is a measured floor, not a target. Actual per-service numbers: identity
85.6%, booking 58.4%, flight 52.1%, hotel 50.4%, payment 49.7% - open
`target/site/jacoco/index.html` after a build for the per-package/per-class
breakdown. The bundle-wide number sits well below the domain/application
layers' own coverage (85-95%+) because it also counts framework-glue code
(Mongo document mappers, CDI producers, `RequestLoggingFilter`) that is
simple, low-risk, and already exercised indirectly by the integration
tests. Writing tests against that glue purely to inflate the number would
be coverage theater, not better testing - this gate exists to catch a
wholesale regression (a class shipped with no tests at all), not to chase a
percentage.

`search-service` overrides the floor down to 0.30 in its own `pom.xml`
(measured at 0.34) - a disproportionate share of its code is OpenSearch
client-wiring glue (index mapping bootstrap, CDI producers), already
exercised indirectly by its integration tests, not meaningfully
unit-testable in isolation. Same "floor, not target" philosophy, just a
different measured number - see docs/adr/0012-search-service-opensearch.md.

## Testing against OpenSearch - no Dev Services

Every other service's integration tests lean on Quarkus Dev Services
(MongoDB, Kafka) to provision infrastructure automatically. OpenSearch has
no Dev Services integration in this Quarkus version, so `search-service`
starts a real OpenSearch node itself via the official
`org.opensearch:opensearch-testcontainers` module, wired in as a
`QuarkusTestResourceLifecycleManager` (`OpenSearchTestResource`) shared
across its `@QuarkusTest` classes. Everything else about its tests - real
infrastructure over mocks for consumers/resources, `RetryRelay.relay()`
driven directly, `@DisplayName` everywhere - follows the same conventions
as every other service.

## Mutation testing (PIT) - currently blocked upstream

`pitest-maven` is configured in the parent POM (`domain`/`application`
packages only - the layers the unit tests actually target, where a
surviving mutant means a real behavioral gap rather than framework glue).
It is **not** bound to any lifecycle phase - PIT reruns the suite once per
surviving mutant, which is minutes rather than seconds, so it would make
`mvn test`/CI slow for no benefit on every commit. Run it on demand:

```bash
mvn -f backend/pom.xml -pl <service> -am org.pitest:pitest-maven:mutationCoverage
```

As of this writing (PIT 1.19.1, the latest release), running it against
this project fails with `Unsupported class file major version 69` - PIT's
bytecode analysis does not yet understand Java 25 class files. This is the
same category of issue already hit and fixed for ArchUnit (1.3.0 → 1.4.1)
and Spotless/google-java-format earlier in this project, but there is no
newer PIT release to bump to yet, and overriding PIT's bundled ASM
dependency to a newer version did not resolve it either (PIT appears to
resolve its bytecode-reading dependency independently of the plugin
classpath override). The configuration is left in place, ready to use the
moment upstream adds Java 25 support - revisit by simply re-running the
command above.

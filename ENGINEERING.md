# Engineering practices

How work actually happens in this repository, day to day.

## Branching & commits

Covered in full in [CONTRIBUTING.md](CONTRIBUTING.md). Summary: Git Flow with
`main`/`develop` protected, `feature/JLJS-XXXX` branches, Conventional
Commits, English everywhere.

## Definition of done

A change is done when:

1. It compiles and every test passes locally.
2. Unit tests cover the new logic; integration tests cover new adapters
   (database, Kafka, HTTP) using Testcontainers, not mocks, for anything that
   talks to real infrastructure.
3. If it changes a package dependency direction, the ArchUnit rule for that
   service still passes.
4. Structured logging is present on any new code path that can fail.
5. Documentation is updated in the same PR: README, OpenAPI/AsyncAPI spec,
   ADR, or runbook — whichever applies.
6. CI is green: lint, tests, coverage threshold, dependency/secret/container
   scans.

Nothing merges to `develop` short of this list. Nothing merges to `main`
except through a release PR from a stable `develop`.

## Testing strategy

| Level                        | Tool                                           | Runs                                                |
| ---------------------------- | ---------------------------------------------- | --------------------------------------------------- |
| Unit                         | JUnit 5, Mockito, AssertJ                      | every build, seconds                                |
| Integration                  | Testcontainers (Mongo, Kafka, Redis), WireMock | every PR                                            |
| Architecture                 | ArchUnit                                       | every PR                                            |
| Contract                     | OpenAPI validation, Pact                       | every PR touching an API                            |
| Mutation                     | PIT                                            | scheduled / pre-release, not on every PR (too slow) |
| Load / stress / spike / soak | k6, Gatling                                    | scheduled, pre-release                              |
| Chaos                        | Toxiproxy                                      | scheduled, pre-release                              |

Rationale for tiering: fast feedback loops stay in the PR gate; expensive
suites (mutation, load, chaos) run on a schedule or before a release so the
PR loop stays fast without skipping the checks entirely.

## Preventing bugs, not just fixing them

- **Static analysis as a gate, not a suggestion**: Checkstyle, PMD, SpotBugs,
  Error Prone, SonarQube run in CI; violations above the configured severity
  fail the build.
- **Coverage floor**: JaCoCo enforces a minimum (target >90% on domain and
  application layers) — coverage on adapters is a side effect, not a target
  in itself, to avoid tests written only to hit a number.
- **Mutation testing** validates that the test suite actually detects
  breakage, not just that it runs.
- **ArchUnit** stops architectural erosion (a domain class importing a
  framework type) at PR time, not at a much more expensive later refactor.

## Finding and fixing production issues

Every request carries a `traceId`/`spanId`/`correlationId` from the gateway
through every downstream service and Kafka message. The debugging path for
any incident is: alert → dashboard (which service, which metric) → trace
(which request, which hop failed) → structured logs filtered by `traceId` →
root cause. This path is documented per-failure-mode in
[docs/runbooks](docs/runbooks) as those runbooks are written.

## Pipeline philosophy

The CI pipeline (introduced in Phase 1, documented as it is built in
[docs/development](docs/development)) is designed to fail fast: cheap,
high-signal checks (compile, lint, unit tests) run before expensive ones
(integration tests, security scans, image builds). Independent checks run in
parallel. A red pipeline blocks merge — there is no override on `develop` or
`main` outside of an admin hotfix.

## Scaling

Horizontal scaling is the default (stateless services behind the gateway,
session state in Redis, not in memory). Vertical scaling (JVM heap, container
memory/CPU requests-limits) is tuned per service based on load-test data, not
guessed — documented per service once load testing exists (Phase 4).

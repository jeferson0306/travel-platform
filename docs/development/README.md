# Development guides

Per-service "how to run, test, and extend this service" guides, the shared
service template new services are scaffolded from, and CI pipeline
documentation. See [testing.md](testing.md) for the test pyramid, naming
conventions, and the coverage/mutation-testing gates (ROADMAP M5).

## CI pipeline (`.github/workflows/ci.yml`)

Runs on every push/PR to `main` and `develop`, alongside the formatting
checks in `lint.yml`. Ordered fail-fast: cheap checks that don't need a
build run immediately in parallel with the test suite; the Docker image is
only built and scanned once tests pass.

| Job               | What                                                                                                                                                                | Blocking?                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `test`            | `mvn test` for the whole reactor - unit, integration (real MongoDB/Kafka via Quarkus Dev Services and the in-memory connector), architecture (ArchUnit) per service | Yes                                               |
| `secret-scan`     | gitleaks against the full history                                                                                                                                   | Yes                                               |
| `dependency-scan` | Trivy filesystem scan per service (matrix), for vulnerable dependencies                                                                                             | Report-only for now (see below)                   |
| `dockerfile-lint` | hadolint per service's `Dockerfile.jvm` (matrix)                                                                                                                    | Yes, on errors (warnings don't block)             |
| `docker`          | Package, build the JVM image, Trivy image scan, per service (matrix)                                                                                                | Build/package yes; image scan report-only for now |

Per-service jobs (`dependency-scan`, `dockerfile-lint`, `docker`) use a
build matrix - adding a service means adding one line to that job's
`matrix.service` list, not duplicating the job.

"Report-only for now": `dependency-scan` and the image-scan step in `docker`
run with `exit-code: 0` until a reviewed `.trivyignore` baseline exists -
flipping them to blocking before establishing a baseline would just fail
the very first run on pre-existing transitive-dependency CVEs nobody has
triaged yet. Tightening this is tracked alongside the M5 test-pyramid
close-out (mutation testing, coverage gate) rather than done piecemeal
mid-feature-development.

## Adding a new service

Not templated as a generator yet - copy an existing service's structure
(package layout, `pom.xml` parent reference, `.mvn/wrapper`,
`RequestLoggingFilter`, canonical `ErrorResponse`/exception mappers) as the
starting point. Then:

1. Add the new module to `backend/pom.xml`'s `<modules>`.
2. Add it to `ci.yml`'s three `matrix.service` lists
   (`dependency-scan`, `dockerfile-lint`, `docker`).
3. Fix the Dockerfile's base image - the Quarkus-generated default targets
   Java 21, not this project's Java 25 (see ADR 0002); copy the fix from
   `identity-service` or `booking-service`'s `Dockerfile.jvm`.

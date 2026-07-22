# Development guides

Per-service "how to run, test, and extend this service" guides, the shared
service template new services are scaffolded from, and CI pipeline
documentation.

## CI pipeline (`.github/workflows/ci.yml`)

Runs on every push/PR to `main` and `develop`, alongside the formatting
checks in `lint.yml`. Ordered fail-fast: cheap checks that don't need a
build run immediately in parallel with the test suite; the Docker image is
only built and scanned once tests pass.

| Job               | What                                                                                                                                                                 | Blocking?                                         |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `test`            | `mvn test` for `identity-service` - unit, integration (real MongoDB via Quarkus Dev Services, Docker is available on GitHub-hosted runners), architecture (ArchUnit) | Yes                                               |
| `secret-scan`     | gitleaks against the full history                                                                                                                                    | Yes                                               |
| `dependency-scan` | Trivy filesystem scan for vulnerable dependencies                                                                                                                    | Report-only for now (see below)                   |
| `dockerfile-lint` | hadolint against `Dockerfile.jvm`                                                                                                                                    | Yes, on errors (warnings don't block)             |
| `docker`          | Package, build the JVM image, Trivy image scan                                                                                                                       | Build/package yes; image scan report-only for now |

"Report-only for now": `dependency-scan` and the image-scan step in `docker`
run with `exit-code: 0` until a reviewed `.trivyignore` baseline exists -
flipping them to blocking before establishing a baseline would just fail
the very first run on pre-existing transitive-dependency CVEs nobody has
triaged yet. Tightening this is tracked alongside the M5 test-pyramid
close-out (mutation testing, coverage gate) rather than done piecemeal
mid-feature-development.

## Adding a new service

Not templated as a generator yet - copy `identity-service`'s structure
(package layout, `pom.xml` parent reference, `.mvn/wrapper`) as the starting
point, and add the new module to `backend/pom.xml`'s `<modules>` plus a job
in `ci.yml` once the service exists.

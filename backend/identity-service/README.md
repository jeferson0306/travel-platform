# identity-service

Authentication and user identity. Owns user registration, credential
verification, and access token issuance. See
[ARCHITECTURE.md](../../ARCHITECTURE.md) for how this fits into the rest of
the platform and the package layout every service follows.

## Run locally

```bash
# from the repo root
make up   # starts MongoDB (and the rest of the local infra)
cd backend/identity-service
./mvnw quarkus:dev
```

The service listens on `:8081`. OpenAPI/Swagger UI is under `/openapi` and
`/q/swagger-ui`; health checks under `/health`. The Dev UI is available in
dev mode at `/q/dev/`.

## Endpoints (Phase 1 / M4 scope)

| Method | Path                     | Purpose                     |
| ------ | ------------------------ | ---------------------------- |
| POST   | `/api/v1/auth/register`  | Register a new user          |
| POST   | `/api/v1/auth/login`     | Authenticate, receive a JWT  |

## Test

```bash
./mvnw test
```

Unit tests cover the domain and application layers with no infrastructure
dependency. `AuthResourceTest` exercises registration and login over real
HTTP against a real MongoDB, started automatically by Quarkus Dev Services
(Testcontainers) when Docker is available - no mocks. `HexagonalArchitectureTest`
enforces with ArchUnit that dependencies only point inward and that the
domain package never references a framework type.

## Configuration

See `src/main/resources/application.yml`. Notable environment variables
(see [.env.example](../../.env.example)):

- `MONGO_URI` - MongoDB connection string (only read in `%prod`; local/dev
  and tests use Dev Services or the Docker Compose instance).
- `JWT_SECRET` - HMAC secret used to sign issued tokens. The committed
  default is an insecure, clearly-labeled local-only fallback - every real
  environment must override it.

## Packaging

```bash
./mvnw package                                    # target/quarkus-app/quarkus-run.jar
./mvnw package -Dquarkus.package.jar.type=uber-jar
./mvnw package -Dnative                           # requires GraalVM, or:
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

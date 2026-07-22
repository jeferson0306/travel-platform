# hotel-service

Hotel inventory and search. See [ARCHITECTURE.md](../../ARCHITECTURE.md)
for how this fits into the platform.

## Run locally

```bash
# from the repo root
make up   # starts MongoDB, ...
cd backend/hotel-service
./mvnw quarkus:dev
```

The service listens on `:8084`. OpenAPI/Swagger UI under `/openapi` and
`/q/swagger-ui`; health under `/health`.

## Endpoints (Phase 2 / M9 scope)

| Method | Path                            | Auth                                              | Purpose        |
| ------ | -------------------------------- | --------------------------------------------------- | ---------------- |
| GET    | `/api/v1/hotels?city=`           | None (public)                                        | Search hotels  |
| POST   | `/api/v1/hotels`                 | Bearer JWT, role `MANAGER`/`ADMIN`/`SUPER_ADMIN`     | Create a hotel |

Authorization is enforced by validating the JWT `identity-service` issues -
see [Configuration](#configuration) below and
[docs/adr/0006-rbac-roles.md](../../docs/adr/0006-rbac-roles.md).

## Test

```bash
./mvnw test
```

Unit tests cover the domain and application layers with no infrastructure
dependency. `HotelResourceTest` exercises search and create end to end over
HTTP against a real MongoDB (Quarkus Dev Services), minting real JWTs with
`src/test/resources/privateKey.pem` (mirrors identity-service's signing
key) - including the 401/403 paths. `HexagonalArchitectureTest` enforces
the same layering rule as every other service.

## Configuration

See `src/main/resources/application.yml`. Notable environment variables
(see [.env.example](../../.env.example)):

- `MONGO_URI` - MongoDB connection string (only read in `%prod`).

Token verification uses `src/main/resources/publicKey.pem` (RS256), the
public half of identity-service's signing key - a committed dev/test
fixture, not sensitive by design. A real deployment points
`mp.jwt.verify.publickey.location` at its own public key.

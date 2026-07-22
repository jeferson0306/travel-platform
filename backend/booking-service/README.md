# booking-service

Reservation lifecycle: create and cancel bookings, publishing
`booking-created`/`booking-cancelled` domain events via a transactional
outbox. See [ARCHITECTURE.md](../../ARCHITECTURE.md) for how this fits into
the platform and [docs/adr/0007-transactional-outbox.md](../../docs/adr/0007-transactional-outbox.md)
for how the outbox actually works.

## Run locally

```bash
# from the repo root
make up   # starts MongoDB (replica-set enabled), Kafka (with topics created), ...
cd backend/booking-service
./mvnw quarkus:dev
```

The service listens on `:8082`. OpenAPI/Swagger UI under `/openapi` and
`/q/swagger-ui`; health under `/health`.

## Endpoints (Phase 2 / M8 scope)

| Method | Path                        | Purpose           |
| ------ | ---------------------------- | ------------------ |
| POST   | `/api/v1/bookings`           | Create a booking   |
| POST   | `/api/v1/bookings/{id}/cancel` | Cancel a booking |

`travelerId` is trusted client input for now - there is no Gateway/JWT
enforcement in front of this service yet (ROADMAP M14).

## Test

```bash
./mvnw test
```

Unit tests cover the domain and application layers with no infrastructure
dependency. `BookingResourceTest` exercises the full path end to end: a real
MongoDB (Quarkus Dev Services, replica-set-enabled so the outbox
transaction actually runs) and the in-memory Reactive Messaging connector
in place of a real Kafka broker, asserting the published event lands on the
correct topic (`booking-created` vs `booking-cancelled`) - see
`application.yml`'s `%test` profile. `HexagonalArchitectureTest` enforces
the same layering rule as every other service.

## Configuration

See `src/main/resources/application.yml`. Notable environment variables
(see [.env.example](../../.env.example)):

- `MONGO_URI` - MongoDB connection string (only read in `%prod`).
- `KAFKA_BOOTSTRAP_SERVERS` - only read in `%prod`; local/dev/test never hit
  a real broker (Dev Services is explicitly disabled - see
  `quarkus.kafka.devservices.enabled: false` - because tests use the
  in-memory connector and local dev uses docker-compose's Kafka).

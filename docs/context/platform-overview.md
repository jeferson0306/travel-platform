# Platform overview (AI-context layer, ROADMAP M18)

One-page factual map of the platform for AI agents and new engineers.
Everything here is derived from the code as it exists - when this file and
the code disagree, the code wins and this file has a bug.

## What this is

An event-driven travel platform: 8 Quarkus (Java 25) microservices behind
a single gateway, communicating exclusively via Kafka events (no
synchronous service-to-service REST anywhere - ADR 0004, verified by audit
in ADR 0014). Bookings drive a choreography saga:
booking → payment authorization → booking confirmation → notification.

## Services

| Service                | Port | Datastore              | Auth on its API                       | Purpose                                                    |
| ---------------------- | ---- | ---------------------- | ------------------------------------- | ---------------------------------------------------------- |
| `gateway`              | 8080 | Redis (rate limits)    | JWT fast-fail + rate limit (ADR 0013) | Single entry point; reverse proxy by first path segment    |
| `identity-service`     | 8081 | MongoDB `identity`     | Public register/login                 | Users, bcrypt passwords, RS256 JWT issuance (ADR 0006)     |
| `booking-service`      | 8082 | MongoDB `booking`      | None at service level (trusted input) | Create/cancel bookings; saga orchestrating aggregate       |
| `flight-service`       | 8083 | MongoDB `flight`       | Create: MANAGER+; search: public      | Flight inventory; decrements seats on booking-created      |
| `hotel-service`        | 8084 | MongoDB `hotel`        | Create: MANAGER+; search: public      | Hotel inventory; decrements rooms on booking-created       |
| `payment-service`      | 8085 | MongoDB `payment`      | Read: SUPPORT/ADMIN+                  | Simulated payment authorization/refund (saga step)         |
| `notification-service` | 8086 | MongoDB `notification` | Read: SUPPORT/ADMIN+                  | Booking confirmation email (simulated); saga terminal step |
| `search-service`       | 8087 | OpenSearch only        | Fully public                          | Flight/hotel search + autocomplete projection (ADR 0012)   |

Infra: MongoDB (single-node replica set `rs0` - transactions for the
outbox), Kafka (single KRaft broker, `kafka:19092` in-network), Redis,
OpenSearch, LocalStack (S3 `booking-receipts` bucket, ADR 0008/0009).

## HTTP API surface (all through `gateway` at `/api/v1/...`)

- `POST /api/v1/auth/register`, `POST /api/v1/auth/login` - public.
- `POST /api/v1/bookings`, `POST /api/v1/bookings/{id}/cancel` - no
  service-level auth; `travelerId`/`travelerEmail`/`amount` are trusted
  client input (documented gap, see BookingResource's own javadoc).
- `POST /api/v1/flights` (MANAGER/ADMIN/SUPER_ADMIN), `GET /api/v1/flights?origin&destination` - public.
- `POST /api/v1/hotels` (MANAGER/ADMIN/SUPER_ADMIN), `GET /api/v1/hotels` - public.
- `GET /api/v1/payments/{bookingId}` (SUPPORT/ADMIN/SUPER_ADMIN).
- `GET /api/v1/notifications/{bookingId}` (SUPPORT/ADMIN/SUPER_ADMIN).
- `GET /api/v1/search/flights`, `GET /api/v1/search/hotels` - public,
  route/city params or `q=` prefix autocomplete.
- Every service: `/health`, `/health/ready`, `/health/live`, `/q/metrics`.

There is no GET-booking-by-id endpoint and no self-service role elevation

- both are known, deliberate gaps (see ADR 0015's RBAC note).

## The saga (ADR 0010/0011)

1. `POST /bookings` → booking `PENDING`, `booking-created` published (via
   the transactional outbox, ADR 0007).
2. `payment-service` consumes it, simulates authorization, publishes
   `payment-authorized` or `payment-failed`.
3. `booking-service` consumes the outcome: confirm (`CONFIRMED`, raises
   `booking-confirmed`) or compensate (cancel).
4. `notification-service` consumes `booking-confirmed`, sends the email,
   stores a `Notification`.
   Meanwhile `flight-service`/`hotel-service` consume `booking-created` to
   decrement inventory, and `search-service` consumes
   `flight-created`/`hotel-created` to index.

## How to run

- Inner loop: `make up` (infra only) + `mvn quarkus:dev` per service.
- Full stack, Docker: `make apps-build && make apps-up` (ADR 0015).
- Full stack, Kubernetes: `infrastructure/kubernetes/` + kind (ADR 0016,
  reproduction steps in docs/runbooks/kubernetes-verification.md).

## Deeper references

- Per-service details: [service-catalog.md](service-catalog.md)
- Kafka topics and payloads: [event-catalog.md](event-catalog.md) and
  [docs/events](../events)
- Rules any change must follow: [conventions.md](conventions.md)
- Design decisions: [docs/adr](../adr) (0001-0016, all Accepted)

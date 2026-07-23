# Load testing (ROADMAP M16)

k6 scripts exercising the platform through `gateway`, the only entry point a
real client would use. Results from running these are in
[docs/runbooks/load-and-chaos-results.md](../../docs/runbooks/load-and-chaos-results.md) -
measured, not estimated.

## Prerequisites

1. Full stack running: `make apps-build && make apps-up` (see the root
   `Makefile` and `infrastructure/docker/docker-compose.yml`).
2. LocalStack's `booking-receipts` S3 bucket provisioned (Terraform, or see
   the manual `aws s3 mb` fallback in `docs/runbooks/load-and-chaos-results.md`
   if Terraform isn't installed).
3. Seed data - `search-load.js` needs at least one indexed flight/hotel,
   `booking-saga-load.js` needs a flight id to book against. Both need a
   `MANAGER`-role user to create inventory (public registration only grants
   `USER` - ADR 0006), which nothing in this platform's API can self-elevate
   to, so it's done directly against Mongo for a **local load-testing seed
   only**, never a pattern for anything user-facing:

   ```bash
   # Register a normal user, then elevate it directly in Mongo (load-test-only, see above).
   curl -s -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" \
     -d '{"email":"loadtest@example.com","password":"Sup3rSecret!","fullName":"Load Test"}'
   docker exec travel-platform-mongodb mongosh --quiet --eval '
     db.getSiblingDB("identity").users.updateOne({email:"loadtest@example.com"}, {$set:{role:"MANAGER"}})'

   # Log in again to get a token carrying the new role, then create a flight and hotel for real
   # through the gateway - this exercises the actual create-inventory path, not a Mongo insert,
   # so flight-created/hotel-created still flow through Kafka into search-service exactly like a
   # real request would.
   TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
     -d '{"email":"loadtest@example.com","password":"Sup3rSecret!"}' | jq -r .accessToken)
   curl -s -X POST http://localhost:8080/api/v1/flights -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" -d '{"origin":"LIS","destination":"GRU",
     "departureAt":"2026-08-15T10:00:00Z","arrivalAt":"2026-08-15T20:00:00Z",
     "priceAmount":450.00,"priceCurrency":"EUR","availableSeats":150}'
   ```

## Running

Without installing k6 locally, via its official image on the same Docker
network as the stack:

```bash
docker run --rm -i --network travel-platform -e BASE_URL=http://gateway:8080 \
  grafana/k6 run - < testing/load/k6/search-load.js

docker run --rm -i --network travel-platform \
  -e BASE_URL=http://gateway:8080 -e FLIGHT_ID=<the flight id from setup> \
  grafana/k6 run - < testing/load/k6/booking-saga-load.js
```

## Scripts

- `search-load.js` - public route/city search + autocomplete, ramping to 20
  VUs. No auth, safe to push harder than this if you want to.
- `booking-saga-load.js` - a full user journey (register, login, create a
  booking), which drives the entire choreography saga (booking → payment →
  booking confirmation → notification, all via Kafka) for every iteration.
  Kept to 5 VUs deliberately: each iteration also registers a brand-new user
  in identity-service, which is the actual bottleneck (bcrypt hashing is
  intentionally slow - see docs/adr/0006-rbac-roles.md), not booking
  creation itself.

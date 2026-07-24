#!/usr/bin/env bash
# Seeds realistic flight/hotel inventory through the real gateway API, so anyone testing the
# frontend locally (make apps-up) sees actual search results instead of an empty list.
#
# Uses the same role-elevation shortcut documented in testing/load/README.md: public
# registration only grants USER (ADR 0006), and nothing in this platform's API can
# self-elevate to MANAGER, so the seed account's role is set directly in Mongo. This is a
# local-only, dev-seed pattern - never how a real user gets MANAGER - and the account itself
# (fake name/email, fixed password) only exists in your local Mongo volume, never committed
# or pushed anywhere.
#
# Safe to re-run: flight/hotel creation isn't idempotent at the API level, so re-running adds
# a second copy of each listing rather than erroring - harmless for local demo data, but if
# you want a clean slate first, run `make destroy` to wipe local volumes.
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
MONGO_CONTAINER="${MONGO_CONTAINER:-travel-platform-mongodb}"
SEED_EMAIL="${SEED_EMAIL:-demo-seed@example.test}"
SEED_PASSWORD="${SEED_PASSWORD:-Sup3rSecret!}"

command -v jq >/dev/null 2>&1 || { echo "jq is required (brew install jq)"; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "curl is required"; exit 1; }

echo "==> Registering seed account (ignored if it already exists)"
curl -s -X POST "$GATEWAY_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$SEED_EMAIL\",\"password\":\"$SEED_PASSWORD\",\"fullName\":\"Demo Seed\"}" \
  >/dev/null || true

echo "==> Elevating seed account to MANAGER (local Mongo only, dev-seed pattern, see script header)"
docker exec "$MONGO_CONTAINER" mongosh --quiet --eval "
  db.getSiblingDB('identity').users.updateOne({email:'$SEED_EMAIL'}, {\$set:{role:'MANAGER'}})" \
  >/dev/null

echo "==> Logging in"
TOKEN=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$SEED_EMAIL\",\"password\":\"$SEED_PASSWORD\"}" | jq -r .accessToken)

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "Login failed - is the stack up? (make apps-build && make apps-up)" >&2
  exit 1
fi

create_flight() {
  curl -s -X POST "$GATEWAY_URL/api/v1/flights" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "$1" | jq -c '. + {ok: (.flightId != null)}'
}

create_hotel() {
  curl -s -X POST "$GATEWAY_URL/api/v1/hotels" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "$1" | jq -c '. + {ok: (.hotelId != null)}'
}

echo "==> Creating flights"
create_flight '{"origin":"LIS","destination":"GRU","departureAt":"2026-09-10T09:00:00Z","arrivalAt":"2026-09-10T19:30:00Z","priceAmount":589.00,"priceCurrency":"EUR","availableSeats":120}'
create_flight '{"origin":"LIS","destination":"JFK","departureAt":"2026-09-15T22:00:00Z","arrivalAt":"2026-09-16T06:15:00Z","priceAmount":432.50,"priceCurrency":"EUR","availableSeats":80}'
create_flight '{"origin":"OPO","destination":"LHR","departureAt":"2026-09-05T07:20:00Z","arrivalAt":"2026-09-05T09:10:00Z","priceAmount":128.00,"priceCurrency":"EUR","availableSeats":150}'
create_flight '{"origin":"LIS","destination":"CDG","departureAt":"2026-09-08T14:00:00Z","arrivalAt":"2026-09-08T17:20:00Z","priceAmount":165.90,"priceCurrency":"EUR","availableSeats":95}'
create_flight '{"origin":"LIS","destination":"MAD","departureAt":"2026-09-12T11:00:00Z","arrivalAt":"2026-09-12T12:40:00Z","priceAmount":89.90,"priceCurrency":"EUR","availableSeats":60}'

echo "==> Creating hotels"
create_hotel '{"name":"Lisbon Riverside Hotel","city":"Lisbon","pricePerNightAmount":142.00,"pricePerNightCurrency":"EUR","availableRooms":18}'
create_hotel '{"name":"Porto Old Town Inn","city":"Porto","pricePerNightAmount":98.50,"pricePerNightCurrency":"EUR","availableRooms":12}'
create_hotel '{"name":"Sao Paulo Business Suites","city":"Sao Paulo","pricePerNightAmount":210.00,"pricePerNightCurrency":"EUR","availableRooms":30}'
create_hotel '{"name":"New York Midtown Hotel","city":"New York","pricePerNightAmount":315.00,"pricePerNightCurrency":"EUR","availableRooms":22}'
create_hotel '{"name":"Madrid Central Boutique Hotel","city":"Madrid","pricePerNightAmount":124.00,"pricePerNightCurrency":"EUR","availableRooms":15}'

echo "==> Done. Search e.g. LIS -> GRU or city 'Lisbon' in the frontend to see results."
echo "    (search-service indexes asynchronously via Kafka - if a listing doesn't show up"
echo "    immediately, wait a couple of seconds and search again.)"

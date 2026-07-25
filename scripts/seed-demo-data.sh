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
create_flight '{"origin":"LIS","destination":"GRU","departureAt":"2026-09-10T09:00:00Z","arrivalAt":"2026-09-10T19:30:00Z","priceAmount":589.00,"priceCurrency":"EUR","availableSeats":120,"airline":"TAP Air Portugal","airlineCode":"TP","flightNumber":"TP123","cabinClass":"ECONOMY","stops":0}'
create_flight '{"origin":"LIS","destination":"JFK","departureAt":"2026-09-15T22:00:00Z","arrivalAt":"2026-09-16T06:15:00Z","priceAmount":432.50,"priceCurrency":"EUR","availableSeats":80,"airline":"Delta Air Lines","airlineCode":"DL","flightNumber":"DL245","cabinClass":"ECONOMY","stops":0}'
create_flight '{"origin":"OPO","destination":"LHR","departureAt":"2026-09-05T07:20:00Z","arrivalAt":"2026-09-05T09:10:00Z","priceAmount":128.00,"priceCurrency":"EUR","availableSeats":150,"airline":"TAP Air Portugal","airlineCode":"TP","flightNumber":"TP1361","cabinClass":"ECONOMY","stops":0}'
create_flight '{"origin":"LIS","destination":"CDG","departureAt":"2026-09-08T14:00:00Z","arrivalAt":"2026-09-08T17:20:00Z","priceAmount":165.90,"priceCurrency":"EUR","availableSeats":95,"airline":"Air France","airlineCode":"AF","flightNumber":"AF1682","cabinClass":"PREMIUM_ECONOMY","stops":0}'
create_flight '{"origin":"LIS","destination":"MAD","departureAt":"2026-09-12T11:00:00Z","arrivalAt":"2026-09-12T12:40:00Z","priceAmount":89.90,"priceCurrency":"EUR","availableSeats":60,"airline":"Iberia","airlineCode":"IB","flightNumber":"IB3172","cabinClass":"ECONOMY","stops":0}'
create_flight '{"origin":"LIS","destination":"GRU","departureAt":"2026-09-18T20:30:00Z","arrivalAt":"2026-09-19T09:15:00Z","priceAmount":1240.00,"priceCurrency":"EUR","availableSeats":24,"airline":"LATAM Airlines","airlineCode":"LA","flightNumber":"LA8181","cabinClass":"BUSINESS","stops":1}'

echo "==> Creating hotels"
create_hotel '{"name":"Lisbon Riverside Hotel","city":"Lisbon","pricePerNightAmount":142.00,"pricePerNightCurrency":"EUR","availableRooms":18,"address":"Avenida Ribeira das Naus 10","starRating":4,"amenities":["Free WiFi","Breakfast included","River view","Air conditioning"],"description":"A riverside stay in the heart of Lisbon, steps from the tram lines and pastel-facade streets.","reviewScore":8.7,"reviewCount":642}'
create_hotel '{"name":"Porto Old Town Inn","city":"Porto","pricePerNightAmount":98.50,"pricePerNightCurrency":"EUR","availableRooms":12,"address":"Rua das Flores 88","starRating":3,"amenities":["Free WiFi","Breakfast included"],"description":"A cosy inn tucked into Porto old town, close to the port wine cellars.","reviewScore":8.2,"reviewCount":301}'
create_hotel '{"name":"Sao Paulo Business Suites","city":"Sao Paulo","pricePerNightAmount":210.00,"pricePerNightCurrency":"EUR","availableRooms":30,"address":"Avenida Paulista 1500","starRating":5,"amenities":["Free WiFi","Pool","Gym","Business center","Airport shuttle"],"description":"Modern suites on Avenida Paulista, built for business travelers who still want a pool.","reviewScore":9.1,"reviewCount":1204}'
create_hotel '{"name":"New York Midtown Hotel","city":"New York","pricePerNightAmount":315.00,"pricePerNightCurrency":"EUR","availableRooms":22,"address":"7th Avenue 250","starRating":4,"amenities":["Free WiFi","Gym","Rooftop bar"],"description":"Midtown Manhattan, a short walk from Times Square and the theater district.","reviewScore":8.5,"reviewCount":978}'
create_hotel '{"name":"Madrid Central Boutique Hotel","city":"Madrid","pricePerNightAmount":124.00,"pricePerNightCurrency":"EUR","availableRooms":15,"address":"Calle Gran Via 45","starRating":4,"amenities":["Free WiFi","Breakfast included","Bar"],"description":"A boutique stay on Gran Via, close to the Prado and late-night tapas.","reviewScore":8.9,"reviewCount":517}'

echo "==> Done. Search e.g. LIS -> GRU or city 'Lisbon' in the frontend to see results."
echo "    (search-service indexes asynchronously via Kafka - if a listing doesn't show up"
echo "    immediately, wait a couple of seconds and search again.)"

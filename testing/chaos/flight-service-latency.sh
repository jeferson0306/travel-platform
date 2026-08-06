#!/usr/bin/env bash
# Chaos experiment (ROADMAP M16): injects latency in front of flight-service via Toxiproxy and
# observes gateway's per-backend circuit breaker (ADR 0014) actually trip and recover - not a
# simulation, real HTTP calls against a real running stack. Results from running this are in
# docs/runbooks/load-and-chaos-results.md.
#
# Prerequisites: full stack up (`make apps-up`) and toxiproxy up
# (`docker compose -f infrastructure/docker/docker-compose.yml --profile chaos up -d toxiproxy`).
#
# This script points gateway's "flight" upstream at Toxiproxy instead of flight-service directly
# by recreating the gateway container with an env var override - docker-compose's default wiring
# (flight-service reached directly) is restored at the end regardless of how the script exits.
set -euo pipefail

NETWORK="travel-platform"
TOXIPROXY_API="http://localhost:8474"
GATEWAY_URL="http://localhost:8080"
COMPOSE="docker compose -f infrastructure/docker/docker-compose.yml --env-file .env"

cleanup() {
  echo "--- restoring gateway to its normal docker-compose configuration ---"
  docker stop gateway >/dev/null 2>&1 || true
  docker rm gateway >/dev/null 2>&1 || true
  $COMPOSE --profile apps up -d gateway >/dev/null
  curl -s -X DELETE "$TOXIPROXY_API/proxies/flight-service" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "--- creating a toxiproxy proxy in front of flight-service ---"
curl -s -X POST "$TOXIPROXY_API/proxies" -H "Content-Type: application/json" -d '{
  "name": "flight-service", "listen": "0.0.0.0:18999", "upstream": "flight-service:8083"
}'
echo

echo "--- pointing gateway's flight upstream at the proxy ---"
docker stop gateway >/dev/null 2>&1 || true
docker rm gateway >/dev/null 2>&1 || true
docker run -d --name gateway --network "$NETWORK" -p 8080:8080 \
  -e REDIS_URL=redis://redis:6379 \
  -e GATEWAY_UPSTREAM_FLIGHT=http://toxiproxy:18999 \
  travel-platform-gateway:latest >/dev/null
until curl -sf "$GATEWAY_URL/health" >/dev/null 2>&1; do sleep 2; done

echo "--- baseline: flights and hotels both healthy ---"
curl -s -o /dev/null -w "flights: HTTP %{http_code}, %{time_total}s\n" \
  "$GATEWAY_URL/api/v1/search/flights?origin=LIS&destination=GRU"
curl -s -o /dev/null -w "hotels:  HTTP %{http_code}, %{time_total}s\n" \
  "$GATEWAY_URL/api/v1/search/hotels?city=Lisbon"

echo "--- injecting 3000ms latency (exceeds the 2000ms idempotent-path timeout) ---"
curl -s -X POST "$TOXIPROXY_API/proxies/flight-service/toxics" -H "Content-Type: application/json" -d '{
  "name": "latency-toxic", "type": "latency", "stream": "downstream",
  "attributes": {"latency": 3000, "jitter": 0}
}'
echo

echo "--- flights: first call times out + retries (slow), then the circuit opens (fast 503s) ---"
for i in $(seq 1 6); do
  t0=$(date +%s.%N)
  code=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/api/v1/search/flights?origin=LIS&destination=GRU")
  t1=$(date +%s.%N)
  printf "  request %d: HTTP %s, %.2fs\n" "$i" "$code" "$(echo "$t1 - $t0" | bc)"
done

echo "--- hotels and auth stay completely unaffected while flights is broken (per-backend isolation) ---"
curl -s -o /dev/null -w "hotels: HTTP %{http_code}\n" "$GATEWAY_URL/api/v1/search/hotels?city=Lisbon"
curl -s -o /dev/null -w "search flights again (still open): HTTP %{http_code}\n" \
  "$GATEWAY_URL/api/v1/search/flights?origin=LIS&destination=GRU"

echo "--- removing the toxic ---"
curl -s -X DELETE "$TOXIPROXY_API/proxies/flight-service/toxics/latency-toxic"
echo

echo "--- waiting out the circuit breaker's 5s recovery delay ---"
sleep 6

echo "--- flights: circuit half-opens, trial succeeds, back to normal ---"
for i in 1 2 3; do
  t0=$(date +%s.%N)
  code=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/api/v1/search/flights?origin=LIS&destination=GRU")
  t1=$(date +%s.%N)
  printf "  recovery request %d: HTTP %s, %.2fs\n" "$i" "$code" "$(echo "$t1 - $t0" | bc)"
done

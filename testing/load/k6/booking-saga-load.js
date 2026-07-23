// Full user journey through the gateway: register -> login -> create a booking, which drives
// the whole choreography saga (booking-service -> payment-service -> booking-service ->
// notification-service, via Kafka - ADR 0010/0011). Each VU registers its own user, so this
// exercises identity-service's write path too, not just booking creation.
//
// Needs an existing flight to book against - pass its id via FLIGHT_ID (see
// testing/load/README.md for how it was seeded for the numbers in
// docs/runbooks/load-and-chaos-results.md).
//
// Run: k6 run -e FLIGHT_ID=<uuid> testing/load/k6/booking-saga-load.js

import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const FLIGHT_ID = __ENV.FLIGHT_ID;

export const options = {
  scenarios: {
    booking_saga: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 5 },
        { duration: "20s", target: 5 },
        { duration: "10s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    "http_req_duration{name:createBooking}": ["p(95)<1000"],
  },
};

export function setup() {
  if (!FLIGHT_ID) {
    throw new Error("FLIGHT_ID env var is required - see testing/load/README.md");
  }
}

export default function () {
  const email = `k6-vu${__VU}-iter${__ITER}-${Date.now()}@example.com`;
  const password = "Sup3rSecret!";

  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ email, password, fullName: "K6 Load Test" }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "register" } },
  );
  // sleep(1) always runs, even on failure - a client that retries a 429/5xx with no backoff at
  // all is a pathological case (an earlier, buggy version of this script that skipped sleep()
  // on failure produced an accidental ~8,200 req/s runaway loop against a 5-VU scenario; the
  // gateway held up under it without crashing, which is itself a real data point, but it is not
  // representative traffic, so this script deliberately never removes the backoff).
  const registered = check(registerRes, { "register: 201": (r) => r.status === 201 });
  if (!registered) {
    sleep(1);
    return;
  }
  const userId = JSON.parse(registerRes.body).userId;

  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "login" } },
  );
  const loggedIn = check(loginRes, { "login: 200": (r) => r.status === 200 });
  if (!loggedIn) {
    sleep(1);
    return;
  }
  const token = JSON.parse(loginRes.body).accessToken;

  const bookingRes = http.post(
    `${BASE_URL}/api/v1/bookings`,
    JSON.stringify({
      travelerId: userId,
      travelerEmail: email,
      itemType: "FLIGHT",
      itemId: FLIGHT_ID,
      quantity: 1,
      amount: 450.0,
      currency: "EUR",
    }),
    {
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      tags: { name: "createBooking" },
    },
  );
  check(bookingRes, { "createBooking: 201": (r) => r.status === 201 });

  sleep(1);
}

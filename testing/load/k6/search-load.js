// Public, read-only search traffic through the gateway - no auth, safe to ramp hard.
// Requires the gateway + search-service to be up with at least one flight/hotel indexed
// (see testing/load/README.md for the seed commands used to produce the numbers in
// docs/runbooks/load-and-chaos-results.md).
//
// Run: k6 run testing/load/k6/search-load.js
//   (or, without installing k6: docker run --rm -i --network travel-platform \
//     -e BASE_URL=http://gateway:8080 grafana/k6 run - < testing/load/k6/search-load.js)

import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    search_ramp: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 20 },
        { duration: "30s", target: 20 },
        { duration: "15s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

export default function () {
  const flightRes = http.get(`${BASE_URL}/api/v1/search/flights?origin=LIS&destination=GRU`);
  check(flightRes, {
    "flight search: 200": (r) => r.status === 200,
  });

  const hotelRes = http.get(`${BASE_URL}/api/v1/search/hotels?city=Lisbon`);
  check(hotelRes, {
    "hotel search: 200": (r) => r.status === 200,
  });

  const autocompleteRes = http.get(`${BASE_URL}/api/v1/search/flights?q=LI`);
  check(autocompleteRes, {
    "flight autocomplete: 200": (r) => r.status === 200,
  });

  sleep(1);
}

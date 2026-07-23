# Public demo verification

How to test the public demo once it is live (ADR
[0019](../adr/0019-public-demo-deployment.md)). Deployment is currently
pending - the user has to add a paid Railway plan first (Railway's free
trial expired mid-milestone) - so the URLs below are placeholders showing
the _shape_ each one will take, not live links yet. Fill in the real
values here the moment `railway domain` and the Vercel deploy return them,
and update the root [README.md](../../README.md) "Live demo" section to
match.

## The links

| What                         | URL pattern                                                       | Purpose                                                        |
| ---------------------------- | ----------------------------------------------------------------- | -------------------------------------------------------------- |
| Frontend (Vercel)            | `https://travel-platform.vercel.app`                              | The actual demo - register, search, book.                      |
| Gateway (Railway)            | `https://gateway-<hash>.up.railway.app`                           | Public API entry point. Every frontend call goes through this. |
| identity-service Swagger     | `https://identity-service-<hash>.up.railway.app/q/swagger-ui`     | API contract for register/login.                               |
| booking-service Swagger      | `https://booking-service-<hash>.up.railway.app/q/swagger-ui`      | API contract for bookings.                                     |
| payment-service Swagger      | `https://payment-service-<hash>.up.railway.app/q/swagger-ui`      | API contract for the payment leg of the saga.                  |
| flight-service Swagger       | `https://flight-service-<hash>.up.railway.app/q/swagger-ui`       | API contract for flight inventory/search.                      |
| hotel-service Swagger        | `https://hotel-service-<hash>.up.railway.app/q/swagger-ui`        | API contract for hotel inventory/search.                       |
| notification-service Swagger | `https://notification-service-<hash>.up.railway.app/q/swagger-ui` | API contract for the saga's terminal step.                     |

Each Railway service gets its own public domain in addition to the
gateway's - the gateway only proxies `/api/v1/...` and does not expose
`/q/swagger-ui` (see ADR 0019's "API docs" decision). `search-service` and
`assistant-service` are not deployed publicly at all (OpenSearch/Ollama
don't fit a free tier) - their Swagger UIs only exist when running the
full stack locally (`make apps-up`, then e.g.
`http://localhost:8087/q/swagger-ui` for search-service).

Health endpoints follow the same per-service pattern, e.g.
`https://identity-service-<hash>.up.railway.app/health/ready` - useful for
confirming a service is actually up before testing against it, without
generating real traffic.

## Golden-path test (do this first, every time)

Run this against the frontend URL - it exercises the real choreography
saga end to end, not just individual endpoints:

1. Open the frontend URL. Register a new account (any email - identity-service
   doesn't verify addresses).
2. Log in - lands on `/search`.
3. Search flights (any origin/destination - inventory is whatever was
   seeded; empty results are a valid outcome, not a bug, if nothing
   matches).
4. Search hotels (any city) the same way.
5. Click "Book" on a result. This calls `POST /api/v1/bookings` through
   the gateway with a JWT.
6. The confirmation page is intentionally static (no
   `GET /api/v1/bookings/{id}` exists on the platform - a documented gap,
   see `docs/context/platform-overview.md`). The real saga
   (booking-created -> payment authorization -> booking-confirmed ->
   notification sent) runs asynchronously via Kafka regardless of what the
   page shows.
7. To actually confirm the saga completed (not just that the booking
   request was accepted), check `notification-service`'s data or
   `booking-service`'s own state directly - there is no public endpoint
   for this by design; use `railway logs --service booking-service` or
   `railway logs --service notification-service` to see the saga's Kafka
   consumers process the events.

## Testing individual services directly

Each service's Swagger UI (table above) is a live "try it out" console
against the real deployed service - useful for testing something the
frontend doesn't cover (e.g. `GET /api/v1/flights` with specific query
params) without going through the SPA. Requests still need a JWT for
anything but public search/register/login - get one via
identity-service's `/api/v1/auth/login` in its own Swagger UI first, then
paste it into the "Authorize" button on any other service's Swagger UI.

## What is deliberately not publicly testable

- `search-service` (autocomplete/geo search) - local-only, no OpenSearch
  in the public deployment.
- `assistant-service` (engineering Q&A) - local-only, no Ollama in the
  public deployment.
- Kafka topics, MongoDB documents, Redis rate-limit counters directly -
  no public access to any datastore, by design (defense in depth, ADR
  0013). Use `railway logs`/`railway run` for backend-side inspection
  instead.

## Before publishing the URLs above

- [ ] Replace every `<hash>` placeholder with the real Railway-assigned
      domain (`railway domain list --service <svc> --json`).
- [ ] Confirm each service's `/health/ready` returns 200 before linking
      its Swagger UI publicly.
- [ ] Run the golden-path test above against the real URLs, not just
      locally.
- [ ] Update the root README's "Live demo" section with the frontend URL
      (the only link most visitors need) and a link back to this runbook
      for anyone who wants to exercise individual services.
- [ ] Set `VITE_API_BASE_URL` on the Vercel project to the gateway's
      Railway URL before deploying the frontend - it defaults to
      `http://localhost:8080`.

# frontend

React + TypeScript + Vite single-page application. Talks to the backend only
through the API Gateway (see [ARCHITECTURE.md](../ARCHITECTURE.md)) - never
directly to a backend service.

Minimal by design (ROADMAP M20): register, log in, search flights/hotels,
create a booking. No i18n framework - a demo companion this small doesn't
warrant one; UI text is plain English strings in the components.

## Pages

- `/register`, `/login` - identity-service's public endpoints.
- `/search` - public flight/hotel search (`GET /api/v1/flights`,
  `GET /api/v1/hotels`, both unauthenticated at the service level) plus a
  "Book" action per result (`POST /api/v1/bookings`, requires a JWT).
- `/booking/:bookingId` - static confirmation. There is no
  GET-booking-by-id endpoint on the platform (a documented gap, see
  `docs/context/platform-overview.md`), so this page cannot poll status -
  it explains that the real choreography saga (payment, confirmation,
  notification) runs asynchronously via Kafka regardless.

## Run locally

```bash
cp .env.example .env.local   # VITE_API_BASE_URL, defaults to http://localhost:8080
npm install
npm run dev
```

Needs the gateway (and whatever it fronts) reachable at `VITE_API_BASE_URL`

- `make apps-up` from the repo root brings up the full stack locally.

## Deploy

Static build (`npm run build`) deployed to Vercel, pointed at the
Railway-hosted gateway via `VITE_API_BASE_URL`. See the root
[README.md](../README.md) for the live demo link and ROADMAP M20 for the
essential-flow scope the public backend deployment covers.

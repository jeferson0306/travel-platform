# Deploying the backend to Render

Status: **prepared, not yet deployed** - kept local intentionally while a
Kafka-hosting decision is made. See
[docs/adr/0019-public-demo-deployment.md](../adr/0019-public-demo-deployment.md)'s
2026-08-05 addendum for the full trade-off this migration reopens.

## Why Render, and what it doesn't solve

Railway's trial expired (2026-08-04) before the original plan (full stack,
paid Railway - see ADR 0019) was finished. Render is a genuinely free
alternative for web services, but its free tier has no managed Kafka - the
exact reason ADR 0019 rejected it the first time. This migration does not
pretend that gap away; `render.yaml` only defines `identity-service` and
`gateway` today because those are the two essential-flow services that
never touch Kafka.

Two ways to close the gap, neither chosen yet:

1. Deploy the rest Kafka-less (booking still works via the outbox pattern;
   inventory decrement and confirmation emails just won't fire).
2. Point every service's `KAFKA_BOOTSTRAP_SERVERS` at a free external
   broker (Upstash Kafka is the leading candidate - reachable over the
   internet, no self-hosting needed) and deploy the full saga unchanged.

## Prerequisites

- A Render account (free, no card required for the plans used here).
- MongoDB Atlas cluster already provisioned (ROADMAP M20, done) - have its
  connection string ready.
- The Vercel frontend URL, to set as `CORS_ORIGINS` (already deployed -
  see the README's Live demo section).

## Steps (once a Kafka decision is made and this is actually run)

1. Push this branch/main to GitHub if not already (Render Blueprints read
   from a connected Git repo, not local files).
2. In the Render dashboard: **New > Blueprint**, point it at this repo,
   Render reads `render.yaml` from the root automatically.
3. For each service Render creates, fill in the `sync: false` env vars it
   prompts for (`MONGO_URI` per service, from Atlas - each service should
   get its own database name via the connection string's path segment,
   matching `quarkus.mongodb.database` in that service's
   `application.yml`).
4. Deploy. Render builds `backend/Dockerfile.railway` with
   `SERVICE_MODULE=<service>` per service - same multi-stage build
   Railway would have used, this file is provider-agnostic.
5. Once `aerostay-gateway` has a public URL, set the frontend's
   `VITE_API_BASE_URL` (Vercel project env var) to it and redeploy the
   frontend.
6. Update this repo's `frontend/src/config/services.ts` per-service
   `VITE_*_HEALTH_URL` env vars (set in Vercel) to the new Render URLs so
   `/status` starts reporting real public health instead of `localhost`.
7. Update the README's Live demo section and
   [docs/runbooks/public-demo-verification.md](../runbooks/public-demo-verification.md)
   with the real URLs, replacing the placeholders.

## Known limitation while only identity + gateway are deployed

The gateway will proxy `/api/v1/flights`, `/api/v1/hotels`,
`/api/v1/bookings` to services that don't exist yet publicly - those
calls will fail. Only `/api/v1/auth/*` (identity-service) will work
end-to-end until the Kafka decision unblocks the rest.

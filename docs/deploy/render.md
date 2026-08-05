# Deploying the backend to Render

Status: **prepared, not yet deployed** - kept local intentionally until the
user provisions a CloudAMQP account. See
[docs/adr/0019-public-demo-deployment.md](../adr/0019-public-demo-deployment.md)
and [docs/adr/0004-use-kafka-for-event-driven-communication.md](../adr/0004-use-kafka-for-event-driven-communication.md),
both with 2026-08-05 addenda, for the full trade-off history.

## Why Render works now

Railway's trial expired (2026-08-04) before the original plan (full stack,
paid Railway - see ADR 0019) was finished. Render is genuinely free for web
services, but was rejected once already because its free tier has no
managed Kafka. That gap is closed differently than originally planned: the
platform's messaging backbone itself moved from Kafka to RabbitMQ (ADR
0004's addendum), and CloudAMQP has a real, permanent free RabbitMQ tier
(1M messages/month) - no paid host needed for either the app services or
the broker. `render.yaml` now defines all six essential-flow services from
ADR 0019 (`identity`, `flight`, `hotel`, `booking`, `payment`,
`notification`, plus `gateway`).

## Prerequisites

- A Render account (free, no card required for the plans used here).
- A CloudAMQP account (free "Little Lemur" plan, no card required) -
  create one instance, copy its AMQP URL.
- MongoDB Atlas cluster already provisioned (ROADMAP M20, done) - have its
  connection string ready.
- The Vercel frontend URL, to set as `CORS_ORIGINS` (already deployed -
  see the README's Live demo section).

## Steps (not yet run)

1. Create a CloudAMQP instance and copy its AMQP URL - it has the shape
   `amqps://<user>:<password>@<host>/<vhost>`. Split it into the four
   `RABBITMQ_HOST`/`RABBITMQ_PORT`/`RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD`
   values each service's `render.yaml` entry expects (port is `5671` for
   CloudAMQP's TLS endpoint, not the local dev default of `5672` - set
   `RABBITMQ_PORT` explicitly and confirm `rabbitmq-ssl=true` gets set too,
   since CloudAMQP requires TLS; local dev's `%prod` block does not set
   this, add it as an extra Render env var,
   `QUARKUS_RABBITMQ_SSL: "true"`, when this is actually run).
2. Push this branch/main to GitHub if not already (Render Blueprints read
   from a connected Git repo, not local files).
3. In the Render dashboard: **New > Blueprint**, point it at this repo,
   Render reads `render.yaml` from the root automatically.
4. For each service Render creates, fill in the `sync: false` env vars it
   prompts for (`MONGO_URI` per service, from Atlas - each service should
   get its own database name via the connection string's path segment,
   matching `quarkus.mongodb.database` in that service's
   `application.yml`; the four `RABBITMQ_*` vars from step 1, same values
   across every service since they all share one CloudAMQP instance).
5. Deploy. Render builds `backend/Dockerfile.railway` with
   `SERVICE_MODULE=<service>` per service - same multi-stage build
   Railway would have used, this file is provider-agnostic.
6. Once `aerostay-gateway` has a public URL, set the frontend's
   `VITE_API_BASE_URL` (Vercel project env var) to it and redeploy the
   frontend.
7. Update this repo's `frontend/src/config/services.ts` per-service
   `VITE_*_HEALTH_URL` env vars (set in Vercel) to the new Render URLs so
   `/status` starts reporting real public health instead of `localhost`.
8. Update the README's Live demo section and
   [docs/runbooks/public-demo-verification.md](../runbooks/public-demo-verification.md)
   with the real URLs, replacing the placeholders.

## Verified locally before this plan was written

The full choreography saga was run end-to-end against the local
docker-compose stack (real RabbitMQ, no mocks): a booking decremented
flight inventory by exactly the booked quantity, was authorized by
payment-service, confirmed by booking-service, and triggered a
notification-service confirmation email - all over RabbitMQ. The only
open question for Render specifically is CloudAMQP's TLS requirement
(step 1) and its free-tier message-volume ceiling under real traffic,
neither of which can be verified until this is actually deployed.

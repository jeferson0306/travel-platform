# C4 — Level 2: Containers

```mermaid
C4Container
    title Container Diagram — Travel Platform

    Person(traveler, "Traveler")

    System_Boundary(platform, "Travel Platform") {
        Container(spa, "Web App", "React, TypeScript, Vite", "Search, booking and account UI")
        Container(gateway, "API Gateway", "Kong/Traefik", "Routing, JWT validation, rate limiting, CORS")

        Container(identity, "identity-service", "Quarkus", "Authentication, authorization, sessions")
        Container(booking, "booking-service", "Quarkus", "Reservation lifecycle, outbox")
        Container(payment, "payment-service", "Quarkus", "Payment authorization/capture, idempotency, saga")
        Container(flight, "flight-service", "Quarkus", "Flight inventory & pricing")
        Container(hotel, "hotel-service", "Quarkus", "Hotel inventory & pricing")
        Container(currency, "currency-service", "Quarkus", "FX rates & conversion")
        Container(notification, "notification-service", "Quarkus", "Email/SMS/push delivery")
        Container(search, "search-service", "Quarkus", "Autocomplete, fuzzy & geo search")

        ContainerDb(mongo, "MongoDB", "Document store", "Per-service private collections")
        ContainerDb(redis, "Redis", "Cache / sessions / rate limiting")
        ContainerDb(opensearch, "OpenSearch", "Search index")
        Container(kafka, "Kafka", "Event backbone", "booking-created, payment-approved, ...")
        Container(localstack, "LocalStack", "AWS emulation", "S3, SQS, SNS, SES, Secrets Manager")
    }

    Rel(traveler, spa, "Uses", "HTTPS")
    Rel(spa, gateway, "Calls", "HTTPS/JSON")
    Rel(gateway, identity, "Routes", "HTTPS")
    Rel(gateway, booking, "Routes", "HTTPS")
    Rel(gateway, flight, "Routes", "HTTPS")
    Rel(gateway, hotel, "Routes", "HTTPS")
    Rel(gateway, search, "Routes", "HTTPS")

    Rel(booking, flight, "Checks availability", "HTTPS")
    Rel(booking, hotel, "Checks availability", "HTTPS")
    Rel(booking, payment, "Requests payment (saga)", "HTTPS/Kafka")
    Rel(booking, currency, "Converts amounts", "HTTPS")

    Rel(booking, kafka, "Publishes booking-*", "Kafka")
    Rel(payment, kafka, "Publishes payment-*", "Kafka")
    Rel(notification, kafka, "Consumes *-requested/*-created", "Kafka")
    Rel(search, kafka, "Consumes flight/hotel updates", "Kafka")

    Rel(identity, mongo, "Reads/writes", "MongoDB driver")
    Rel(booking, mongo, "Reads/writes", "MongoDB driver")
    Rel(payment, mongo, "Reads/writes", "MongoDB driver")
    Rel(flight, mongo, "Reads/writes", "MongoDB driver")
    Rel(hotel, mongo, "Reads/writes", "MongoDB driver")

    Rel(identity, redis, "Sessions, JWT blacklist, rate limit")
    Rel(gateway, redis, "Rate limit")

    Rel(search, opensearch, "Indexes & queries")
    Rel(notification, localstack, "Sends via SES")
```

## Notes

- No service reads another service's MongoDB collections directly — all
  cross-service data access is via REST API or Kafka event.
- `gateway` is the only container reachable from outside the platform
  boundary besides the SPA build artifacts themselves.
- This diagram reflects the target shape. Containers are implemented
  incrementally per [ROADMAP.md](../../ROADMAP.md); until a service exists,
  it is documented here as planned, not as running.

# C4 — Level 1: System Context

```mermaid
C4Context
    title System Context — Travel Platform

    Person(traveler, "Traveler", "Searches, books and manages trips")
    Person(admin, "Operator", "Monitors and administers the platform")

    System(platform, "Travel Platform", "Search, book, pay for, and manage flights, hotels and reservations")

    System_Ext(paymentNet, "Payment network", "Authorizes and captures card payments")
    System_Ext(emailProvider, "Email provider (SES via LocalStack)", "Delivers transactional email")
    System_Ext(mapsGeo, "Geo/Maps data", "Airport/city/hotel location data")

    Rel(traveler, platform, "Searches, books, pays, manages reservations", "HTTPS")
    Rel(admin, platform, "Monitors, operates", "HTTPS / Grafana")
    Rel(platform, paymentNet, "Authorizes & captures payments", "HTTPS")
    Rel(platform, emailProvider, "Sends confirmations & notifications", "SMTP/API")
    Rel(platform, mapsGeo, "Resolves locations for search", "HTTPS")
```

## Actors

- **Traveler** — the end user of the platform: searches inventory, books,
  pays, manages their reservations and loyalty status.
- **Operator** — internal user who monitors system health and operates the
  platform (dashboards, incident response).

## External systems

- **Payment network** — simulated locally; production integration point for
  a real payment processor.
- **Email provider** — SES, emulated locally through LocalStack.
- **Geo/Maps data** — reference data source for airports, cities, hotel
  locations, used by `search-service`.

See [container.md](container.md) for the next level of detail (services
inside the platform boundary).

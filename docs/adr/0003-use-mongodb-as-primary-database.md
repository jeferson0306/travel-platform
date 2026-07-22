# 0003 — Use MongoDB as the primary database

- Status: Accepted
- Date: 2026-07-22

## Context

Each service needs a datastore for its own bounded context. Candidates
considered: PostgreSQL (one instance per service, or shared instance with
separate schemas), MongoDB, and a polyglot mix chosen per service.

Domain shapes involved are naturally document-like and heterogeneous across
services: user profiles with nested preferences, flight/hotel inventory with
variable, provider-dependent attributes, bookings that aggregate snapshots of
flight/hotel/payment data at time of booking. Several of these do not need
cross-entity joins in the hot path, and schema shape varies faster early on
than a relational migration workflow comfortably absorbs.

## Decision

Use **MongoDB**, one logical database per service (never shared collections
across service boundaries — see [ARCHITECTURE.md](../../ARCHITECTURE.md)),
run locally via the official `mongo` Docker image.

Per-service considerations documented as they are built:

- Indexes are explicit and reviewed, not implicit.
- TTL indexes are used for time-bound data (e.g. session/cache-like
  collections) instead of manual cleanup jobs.
- Aggregation pipelines are used for read models instead of pulling documents
  into application code to transform them.
- Sharding and replica-set topology are documented as the target production
  configuration in [docs/deploy](../deploy), even though local development
  runs a single node.

Rejected alternatives:

- **PostgreSQL** — a reasonable and arguably safer default, but chosen
  against here specifically to demonstrate deliberate NoSQL data modeling
  (aggregation pipelines, document design, denormalization trade-offs)
  rather than mapping relational habits onto a document store. Relational
  concepts (transactions, joins) are addressed explicitly in
  [docs/business-rules](../business-rules) where MongoDB requires a different
  approach (e.g. multi-document transactions within a replica set, or the
  outbox pattern instead of a cross-table transaction).
- **Polyglot per service** — adds operational surface (backup, monitoring,
  expertise) disproportionate to the benefit at this project's scale; a
  single well-understood store used well beats several stores used
  shallowly.

## Consequences

- Cross-service consistency must be handled explicitly (outbox, saga,
  eventual consistency) rather than relying on a shared relational
  transaction — this is intentional and documented per use case.
- Schema evolution is flexible but not free: every collection's shape is
  documented in [docs/business-rules](../business-rules) and validated at
  the application boundary (Hibernate Validator on DTOs), not left implicit.
- MongoDB Atlas free tier is the target for the deployed demo; local
  development always runs against the containerized instance.

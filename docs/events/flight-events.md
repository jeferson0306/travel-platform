# Flight events

See [docs/adr/0012-search-service-opensearch.md](../adr/0012-search-service-opensearch.md)
for why this event and its consumer exist.

## `flight-created`

Published by `flight-service` when a flight is created
(`POST /api/v1/flights`). Raised by `Flight.create()` via the same generic
outbox mechanism `booking-service`'s events already use - no separate
publish code path. Flights have no update lifecycle yet (see `Flight`'s own
class comment), so this is the only flight event.

| Field            | Type                   | Meaning                                           |
| ---------------- | ---------------------- | ------------------------------------------------- |
| `flightId`       | object (`FlightId`)    | The new flight's id                               |
| `origin`         | object (`AirportCode`) | Departure airport (IATA code)                     |
| `destination`    | object (`AirportCode`) | Arrival airport (IATA code)                       |
| `departureAt`    | timestamp              | Scheduled departure                               |
| `arrivalAt`      | timestamp              | Scheduled arrival                                 |
| `price`          | object (`Money`)       | Ticket price                                      |
| `availableSeats` | int                    | Seats available at creation time                  |
| `airline`        | string                 | Operating airline's name, e.g. "TAP Air Portugal" |
| `airlineCode`    | string                 | Operating airline's IATA code, e.g. "TP"          |
| `flightNumber`   | string                 | e.g. "TP123"                                      |
| `cabinClass`     | string, nullable       | e.g. "ECONOMY"/"BUSINESS" - freeform, not an enum |
| `stops`          | int                    | Number of stops (0 = nonstop)                     |
| `occurredOn`     | timestamp              | When the flight was created                       |

Consumer: `search-service` (consumer group `search-indexer`) - indexes the
flight for route/autocomplete search (ROADMAP M13). No idempotency claim
collection needed: indexing by `flightId` is an upsert, so a duplicate
delivery is a harmless overwrite - see ADR 0012.

**Known gap**: `airline`/`airlineCode`/`flightNumber`/`cabinClass`/`stops`
were added to enrich search-result display (the frontend calls
`flight-service` directly, not `search-service` - see
`docs/context/platform-overview.md`). `search-service`'s own
`SearchableFlight`/`FlightSearchDocument`/`FlightSearchResponse` were left
as they were and do **not** carry these fields yet - a deliberate,
documented gap rather than an oversight, since the frontend doesn't query
`search-service` today.

## Delivery guarantees

At-least-once, same transactional-outbox mechanism as every other service's
events (ADR 0007) - `flight-service` writes its own `flights` + `outbox`
collections in one MongoDB transaction, relayed to Kafka by its own
`OutboxRelay`.

## Failure story

`search-service`'s `flight-created` consumer:

- **Idempotency**: none needed - see above.
- **Technical failure** (OpenSearch unavailable): recorded in an OpenSearch
  `retry_tasks` index (not MongoDB - this service has none, see ADR 0012),
  retried with exponential backoff, moved to `dead_letters` and published to
  `flight-created.search-indexer.dlq` after too many attempts.
- **Malformed message**: logged and dropped immediately.

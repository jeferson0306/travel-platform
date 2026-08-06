# Hotel events

See [docs/adr/0012-search-service-opensearch.md](../adr/0012-search-service-opensearch.md)
for why this event and its consumer exist. Mirrors
[docs/events/flight-events.md](flight-events.md).

## `hotel-created`

Published by `hotel-service` when a hotel is created
(`POST /api/v1/hotels`). Raised by `Hotel.create()` via the same generic
outbox mechanism `booking-service`'s events already use. Hotels have no
update lifecycle yet (see `Hotel`'s own class comment), so this is the only
hotel event.

| Field            | Type                 | Meaning                                                     |
| ---------------- | -------------------- | ----------------------------------------------------------- |
| `hotelId`        | object (`HotelId`)   | The new hotel's id                                          |
| `name`           | object (`HotelName`) | Hotel name                                                  |
| `city`           | object (`City`)      | Freeform city (no geo data yet)                             |
| `pricePerNight`  | object (`Money`)     | Price per night                                             |
| `availableRooms` | int                  | Rooms available at creation time                            |
| `address`        | string, nullable     | Short street address                                        |
| `starRating`     | int (1-5)            | Star rating                                                 |
| `amenities`      | list of string       | e.g. ["Free WiFi", "Pool"] - never null, empty list if none |
| `description`    | string, nullable     | Short blurb                                                 |
| `reviewScore`    | double, nullable     | e.g. 8.7 - absent means no reviews yet                      |
| `reviewCount`    | int                  | Number of reviews backing `reviewScore`                     |
| `occurredOn`     | timestamp            | When the hotel was created                                  |

Consumer: `search-service` (consumer group `search-indexer`) - indexes the
hotel for city/autocomplete search (ROADMAP M13). No idempotency claim
collection needed - same free-idempotency reasoning as `flight-created`.

**Known gap**: `address`/`starRating`/`amenities`/`description`/
`reviewScore`/`reviewCount` were added to enrich search-result display (the
frontend calls `hotel-service` directly, not `search-service`). Same
documented gap as `flight-created` - `search-service`'s own
`SearchableHotel`/`HotelSearchDocument`/`HotelSearchResponse` don't carry
these fields yet.

## Delivery guarantees

At-least-once, same transactional-outbox mechanism as every other service's
events (ADR 0007).

## Failure story

Identical shape to `flight-created`'s consumer (see
[docs/events/flight-events.md](flight-events.md)), against its own
`hotel-created.search-indexer.dlq` DLQ topic.

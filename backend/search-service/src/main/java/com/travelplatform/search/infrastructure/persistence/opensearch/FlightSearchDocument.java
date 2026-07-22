package com.travelplatform.search.infrastructure.persistence.opensearch;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire shape stored in the "flights" OpenSearch index - the Jackson counterpart to {@link
 * com.travelplatform.search.domain.flight.SearchableFlight}, kept separate so the domain type never
 * depends on the storage format.
 */
public class FlightSearchDocument {

    public String flightId;
    public String origin;
    public String destination;
    public Instant departureAt;
    public Instant arrivalAt;
    public BigDecimal priceAmount;
    public String priceCurrency;
    public int availableSeats;

    public FlightSearchDocument() {}

    public FlightSearchDocument(
            String flightId,
            String origin,
            String destination,
            Instant departureAt,
            Instant arrivalAt,
            BigDecimal priceAmount,
            String priceCurrency,
            int availableSeats) {
        this.flightId = flightId;
        this.origin = origin;
        this.destination = destination;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.priceAmount = priceAmount;
        this.priceCurrency = priceCurrency;
        this.availableSeats = availableSeats;
    }
}

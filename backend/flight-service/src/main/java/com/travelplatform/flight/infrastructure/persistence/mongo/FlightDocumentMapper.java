package com.travelplatform.flight.infrastructure.persistence.mongo;

import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import com.travelplatform.flight.domain.flight.FlightId;
import com.travelplatform.flight.domain.flight.Money;
import java.math.BigDecimal;
import java.util.Date;
import org.bson.Document;

public final class FlightDocumentMapper {

    private FlightDocumentMapper() {}

    public static Document toDocument(Flight flight) {
        return new Document("_id", flight.id().value().toString())
                .append("origin", flight.origin().value())
                .append("destination", flight.destination().value())
                .append("departureAt", Date.from(flight.departureAt()))
                .append("arrivalAt", Date.from(flight.arrivalAt()))
                .append("priceAmount", flight.price().amount().toPlainString())
                .append("priceCurrency", flight.price().currency())
                .append("availableSeats", flight.availableSeats())
                .append("airline", flight.airline())
                .append("airlineCode", flight.airlineCode())
                .append("flightNumber", flight.flightNumber())
                .append("cabinClass", flight.cabinClass())
                .append("stops", flight.stops());
    }

    public static Flight toDomain(Document document) {
        return Flight.reconstitute(
                FlightId.of(document.getString("_id")),
                new AirportCode(document.getString("origin")),
                new AirportCode(document.getString("destination")),
                document.getDate("departureAt").toInstant(),
                document.getDate("arrivalAt").toInstant(),
                new Money(
                        new BigDecimal(document.getString("priceAmount")),
                        document.getString("priceCurrency")),
                document.getInteger("availableSeats"),
                document.getString("airline"),
                document.getString("airlineCode"),
                document.getString("flightNumber"),
                document.getString("cabinClass"),
                document.getInteger("stops", 0));
    }
}

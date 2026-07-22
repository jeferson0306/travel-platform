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
                .append("availableSeats", flight.availableSeats());
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
                document.getInteger("availableSeats"));
    }
}

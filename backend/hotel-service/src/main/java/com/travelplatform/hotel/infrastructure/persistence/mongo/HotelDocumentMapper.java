package com.travelplatform.hotel.infrastructure.persistence.mongo;

import com.travelplatform.hotel.domain.hotel.City;
import com.travelplatform.hotel.domain.hotel.Hotel;
import com.travelplatform.hotel.domain.hotel.HotelId;
import com.travelplatform.hotel.domain.hotel.HotelName;
import com.travelplatform.hotel.domain.hotel.Money;
import java.math.BigDecimal;
import org.bson.Document;

public final class HotelDocumentMapper {

    private HotelDocumentMapper() {}

    public static Document toDocument(Hotel hotel) {
        return new Document("_id", hotel.id().value().toString())
                .append("name", hotel.name().value())
                .append("city", hotel.city().value())
                .append("pricePerNightAmount", hotel.pricePerNight().amount().toPlainString())
                .append("pricePerNightCurrency", hotel.pricePerNight().currency())
                .append("availableRooms", hotel.availableRooms());
    }

    public static Hotel toDomain(Document document) {
        return Hotel.reconstitute(
                HotelId.of(document.getString("_id")),
                new HotelName(document.getString("name")),
                new City(document.getString("city")),
                new Money(
                        new BigDecimal(document.getString("pricePerNightAmount")),
                        document.getString("pricePerNightCurrency")),
                document.getInteger("availableRooms"));
    }
}

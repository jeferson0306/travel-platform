package com.travelplatform.booking.infrastructure.persistence.mongo;

import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.BookingStatus;
import com.travelplatform.booking.domain.booking.ItemType;
import com.travelplatform.booking.domain.booking.TravelerId;
import java.util.Date;
import org.bson.Document;

/** Converts between the {@link Booking} aggregate and its MongoDB representation. */
public final class BookingDocumentMapper {

    private BookingDocumentMapper() {}

    public static Document toDocument(Booking booking) {
        return new Document("_id", booking.id().value().toString())
                .append("travelerId", booking.travelerId().value().toString())
                .append("itemType", booking.reference().itemType().name())
                .append("itemId", booking.reference().itemId())
                .append("quantity", booking.reference().quantity())
                .append("status", booking.status().name())
                .append("createdAt", Date.from(booking.createdAt()));
    }

    public static Booking toDomain(Document document) {
        return Booking.reconstitute(
                BookingId.of(document.getString("_id")),
                TravelerId.of(document.getString("travelerId")),
                new BookingReference(
                        ItemType.valueOf(document.getString("itemType")),
                        document.getString("itemId"),
                        document.getInteger("quantity")),
                BookingStatus.valueOf(document.getString("status")),
                document.getDate("createdAt").toInstant());
    }
}

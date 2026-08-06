package com.travelplatform.booking.application.usecase;

import com.travelplatform.booking.application.port.in.CreateBookingUseCase;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.application.port.out.ReceiptStorage;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.Email;
import com.travelplatform.booking.domain.booking.ItemType;
import com.travelplatform.booking.domain.booking.Money;
import com.travelplatform.booking.domain.booking.TravelerId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreateBookingService implements CreateBookingUseCase {

    private final BookingRepository bookingRepository;
    private final ReceiptStorage receiptStorage;

    public CreateBookingService(
            BookingRepository bookingRepository, ReceiptStorage receiptStorage) {
        this.bookingRepository = bookingRepository;
        this.receiptStorage = receiptStorage;
    }

    @Override
    public BookingId create(CreateBookingCommand command) {
        var booking =
                Booking.create(
                        TravelerId.of(command.travelerId()),
                        new Email(command.travelerEmail()),
                        new BookingReference(
                                ItemType.valueOf(command.itemType()),
                                command.itemId(),
                                command.quantity(),
                                command.itemSummary()),
                        new Money(command.amount(), command.currency()));
        bookingRepository.save(booking);
        // Best-effort, not transactional with the write above - see
        // docs/adr/0009-booking-receipts-in-s3.md.
        receiptStorage.store(booking);
        return booking.id();
    }
}

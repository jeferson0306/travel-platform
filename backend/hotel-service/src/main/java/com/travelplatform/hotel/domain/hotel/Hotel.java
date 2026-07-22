package com.travelplatform.hotel.domain.hotel;

import com.travelplatform.hotel.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root for the hotel bounded context. No lifecycle yet beyond creation and room
 * reservation (no update/delete endpoint); revisit once a real need appears.
 */
public final class Hotel {

    private final HotelId id;
    private final HotelName name;
    private final City city;
    private final Money pricePerNight;
    private final int availableRooms;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Hotel(HotelId id, HotelName name, City city, Money pricePerNight, int availableRooms) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.pricePerNight = pricePerNight;
        this.availableRooms = availableRooms;
    }

    public static Hotel create(HotelName name, City city, Money pricePerNight, int availableRooms) {
        if (availableRooms < 0) {
            throw new IllegalArgumentException("availableRooms must not be negative");
        }
        var hotel = new Hotel(HotelId.newId(), name, city, pricePerNight, availableRooms);
        hotel.domainEvents.add(
                new HotelCreated(
                        hotel.id,
                        hotel.name,
                        hotel.city,
                        hotel.pricePerNight,
                        hotel.availableRooms,
                        Instant.now()));
        return hotel;
    }

    public static Hotel reconstitute(
            HotelId id, HotelName name, City city, Money pricePerNight, int availableRooms) {
        return new Hotel(id, name, city, pricePerNight, availableRooms);
    }

    public HotelId id() {
        return id;
    }

    public HotelName name() {
        return name;
    }

    public City city() {
        return city;
    }

    public Money pricePerNight() {
        return pricePerNight;
    }

    public int availableRooms() {
        return availableRooms;
    }

    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}

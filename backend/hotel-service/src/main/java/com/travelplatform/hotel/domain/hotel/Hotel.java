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
    private final String address;
    private final int starRating;
    private final List<String> amenities;
    private final String description;
    private final Double reviewScore;
    private final int reviewCount;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Hotel(
            HotelId id,
            HotelName name,
            City city,
            Money pricePerNight,
            int availableRooms,
            String address,
            int starRating,
            List<String> amenities,
            String description,
            Double reviewScore,
            int reviewCount) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.pricePerNight = pricePerNight;
        this.availableRooms = availableRooms;
        this.address = address;
        this.starRating = starRating;
        this.amenities = amenities == null ? List.of() : List.copyOf(amenities);
        this.description = description;
        this.reviewScore = reviewScore;
        this.reviewCount = reviewCount;
    }

    public static Hotel create(
            HotelName name,
            City city,
            Money pricePerNight,
            int availableRooms,
            String address,
            int starRating,
            List<String> amenities,
            String description,
            Double reviewScore,
            int reviewCount) {
        if (availableRooms < 0) {
            throw new IllegalArgumentException("availableRooms must not be negative");
        }
        if (starRating < 1 || starRating > 5) {
            throw new IllegalArgumentException("starRating must be between 1 and 5");
        }
        var hotel =
                new Hotel(
                        HotelId.newId(),
                        name,
                        city,
                        pricePerNight,
                        availableRooms,
                        address,
                        starRating,
                        amenities,
                        description,
                        reviewScore,
                        reviewCount);
        hotel.domainEvents.add(
                new HotelCreated(
                        hotel.id,
                        hotel.name,
                        hotel.city,
                        hotel.pricePerNight,
                        hotel.availableRooms,
                        hotel.address,
                        hotel.starRating,
                        hotel.amenities,
                        hotel.description,
                        hotel.reviewScore,
                        hotel.reviewCount,
                        Instant.now()));
        return hotel;
    }

    public static Hotel reconstitute(
            HotelId id,
            HotelName name,
            City city,
            Money pricePerNight,
            int availableRooms,
            String address,
            int starRating,
            List<String> amenities,
            String description,
            Double reviewScore,
            int reviewCount) {
        return new Hotel(
                id,
                name,
                city,
                pricePerNight,
                availableRooms,
                address,
                starRating,
                amenities,
                description,
                reviewScore,
                reviewCount);
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

    public String address() {
        return address;
    }

    public int starRating() {
        return starRating;
    }

    public List<String> amenities() {
        return amenities;
    }

    public String description() {
        return description;
    }

    public Double reviewScore() {
        return reviewScore;
    }

    public int reviewCount() {
        return reviewCount;
    }

    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}

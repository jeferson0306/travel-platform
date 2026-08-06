package com.travelplatform.flight.application.usecase;

import com.travelplatform.flight.application.port.in.CreateFlightUseCase;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import com.travelplatform.flight.domain.flight.FlightId;
import com.travelplatform.flight.domain.flight.Money;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreateFlightService implements CreateFlightUseCase {

    private final FlightRepository flightRepository;

    public CreateFlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public FlightId create(CreateFlightCommand command) {
        var flight =
                Flight.create(
                        new AirportCode(command.origin()),
                        new AirportCode(command.destination()),
                        command.departureAt(),
                        command.arrivalAt(),
                        new Money(command.priceAmount(), command.priceCurrency()),
                        command.availableSeats(),
                        command.airline(),
                        command.airlineCode(),
                        command.flightNumber(),
                        command.cabinClass(),
                        command.stops());
        flightRepository.save(flight);
        return flight.id();
    }
}

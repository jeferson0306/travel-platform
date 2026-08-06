package com.travelplatform.search.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.travelplatform.search.OpenSearchTestResource;
import com.travelplatform.search.application.port.in.IndexFlightUseCase;
import com.travelplatform.search.application.port.in.IndexFlightUseCase.IndexFlightCommand;
import com.travelplatform.search.application.port.in.IndexHotelUseCase;
import com.travelplatform.search.application.port.in.IndexHotelUseCase.IndexHotelCommand;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises search over HTTP end to end against a real OpenSearch (Testcontainers). No
 * authentication involved anywhere - search is fully public (see
 * docs/adr/0012-search-service-opensearch.md), unlike every other resource test in this platform.
 */
@QuarkusTest
@QuarkusTestResource(OpenSearchTestResource.class)
@DisplayName("SearchResource")
class SearchResourceTest {

    @Inject IndexFlightUseCase indexFlightUseCase;
    @Inject IndexHotelUseCase indexHotelUseCase;

    private String seedAFlight(String origin, String destination) {
        var flightId = UUID.randomUUID().toString();
        indexFlightUseCase.index(
                new IndexFlightCommand(
                        flightId,
                        origin,
                        destination,
                        Instant.now(),
                        Instant.now().plusSeconds(36000),
                        new BigDecimal("450.00"),
                        "EUR",
                        150));
        return flightId;
    }

    private String seedAHotel(String name, String city) {
        var hotelId = UUID.randomUUID().toString();
        indexHotelUseCase.index(
                new IndexHotelCommand(hotelId, name, city, new BigDecimal("120.00"), "EUR", 30));
        return hotelId;
    }

    @Test
    void searchesFlightsByRoute() {
        var flightId = seedAFlight("MAD", "EZE");

        given().when()
                .get("/api/v1/search/flights?origin=MAD&destination=EZE")
                .then()
                .statusCode(200)
                .body("find { it.flightId == '" + flightId + "' }.origin", equalTo("MAD"));
    }

    @Test
    void autocompletesFlightsByAirportCodePrefix() {
        seedAFlight("BCN", "MIA");

        given().when()
                .get("/api/v1/search/flights?q=BC")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void searchesHotelsByCity() {
        var hotelId = seedAHotel("Ocean View", "Rio de Janeiro");

        given().queryParam("city", "Rio de Janeiro")
                .when()
                .get("/api/v1/search/hotels")
                .then()
                .statusCode(200)
                .body("find { it.hotelId == '" + hotelId + "' }.city", equalTo("Rio de Janeiro"));
    }

    @Test
    void autocompletesHotelsByNamePrefix() {
        seedAHotel("Sunset Resort", "Faro");

        given().when()
                .get("/api/v1/search/hotels?q=suns")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void anUnknownRouteReturnsAnEmptyListNotAnError() {
        given().when()
                .get("/api/v1/search/flights?origin=ZZZ&destination=YYY")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }
}

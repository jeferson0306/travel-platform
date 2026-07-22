package com.travelplatform.flight.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AirportCode")
class AirportCodeTest {

    @Test
    void normalizesToUppercase() {
        assertThat(new AirportCode("lis").value()).isEqualTo("LIS");
    }

    @Test
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> new AirportCode("LISX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AirportCode("LI"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

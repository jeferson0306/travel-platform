package com.travelplatform.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-domain@", "@missing-local.com", "no-at.com"})
    void rejectsInvalidFormats(String invalid) {
        assertThatThrownBy(() -> new Email(invalid)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesToLowerCaseAndTrimmed() {
        var email = new Email("  Traveler@Example.COM  ");

        assertThat(email.value()).isEqualTo("traveler@example.com");
    }
}

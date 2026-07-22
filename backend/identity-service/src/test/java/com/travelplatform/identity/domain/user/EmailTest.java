package com.travelplatform.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Email")
class EmailTest {

    @ParameterizedTest(name = "rejects \"{0}\" as an invalid email")
    @ValueSource(strings = {"not-an-email", "missing-domain@", "@missing-local.com", "no-at.com"})
    @DisplayName("rejects malformed email addresses")
    void rejectsInvalidFormats(String invalid) {
        assertThatThrownBy(() -> new Email(invalid)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("normalizes to lower case and trims surrounding whitespace")
    void normalizesToLowerCaseAndTrimmed() {
        var email = new Email("  Traveler@Example.COM  ");

        assertThat(email.value()).isEqualTo("traveler@example.com");
    }
}

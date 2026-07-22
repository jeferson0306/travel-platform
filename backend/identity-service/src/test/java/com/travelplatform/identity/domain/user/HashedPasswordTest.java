package com.travelplatform.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HashedPassword")
class HashedPasswordTest {

    @Test
    @DisplayName("rejects a null value")
    void rejectsNull() {
        assertThatThrownBy(() -> new HashedPassword(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rejects a blank value")
    void rejectsBlank() {
        assertThatThrownBy(() -> new HashedPassword("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

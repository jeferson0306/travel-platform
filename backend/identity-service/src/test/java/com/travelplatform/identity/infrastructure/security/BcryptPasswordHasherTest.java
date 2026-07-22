package com.travelplatform.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BcryptPasswordHasher")
class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

    @Test
    @DisplayName("hashes a password so it never matches the raw value")
    void hashedValueIsNeverTheRawPassword() {
        var hashed = hasher.hash("s3cret-pass");

        assertThat(hashed.value()).isNotEqualTo("s3cret-pass");
    }

    @Test
    @DisplayName("matches the correct raw password against its own hash")
    void matchesTheCorrectPassword() {
        var hashed = hasher.hash("s3cret-pass");

        assertThat(hasher.matches("s3cret-pass", hashed)).isTrue();
    }

    @Test
    @DisplayName("rejects the wrong raw password against a hash")
    void rejectsTheWrongPassword() {
        var hashed = hasher.hash("s3cret-pass");

        assertThat(hasher.matches("wrong-pass", hashed)).isFalse();
    }

    @Test
    @DisplayName("salts each hash so the same password never hashes the same way twice")
    void saltsEachHashDifferently() {
        var first = hasher.hash("s3cret-pass");
        var second = hasher.hash("s3cret-pass");

        assertThat(first.value()).isNotEqualTo(second.value());
    }
}

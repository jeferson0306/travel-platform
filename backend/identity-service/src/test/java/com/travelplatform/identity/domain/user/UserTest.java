package com.travelplatform.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User")
class UserTest {

    @Test
    @DisplayName("register() creates an active USER and raises UserRegistered")
    void registerCreatesActiveUserAndRaisesDomainEvent() {
        var email = new Email("traveler@example.com");
        var password = new HashedPassword("hashed-value");

        var user = User.register(email, password);

        assertThat(user.isActive()).isTrue();
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(
                        UserRegistered.class,
                        event -> {
                            assertThat(event.userId()).isEqualTo(user.id());
                            assertThat(event.email()).isEqualTo(email);
                        });
    }

    @Test
    @DisplayName("pullDomainEvents() clears the list after the first read")
    void pullDomainEventsClearsAfterFirstRead() {
        var user = User.register(new Email("traveler@example.com"), new HashedPassword("hash"));

        user.pullDomainEvents();

        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("reconstitute() never raises a domain event")
    void reconstitutedUserRaisesNoDomainEvents() {
        var user =
                User.reconstitute(
                        UserId.newId(),
                        new Email("traveler@example.com"),
                        new HashedPassword("hash"),
                        Role.USER,
                        UserStatus.ACTIVE,
                        java.time.Instant.now());

        assertThat(user.pullDomainEvents()).isEmpty();
    }
}

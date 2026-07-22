package com.travelplatform.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.travelplatform.identity.application.port.in.AuthenticateUserUseCase.AuthenticateCommand;
import com.travelplatform.identity.application.port.out.PasswordHasher;
import com.travelplatform.identity.application.port.out.TokenIssuer;
import com.travelplatform.identity.application.port.out.TokenIssuer.IssuedToken;
import com.travelplatform.identity.application.port.out.UserRepository;
import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.HashedPassword;
import com.travelplatform.identity.domain.user.InvalidCredentialsException;
import com.travelplatform.identity.domain.user.Role;
import com.travelplatform.identity.domain.user.User;
import com.travelplatform.identity.domain.user.UserId;
import com.travelplatform.identity.domain.user.UserStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticateUserService")
class AuthenticateUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;
    @Mock TokenIssuer tokenIssuer;

    AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(userRepository, passwordHasher, tokenIssuer);
    }

    @Test
    @DisplayName("issues a token when the email and password are both correct")
    void issuesTokenWhenCredentialsAreValid() {
        var user = User.register(new Email("traveler@example.com"), new HashedPassword("hashed"));
        when(userRepository.findByEmail(new Email("traveler@example.com")))
                .thenReturn(Optional.of(user));
        when(passwordHasher.matches("s3cret-pass", user.password())).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(new IssuedToken("jwt-token", 3600));

        var result =
                service.authenticate(
                        new AuthenticateCommand("traveler@example.com", "s3cret-pass"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("rejects an email that is not registered")
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail(new Email("ghost@example.com")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.authenticate(
                                        new AuthenticateCommand("ghost@example.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("rejects the correct email with the wrong password")
    void rejectsWrongPassword() {
        var user = User.register(new Email("traveler@example.com"), new HashedPassword("hashed"));
        when(userRepository.findByEmail(new Email("traveler@example.com")))
                .thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-pass", user.password())).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.authenticate(
                                        new AuthenticateCommand(
                                                "traveler@example.com", "wrong-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("rejects a correct password for a deactivated account")
    void rejectsInactiveUser() {
        var user =
                User.reconstitute(
                        UserId.newId(),
                        new Email("traveler@example.com"),
                        new HashedPassword("hashed"),
                        Role.USER,
                        UserStatus.DISABLED,
                        Instant.now());
        when(userRepository.findByEmail(new Email("traveler@example.com")))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                service.authenticate(
                                        new AuthenticateCommand(
                                                "traveler@example.com", "s3cret-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

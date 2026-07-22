package com.travelplatform.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.identity.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.travelplatform.identity.application.port.out.PasswordHasher;
import com.travelplatform.identity.application.port.out.UserRepository;
import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.EmailAlreadyRegisteredException;
import com.travelplatform.identity.domain.user.HashedPassword;
import com.travelplatform.identity.domain.user.Role;
import com.travelplatform.identity.domain.user.User;
import com.travelplatform.identity.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserService")
class RegisterUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;

    RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(userRepository, passwordHasher);
    }

    @Test
    @DisplayName("registers a user with the default USER role and ACTIVE status")
    void registersUserWhenEmailIsNotTaken() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordHasher.hash("s3cret-pass")).thenReturn(new HashedPassword("hashed"));

        var userId =
                service.register(new RegisterUserCommand("traveler@example.com", "s3cret-pass"));

        assertThat(userId).isNotNull();
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(userId);
        assertThat(saved.email().value()).isEqualTo("traveler@example.com");
        assertThat(saved.password().value()).isEqualTo("hashed");
        assertThat(saved.role()).isEqualTo(Role.USER);
        assertThat(saved.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("rejects registration when the email is already taken")
    void rejectsRegistrationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(new Email("traveler@example.com"))).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.register(
                                        new RegisterUserCommand(
                                                "traveler@example.com", "s3cret-pass")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}

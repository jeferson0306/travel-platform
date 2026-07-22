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
import com.travelplatform.identity.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;

    RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(userRepository, passwordHasher);
    }

    @Test
    void registersUserWhenEmailIsNotTaken() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordHasher.hash("s3cret-pass")).thenReturn(new HashedPassword("hashed"));

        var userId =
                service.register(new RegisterUserCommand("traveler@example.com", "s3cret-pass"));

        assertThat(userId).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
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

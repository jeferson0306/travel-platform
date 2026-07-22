package com.travelplatform.identity.application.usecase;

import com.travelplatform.identity.application.port.in.RegisterUserUseCase;
import com.travelplatform.identity.application.port.out.PasswordHasher;
import com.travelplatform.identity.application.port.out.UserRepository;
import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.EmailAlreadyRegisteredException;
import com.travelplatform.identity.domain.user.User;
import com.travelplatform.identity.domain.user.UserId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserId register(RegisterUserCommand command) {
        var email = new Email(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        var hashedPassword = passwordHasher.hash(command.rawPassword());
        var user = User.register(email, hashedPassword);
        userRepository.save(user);

        return user.id();
    }
}

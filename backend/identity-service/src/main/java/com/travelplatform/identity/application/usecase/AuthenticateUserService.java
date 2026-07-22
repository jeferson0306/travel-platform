package com.travelplatform.identity.application.usecase;

import com.travelplatform.identity.application.port.in.AuthenticateUserUseCase;
import com.travelplatform.identity.application.port.out.PasswordHasher;
import com.travelplatform.identity.application.port.out.TokenIssuer;
import com.travelplatform.identity.application.port.out.UserRepository;
import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.InvalidCredentialsException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserService(
            UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public AuthenticationResult authenticate(AuthenticateCommand command) {
        var email = new Email(command.email());
        var user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive() || !passwordHasher.matches(command.rawPassword(), user.password())) {
            throw new InvalidCredentialsException();
        }

        var token = tokenIssuer.issue(user);
        return new AuthenticationResult(token.value(), token.expiresInSeconds());
    }
}

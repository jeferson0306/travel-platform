package com.travelplatform.identity.application.port.in;

public interface AuthenticateUserUseCase {

    AuthenticationResult authenticate(AuthenticateCommand command);

    record AuthenticateCommand(String email, String rawPassword) {}

    record AuthenticationResult(String accessToken, long expiresInSeconds) {}
}

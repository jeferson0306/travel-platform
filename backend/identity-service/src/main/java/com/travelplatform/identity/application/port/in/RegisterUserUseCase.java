package com.travelplatform.identity.application.port.in;

import com.travelplatform.identity.domain.user.UserId;

public interface RegisterUserUseCase {

    UserId register(RegisterUserCommand command);

    record RegisterUserCommand(String email, String rawPassword) {}
}

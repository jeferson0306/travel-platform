package com.travelplatform.identity.infrastructure.security;

import com.travelplatform.identity.application.port.out.PasswordHasher;
import com.travelplatform.identity.domain.user.HashedPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHasher {

    @Override
    public HashedPassword hash(String rawPassword) {
        return new HashedPassword(BcryptUtil.bcryptHash(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, HashedPassword hashedPassword) {
        return BcryptUtil.matches(rawPassword, hashedPassword.value());
    }
}

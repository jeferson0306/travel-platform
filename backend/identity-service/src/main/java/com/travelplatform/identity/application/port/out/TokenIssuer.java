package com.travelplatform.identity.application.port.out;

import com.travelplatform.identity.domain.user.User;

/** Outbound port for issuing access tokens. Implemented by an infrastructure adapter. */
public interface TokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String value, long expiresInSeconds) {}
}

package com.travelplatform.identity.application.port.out;

import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.User;
import com.travelplatform.identity.domain.user.UserId;
import java.util.Optional;

/** Outbound port for user persistence. Implemented by an infrastructure adapter. */
public interface UserRepository {

    void save(User user);

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId id);

    boolean existsByEmail(Email email);
}

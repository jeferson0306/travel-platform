package com.travelplatform.identity.infrastructure.persistence.mongo;

import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.HashedPassword;
import com.travelplatform.identity.domain.user.User;
import com.travelplatform.identity.domain.user.UserId;
import com.travelplatform.identity.domain.user.UserStatus;
import java.util.Date;
import org.bson.Document;

/** Converts between the {@link User} aggregate and its MongoDB representation. */
public final class UserDocumentMapper {

    private UserDocumentMapper() {}

    public static Document toDocument(User user) {
        return new Document("_id", user.id().value().toString())
                .append("email", user.email().value())
                .append("passwordHash", user.password().value())
                .append("status", user.status().name())
                .append("registeredAt", Date.from(user.registeredAt()));
    }

    public static User toDomain(Document document) {
        return User.reconstitute(
                UserId.of(document.getString("_id")),
                new Email(document.getString("email")),
                new HashedPassword(document.getString("passwordHash")),
                UserStatus.valueOf(document.getString("status")),
                document.getDate("registeredAt").toInstant());
    }
}

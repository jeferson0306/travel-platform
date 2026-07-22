package com.travelplatform.identity.infrastructure.persistence.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.travelplatform.identity.application.port.out.UserRepository;
import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.User;
import com.travelplatform.identity.domain.user.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MongoUserRepository implements UserRepository {

    private final MongoCollection<Document> collection;

    public MongoUserRepository(
            MongoClient mongoClient,
            @ConfigProperty(name = "identity.mongo.database", defaultValue = "identity")
                    String database) {
        this.collection = mongoClient.getDatabase(database).getCollection("users");
    }

    @Override
    public void save(User user) {
        var document = UserDocumentMapper.toDocument(user);
        collection.replaceOne(
                Filters.eq("_id", document.getString("_id")),
                document,
                new ReplaceOptions().upsert(true));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return Optional.ofNullable(collection.find(Filters.eq("email", email.value())).first())
                .map(UserDocumentMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(
                        collection.find(Filters.eq("_id", id.value().toString())).first())
                .map(UserDocumentMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return collection.countDocuments(Filters.eq("email", email.value())) > 0;
    }
}

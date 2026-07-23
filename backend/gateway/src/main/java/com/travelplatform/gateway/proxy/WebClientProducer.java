package com.travelplatform.gateway.proxy;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class WebClientProducer {

    @Produces
    @Singleton
    WebClient webClient(Vertx vertx) {
        return WebClient.create(vertx);
    }
}

package com.github.mattiaforc.keycloak;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Optional.ofNullable;

public class FalcoEventListenerProviderFactory implements EventListenerProviderFactory {
    private static final String ID = "falco";
    private static final Logger logger = Logger.getLogger(FalcoEventListenerProviderFactory.class);
    private final FalcoEventListenerConfiguration configuration = new FalcoEventListenerConfiguration();
    private FalcoEventListenerProvider instance;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        var httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

        if (instance == null) {
            instance = new FalcoEventListenerProvider(httpClient, configuration);
        }

        return instance;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Scope config) {
        logger.debug("Init falco event listener");
        configuration.setFalcoEndpoint(ofNullable(config.get("falcoEndpoint", System.getenv("FALCO_ENDPOINT"))).orElseThrow(() -> new NullPointerException("falco host must not be null.")));
        // By default, we listen to all user events if not specified otherwise
        configuration.setEvents(ofNullable(config.get("falcoEvents", System.getenv("FALCO_EVENTS")))
                .map(e -> e.split(","))
                .filter(e -> e.length >= 1 && !e[0].trim().isBlank())
                .map(evs -> Arrays.stream(evs).map(e -> EventType.valueOf(e.toUpperCase())).toList())
                .orElse(Collections.emptyList()));
        configuration.setAdminEvents(config.getBoolean("falcoAdminEvents", ofNullable(System.getenv("FALCO_ADMIN_EVENTS"))
                .map(enabled -> enabled.trim().equalsIgnoreCase("true"))
                .orElse(false)));
        configuration.setAdditionalHeaders((Header[]) ofNullable(config.get("falcoAdditionalHeaders", System.getenv("FALCO_ADDITIONAL_HEADERS")))
                .map(h -> Arrays.stream(h.split(";"))
                        .map(hh -> {
                            var headerParts = hh.split("=");
                            if (headerParts.length != 2) {
                                logger.fatalf("Falco additional headers are not configured correctly and will not be added to the HTTP call to Falco. The malformed header is %s", hh);
                                throw new RuntimeException(String.format("Falco additional headers are not configured correctly and will not be added to the HTTP call to Falco. The malformed header is %s", hh));
                            }
                            return new BasicHeader(headerParts[0], headerParts[1]);
                        })
                        .toArray()
                )
                .orElse(new Header[]{}));
    }

    @Override
    public void postInit(KeycloakSessionFactory arg0) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}

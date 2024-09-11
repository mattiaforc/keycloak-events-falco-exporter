package com.github.mattiaforc.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.io.IOException;
import java.net.URI;

public class FalcoEventListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(FalcoEventListenerProvider.class);
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FalcoEventListenerConfiguration configuration;

    public FalcoEventListenerProvider(CloseableHttpClient httpClient, FalcoEventListenerConfiguration configuration) {
        this.httpClient = httpClient;
        this.configuration = configuration;
    }

    @Override
    public void onEvent(Event event) {
        if (!configuration.getEvents().isEmpty() && !configuration.getEvents().contains(event.getType())) {
            log.debug(String.format("Event with id=%s and type=%s was discarded due to the Falco exporter configuration.", event.getId(), event.getType().toString()));
            return;
        }
        try {
            sendEventToFalco(event.getId(), event.getType().toString(), eventToJson(event));
        } catch (IOException e) {
            log.error(String.format("IOException from Falco upstream %s while emitting event with type=%s and id=%s.", configuration.getFalcoEndpoint(), event.getType().toString(), event.getId()), e);
        }
    }

    private void sendEventToFalco(String eventId, String eventType, String eventAsJson) throws IOException {
        log.debug(String.format("Sending event with id=%s and type=%s to falco endpoint %s", eventId, eventType, configuration.getFalcoEndpoint()));

        var request = new HttpPost();
        request.setEntity(new StringEntity(eventAsJson));
        request.setHeaders(configuration.getAdditionalHeaders());
        request.setURI(URI.create(configuration.getFalcoEndpoint()));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            var responseStatus = response.getStatusLine().getStatusCode();
            if (responseStatus != 200) {
                log.error(String.format("Error in response from Falco (upstream %s) while emitting event with type=%s and id=%s. Response was code=%d, body=%s", configuration.getFalcoEndpoint(), eventType, eventId, responseStatus, new String(response.getEntity().getContent().readAllBytes())));
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (!configuration.isAdminEvents()) {
            log.debug(String.format("Admin event with id=%s was discarded due to the Falco exporter configuration.", event.getId()));
            return;
        }
        try {
            sendEventToFalco(event.getId(), "ADMIN", eventToJson(event));
        } catch (IOException e) {
            log.error(String.format("IOException from Falco upstream %s while emitting admin event with id=%s.", configuration.getFalcoEndpoint(), event.getId()), e);
        }
    }

    private String eventToJson(Object event) {
        String eventAsJson;
        try {
            eventAsJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falco event exporter error while converting admin event to JSON.", e);
        }
        return eventAsJson;
    }

    @Override
    public void close() {
        // no-op
    }
}

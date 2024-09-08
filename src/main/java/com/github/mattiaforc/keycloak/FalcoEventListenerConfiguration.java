package com.github.mattiaforc.keycloak;

import org.apache.http.Header;
import org.keycloak.events.EventType;

import java.util.List;

public final class FalcoEventListenerConfiguration {
    /**
     * Mandatory configuration for the Falco host that will receive the events.
     * This could be both HTTP or HTTPS.
     * Its configuration is left to user discretion,
     * see the README for more details on the plugin architecture on the Falco side.
     */
    private String falcoEndpoint;
    /**
     * The kind of user events that we want to forward to Falco.
     * If this is an empty array, then we forward every type of event.
     */
    private List<EventType> events;

    /**
     * Boolean flag to indicate whether we want to process keycloak admin events.
     */
    private boolean adminEvents = false;
    /**
     * Additional headers that we may want to send to Falco when making HTTP calls.
     * Can be useful if Falco is protected by some auhtz mechanism (for example, an API Key)
     */
    private Header[] additionalHeaders;

    public FalcoEventListenerConfiguration() {
    }

    public String getFalcoEndpoint() {
        return falcoEndpoint;
    }

    public void setFalcoEndpoint(String falcoEndpoint) {
        this.falcoEndpoint = falcoEndpoint;
    }

    public List<EventType> getEvents() {
        return events;
    }

    public void setEvents(List<EventType> events) {
        this.events = events;
    }

    public Header[] getAdditionalHeaders() {
        return additionalHeaders;
    }

    public void setAdditionalHeaders(Header[] additionalHeaders) {
        this.additionalHeaders = additionalHeaders;
    }

    public boolean isAdminEvents() {
        return adminEvents;
    }

    public void setAdminEvents(boolean adminEvents) {
        this.adminEvents = adminEvents;
    }
}

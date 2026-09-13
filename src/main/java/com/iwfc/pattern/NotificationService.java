package com.iwfc.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Observer Pattern — Event Publisher.
 * Manages subscriptions and dispatches events to registered listeners.
 * Decouples services (publishers) from UI components (subscribers).
 *
 * Event types used in the IWFC system:
 * - BOOKING_CONFIRMED: fired when a member successfully books a session
 * - MAINTENANCE_STATUS_CHANGED: fired when a maintenance request status transitions
 * - MAINTENANCE_ALERT: fired when equipment usage exceeds threshold
 * - EQUIPMENT_THRESHOLD: fired when equipment nears maintenance threshold
 */
public class NotificationService {

    private final Map<String, List<EventListener<String>>> listeners;

    public NotificationService() {
        this.listeners = new HashMap<>();
    }

    /**
     * Subscribes a listener to a specific event type.
     *
     * @param eventType the event type to listen for
     * @param listener  the listener to register
     */
    public void subscribe(String eventType, EventListener<String> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Unsubscribes a listener from a specific event type.
     *
     * @param eventType the event type to stop listening for
     * @param listener  the listener to remove
     */
    public void unsubscribe(String eventType, EventListener<String> listener) {
        List<EventListener<String>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    /**
     * Notifies all listeners subscribed to the given event type.
     *
     * @param eventType the event type that occurred
     * @param message   the event message/payload
     */
    public void notifyListeners(String eventType, String message) {
        List<EventListener<String>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            for (EventListener<String> listener : eventListeners) {
                listener.onEvent(eventType, message);
            }
        }
    }

    /**
     * Returns the number of listeners for a given event type.
     * Useful for testing.
     */
    public int getListenerCount(String eventType) {
        List<EventListener<String>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
}

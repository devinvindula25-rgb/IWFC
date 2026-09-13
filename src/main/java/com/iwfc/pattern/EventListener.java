package com.iwfc.pattern;

/**
 * Observer Pattern — Listener Interface.
 * Generic interface for components that want to receive event notifications.
 * Used by the NotificationService to decouple event publishers from subscribers.
 *
 * @param <T> the type of data associated with the event
 */
public interface EventListener<T> {

    /**
     * Called when an event of interest occurs.
     *
     * @param eventType a string identifier for the event (e.g., "BOOKING_CONFIRMED")
     * @param data      the event payload
     */
    void onEvent(String eventType, T data);
}

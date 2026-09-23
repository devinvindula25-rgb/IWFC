package com.iwfc.pattern;

import com.iwfc.model.Notification;
import com.iwfc.model.NotificationCategory;
import com.iwfc.model.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Observer Pattern — Event Publisher & Notification Persistence.
 * Manages subscriptions and dispatches transient events to registered listeners,
 * while also persisting all events as Notification objects.
 */
public class NotificationService {

    private final Map<String, List<EventListener<String>>> listeners;
    private final DataStore dataStore;

    public NotificationService() {
        this.listeners = new HashMap<>();
        this.dataStore = DataStore.getInstance();
    }

    /**
     * Subscribes a listener to a specific event type.
     */
    public void subscribe(String eventType, EventListener<String> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Unsubscribes a listener from a specific event type.
     */
    public void unsubscribe(String eventType, EventListener<String> listener) {
        List<EventListener<String>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    /**
     * Notifies all listeners subscribed to the given event type AND persists the notification.
     */
    public void notifyListeners(String eventType, String message) {
        // Map event type to category (simple mapping based on string content)
        NotificationCategory category = mapEventTypeToCategory(eventType);
        Role targetRole = mapEventTypeToRole(eventType);
        
        // Persist
        addNotification(category, getTitleForEvent(eventType), message, targetRole);

        // Notify transient listeners (like Toasts)
        List<EventListener<String>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            for (EventListener<String> listener : eventListeners) {
                listener.onEvent(eventType, message);
            }
        }
    }
    
    private NotificationCategory mapEventTypeToCategory(String eventType) {
        if (eventType.startsWith("BOOKING")) return NotificationCategory.BOOKING;
        if (eventType.startsWith("SESSION")) return NotificationCategory.SESSION;
        if (eventType.startsWith("MAINTENANCE")) return NotificationCategory.MAINTENANCE;
        if (eventType.startsWith("EQUIPMENT")) return NotificationCategory.EQUIPMENT;
        return NotificationCategory.SYSTEM;
    }
    
    private String getTitleForEvent(String eventType) {
        return eventType.replace("_", " ").substring(0, 1).toUpperCase() + 
               eventType.replace("_", " ").substring(1).toLowerCase();
    }
    
    private Role mapEventTypeToRole(String eventType) {
        if (eventType.startsWith("BOOKING")) return Role.MEMBER; // Just as an example, actual logic might be more complex
        if (eventType.startsWith("MAINTENANCE") || eventType.startsWith("EQUIPMENT")) return Role.INSTRUCTOR;
        return null; // For everyone
    }

    /**
     * Persists a new notification.
     */
    private void addNotification(NotificationCategory category, String title, String message, Role targetRole) {
        String id = "NTF" + String.format("%04d", dataStore.getNotifications().findAll().size() + 1);
        Notification notification = new Notification(id, category, title, message, targetRole);
        try {
            dataStore.getNotifications().save(notification);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            throw new RuntimeException("Failed to persist notification: " + e.getMessage());
        }
    }

    // --- Data retrieval & management ---

    public List<Notification> getAllNotifications() {
        return dataStore.getNotifications().findAll().stream()
                .sorted((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    public List<Notification> getNotificationsForRole(Role role) {
        if (role == Role.ADMIN) {
            return getAllNotifications();
        }
        return getAllNotifications().stream()
                .filter(n -> n.getTargetRole() == null || n.getTargetRole() == role)
                .collect(Collectors.toList());
    }

    public List<Notification> getNotificationsByCategory(NotificationCategory category, Role role) {
        return getNotificationsForRole(role).stream()
                .filter(n -> n.getCategory() == category)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Role role) {
        return getNotificationsForRole(role).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    public void markAsRead(String notificationId) {
        dataStore.getNotifications().findAll().stream()
                .filter(n -> n.getNotificationId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setRead(true));
    }

    public void markAllAsRead(Role role) {
        getNotificationsForRole(role).forEach(n -> n.setRead(true));
    }

    public void clearAll() {
        dataStore.getNotifications().findAll().forEach(n -> dataStore.getNotifications().delete(n.getId()));
    }

    public int getListenerCount(String eventType) {
        List<EventListener<String>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
}

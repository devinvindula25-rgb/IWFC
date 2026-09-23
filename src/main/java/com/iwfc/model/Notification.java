package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents a single persisted notification in the IWFC system.
 */
public class Notification implements Identifiable {

    private String notificationId;
    private NotificationCategory category;
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
    private Role targetRole; // null if meant for all users

    public Notification(String notificationId, NotificationCategory category, String title, String message, Role targetRole) {
        this.notificationId = notificationId;
        this.category = category;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.targetRole = targetRole;
    }

    public String getNotificationId() {
        return notificationId;
    }

    @Override
    public String getId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public void setCategory(NotificationCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Role getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(Role targetRole) {
        this.targetRole = targetRole;
    }
}

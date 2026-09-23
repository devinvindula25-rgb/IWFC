package com.iwfc.model;

/**
 * Categories for grouping notifications in the IWFC system.
 */
public enum NotificationCategory {
    BOOKING("📅", "#6366f1", "Booking"),
    SESSION("🏋️", "#8b5cf6", "Session"),
    MAINTENANCE("🔧", "#f59e0b", "Maintenance"),
    EQUIPMENT("⚙️", "#ef4444", "Equipment"),
    SYSTEM("🔔", "#3b82f6", "System");

    private final String icon;
    private final String color;
    private final String displayName;

    NotificationCategory(String icon, String color, String displayName) {
        this.icon = icon;
        this.color = color;
        this.displayName = displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }
}

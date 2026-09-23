package com.iwfc.ui.components;

import com.iwfc.model.BookingStatus;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.RequestStatus;
import com.iwfc.model.Urgency;
import javafx.scene.control.Label;

/**
 * Color-coded status badge label component.
 * Auto-colors based on the type of status being displayed.
 */
public class StatusBadge extends Label {

    public StatusBadge(String text, String colorClass) {
        super("● " + text);
        getStyleClass().addAll("status-badge", colorClass);
        
        if (colorClass.equals("badge-warning") || colorClass.equals("badge-danger")) {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(1000), this);
            ft.setFromValue(1.0);
            ft.setToValue(0.6);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
        }
    }

    /**
     * Creates a badge for equipment status.
     */
    public static StatusBadge forEquipmentStatus(EquipmentStatus status) {
        String colorClass = switch (status) {
            case OPERATIONAL -> "badge-success";
            case FAULTY -> "badge-danger";
            case UNDER_MAINTENANCE -> "badge-warning";
        };
        return new StatusBadge(status.getDisplayName(), colorClass);
    }

    /**
     * Creates a badge for maintenance request status.
     */
    public static StatusBadge forRequestStatus(RequestStatus status) {
        String colorClass = switch (status) {
            case PENDING -> "badge-warning";
            case ASSIGNED -> "badge-info";
            case COMPLETED -> "badge-success";
        };
        return new StatusBadge(status.getDisplayName(), colorClass);
    }

    /**
     * Creates a badge for urgency level.
     */
    public static StatusBadge forUrgency(Urgency urgency) {
        String colorClass = switch (urgency) {
            case LOW -> "badge-success";
            case MEDIUM -> "badge-warning";
            case HIGH -> "badge-danger";
        };
        return new StatusBadge(urgency.getDisplayName(), colorClass);
    }

    /**
     * Creates a badge for booking status.
     */
    public static StatusBadge forBookingStatus(BookingStatus status) {
        if (status == null) {
            return new StatusBadge("Unknown", "badge-info");
        }
        String colorClass = switch (status) {
            case CONFIRMED -> "badge-success";
            case CANCELLED -> "badge-danger";
            default -> "badge-info";
        };
        return new StatusBadge(status.getDisplayName(), colorClass);
    }
}

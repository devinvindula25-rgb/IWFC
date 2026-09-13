package com.iwfc.ui.components;

import com.iwfc.pattern.EventListener;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Toast notification component that implements the Observer pattern's EventListener.
 * Slides in from the top-right with a fade animation, auto-dismisses after 4 seconds.
 * Color-coded by event type.
 */
public class ToastNotification implements EventListener<String> {

    private final StackPane container;

    public ToastNotification(StackPane container) {
        this.container = container;
    }

    @Override
    public void onEvent(String eventType, String message) {
        Platform.runLater(() -> showToast(eventType, message));
    }

    private void showToast(String eventType, String message) {
        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(12, 20, 12, 20));
        toast.setMaxWidth(400);
        toast.setMaxHeight(60);
        toast.getStyleClass().add("toast-notification");
        toast.getStyleClass().add(getToastStyleClass(eventType));

        Label icon = new Label(getToastIcon(eventType));
        icon.setStyle("-fx-font-size: 18px;");

        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);
        text.setMaxWidth(340);

        toast.getChildren().addAll(icon, text);

        // Position at top-right
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(20, 20, 0, 0));

        // Slide-in animation
        toast.setTranslateX(420);
        toast.setOpacity(0);
        container.getChildren().add(toast);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), toast);
        slideIn.setFromX(420);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition showAnim = new ParallelTransition(slideIn, fadeIn);
        showAnim.play();

        // Auto-dismiss after 4 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> container.getChildren().remove(toast));
            fadeOut.play();
        });
        pause.play();
    }

    private String getToastStyleClass(String eventType) {
        return switch (eventType) {
            case "BOOKING_CONFIRMED" -> "toast-success";
            case "MAINTENANCE_STATUS_CHANGED" -> "toast-info";
            case "MAINTENANCE_ALERT", "EQUIPMENT_THRESHOLD" -> "toast-warning";
            default -> "toast-info";
        };
    }

    private String getToastIcon(String eventType) {
        return switch (eventType) {
            case "BOOKING_CONFIRMED" -> "✓";
            case "MAINTENANCE_STATUS_CHANGED" -> "📋";
            case "MAINTENANCE_ALERT" -> "🔧";
            case "EQUIPMENT_THRESHOLD" -> "⚠";
            default -> "ℹ";
        };
    }
}

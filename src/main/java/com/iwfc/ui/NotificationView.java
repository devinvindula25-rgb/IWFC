package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Notification;
import com.iwfc.model.NotificationCategory;
import com.iwfc.model.Role;
import com.iwfc.exception.UnauthorizedAccessException;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationView extends BorderPane {

    private final IWFCSystemFacade facade;
    private final VBox listContainer;
    private NotificationCategory currentFilter = null;

    public NotificationView(IWFCSystemFacade facade) {
        this.facade = facade;
        this.listContainer = new VBox(15);
        this.listContainer.setPadding(new Insets(10));
        
        getStyleClass().add("content-area");

        buildHeader();
        
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        setCenter(scrollPane);
        
        refreshNotifications();
    }

    private void buildHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(20, 30, 20, 30));

        // Title and Actions
        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLbl = new Label("Notification Center");
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 28));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button markAllBtn = new Button("Mark All Read");
        markAllBtn.getStyleClass().addAll("action-button", "action-primary");
        markAllBtn.setOnAction(e -> {
            facade.markAllNotificationsAsRead();
            refreshNotifications();
        });
        
        Button clearAllBtn = new Button("Clear All");
        clearAllBtn.getStyleClass().addAll("action-button", "action-danger");
        if (facade.getCurrentUser().getRole() != Role.ADMIN) {
            clearAllBtn.setVisible(false);
            clearAllBtn.setManaged(false);
        }
        clearAllBtn.setOnAction(e -> {
            try {
                facade.clearAllNotifications();
                refreshNotifications();
            } catch (UnauthorizedAccessException ex) {
                // Ignore if unauthorized
            }
        });
        
        topRow.getChildren().addAll(titleLbl, spacer, markAllBtn, clearAllBtn);
        
        // Filter Chips
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        
        Button allChip = createFilterChip("All", null);
        filterBar.getChildren().add(allChip);
        
        for (NotificationCategory cat : NotificationCategory.values()) {
            Button chip = createFilterChip(cat.getIcon() + " " + cat.getDisplayName(), cat);
            filterBar.getChildren().add(chip);
        }
        
        header.getChildren().addAll(topRow, filterBar);
        setTop(header);
    }
    
    private Button createFilterChip(String text, NotificationCategory category) {
        Button chip = new Button(text);
        chip.getStyleClass().add("category-chip");
        if (category == currentFilter) {
            chip.getStyleClass().add("category-chip-active");
        }
        
        chip.setOnAction(e -> {
            currentFilter = category;
            refreshNotifications();
            
            // Re-style chips
            HBox filterBar = (HBox) ((VBox) getTop()).getChildren().get(1);
            for (javafx.scene.Node node : filterBar.getChildren()) {
                if (node instanceof Button b) {
                    b.getStyleClass().remove("category-chip-active");
                    if (b == chip) {
                        b.getStyleClass().add("category-chip-active");
                    }
                }
            }
        });
        
        return chip;
    }

    private void refreshNotifications() {
        listContainer.getChildren().clear();
        
        List<Notification> notifications;
        if (currentFilter == null) {
            notifications = facade.getNotifications();
        } else {
            notifications = facade.getNotificationsByCategory(currentFilter);
        }
        
        if (notifications.isEmpty()) {
            VBox emptyState = new VBox(15);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(100, 50, 50, 50));
            
            Label iconLbl = new Label("📭");
            iconLbl.setStyle("-fx-font-size: 64px; -fx-opacity: 0.5;");
            
            Label emptyLbl = new Label("No notifications right now.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-font-weight: bold;");
            
            Label subLbl = new Label("We'll let you know when something comes up.");
            subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            
            emptyState.getChildren().addAll(iconLbl, emptyLbl, subLbl);
            listContainer.getChildren().add(emptyState);
            return;
        }
        
        int delay = 0;
        for (Notification n : notifications) {
            HBox card = createNotificationCard(n);
            card.setOpacity(0.0); // Fix for initial flash
            listContainer.getChildren().add(card);
            
            // Slide-in animation
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
            tt.setFromX(50);
            tt.setToX(0);
            tt.setDelay(Duration.millis(delay));
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            
            tt.play();
            ft.play();
            
            delay += 50;
        }
    }

    private HBox createNotificationCard(Notification n) {
        HBox card = new HBox(15);
        card.getStyleClass().add("notification-card-premium");
        if (n.isRead()) {
            card.getStyleClass().add("notification-read");
        } else {
            card.getStyleClass().add("notification-unread");
        }
        card.setStyle("-fx-border-left-color: " + n.getCategory().getColor() + ";");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLbl = new Label(n.getCategory().getIcon());
        iconLbl.setFont(Font.font(24));
        
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(n.getTitle());
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Label timeLbl = new Label(n.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        timeLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        
        Circle unreadDot = new Circle(4, Color.web("#3b82f6"));
        unreadDot.setVisible(!n.isRead());
        if (!n.isRead()) {
            unreadDot.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#3b82f6")));
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(unreadDot.radiusProperty(), 4)),
                new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(unreadDot.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1), new javafx.animation.KeyValue(unreadDot.radiusProperty(), 7)),
                new KeyFrame(Duration.seconds(1), new javafx.animation.KeyValue(unreadDot.opacityProperty(), 0.5)),
                new KeyFrame(Duration.seconds(2), new javafx.animation.KeyValue(unreadDot.radiusProperty(), 4)),
                new KeyFrame(Duration.seconds(2), new javafx.animation.KeyValue(unreadDot.opacityProperty(), 1.0))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        }
        
        titleRow.getChildren().addAll(titleLbl, titleSpacer, timeLbl, unreadDot);
        
        Label msgLbl = new Label(n.getMessage());
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: #cbd5e1;");
        
        content.getChildren().addAll(titleRow, msgLbl);
        card.getChildren().addAll(iconLbl, content);
        
        // Interaction
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.015);
            st.setToY(1.015);
            st.play();
        });
        
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        
        card.setOnMouseClicked(e -> {
            if (!n.isRead()) {
                facade.markNotificationAsRead(n.getNotificationId());
                n.setRead(true);
                card.getStyleClass().remove("notification-unread");
                card.getStyleClass().add("notification-read");
                unreadDot.setVisible(false);
            }
        });
        
        return card;
    }
}

package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Notification;
import com.iwfc.model.NotificationCategory;
import com.iwfc.model.Role;
import com.iwfc.exception.UnauthorizedAccessException;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
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
    private final Label footerStatsLbl;
    private final Label footerTotalLbl;
    private javafx.scene.control.TextField searchField;

    public NotificationView(IWFCSystemFacade facade) {
        this.facade = facade;
        this.listContainer = new VBox(15);
        this.listContainer.setPadding(new Insets(10, 20, 10, 20));
        
        getStyleClass().add("content-area");
        // Deeper sapphire/obsidian textured background with subtle cyan/magenta light hints
        setStyle("-fx-background-color: radial-gradient(focus-angle 45deg, focus-distance 20%, center 50% 50%, radius 120%, #0f172a 0%, #020617 70%, #1e1b4b 100%);");

        buildHeader();
        
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(scrollPane);
        
        HBox footer = new HBox();
        footer.setPadding(new Insets(15, 30, 15, 30));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-border-color: rgba(255,255,255,0.05) transparent transparent transparent;");
        
        footerStatsLbl = new Label("0 Unread Notifications | Load Old Notifications");
        footerStatsLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-cursor: hand;");
        Region fSpacer = new Region(); HBox.setHgrow(fSpacer, Priority.ALWAYS);
        footerTotalLbl = new Label("Total: 0");
        footerTotalLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        
        footer.getChildren().addAll(footerStatsLbl, fSpacer, footerTotalLbl);
        setBottom(footer);
        
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
        
        searchField = new javafx.scene.control.TextField();
        searchField.setPromptText("Search Notifications...");
        searchField.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 6 16;");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldV, newV) -> refreshNotifications());
        
        Button markAllBtn = new Button("Mark All Read");
        markAllBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #38bdf8; -fx-border-color: rgba(56, 189, 248, 0.5); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");
        markAllBtn.setOnAction(e -> {
            facade.markAllNotificationsAsRead();
            refreshNotifications();
        });
        
        Button clearAllBtn = new Button("Clear All");
        clearAllBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #f87171; -fx-border-color: rgba(248, 113, 113, 0.5); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");
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
        
        topRow.getChildren().addAll(titleLbl, spacer, searchField, markAllBtn, clearAllBtn);
        
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
        
        String query = searchField.getText().toLowerCase();
        if (!query.isEmpty()) {
            notifications = notifications.stream()
                .filter(n -> n.getTitle().toLowerCase().contains(query) || n.getMessage().toLowerCase().contains(query))
                .collect(java.util.stream.Collectors.toList());
        }
        
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        footerStatsLbl.setText(unreadCount + " Unread Notifications | Load Old Notifications");
        footerTotalLbl.setText("Total: " + notifications.size());
        
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
            card.setOpacity(0.0);
            listContainer.getChildren().add(card);
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
            tt.setFromX(30);
            tt.setToX(0);
            tt.setDelay(Duration.millis(delay));
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            
            tt.play();
            ft.play();
            
            delay += 40;
        }
    }

    private HBox createNotificationCard(Notification n) {
        HBox card = new HBox(20);
        
        // Base Styling based on category
        String baseBg = "rgba(255,255,255,0.03)";
        String baseBorder = "rgba(255,255,255,0.1)";
        String effect = "dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 5)";
        
        if (n.getCategory() == NotificationCategory.MAINTENANCE && n.getTitle().contains("Urgency")) {
            baseBg = "rgba(239, 68, 68, 0.05)";
            baseBorder = "rgba(239, 68, 68, 0.6)";
            effect = "dropshadow(gaussian, rgba(239, 68, 68, 0.3), 20, 0, 0, 0)";
        } else if (n.getCategory() == NotificationCategory.SESSION) {
            baseBg = "rgba(16, 185, 129, 0.05)";
            baseBorder = "rgba(16, 185, 129, 0.4)";
            effect = "dropshadow(gaussian, rgba(16, 185, 129, 0.2), 15, 0, 0, 0)";
        }
        
        card.setStyle("-fx-background-color: " + baseBg + "; -fx-border-color: " + baseBorder + "; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 15; -fx-effect: " + effect + ";");
        card.setAlignment(Pos.CENTER_LEFT);
        
        // Left specific content
        Region leftVisual = createLeftVisual(n);
        
        VBox content = new VBox(8);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(n.getCategory().getIcon());
        iconLbl.setFont(Font.font(16));
        Label titleLbl = new Label(n.getTitle());
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLbl.setStyle("-fx-text-fill: white;");
        
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        
        Label timeLbl = new Label(n.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        timeLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        
        Circle unreadDot = new Circle(4, Color.web(n.getCategory() == NotificationCategory.SESSION ? "#10b981" : (n.getCategory() == NotificationCategory.MAINTENANCE && n.getTitle().contains("Urgency") ? "#ef4444" : "#3b82f6")));
        unreadDot.setVisible(!n.isRead());
        
        titleRow.getChildren().addAll(iconLbl, titleLbl, titleSpacer, timeLbl, unreadDot);
        
        Label msgLbl = new Label(n.getMessage());
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        
        HBox customContent = createCustomContent(n);
        
        content.getChildren().addAll(titleRow, msgLbl, customContent);
        
        if (leftVisual != null) {
            card.getChildren().addAll(leftVisual, content);
        } else {
            card.getChildren().add(content);
        }
        
        // Interaction
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.01);
            st.setToY(1.01);
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
                unreadDot.setVisible(false);
            }
        });
        
        return card;
    }

    private Region createLeftVisual(Notification n) {
        if (n.getCategory() == NotificationCategory.EQUIPMENT && n.getTitle().contains("Added")) {
            // Yoga mat visual representation
            StackPane sp = new StackPane();
            sp.setPrefSize(100, 60);
            sp.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-background-radius: 8px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 8px;");
            Label svg = new Label();
            svg.setStyle("-fx-shape: 'M4 4h16v2H4zm0 4h16v8H4z'; -fx-background-color: #38bdf8; -fx-min-width: 40px; -fx-min-height: 24px;");
            Label badge = new Label("x20");
            badge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 10px; -fx-font-size: 10px;");
            StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
            sp.getChildren().addAll(svg, badge);
            return sp;
        } else if (n.getCategory() == NotificationCategory.BOOKING) {
            // Calendar visual
            StackPane sp = new StackPane();
            sp.setPrefSize(120, 60);
            sp.setStyle("-fx-background-color: white; -fx-background-radius: 4px;");
            GridPane calGrid = new GridPane();
            calGrid.setHgap(2); calGrid.setVgap(2);
            for(int r=0; r<3; r++) {
                for(int c=0; c<5; c++) {
                    Region block = new Region();
                    block.setPrefSize(20, 15);
                    block.setStyle(r==1 && c==2 ? "-fx-background-color: #3b82f6;" : "-fx-background-color: #f1f5f9;");
                    calGrid.add(block, c, r);
                }
            }
            calGrid.setAlignment(Pos.CENTER);
            sp.getChildren().add(calGrid);
            return sp;
        } else if (n.getCategory() == NotificationCategory.SYSTEM) {
            // Line chart visual
            VBox vb = new VBox();
            vb.setPrefSize(100, 60);
            vb.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 8px;");
            javafx.scene.shape.Polyline line = new javafx.scene.shape.Polyline(0,30, 20,20, 40,40, 60,10, 80,30, 100,30);
            line.setStroke(Color.web("#38bdf8"));
            line.setStrokeWidth(2);
            line.setEffect(new javafx.scene.effect.DropShadow(5, Color.web("#38bdf8")));
            vb.getChildren().add(line);
            vb.setAlignment(Pos.CENTER);
            return vb;
        } else if (n.getCategory() == NotificationCategory.MAINTENANCE && n.getTitle().contains("Urgency")) {
            // Machine outline visual
            StackPane sp = new StackPane();
            sp.setPrefSize(80, 80);
            sp.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px;");
            Label machineSvg = new Label();
            machineSvg.setStyle("-fx-shape: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z'; -fx-background-color: #94a3b8; -fx-min-width: 40px; -fx-min-height: 40px;");
            sp.getChildren().add(machineSvg);
            return sp;
        } else if (n.getCategory() == NotificationCategory.SESSION) {
            StackPane sp = new StackPane();
            sp.setPrefSize(80, 60);
            sp.setStyle("-fx-background-color: rgba(16, 185, 129, 0.1); -fx-border-color: rgba(16, 185, 129, 0.4); -fx-border-radius: 8px; -fx-background-radius: 8px;");
            Label svg = new Label();
            svg.setStyle("-fx-shape: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z'; -fx-background-color: #10b981; -fx-min-width: 32px; -fx-min-height: 32px;");
            sp.getChildren().add(svg);
            return sp;
        }
        return null;
    }

    private HBox createCustomContent(Notification n) {
        HBox customBox = new HBox(15);
        customBox.setAlignment(Pos.CENTER_LEFT);
        
        if (n.getCategory() == NotificationCategory.BOOKING) {
            Button addCal = new Button("Add to Calendar");
            addCal.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;");
            addCal.setOnMouseEntered(e -> addCal.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(56,189,248,0.6); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            addCal.setOnMouseExited(e -> addCal.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            addCal.setOnAction(e -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Booking automatically synced to your Calendar.");
                com.iwfc.ui.components.DialogUtils.stylePremiumDialog(a);
                a.show();
            });
            customBox.getChildren().add(addCal);
        } else if (n.getCategory() == NotificationCategory.SYSTEM) {
            Label data = new Label("System Load: 35%   |   Uptime: 99.9%");
            data.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px;");
            Button lm = new Button("Learn More");
            lm.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;");
            lm.setOnMouseEntered(e -> lm.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(56,189,248,0.6); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            lm.setOnMouseExited(e -> lm.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            lm.setOnAction(e -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Detailed system metrics report opened.");
                com.iwfc.ui.components.DialogUtils.stylePremiumDialog(a);
                a.show();
            });
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            customBox.getChildren().addAll(data, sp, lm);
        } else if (n.getCategory() == NotificationCategory.MAINTENANCE && n.getTitle().contains("Urgency")) {
            Label status = new Label("Technician Dispatched (Est. 30 min)");
            status.setStyle("-fx-text-fill: #fca5a5; -fx-font-weight: bold; -fx-font-size: 12px;");
            Button ackBtn = new Button("Acknowledge");
            ackBtn.setStyle("-fx-background-color: rgba(234, 88, 12, 0.2); -fx-text-fill: #fdba74; -fx-border-color: #ea580c; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;");
            ackBtn.setOnAction(e -> {
                status.setText("Acknowledged by " + facade.getCurrentUser().getName());
                ackBtn.setDisable(true);
            });
            Button viewBtn = new Button("View Report Details");
            viewBtn.setStyle("-fx-background-color: rgba(220, 38, 38, 0.2); -fx-text-fill: #fca5a5; -fx-border-color: #dc2626; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;");
            viewBtn.setOnMouseEntered(e -> viewBtn.setStyle("-fx-background-color: rgba(220, 38, 38, 0.5); -fx-text-fill: white; -fx-border-color: #ef4444; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            viewBtn.setOnMouseExited(e -> viewBtn.setStyle("-fx-background-color: rgba(220, 38, 38, 0.2); -fx-text-fill: #fca5a5; -fx-border-color: #dc2626; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            viewBtn.setOnAction(e -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Loading full diagnostic report for Cable Machine.");
                com.iwfc.ui.components.DialogUtils.stylePremiumDialog(a);
                a.show();
            });
            customBox.getChildren().addAll(status, ackBtn, viewBtn);
        } else if (n.getCategory() == NotificationCategory.SESSION) {
            Button bookBtn = new Button("Book Session");
            bookBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: rgba(16, 185, 129, 0.5); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;");
            bookBtn.setOnMouseEntered(e -> bookBtn.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: white; -fx-border-color: #10b981; -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            bookBtn.setOnMouseExited(e -> bookBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: rgba(16, 185, 129, 0.5); -fx-border-radius: 20px; -fx-padding: 4 12; -fx-cursor: hand;"));
            bookBtn.setOnAction(e -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Booking slot secured for Pilates Reformer Flow.");
                com.iwfc.ui.components.DialogUtils.stylePremiumDialog(a);
                a.show();
            });
            customBox.getChildren().add(bookBtn);
        }
        
        return customBox;
    }
}

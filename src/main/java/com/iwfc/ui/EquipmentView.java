package com.iwfc.ui;
import com.iwfc.ui.components.DialogUtils;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.StatusBadge;
import com.iwfc.ui.components.UsageRing;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import java.time.format.DateTimeFormatter;

import java.util.List;

/**
 * Equipment management view with card grid, search/filters, and add/edit functionality.
 */
public class EquipmentView extends VBox {

    private final IWFCSystemFacade facade;
    private VBox categoriesContainer;
    private String selectedCategory = null;
    private HBox statsContainer;
    private VBox rightColumn;
    private ComboBox<String> statusFilter;

    public EquipmentView(IWFCSystemFacade facade) {
        this.facade = facade;
        setSpacing(20);
        setPadding(new Insets(25));
        getStyleClass().add("equipment-view");
        buildUI();
    }

    private void buildUI() {
        // Header
        Label title = new Label("Equipment Management");
        title.getStyleClass().add("view-title");

        // Toolbar: search + filters + add button
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Search equipment...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(250);
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("action-button");
        
        statusFilter = new ComboBox<>();
        statusFilter.getItems().add("All Statuses");
        for (EquipmentStatus s : EquipmentStatus.values()) statusFilter.getItems().add(s.getDisplayName());
        statusFilter.setPromptText("Filter by Status");

        ComboBox<String> locationFilter = new ComboBox<>();
        locationFilter.getItems().add("All Locations");
        for (Location l : Location.values()) locationFilter.getItems().add(l.getDisplayName());
        locationFilter.setPromptText("Filter by Location");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Equipment");
        addBtn.getStyleClass().add("add-button");

        boolean isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;
        addBtn.setVisible(isAdmin);
        addBtn.setManaged(isAdmin);
        addBtn.setOnAction(e -> showAddDialog());

        Runnable applyFilters = () -> {
            String text = searchField.getText();
            EquipmentStatus filterStatus = null;
            String selStatus = statusFilter.getValue();
            if (selStatus != null && !"All Statuses".equals(selStatus)) {
                for (EquipmentStatus s : EquipmentStatus.values()) {
                    if (s.getDisplayName().equals(selStatus)) { filterStatus = s; break; }
                }
            }
            Location filterLoc = null;
            String selLoc = locationFilter.getValue();
            if (selLoc != null && !"All Locations".equals(selLoc)) {
                for (Location l : Location.values()) {
                    if (l.getDisplayName().equals(selLoc)) { filterLoc = l; break; }
                }
            }
            filterCards(text, filterStatus, filterLoc);
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        statusFilter.setOnAction(e -> applyFilters.run());
        locationFilter.setOnAction(e -> applyFilters.run());

        refreshBtn.setOnAction(e -> {
            searchField.clear();
            statusFilter.getSelectionModel().clearSelection();
            locationFilter.getSelectionModel().clearSelection();
            refreshCards();
        });

        toolbar.getChildren().addAll(searchField, statusFilter, locationFilter, refreshBtn, spacer, addBtn);

        // Stats container
        statsContainer = new HBox(15);
        statsContainer.setAlignment(Pos.CENTER_LEFT);
        statsContainer.setPadding(new Insets(10, 0, 10, 0));

        // Card grid
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("equipment-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        categoriesContainer = new VBox(25);
        categoriesContainer.setPadding(new Insets(10, 0, 20, 0));

        scrollPane.setContent(categoriesContainer);

        HBox mainContent = new HBox(20);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        
        VBox leftColumn = new VBox(20);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        
        rightColumn = new VBox(20);
        rightColumn.setPrefWidth(350);
        rightColumn.setMinWidth(350);
        
        leftColumn.getChildren().addAll(statsContainer, scrollPane);
        mainContent.getChildren().addAll(leftColumn, rightColumn);

        getChildren().addAll(title, toolbar, mainContent);

        refreshCards();
    }

    private void refreshCards() {
        categoriesContainer.getChildren().clear();
        List<Equipment> equipment = facade.getAllEquipment();
        renderCategorizedEquipment(equipment);
        updateStats();
        updateRightColumn();
    }
    
    private void updateRightColumn() {
        rightColumn.getChildren().clear();
        
        // 1. Gym Floor Map
        VBox mapPanel = new VBox(15);
        mapPanel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12px; -fx-padding: 15;");
        Label mapTitle = new Label("Gym Floor Map");
        mapTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Pane mapPane = new Pane();
        mapPane.setPrefHeight(250);
        mapPane.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px;");
        
        // Generate plotting nodes based on equipment data
        for(Equipment eq : facade.getAllEquipment()) {
            Circle dot = new Circle(5);
            switch (eq.getStatus()) {
                case OPERATIONAL -> dot.setFill(Color.web("#4ade80"));
                case UNDER_MAINTENANCE -> dot.setFill(Color.web("#f59e0b"));
                default -> dot.setFill(Color.web("#ef4444"));
            }
            
            // Generate stable dummy positions
            dot.setCenterX(15 + Math.abs(eq.hashCode()) % 290);
            dot.setCenterY(15 + Math.abs(eq.hashCode() * 31) % 220);
            
            Tooltip tip = new Tooltip(eq.getName() + " (" + eq.getStatus().getDisplayName() + ")");
            Tooltip.install(dot, tip);
            mapPane.getChildren().add(dot);
        }
        mapPanel.getChildren().addAll(mapTitle, mapPane);
        
        // 2. Recent Maintenance & Alerts
        VBox alertsPanel = new VBox(15);
        alertsPanel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12px; -fx-padding: 15;");
        VBox.setVgrow(alertsPanel, Priority.ALWAYS);
        Label alertsTitle = new Label("Recent Maintenance & Alerts");
        alertsTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        VBox alertsList = new VBox(10);
        List<MaintenanceRequest> requests = facade.getAllMaintenanceRequests();
        // Sort by reported date descending (most recent first)
        requests.sort((a,b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));

        for(int i=0; i<Math.min(5, requests.size()); i++) {
            MaintenanceRequest req = requests.get(i);
            HBox alertRow = new HBox(10);
            alertRow.setAlignment(Pos.CENTER_LEFT);
            alertRow.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.1) transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
            
            Label icon = new Label(req.getUrgency() == Urgency.HIGH ? "🔴" : "⚠️");
            VBox textCol = new VBox(2);
            Label eqName = new Label(req.getEquipment().getName() + " - " + req.getDescription());
            eqName.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-wrap-text: true;");
            eqName.setMaxWidth(280);
            
            Label statDate = new Label(req.getStatus().getDisplayName() + " - " + req.getCreatedDate().format(DateTimeFormatter.ofPattern("MMM d, HH:mm")));
            statDate.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            
            textCol.getChildren().addAll(eqName, statDate);
            alertRow.getChildren().addAll(icon, textCol);
            alertsList.getChildren().add(alertRow);
        }
        
        if(requests.isEmpty()) {
            Label empty = new Label("No recent alerts.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-padding: 10 0;");
            alertsList.getChildren().add(empty);
        }

        alertsPanel.getChildren().addAll(alertsTitle, alertsList);
        
        rightColumn.getChildren().addAll(mapPanel, alertsPanel);
    }

    private void updateStats() {
        statsContainer.getChildren().clear();
        statsContainer.setSpacing(15);
        java.util.Map<EquipmentStatus, Long> summary = facade.getEquipmentSummary();
        
        VBox overviewPanel = new VBox(15);
        overviewPanel.getStyleClass().add("overview-panel");
        HBox.setHgrow(overviewPanel, Priority.ALWAYS);
        
        Label overviewTitle = new Label("Equipment Summary");
        overviewTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        
        HBox blocksContainer = new HBox(15);
        blocksContainer.setAlignment(Pos.CENTER_LEFT);
        
        VBox opBlock = createStatBlock("OPERATIONAL", summary.getOrDefault(EquipmentStatus.OPERATIONAL, 0L), "#4ade80");
        VBox faultyBlock = createStatBlock("FAULTY", summary.getOrDefault(EquipmentStatus.FAULTY, 0L), "#ef4444");
        VBox maintBlock = createStatBlock("MAINTENANCE", summary.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L), "#f59e0b");
        
        blocksContainer.getChildren().addAll(opBlock, faultyBlock, maintBlock);
        
        overviewPanel.getChildren().addAll(overviewTitle, blocksContainer);
        
        statsContainer.getChildren().add(overviewPanel);
    }
    
    private VBox createStatBlock(String title, long count, String colorHex) {
        VBox block = new VBox(5);
        block.setPadding(new Insets(15));
        block.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 8px; -fx-cursor: hand;");
        HBox.setHgrow(block, Priority.ALWAYS);
        
        block.setOnMouseClicked(e -> {
            switch (title) {
                case "OPERATIONAL" -> statusFilter.setValue(EquipmentStatus.OPERATIONAL.getDisplayName());
                case "FAULTY" -> statusFilter.setValue(EquipmentStatus.FAULTY.getDisplayName());
                case "MAINTENANCE" -> statusFilter.setValue(EquipmentStatus.UNDER_MAINTENANCE.getDisplayName());
            }
        });
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String icon = "✔️";
        if (title.equals("FAULTY")) icon = "⚠️";
        if (title.equals("MAINTENANCE")) icon = "📅";
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-text-fill: rgba(0,0,0,0.4); -fx-font-size: 16px; -fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 12px; -fx-padding: 2 6;");
        
        topRow.getChildren().addAll(titleLbl, spacer, iconLbl);
        
        HBox bottomRow = new HBox(15);
        bottomRow.setAlignment(Pos.BOTTOM_LEFT);
        Label countLbl = new Label(String.valueOf(count));
        countLbl.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        Polyline sparkline = new Polyline();
        // Generate stable dummy data based on title hash to simulate a trend
        double[] points = new double[10];
        java.util.Random rnd = new java.util.Random(title.hashCode());
        double y = 20;
        for (int i=0; i<5; i++) {
            points[i*2] = i * 20.0;
            points[i*2 + 1] = y + (rnd.nextDouble() * 15 - 7.5);
            y = points[i*2 + 1];
        }
        for (Double d : points) sparkline.getPoints().add(d);
        sparkline.setStroke(Color.web("rgba(0,0,0,0.5)"));
        sparkline.setStrokeWidth(2);
        sparkline.setFill(Color.TRANSPARENT);
        
        bottomRow.getChildren().addAll(countLbl, spacer2, sparkline);
        
        block.getChildren().addAll(topRow, bottomRow);
        
        block.setOnMouseEntered(e -> block.setStyle("-fx-background-color: " + colorHex + "dd; -fx-background-radius: 8px; -fx-cursor: hand;"));
        block.setOnMouseExited(e -> block.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 8px; -fx-cursor: hand;"));
        
        return block;
    }

    private VBox createEquipmentCard(Equipment eq) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setPrefWidth(260); // slightly wider to fit everything nicely
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.getStyleClass().add("equipment-card-premium");

        // Hover animation
        card.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
            card.setStyle("-fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-background-color: rgba(255,255,255,0.08);");
        });
        card.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            card.setStyle("");
        });

        // Top row: icon + name + ID + menu
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label iconLbl = new Label("🏋️"); // default icon
        if (eq.getName().toLowerCase().contains("treadmill")) iconLbl.setText("🏃");
        else if (eq.getName().toLowerCase().contains("bike")) iconLbl.setText("🚴");
        else if (eq.getName().toLowerCase().contains("row")) iconLbl.setText("🚣");
        iconLbl.setStyle("-fx-font-size: 18px;");

        VBox nameBox = new VBox(2);
        Label name = new Label(eq.getName());
        name.getStyleClass().add("equipment-name");
        name.setWrapText(true);
        Label id = new Label("ID: " + eq.getEquipmentId());
        id.getStyleClass().add("equipment-id");
        nameBox.getChildren().addAll(name, id);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label menuBtn = new Label("⋮");
        menuBtn.setStyle("-fx-font-size: 18px; -fx-text-fill: #9ca3af; -fx-cursor: hand;");

        topRow.getChildren().addAll(iconLbl, nameBox, spacer, menuBtn);

        // Middle section: Usage Ring + Details
        HBox middleRow = new HBox(15);
        middleRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox leftMiddle = new VBox(5);
        leftMiddle.setAlignment(Pos.CENTER);
        UsageRing ring = new UsageRing(eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold(), eq.getStatus());
        StatusBadge statusBadge = StatusBadge.forEquipmentStatus(eq.getStatus());
        leftMiddle.getChildren().addAll(ring, statusBadge);
        
        VBox rightMiddle = new VBox(8);
        rightMiddle.setAlignment(Pos.CENTER_LEFT);
        
        double percentage = (eq.getCumulativeUsageHours() / eq.getMaintenanceThreshold()) * 100;
        Label usageTxt = new Label(String.format("%.0f of %.0f hours (%.0f%%)", 
            eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold(), percentage));
        usageTxt.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        
        Label estMaint = new Label("Est. Maint: 12/23");
        estMaint.setStyle("-fx-text-fill: #51CF66; -fx-font-size: 11px;");
        if (eq.getStatus() == EquipmentStatus.FAULTY) estMaint.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 11px;");
        else if (eq.getStatus() == EquipmentStatus.UNDER_MAINTENANCE) estMaint.setStyle("-fx-text-fill: #FFD43B; -fx-font-size: 11px;");
        
        Label location = new Label("📍 " + eq.getLocation().getDisplayName());
        location.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        
        rightMiddle.getChildren().addAll(usageTxt, estMaint, location);
        middleRow.getChildren().addAll(leftMiddle, rightMiddle);

        // Last service text
        Label lastService = new Label("Last Service: 12/21");
        lastService.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        // Bottom row buttons
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER);
        Button viewDetailsCardBtn = new Button("View Details");
        viewDetailsCardBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-text-fill: #e5e7eb; -fx-font-size: 11px; -fx-padding: 5 15; -fx-cursor: hand;");
        Button logMaintCardBtn = new Button("Log Maintenance");
        logMaintCardBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-text-fill: #e5e7eb; -fx-font-size: 11px; -fx-padding: 5 15; -fx-cursor: hand;");
        
        // Hover effects for card buttons
        viewDetailsCardBtn.setOnMouseEntered(e -> viewDetailsCardBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-border-color: rgba(255,255,255,0.4); -fx-border-radius: 4px; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-padding: 5 15; -fx-cursor: hand;"));
        viewDetailsCardBtn.setOnMouseExited(e -> viewDetailsCardBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-text-fill: #e5e7eb; -fx-font-size: 11px; -fx-padding: 5 15; -fx-cursor: hand;"));
        logMaintCardBtn.setOnMouseEntered(e -> logMaintCardBtn.setStyle("-fx-background-color: rgba(81, 207, 102, 0.15); -fx-border-color: #51CF66; -fx-border-radius: 4px; -fx-text-fill: #51CF66; -fx-font-size: 11px; -fx-padding: 5 15; -fx-cursor: hand;"));
        logMaintCardBtn.setOnMouseExited(e -> logMaintCardBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-text-fill: #e5e7eb; -fx-font-size: 11px; -fx-padding: 5 15; -fx-cursor: hand;"));

        viewDetailsCardBtn.setOnAction(e -> showEquipmentDetailsDialog(eq));
        
        logMaintCardBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Mark equipment as Operational and log maintenance?", ButtonType.YES, ButtonType.NO);
        DialogUtils.stylePremiumDialog(confirm);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try {
                        eq.resetUsageHours();
                        facade.editEquipment(eq.getEquipmentId(), eq.getName(), eq.getCategory(), EquipmentStatus.OPERATIONAL, eq.getLocation(), eq.getMaintenanceThreshold());
                        refreshCards();
                    } catch (UnauthorizedAccessException ex) {
                        showAlert("Access Denied", ex.getMessage());
                    }
                }
            });
        });
        HBox.setHgrow(viewDetailsCardBtn, Priority.ALWAYS);
        HBox.setHgrow(logMaintCardBtn, Priority.ALWAYS);
        viewDetailsCardBtn.setMaxWidth(Double.MAX_VALUE);
        logMaintCardBtn.setMaxWidth(Double.MAX_VALUE);
        bottomRow.getChildren().addAll(viewDetailsCardBtn, logMaintCardBtn);

        card.getChildren().addAll(topRow, middleRow, lastService, bottomRow);

        // Context menu for admin & instructor
        Role role = facade.getCurrentUser().getRole();
        if (role == Role.ADMIN || role == Role.INSTRUCTOR) {
            ContextMenu contextMenu = new ContextMenu();

            MenuItem logUsageItem = new MenuItem("Log Usage");
            logUsageItem.setOnAction(e -> showLogUsageDialog(eq));

            MenuItem historyItem = new MenuItem("View Usage History");
            historyItem.setOnAction(e -> showUsageHistoryDialog(eq));

            contextMenu.getItems().addAll(logUsageItem, historyItem);

            if (role == Role.ADMIN) {
                MenuItem editItem = new MenuItem("Edit");
                editItem.setOnAction(e -> showEditDialog(eq));

                MenuItem deactivateItem = new MenuItem("Deactivate");
                deactivateItem.setOnAction(e -> {
                    try {
                        facade.deactivateEquipment(eq.getEquipmentId());
                        refreshCards();
                    } catch (UnauthorizedAccessException ex) {
                        showAlert("Access Denied", ex.getMessage());
                    }
                });

                MenuItem removeItem = new MenuItem("Remove");
                removeItem.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to completely remove this equipment?", ButtonType.YES, ButtonType.NO);
        DialogUtils.stylePremiumDialog(confirm);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            try {
                                facade.removeEquipment(eq.getEquipmentId());
                                refreshCards();
                            } catch (UnauthorizedAccessException | IllegalStateException ex) {
                                showAlert("Error", ex.getMessage());
                            }
                        }
                    });
                });

                contextMenu.getItems().addAll(editItem, deactivateItem, removeItem);
            }

            // Attach menu to the menu button instead of the entire card
            menuBtn.setOnMouseClicked(e -> {
                contextMenu.show(menuBtn, e.getScreenX(), e.getScreenY());
            });
        }

        return card;
    }

    private void filterCards(String searchText, EquipmentStatus statusFilter,
                             Location locationFilter) {
        categoriesContainer.getChildren().clear();
        List<Equipment> equipment = facade.getAllEquipment();
        List<Equipment> filtered = new java.util.ArrayList<>();

        for (Equipment eq : equipment) {
            boolean matches = true;
            if (searchText != null && !searchText.isEmpty()) {
                String lower = searchText.toLowerCase();
                matches = eq.getName().toLowerCase().contains(lower) ||
                          eq.getEquipmentId().toLowerCase().contains(lower);
            }
            if (statusFilter != null && eq.getStatus() != statusFilter) {
                matches = false;
            }
            if (locationFilter != null && eq.getLocation() != locationFilter) {
                matches = false;
            }
            if (matches) {
                filtered.add(eq);
            }
        }
        renderCategorizedEquipment(filtered);
    }
    
    private void renderCategorizedEquipment(List<Equipment> equipment) {
        if (selectedCategory == null) {
            java.util.Map<String, java.util.List<Equipment>> categoryMap = new java.util.LinkedHashMap<>();
            for (Equipment eq : equipment) {
                categoryMap.computeIfAbsent(eq.getCategory(), k -> new java.util.ArrayList<>()).add(eq);
            }
            
            FlowPane categoryGrid = new FlowPane(20, 20);
            categoryGrid.setPadding(new Insets(10));
            
            for (java.util.Map.Entry<String, java.util.List<Equipment>> entry : categoryMap.entrySet()) {
                VBox catCard = createCategoryCard(entry.getKey(), entry.getValue());
                catCard.setOnMouseClicked(e -> {
                    selectedCategory = entry.getKey();
                    refreshCards();
                });
                categoryGrid.getChildren().add(catCard);
            }
            categoriesContainer.getChildren().add(categoryGrid);
            
        } else {
            Button backBtn = new Button("← Back to Categories");
            backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #06b6d4; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 10 5;");
            backBtn.setOnAction(e -> {
                selectedCategory = null;
                refreshCards();
            });
            
            Label title = new Label(selectedCategory + " Equipment");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 15 5;");
            
            FlowPane eqGrid = new FlowPane(15, 15);
            eqGrid.setPadding(new Insets(5));
            
            for (Equipment eq : equipment) {
                if (eq.getCategory().equals(selectedCategory)) {
                    eqGrid.getChildren().add(createEquipmentCard(eq));
                }
            }
            
            categoriesContainer.getChildren().addAll(backBtn, title, eqGrid);
        }
    }
    
    private VBox createCategoryCard(String categoryName, java.util.List<Equipment> items) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(230, 260);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12px; -fx-cursor: hand;");
        
        String tempIcon = "🏋️";
        String tempColor = "#3b82f6";
        if (categoryName.toLowerCase().contains("cardio")) { tempIcon = "🏃"; tempColor = "#ef4444"; }
        else if (categoryName.toLowerCase().contains("strength")) { tempIcon = "💪"; tempColor = "#eab308"; }
        else if (categoryName.toLowerCase().contains("studio")) { tempIcon = "🧘"; tempColor = "#a855f7"; }
        else if (categoryName.toLowerCase().contains("functional")) { tempIcon = "🤸"; tempColor = "#f97316"; }
        else if (categoryName.toLowerCase().contains("mobility")) { tempIcon = "🩹"; tempColor = "#06b6d4"; }
        
        final String icon = tempIcon;
        final String color = tempColor;
        
        // Calculate status counts
        int good = 0; int minor = 0; int faulty = 0;
        for(Equipment eq : items) {
            switch (eq.getStatus()) {
                case OPERATIONAL -> good++;
                case UNDER_MAINTENANCE -> minor++;
                default -> faulty++;
            }
        }
        int total = items.size();
        
        // Donut Chart (Health Ring)
        StackPane ringPane = new StackPane();
        ringPane.setPrefSize(100, 100);
        ringPane.setMaxSize(100, 100);
        
        double radiusX = 40;
        double radiusY = 40;
        double strokeWidth = 8;
        
        // Base ring (background)
        Arc bgArc = new Arc(0, 0, radiusX, radiusY, 0, 360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStrokeWidth(strokeWidth);
        bgArc.setStroke(Color.web("rgba(255,255,255,0.1)"));
        bgArc.setFill(Color.TRANSPARENT);
        ringPane.getChildren().add(bgArc);
        
        // Good Arc (Green)
        double goodAngle = (double) good / total * 360.0;
        if(good > 0) {
            Arc goodArc = new Arc(0, 0, radiusX, radiusY, 90, -goodAngle); // Start top, go clockwise
            goodArc.setType(ArcType.OPEN);
            goodArc.setStrokeWidth(strokeWidth);
            goodArc.setStroke(Color.web("#4ade80")); // Operational green
            goodArc.setFill(Color.TRANSPARENT);
            ringPane.getChildren().add(goodArc);
        }
        
        // Minor Arc (Yellow)
        double minorAngle = (double) minor / total * 360.0;
        if(minor > 0) {
            Arc minorArc = new Arc(0, 0, radiusX, radiusY, 90 - goodAngle, -minorAngle);
            minorArc.setType(ArcType.OPEN);
            minorArc.setStrokeWidth(strokeWidth);
            minorArc.setStroke(Color.web("#f59e0b")); // Maintenance yellow
            minorArc.setFill(Color.TRANSPARENT);
            ringPane.getChildren().add(minorArc);
        }
        
        // Faulty Arc (Red)
        double faultyAngle = (double) faulty / total * 360.0;
        if(faulty > 0) {
            Arc faultyArc = new Arc(0, 0, radiusX, radiusY, 90 - goodAngle - minorAngle, -faultyAngle);
            faultyArc.setType(ArcType.OPEN);
            faultyArc.setStrokeWidth(strokeWidth);
            faultyArc.setStroke(Color.web("#ef4444")); // Faulty red
            faultyArc.setFill(Color.TRANSPARENT);
            ringPane.getChildren().add(faultyArc);
        }
        
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 36px; -fx-text-fill: " + color + "; -fx-effect: dropshadow(gaussian, " + color + "66, 15, 0, 0, 0);");
        ringPane.getChildren().add(iconLbl);
        
        Label nameLbl = new Label(categoryName);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label countLbl = new Label(total + " Items");
        countLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Subtitle legend
        Label legendLbl = new Label(good + " Good / " + minor + " Minor / " + faulty + " Faulty");
        legendLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        
        // Fake Next Maintenance
        String nextMaint = "Next Maintenance: 15 Nov (" + (total/2 + 1) + " items)";
        Label maintLbl = new Label(nextMaint);
        maintLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 10px;");
        
        // Bottom Action Row
        HBox actionRow = new HBox(10);
        actionRow.setPadding(new Insets(10, 20, 0, 20));
        actionRow.setAlignment(Pos.CENTER);
        
        Button locBtn = new Button("📍");
        locBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #3b82f6; -fx-background-radius: 5px; -fx-padding: 3 8; -fx-cursor: hand;");
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        Button srvBtn = new Button("🔧");
        srvBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #3b82f6; -fx-background-radius: 5px; -fx-padding: 3 8; -fx-cursor: hand;");
        actionRow.getChildren().addAll(locBtn, actionSpacer, srvBtn);
        
        card.getChildren().addAll(ringPane, nameLbl, countLbl, legendLbl, maintLbl, actionRow);
        
        card.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
            card.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 12px; -fx-border-color: " + color + "88; -fx-border-radius: 12px; -fx-cursor: hand;");
        });
        
        card.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12px; -fx-cursor: hand;");
        });
        
        return card;
    }

    private void showEquipmentDetailsDialog(Equipment eq) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Equipment Details — " + eq.getName());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label title = new Label(eq.getName() + " (" + eq.getEquipmentId() + ")");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        grid.add(new Label("Category:"), 0, 0);
        grid.add(new Label(eq.getCategory()), 1, 0);
        
        grid.add(new Label("Location:"), 0, 1);
        grid.add(new Label(eq.getLocation().getDisplayName()), 1, 1);
        
        grid.add(new Label("Status:"), 0, 2);
        grid.add(new Label(eq.getStatus().getDisplayName()), 1, 2);
        
        grid.add(new Label("Usage Hours:"), 0, 3);
        grid.add(new Label(String.format("%.1f / %.1f", eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold())), 1, 3);
        
        Label historyLbl = new Label("Recent Usage History:");
        historyLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0; -fx-padding: 10 0 0 0;");
        
        ListView<String> listView = new ListView<>();
        for (UsageLogEntry entry : eq.getUsageLog()) {
            String ts = entry.getTimestamp().toString();
            String notes = entry.getNotes() != null && !entry.getNotes().isEmpty() ? " - " + entry.getNotes() : "";
            listView.getItems().add(String.format("%s - %.1f hrs by %s%s",
                ts, entry.getHoursLogged(), entry.getLoggedByUserId(), notes));
        }
        listView.setPrefSize(400, 200);
        
        content.getChildren().addAll(title, grid, historyLbl, listView);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }

    private void showAddDialog() {
        Dialog<Equipment> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Add Equipment");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        TextField nameField = new TextField();
        nameField.setPromptText("Equipment Name");
        TextField idField = new TextField();
        idField.setPromptText("Equipment ID (e.g., EQ009)");
        ComboBox<Location> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll(Location.values());
        locationCombo.setPromptText("Select Location");
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Cardio", "Strength (Machines)", "Strength (Free Weights)", "Studio & Group", "Functional", "Mobility & Recovery", "Other");
        categoryCombo.setPromptText("Select Category");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locationCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !idField.getText().isEmpty()
                    && !nameField.getText().isEmpty() && locationCombo.getValue() != null
                    && categoryCombo.getValue() != null) {
                return new Equipment(idField.getText().trim(), nameField.getText().trim(),
                        categoryCombo.getValue(), locationCombo.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(eq -> {
            try {
                facade.addEquipment(eq);
                refreshCards();
            } catch (DuplicateDataException | UnauthorizedAccessException ex) {
                showAlert("Error", ex.getMessage());
            }
        });
    }

    private void showEditDialog(Equipment eq) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Edit Equipment — " + eq.getEquipmentId());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        TextField nameField = new TextField(eq.getName());
        ComboBox<EquipmentStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(EquipmentStatus.values());
        statusCombo.setValue(eq.getStatus());
        ComboBox<Location> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll(Location.values());
        locationCombo.setValue(eq.getLocation());
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Cardio", "Strength (Machines)", "Strength (Free Weights)", "Studio & Group", "Functional", "Mobility & Recovery", "Other");
        categoryCombo.setValue(eq.getCategory());
        
        TextField thresholdField = new TextField(String.valueOf(eq.getMaintenanceThreshold()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryCombo, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusCombo, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locationCombo, 1, 3);
        grid.add(new Label("Maintenance Threshold:"), 0, 4);
        grid.add(thresholdField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK);

        dialog.showAndWait().ifPresent(ok -> {
            if (ok) {
                try {
                    double threshold = eq.getMaintenanceThreshold();
                    try {
                        threshold = Double.parseDouble(thresholdField.getText().trim());
                    } catch (NumberFormatException ignored) {}
                    
                    facade.editEquipment(eq.getEquipmentId(),
                            nameField.getText(), categoryCombo.getValue(), statusCombo.getValue(), locationCombo.getValue(), threshold);
                    refreshCards();
                } catch (UnauthorizedAccessException ex) {
                    showAlert("Access Denied", ex.getMessage());
                }
            }
        });
    }

    private void showLogUsageDialog(Equipment eq) {
        Dialog<Double> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Log Usage — " + eq.getEquipmentId());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        
        TextField hoursField = new TextField();
        hoursField.setPromptText("Hours to log");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Hours:"), 0, 0);
        grid.add(hoursField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return Double.valueOf(hoursField.getText().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(hours -> {
            try {
                facade.logEquipmentUsage(eq.getEquipmentId(), hours);
                refreshCards();
            } catch (UnauthorizedAccessException | IllegalArgumentException ex) {
                showAlert("Error", ex.getMessage());
            }
        });
    }

    private void showUsageHistoryDialog(Equipment eq) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Usage History — " + eq.getEquipmentId());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        
        ListView<String> listView = new ListView<>();
        for (UsageLogEntry entry : eq.getUsageLog()) {
            String ts = entry.getTimestamp().toString();
            String notes = entry.getNotes() != null && !entry.getNotes().isEmpty() ? " - " + entry.getNotes() : "";
            listView.getItems().add(String.format("%s - %.1f hrs by %s%s",
                ts, entry.getHoursLogged(), entry.getLoggedByUserId(), notes));
        }
        listView.setPrefSize(400, 300);
        
        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        DialogUtils.stylePremiumDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}


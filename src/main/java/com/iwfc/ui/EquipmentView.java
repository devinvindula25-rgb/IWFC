package com.iwfc.ui;

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

import java.util.List;

/**
 * Equipment management view with card grid, search/filters, and add/edit functionality.
 */
public class EquipmentView extends VBox {

    private final IWFCSystemFacade facade;
    private FlowPane cardGrid;
    private HBox statsContainer;

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
        
        ComboBox<String> statusFilter = new ComboBox<>();
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

        cardGrid = new FlowPane(15, 15);
        cardGrid.setPadding(new Insets(5));
        cardGrid.getStyleClass().add("equipment-grid");

        scrollPane.setContent(cardGrid);

        getChildren().addAll(title, toolbar, statsContainer, scrollPane);

        refreshCards();
    }

    private void refreshCards() {
        cardGrid.getChildren().clear();
        List<Equipment> equipment = facade.getAllEquipment();
        for (Equipment eq : equipment) {
            cardGrid.getChildren().add(createEquipmentCard(eq));
        }
        updateStats();
    }

    private void updateStats() {
        statsContainer.getChildren().clear();
        java.util.Map<EquipmentStatus, Long> summary = facade.getEquipmentSummary();
        
        Label opLabel = new Label("🟢 " + summary.getOrDefault(EquipmentStatus.OPERATIONAL, 0L) + " Operational");
        opLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
        
        Label faultyLabel = new Label("🔴 " + summary.getOrDefault(EquipmentStatus.FAULTY, 0L) + " Faulty");
        faultyLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        
        Label maintLabel = new Label("🔧 " + summary.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L) + " Under Maintenance");
        maintLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        
        statsContainer.getChildren().addAll(opLabel, faultyLabel, maintLabel);
    }

    private VBox createEquipmentCard(Equipment eq) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setPrefWidth(250);
        card.setPrefHeight(180);
        card.getStyleClass().add("equipment-card-premium");

        // Hover animation
        card.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        card.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        // Top row: name + usage ring
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(3);
        Label name = new Label(eq.getName());
        name.getStyleClass().add("equipment-name");
        Label category = new Label(eq.getCategory());
        category.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
        Label id = new Label("ID: " + eq.getEquipmentId());
        id.getStyleClass().add("equipment-id");
        nameBox.getChildren().addAll(name, category, id);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        UsageRing ring = new UsageRing(eq.getUsageHours(), eq.getMaintenanceThreshold());

        topRow.getChildren().addAll(nameBox, ring);

        // Status badge
        StatusBadge statusBadge = StatusBadge.forEquipmentStatus(eq.getStatus());

        // Location
        Label location = new Label("📍 " + eq.getLocation().getDisplayName());
        location.getStyleClass().add("equipment-location");

        // Usage text
        Label usage = new Label(String.format("%.0f / %.0f hrs",
                eq.getUsageHours(), eq.getMaintenanceThreshold()));
        usage.getStyleClass().add("equipment-usage");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(topRow, statusBadge, location, spacer, usage);

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

            card.setOnContextMenuRequested(e ->
                contextMenu.show(card, e.getScreenX(), e.getScreenY()));
        }

        return card;
    }

    private void filterCards(String searchText, EquipmentStatus statusFilter,
                             Location locationFilter) {
        cardGrid.getChildren().clear();
        List<Equipment> equipment = facade.getAllEquipment();

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
                cardGrid.getChildren().add(createEquipmentCard(eq));
            }
        }
    }

    private void showAddDialog() {
        Dialog<Equipment> dialog = new Dialog<>();
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
        categoryCombo.getItems().addAll("Treadmill", "Spin Bike", "Rowing Machine", "Free Weights", "Cable Machine", "Mat/Accessory", "Other");
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
        categoryCombo.getItems().addAll("Treadmill", "Spin Bike", "Rowing Machine", "Free Weights", "Cable Machine", "Mat/Accessory", "Other");
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
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

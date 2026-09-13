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
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCards(newVal, null, null));

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setPromptText("Status");
        statusFilter.getItems().add("All Statuses");
        for (EquipmentStatus s : EquipmentStatus.values()) {
            statusFilter.getItems().add(s.getDisplayName());
        }
        statusFilter.getStyleClass().add("filter-combo");

        ComboBox<String> locationFilter = new ComboBox<>();
        locationFilter.setPromptText("Location");
        locationFilter.getItems().add("All Locations");
        for (Location l : Location.values()) {
            locationFilter.getItems().add(l.getDisplayName());
        }
        locationFilter.getStyleClass().add("filter-combo");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Equipment");
        addBtn.getStyleClass().add("add-button");

        boolean isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;
        addBtn.setVisible(isAdmin);
        addBtn.setManaged(isAdmin);
        addBtn.setOnAction(e -> showAddDialog());

        statusFilter.setOnAction(e -> {
            String selected = statusFilter.getValue();
            EquipmentStatus filterStatus = null;
            if (selected != null && !"All Statuses".equals(selected)) {
                for (EquipmentStatus s : EquipmentStatus.values()) {
                    if (s.getDisplayName().equals(selected)) { filterStatus = s; break; }
                }
            }
            filterCards(searchField.getText(), filterStatus, null);
        });

        toolbar.getChildren().addAll(searchField, statusFilter, locationFilter, spacer, addBtn);

        // Card grid
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("equipment-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        cardGrid = new FlowPane(15, 15);
        cardGrid.setPadding(new Insets(5));
        cardGrid.getStyleClass().add("equipment-grid");

        scrollPane.setContent(cardGrid);

        getChildren().addAll(title, toolbar, scrollPane);

        refreshCards();
    }

    private void refreshCards() {
        cardGrid.getChildren().clear();
        List<Equipment> equipment = facade.getAllEquipment();
        for (Equipment eq : equipment) {
            cardGrid.getChildren().add(createEquipmentCard(eq));
        }
    }

    private VBox createEquipmentCard(Equipment eq) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setPrefWidth(250);
        card.setPrefHeight(180);
        card.getStyleClass().add("equipment-card");

        // Top row: name + usage ring
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(3);
        Label name = new Label(eq.getName());
        name.getStyleClass().add("equipment-name");
        Label id = new Label("ID: " + eq.getEquipmentId());
        id.getStyleClass().add("equipment-id");
        nameBox.getChildren().addAll(name, id);
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

        // Context menu for admin
        if (facade.getCurrentUser().getRole() == Role.ADMIN) {
            ContextMenu contextMenu = new ContextMenu();
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
            contextMenu.getItems().addAll(editItem, deactivateItem);
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
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

        TextField nameField = new TextField();
        nameField.setPromptText("Equipment Name");
        TextField idField = new TextField();
        idField.setPromptText("Equipment ID (e.g., EQ009)");
        ComboBox<Location> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll(Location.values());
        locationCombo.setPromptText("Select Location");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Location:"), 0, 2);
        grid.add(locationCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !idField.getText().isEmpty()
                    && !nameField.getText().isEmpty() && locationCombo.getValue() != null) {
                return new Equipment(idField.getText().trim(), nameField.getText().trim(),
                        locationCombo.getValue());
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

        TextField nameField = new TextField(eq.getName());
        ComboBox<EquipmentStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(EquipmentStatus.values());
        statusCombo.setValue(eq.getStatus());
        ComboBox<Location> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll(Location.values());
        locationCombo.setValue(eq.getLocation());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusCombo, 1, 1);
        grid.add(new Label("Location:"), 0, 2);
        grid.add(locationCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK);

        dialog.showAndWait().ifPresent(ok -> {
            if (ok) {
                try {
                    facade.editEquipment(eq.getEquipmentId(),
                            nameField.getText(), statusCombo.getValue(), locationCombo.getValue());
                    refreshCards();
                } catch (UnauthorizedAccessException ex) {
                    showAlert("Access Denied", ex.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

package com.iwfc.ui;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.StatusBadge;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Maintenance management view with request cards, status workflow,
 * and report/assign/complete actions.
 */
public class MaintenanceView extends VBox {

    private final IWFCSystemFacade facade;
    private VBox requestList;

    public MaintenanceView(IWFCSystemFacade facade) {
        this.facade = facade;
        setSpacing(20);
        setPadding(new Insets(25));
        getStyleClass().add("maintenance-view");
        buildUI();
    }

    private void buildUI() {
        // Header
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Maintenance Requests");
        title.getStyleClass().add("view-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean canReport = facade.getCurrentUser().getPermissions().contains("REPORT_MAINTENANCE");
        Button reportBtn = new Button("🔧 Report Issue");
        reportBtn.getStyleClass().add("add-button");
        reportBtn.setVisible(canReport);
        reportBtn.setManaged(canReport);
        reportBtn.setOnAction(e -> showReportDialog());

        headerRow.getChildren().addAll(title, spacer, reportBtn);

        // Filter buttons
        HBox filters = new HBox(8);
        filters.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup filterGroup = new ToggleGroup();
        ToggleButton allBtn = new ToggleButton("All");
        allBtn.setToggleGroup(filterGroup);
        allBtn.setSelected(true);
        allBtn.getStyleClass().add("filter-toggle");

        ToggleButton pendingBtn = new ToggleButton("Pending");
        pendingBtn.setToggleGroup(filterGroup);
        pendingBtn.getStyleClass().add("filter-toggle");

        ToggleButton assignedBtn = new ToggleButton("Assigned");
        assignedBtn.setToggleGroup(filterGroup);
        assignedBtn.getStyleClass().add("filter-toggle");

        ToggleButton completedBtn = new ToggleButton("Completed");
        completedBtn.setToggleGroup(filterGroup);
        completedBtn.getStyleClass().add("filter-toggle");

        allBtn.setOnAction(e -> refreshRequests(null));
        pendingBtn.setOnAction(e -> refreshRequests(RequestStatus.PENDING));
        assignedBtn.setOnAction(e -> refreshRequests(RequestStatus.ASSIGNED));
        completedBtn.setOnAction(e -> refreshRequests(RequestStatus.COMPLETED));

        filters.getChildren().addAll(allBtn, pendingBtn, assignedBtn, completedBtn);

        // Request list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("maintenance-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        requestList = new VBox(10);
        requestList.setPadding(new Insets(5));
        scrollPane.setContent(requestList);

        getChildren().addAll(headerRow, filters, scrollPane);

        refreshRequests(null);
    }

    private void refreshRequests(RequestStatus filterStatus) {
        requestList.getChildren().clear();

        List<MaintenanceRequest> requests;
        if (filterStatus != null) {
            requests = facade.getMaintenanceByStatus(filterStatus);
        } else {
            requests = facade.getAllMaintenanceRequests();
        }

        if (requests.isEmpty()) {
            Label empty = new Label("No maintenance requests found");
            empty.getStyleClass().add("empty-placeholder");
            requestList.getChildren().add(empty);
            return;
        }

        for (MaintenanceRequest req : requests) {
            requestList.getChildren().add(createRequestCard(req));
        }
    }

    private HBox createRequestCard(MaintenanceRequest req) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("maintenance-card");

        // Left: Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label eqName = new Label(req.getEquipment().getName());
        eqName.getStyleClass().add("maintenance-equipment-name");
        Label reqId = new Label("[" + req.getRequestId() + "]");
        reqId.getStyleClass().add("maintenance-id");
        titleRow.getChildren().addAll(eqName, reqId);

        Label desc = new Label(req.getDescription());
        desc.getStyleClass().add("maintenance-description");
        desc.setWrapText(true);

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label reportedBy = new Label("Reported by: " + req.getReportedBy().getName());
        reportedBy.getStyleClass().add("maintenance-meta");
        Label date = new Label(req.getCreatedDate().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        date.getStyleClass().add("maintenance-meta");
        metaRow.getChildren().addAll(reportedBy, date);

        if (req.getAssignedTo() != null) {
            Label assigned = new Label("Assigned to: " + req.getAssignedTo());
            assigned.getStyleClass().add("maintenance-meta");
            metaRow.getChildren().add(assigned);
        }

        info.getChildren().addAll(titleRow, desc, metaRow);

        // Middle: Status workflow dots
        VBox statusFlow = new VBox(3);
        statusFlow.setAlignment(Pos.CENTER);
        statusFlow.setMinWidth(100);

        // Workflow indicator
        HBox workflowDots = new HBox(4);
        workflowDots.setAlignment(Pos.CENTER);

        Label dot1 = new Label("●");
        dot1.getStyleClass().add(req.getStatus().ordinal() >= 0 ? "dot-active" : "dot-inactive");
        Label arrow1 = new Label("→");
        arrow1.getStyleClass().add("dot-arrow");
        Label dot2 = new Label("●");
        dot2.getStyleClass().add(req.getStatus().ordinal() >= 1 ? "dot-active" : "dot-inactive");
        Label arrow2 = new Label("→");
        arrow2.getStyleClass().add("dot-arrow");
        Label dot3 = new Label("●");
        dot3.getStyleClass().add(req.getStatus().ordinal() >= 2 ? "dot-active" : "dot-inactive");

        workflowDots.getChildren().addAll(dot1, arrow1, dot2, arrow2, dot3);

        StatusBadge statusBadge = StatusBadge.forRequestStatus(req.getStatus());
        statusFlow.getChildren().addAll(workflowDots, statusBadge);

        // Right: Urgency + actions
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(120);

        StatusBadge urgencyBadge = StatusBadge.forUrgency(req.getUrgency());
        actions.getChildren().add(urgencyBadge);

        boolean isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;
        if (isAdmin) {
            if (req.getStatus() == RequestStatus.PENDING) {
                Button assignBtn = new Button("Assign");
                assignBtn.getStyleClass().add("action-button");
                assignBtn.setOnAction(e -> showAssignDialog(req));
                actions.getChildren().add(assignBtn);
            } else if (req.getStatus() == RequestStatus.ASSIGNED) {
                Button completeBtn = new Button("Complete");
                completeBtn.getStyleClass().add("action-button-success");
                completeBtn.setOnAction(e -> {
                    try {
                        facade.completeMaintenance(req.getRequestId());
                        refreshRequests(null);
                    } catch (UnauthorizedAccessException ex) {
                        showAlert("Access Denied", ex.getMessage());
                    }
                });
                actions.getChildren().add(completeBtn);
            }
        }

        card.getChildren().addAll(info, statusFlow, actions);
        return card;
    }

    private void showReportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Report Maintenance Issue");

        ComboBox<Equipment> equipCombo = new ComboBox<>();
        equipCombo.getItems().addAll(facade.getAllEquipment());
        equipCombo.setPromptText("Select Equipment");
        equipCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " [" + item.getEquipmentId() + "]");
            }
        });
        equipCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " [" + item.getEquipmentId() + "]");
            }
        });

        TextArea descField = new TextArea();
        descField.setPromptText("Describe the issue...");
        descField.setPrefRowCount(3);

        ComboBox<Urgency> urgencyCombo = new ComboBox<>();
        urgencyCombo.getItems().addAll(Urgency.values());
        urgencyCombo.setPromptText("Urgency");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Equipment:"), 0, 0); grid.add(equipCombo, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("Urgency:"), 0, 2); grid.add(urgencyCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && equipCombo.getValue() != null
                    && !descField.getText().isEmpty() && urgencyCombo.getValue() != null) {
                try {
                    facade.reportMaintenance(equipCombo.getValue(),
                            descField.getText(), urgencyCombo.getValue());
                    refreshRequests(null);
                } catch (UnauthorizedAccessException ex) {
                    showAlert("Access Denied", ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showAssignDialog(MaintenanceRequest req) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Assign Maintenance");
        dialog.setHeaderText("Assign " + req.getRequestId() + " — " + req.getEquipment().getName());
        dialog.setContentText("Technician Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    facade.assignMaintenance(req.getRequestId(), name.trim());
                    refreshRequests(null);
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

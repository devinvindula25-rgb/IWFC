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
    private VBox bannerContainer;
    private HBox statsContainer;

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

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("action-button");
        refreshBtn.setOnAction(e -> {
            getChildren().clear();
            buildUI();
        });

        headerRow.getChildren().addAll(title, refreshBtn, spacer, reportBtn);

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

        bannerContainer = new VBox();
        bannerContainer.setAlignment(Pos.CENTER);
        bannerContainer.setPadding(new Insets(0, 0, 10, 0));
        
        statsContainer = new HBox(15);
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.setPadding(new Insets(10));
        statsContainer.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 8;");

        getChildren().addAll(headerRow, statsContainer, bannerContainer, filters, scrollPane);

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
        
        statsContainer.getChildren().clear();
        java.util.Map<String, Long> stats = facade.getMaintenanceStats();
        Label totalL = new Label("Total: " + stats.get("total"));
        totalL.setStyle("-fx-text-fill: #e5e7eb; -fx-font-weight: bold;");
        Label pendingL = new Label("Pending: " + stats.get("pending"));
        pendingL.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        Label assignedL = new Label("Assigned: " + stats.get("assigned"));
        assignedL.setStyle("-fx-text-fill: #60a5fa; -fx-font-weight: bold;");
        Label completedL = new Label("Completed: " + stats.get("completed"));
        completedL.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
        
        Label sep1 = new Label(" | "); sep1.setStyle("-fx-text-fill: #6b7280;");
        Label sep2 = new Label(" | "); sep2.setStyle("-fx-text-fill: #6b7280;");
        Label sep3 = new Label(" | "); sep3.setStyle("-fx-text-fill: #6b7280;");
        
        statsContainer.getChildren().addAll(totalL, sep1, pendingL, sep2, assignedL, sep3, completedL);

        if (requests.isEmpty()) {
            Label empty = new Label("No maintenance requests found");
            empty.getStyleClass().add("empty-placeholder");
            requestList.getChildren().add(empty);
            return;
        }

        bannerContainer.getChildren().clear();
        List<MaintenanceRequest> highPending = facade.getHighUrgencyPendingRequests();
        if (!highPending.isEmpty()) {
            HBox banner = new HBox(10);
            banner.setAlignment(Pos.CENTER);
            banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
            banner.setPadding(new Insets(10));
            Label alertLabel = new Label("⚠️ " + highPending.size() + " high-urgency request(s) require immediate attention");
            alertLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-weight: bold;");
            banner.getChildren().add(alertLabel);
            bannerContainer.getChildren().add(banner);
        }

        for (MaintenanceRequest req : requests) {
            requestList.getChildren().add(createRequestCard(req));
        }
    }

    private HBox createRequestCard(MaintenanceRequest req) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("maintenance-card-premium");

        if (req.isHighUrgency()) {
            card.getStyleClass().add("glowing-border-danger");
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(javafx.scene.paint.Color.web("#ef4444"));
            glow.setRadius(10);
            glow.setSpread(0.2);
            card.setEffect(glow);

            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(glow.radiusProperty(), 10)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(800), new javafx.animation.KeyValue(glow.radiusProperty(), 25))
            );
            timeline.setAutoReverse(true);
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timeline.play();
        }

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
        
        if (req.isHighUrgency()) {
            Label highUrg = new Label("🔴 High Urgency");
            highUrg.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            titleRow.getChildren().add(highUrg);
        }

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
        
        if (req.getStatus() == RequestStatus.COMPLETED && req.getResolutionNotes() != null && !req.getResolutionNotes().isEmpty()) {
            Label resolutionLabel = new Label("Resolution: " + req.getResolutionNotes());
            resolutionLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #4b5563;");
            info.getChildren().add(resolutionLabel);
        }

        Hyperlink historyLink = new Hyperlink("View Equipment History");
        historyLink.setOnAction(e -> showHistoryDialog(req.getEquipment().getEquipmentId()));
        info.getChildren().add(historyLink);

        // Middle: Status workflow dots
        VBox statusFlow = new VBox(3);
        statusFlow.setAlignment(Pos.CENTER);
        statusFlow.setMinWidth(100);

        // Workflow indicator
        HBox workflowDots = new HBox(4);
        workflowDots.setAlignment(Pos.CENTER);

        Label dot1 = new Label("●");
        dot1.getStyleClass().add(req.getStatus().ordinal() >= 0 ? "workflow-dot-active" : "workflow-dot-inactive");
        Label arrow1 = new Label("→");
        arrow1.getStyleClass().add("dot-arrow");
        Label dot2 = new Label("●");
        dot2.getStyleClass().add(req.getStatus().ordinal() >= 1 ? "workflow-dot-active" : "workflow-dot-inactive");
        Label arrow2 = new Label("→");
        arrow2.getStyleClass().add("dot-arrow");
        Label dot3 = new Label("●");
        dot3.getStyleClass().add(req.getStatus().ordinal() >= 2 ? "workflow-dot-active" : "workflow-dot-inactive");

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
            switch (req.getStatus()) {
                case PENDING -> {
                    Button assignBtn = new Button("Assign");
                    assignBtn.getStyleClass().add("action-button");
                    assignBtn.setOnAction(e -> showAssignDialog(req));
                    actions.getChildren().add(assignBtn);
                }
                case ASSIGNED -> {
                    Button completeBtn = new Button("Complete");
                    completeBtn.getStyleClass().add("action-button-success");
                    completeBtn.setOnAction(e -> showCompleteDialog(req));
                    actions.getChildren().add(completeBtn);
                }
                case COMPLETED -> {
                    Button reopenBtn = new Button("Re-open");
                    reopenBtn.getStyleClass().add("action-button");
                    reopenBtn.setOnAction(e -> {
                        try {
                            facade.reopenMaintenanceRequest(req.getRequestId());
                            refreshRequests(null);
                        } catch (UnauthorizedAccessException ex) {
                            showAlert("Access Denied", ex.getMessage());
                        }
                    });
                    actions.getChildren().add(reopenBtn);
                }
            }
        }

        card.getChildren().addAll(info, statusFlow, actions);
        return card;
    }

    private void showReportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Report Maintenance Issue");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

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
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
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

    private void showCompleteDialog(MaintenanceRequest req) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Complete Maintenance");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        dialog.setHeaderText("Complete " + req.getRequestId() + " — " + req.getEquipment().getName());
        dialog.setContentText("Resolution Notes:");

        dialog.showAndWait().ifPresent(notes -> {
            try {
                facade.completeMaintenance(req.getRequestId(), notes);
                refreshRequests(null);
            } catch (UnauthorizedAccessException ex) {
                showAlert("Access Denied", ex.getMessage());
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showHistoryDialog(String equipmentId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Equipment Maintenance History");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        dialog.setHeaderText("History for " + equipmentId);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        List<MaintenanceRequest> history = facade.getRequestsForEquipment(equipmentId);
        if (history.isEmpty()) {
            content.getChildren().add(new Label("No maintenance history."));
        } else {
            for (MaintenanceRequest req : history) {
                VBox item = new VBox(2);
                item.setStyle("-fx-border-color: #374151; -fx-border-radius: 4; -fx-padding: 8;");
                Label reqId = new Label("Request: " + req.getRequestId() + " (" + req.getStatus() + ")");
                reqId.setStyle("-fx-font-weight: bold; -fx-text-fill: #e5e7eb;");
                Label date = new Label(req.getCreatedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                date.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
                Label desc = new Label(req.getDescription());
                desc.setWrapText(true);
                desc.setStyle("-fx-text-fill: #d1d5db;");
                
                item.getChildren().addAll(reqId, date, desc);
                if (req.getStatus() == RequestStatus.COMPLETED && req.getResolutionNotes() != null) {
                    Label res = new Label("Resolution: " + req.getResolutionNotes());
                    res.setStyle("-fx-font-style: italic; -fx-text-fill: #9ca3af; -fx-font-size: 11px;");
                    item.getChildren().add(res);
                }
                content.getChildren().add(item);
            }
        }
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(300);
        
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}

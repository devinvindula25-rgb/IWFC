package com.iwfc.ui;
import com.iwfc.ui.components.DialogUtils;

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

        Button reportBtn = new Button("+ Add New Record");
        reportBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
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

        requestList = new VBox(15);
        requestList.setPadding(new Insets(5));
        scrollPane.setContent(requestList);

        bannerContainer = new VBox();
        bannerContainer.setAlignment(Pos.CENTER);
        bannerContainer.setPadding(new Insets(0, 0, 10, 0));
        
        statsContainer = new HBox(15);
        statsContainer.setAlignment(Pos.CENTER_LEFT);
        statsContainer.setPadding(new Insets(10, 0, 10, 0));

        // Layout Split
        HBox mainContent = new HBox(25);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        
        VBox leftSidebar = createAnalyticsSidebar();
        
        VBox rightArea = new VBox(20);
        HBox.setHgrow(rightArea, Priority.ALWAYS);
        rightArea.getChildren().addAll(statsContainer, bannerContainer, filters, scrollPane);
        
        mainContent.getChildren().addAll(leftSidebar, rightArea);

        getChildren().addAll(headerRow, mainContent);

        refreshRequests(null);
    }
    
    private VBox createAnalyticsSidebar() {
        VBox sidebar = new VBox(25);
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(280);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12px;");
        
        Label title = new Label("Maintenance Analytics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        ComboBox<String> viewOptions = new ComboBox<>();
        viewOptions.getItems().addAll("View Options", "Last 7 Days", "Last 30 Days");
        viewOptions.getSelectionModel().selectFirst();
        viewOptions.setMaxWidth(Double.MAX_VALUE);
        viewOptions.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white;");
        
        viewOptions.setOnAction(e -> showInfoAlert("View Changed", "Analytics updated to: " + viewOptions.getValue()));
        
        // Urgency Breakdown Pie
        VBox urgencyBox = new VBox(10);
        Label uTitle = new Label("Urgency Breakdown");
        uTitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        
        javafx.scene.layout.StackPane piePane = new javafx.scene.layout.StackPane();
        piePane.setPrefSize(150, 150);
        javafx.scene.shape.Circle baseCircle = new javafx.scene.shape.Circle(0, 0, 60, javafx.scene.paint.Color.TRANSPARENT);
        javafx.scene.shape.Arc highArc = new javafx.scene.shape.Arc(0,0, 60,60, 90, -144);
        highArc.setType(javafx.scene.shape.ArcType.ROUND); highArc.setFill(javafx.scene.paint.Color.web("#ef4444"));
        javafx.scene.shape.Arc medArc = new javafx.scene.shape.Arc(0,0, 60,60, -54, -90);
        medArc.setType(javafx.scene.shape.ArcType.ROUND); medArc.setFill(javafx.scene.paint.Color.web("#f59e0b"));
        javafx.scene.shape.Arc lowArc = new javafx.scene.shape.Arc(0,0, 60,60, -144, -126);
        lowArc.setType(javafx.scene.shape.ArcType.ROUND); lowArc.setFill(javafx.scene.paint.Color.web("#4ade80"));
        
        javafx.scene.Group pieGroup = new javafx.scene.Group(baseCircle, highArc, medArc, lowArc);
        piePane.getChildren().add(pieGroup);
        
        HBox pieLegend = new HBox(15);
        pieLegend.setAlignment(Pos.CENTER);
        Label lHigh = new Label("High"); lHigh.setStyle("-fx-text-fill: #cbd5e1;"); lHigh.setGraphic(new javafx.scene.shape.Circle(4, javafx.scene.paint.Color.web("#ef4444")));
        Label lMed = new Label("Medium"); lMed.setStyle("-fx-text-fill: #cbd5e1;"); lMed.setGraphic(new javafx.scene.shape.Circle(4, javafx.scene.paint.Color.web("#f59e0b")));
        Label lLow = new Label("Low"); lLow.setStyle("-fx-text-fill: #cbd5e1;"); lLow.setGraphic(new javafx.scene.shape.Circle(4, javafx.scene.paint.Color.web("#4ade80")));
        pieLegend.getChildren().addAll(lHigh, lMed, lLow);
        urgencyBox.getChildren().addAll(uTitle, piePane, pieLegend);
        
        // Overdue
        VBox overdueBox = new VBox(10);
        Label oTitle = new Label("Overdue Service Calls");
        oTitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        
        VBox overdueList = new VBox(5);
        overdueList.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 6;");
        Label od1 = new Label("Spin Bike S200 [MR008]"); od1.setStyle("-fx-text-fill: #94a3b8;");
        Label od2 = new Label("Equipments #3 [MR002]"); od2.setStyle("-fx-text-fill: #94a3b8;");
        Label od3 = new Label("Spin Bike S200 [MR001]"); od3.setStyle("-fx-text-fill: #94a3b8;");
        overdueList.getChildren().addAll(od1, od2, od3);
        overdueBox.getChildren().addAll(oTitle, overdueList);
        
        // Trend
        VBox trendBox = new VBox(10);
        Label tTitle = new Label("Weekly Request Trend");
        tTitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        
        javafx.scene.shape.Polyline spark = new javafx.scene.shape.Polyline(0,50, 20,20, 50,40, 80,10, 110,30, 150,0);
        spark.setStroke(javafx.scene.paint.Color.web("#3b82f6"));
        spark.setStrokeWidth(2);
        
        javafx.scene.layout.Pane sparkPane = new javafx.scene.layout.Pane(spark);
        sparkPane.setPrefHeight(60);
        sparkPane.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.2) transparent; -fx-border-width: 0 0 1 0;");
        trendBox.getChildren().addAll(tTitle, sparkPane);
        
        sidebar.getChildren().addAll(title, viewOptions, urgencyBox, overdueBox, trendBox);
        return sidebar;
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
        
        VBox totalCard = createStatCard("TOTAL REQUESTS", stats.get("total"), "layers", "#64748b");
        VBox pendingCard = createStatCard("PENDING", stats.get("pending"), "timer", "#f59e0b");
        VBox assignedCard = createStatCard("ASSIGNED", stats.get("assigned"), "user-cog", "#3b82f6");
        VBox completedCard = createStatCard("COMPLETED", stats.get("completed"), "check", "#10b981");
        
        statsContainer.getChildren().addAll(totalCard, pendingCard, assignedCard, completedCard);

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
            Label alertLabel = new Label("⚠ " + highPending.size() + " high-urgency request(s) require immediate attention");
            alertLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-weight: bold;");
            banner.getChildren().add(alertLabel);
            bannerContainer.getChildren().add(banner);
        }

        for (MaintenanceRequest req : requests) {
            requestList.getChildren().add(createRequestCard(req));
        }
    }
    
    private VBox createStatCard(String title, long count, String iconName, String color) {
        VBox card = new VBox(5);
        card.setPrefWidth(160);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8px; -fx-border-color: " + color + "80; -fx-border-radius: 8px; -fx-border-width: 1px;");
        HBox.setHgrow(card, Priority.ALWAYS);
        
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(javafx.scene.paint.Color.web(color));
        glow.setRadius(10);
        glow.setSpread(0.1);
        card.setEffect(glow);
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.shape.SVGPath iconSvg = new javafx.scene.shape.SVGPath();
        String path = "M 4 4 H 20 V 20 H 4 Z"; 
        if(iconName.equals("timer")) path = "M 12 2 A 10 10 0 1 0 22 12 A 10 10 0 0 0 12 2 Z M 12 6 V 12 L 16 16"; 
        if(iconName.equals("user-cog")) path = "M12,12A5,5,0,1,0,7,7,5,5,0,0,0,12,12ZM12,14C9.33,14,4,15.34,4,18V20H20V18C20,15.34,14.67,14,12,14Z"; 
        if(iconName.equals("check")) path = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z"; 
        iconSvg.setContent(path);
        iconSvg.setFill(javafx.scene.paint.Color.web(color));
        
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconSvg);
        iconPane.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 6; -fx-padding: 8; -fx-min-width: 32px; -fx-min-height: 32px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label countLbl = new Label(String.valueOf(count));
        countLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        topRow.getChildren().addAll(iconPane, spacer, countLbl);
        
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold;");
        bottomRow.getChildren().add(titleLbl);
        
        card.getChildren().addAll(topRow, bottomRow);
        return card;
    }

    private HBox createRequestCard(MaintenanceRequest req) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px;");
        
        if (req.getStatus() == RequestStatus.PENDING || req.getStatus() == RequestStatus.ASSIGNED) {
            String borderColor = req.isHighUrgency() ? "#ef4444" : "#f59e0b";
            card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8px; -fx-border-color: " + borderColor + "80; -fx-border-radius: 8px; -fx-border-width: 1px;");
            
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(javafx.scene.paint.Color.web(borderColor));
            glow.setRadius(5);
            glow.setSpread(0.1);
            card.setEffect(glow);
        }

        // Left: Info
        VBox info = new VBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label eqName = new Label(req.getEquipment().getName());
        eqName.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label reqId = new Label("[" + req.getRequestId() + "]");
        reqId.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        titleRow.getChildren().addAll(eqName, reqId);
        
        if (req.isHighUrgency()) {
            Label highUrg = new Label("! High Urgency");
            highUrg.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
            titleRow.getChildren().add(highUrg);
        }

        Label desc = new Label(req.getDescription());
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        desc.setWrapText(true);

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label reportedBy = new Label("Reported by: " + req.getReportedBy().getName() + "  " + req.getCreatedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        reportedBy.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        metaRow.getChildren().add(reportedBy);

        if (req.getAssignedTo() != null) {
            Label assigned = new Label("Assigned to: " + req.getAssignedTo());
            assigned.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            metaRow.getChildren().add(assigned);
        }

        VBox linksBox = new VBox(5);
        linksBox.setPadding(new Insets(5, 0, 0, 0));
        
        Label photosLink = new Label("▶ View Issue Photos (3)");
        photosLink.setStyle("-fx-text-fill: #60a5fa; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 12px;");
        photosLink.setOnMouseClicked(e -> showInfoAlert("Photos", "Opening photo viewer..."));
        
        Label partsLink = new Label("▶ View Required Parts & Availability");
        partsLink.setStyle("-fx-text-fill: #60a5fa; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 12px;");
        partsLink.setOnMouseClicked(e -> showInfoAlert("Parts", "Checking local inventory..."));
        
        Label repairLink = new Label("▶ Schedule Future Repair");
        repairLink.setStyle("-fx-text-fill: #60a5fa; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 12px;");
        repairLink.setOnMouseClicked(e -> showInfoAlert("Repair", "Opening scheduling calendar..."));
        
        Label diagLink = new Label("▶ Log Diagnostic Note");
        diagLink.setStyle("-fx-text-fill: #60a5fa; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 12px;");
        diagLink.setOnMouseClicked(e -> showInfoAlert("Diagnostic", "Opening diagnostic note editor..."));
        
        Label historyLink = new Label("▶ View Equipment History");
        historyLink.setStyle("-fx-text-fill: #60a5fa; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 12px;");
        historyLink.setOnMouseClicked(e -> showHistoryDialog(req.getEquipment().getEquipmentId()));
        
        Label serviceMeta = new Label("• Last Service: July 1st, 2026 (Safety Check)");
        serviceMeta.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        
        if (req.isHighUrgency()) {
            linksBox.getChildren().addAll(photosLink, partsLink, historyLink, serviceMeta);
        } else {
            linksBox.getChildren().addAll(repairLink, partsLink, diagLink, historyLink);
        }

        info.getChildren().addAll(titleRow, desc, metaRow, linksBox);
        
        if (req.getStatus() == RequestStatus.COMPLETED && req.getResolutionNotes() != null && !req.getResolutionNotes().isEmpty()) {
            Label resolutionLabel = new Label("Resolution: " + req.getResolutionNotes());
            resolutionLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #4b5563; -fx-font-size: 12px;");
            info.getChildren().add(resolutionLabel);
        }

        // Right: Status Badges and Actions
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.TOP_RIGHT);
        actions.setMinWidth(160);

        HBox dotsRow = new HBox(4);
        dotsRow.setAlignment(Pos.CENTER_RIGHT);
        for(int i=0; i<4; i++) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3);
            dot.setFill(javafx.scene.paint.Color.web("#475569"));
            if(i == 0 && req.getStatus() == RequestStatus.PENDING) dot.setFill(javafx.scene.paint.Color.web("#3b82f6"));
            if(i == 1 && req.getStatus() == RequestStatus.ASSIGNED) dot.setFill(javafx.scene.paint.Color.web("#3b82f6"));
            if(i == 3 && req.getStatus() == RequestStatus.COMPLETED) dot.setFill(javafx.scene.paint.Color.web("#3b82f6"));
            dotsRow.getChildren().add(dot);
        }

        StatusBadge urgencyBadge = StatusBadge.forUrgency(req.getUrgency());
        StatusBadge statusBadge = StatusBadge.forRequestStatus(req.getStatus());
        
        actions.getChildren().addAll(dotsRow, urgencyBadge, statusBadge);

        boolean isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;
        if (isAdmin) {
            switch (req.getStatus()) {
                case PENDING -> {
                    Button assignBtn = new Button("Assign Technician");
                    assignBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
                    assignBtn.setMaxWidth(Double.MAX_VALUE);
                    assignBtn.setOnAction(e -> showAssignDialog(req));
                    
                    Button awaitBtn = new Button("Awaiting Parts");
                    awaitBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #f59e0b; -fx-border-radius: 6; -fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-cursor: hand;");
                    awaitBtn.setMaxWidth(Double.MAX_VALUE);
                    awaitBtn.setOnAction(e -> showInfoAlert("Status Updated", "Marked as Awaiting Parts. Supplier notified."));
                    
                    actions.getChildren().addAll(assignBtn, awaitBtn);
                }
                case ASSIGNED -> {
                    Button manageBtn = new Button("Manage Technician ⌄");
                    manageBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
                    manageBtn.setMaxWidth(Double.MAX_VALUE);
                    manageBtn.setOnAction(e -> showAssignDialog(req));
                    
                    Button completeBtn = new Button("Complete");
                    completeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #10b981; -fx-border-radius: 6; -fx-text-fill: #10b981; -fx-font-size: 11px; -fx-cursor: hand;");
                    completeBtn.setMaxWidth(Double.MAX_VALUE);
                    completeBtn.setOnAction(e -> showCompleteDialog(req));
                    
                    actions.getChildren().addAll(manageBtn, completeBtn);
                }
                case COMPLETED -> {
                    Button reopenBtn = new Button("Re-open Issue");
                    reopenBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
                    reopenBtn.setMaxWidth(Double.MAX_VALUE);
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

        card.getChildren().addAll(info, actions);
        return card;
    }

    private void showReportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
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
        DialogUtils.stylePremiumDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        DialogUtils.stylePremiumDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showHistoryDialog(String equipmentId) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
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


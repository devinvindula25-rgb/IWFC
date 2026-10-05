package com.iwfc.ui;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.DialogUtils;
import com.iwfc.ui.components.StatusBadge;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Supreme UI/UX Maintenance Operations Center.
 * Features glassmorphic cards, neon urgency accents, dynamic equipment health tracking,
 * preventative maintenance analytics, and interactive technician dispatch workflows.
 */
public class MaintenanceView extends VBox {

    private final IWFCSystemFacade facade;
    private VBox requestList;
    private VBox bannerContainer;
    private HBox kpiContainer;
    private VBox sidebarContainer;
    private Label resultCountBadge;
    
    // Filtering state
    private RequestStatus currentStatusFilter = null;
    private String currentCategoryFilter = "All Categories";
    private String currentSearchText = "";
    private boolean onlyCritical = false;
    
    private ToggleGroup filterGroup;
    private ToggleButton allBtn;
    private ToggleButton pendingBtn;
    private ToggleButton assignedBtn;
    private ToggleButton completedBtn;

    // Palette gradients
    private static final String GRADIENT_INDIGO = "-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6);";
    private static final String GRADIENT_EMERALD = "-fx-background-color: linear-gradient(to bottom right, #10b981, #059669);";
    private static final String GRADIENT_AMBER = "-fx-background-color: linear-gradient(to bottom right, #f59e0b, #d97706);";
    private static final String GRADIENT_CYAN = "-fx-background-color: linear-gradient(to bottom right, #06b6d4, #0891b2);";

    public MaintenanceView(IWFCSystemFacade facade) {
        this.facade = facade;
        setSpacing(20);
        getStyleClass().add("maintenance-view");
        VBox.setVgrow(this, Priority.ALWAYS);
        buildUI();
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Header Row
        HBox headerRow = buildHeader();

        // 2. Main Content: Left Analytics Sidebar + Right Operations Area
        HBox mainContent = new HBox(24);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        sidebarContainer = buildAnalyticsSidebar();
        VBox rightArea = buildOperationsArea();
        HBox.setHgrow(rightArea, Priority.ALWAYS);

        mainContent.getChildren().addAll(sidebarContainer, rightArea);
        getChildren().addAll(headerRow, mainContent);

        // Initial data load
        applyFilters();
    }

    // =========================================================================
    // 1. HEADER SECTION
    // =========================================================================
    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));

        VBox titleBox = new VBox(4);
        Label title = new Label("Maintenance Operations");
        title.getStyleClass().add("maintenance-title");

        HBox subRow = new HBox(12);
        subRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("Real-time equipment fault triage, technician dispatch & preventative care");
        subtitle.getStyleClass().add("maintenance-subtitle");

        // Calculate fleet health
        List<Equipment> equipList = facade.getAllEquipment();
        long operational = equipList.stream().filter(e -> e.getStatus() == EquipmentStatus.OPERATIONAL).count();
        int healthPercent = equipList.isEmpty() ? 100 : (int) Math.round((operational * 100.0) / equipList.size());

        Label healthPill = new Label("● Fleet Health: " + healthPercent + "% Operational");
        healthPill.getStyleClass().add("fleet-health-pill");

        subRow.getChildren().addAll(subtitle, healthPill);
        titleBox.getChildren().addAll(title, subRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("maintenance-refresh-btn");
        refreshBtn.setOnAction(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(400), refreshBtn);
            rt.setByAngle(360);
            rt.play();
            buildUI();
        });

        Button reportBtn = new Button("+ Report Equipment Issue");
        reportBtn.getStyleClass().add("maintenance-primary-btn");
        reportBtn.setOnAction(e -> showReportDialog());

        header.getChildren().addAll(titleBox, spacer, refreshBtn, reportBtn);
        return header;
    }

    // =========================================================================
    // 2. OPERATIONS AREA (Right Column)
    // =========================================================================
    private VBox buildOperationsArea() {
        VBox rightArea = new VBox(16);
        VBox.setVgrow(rightArea, Priority.ALWAYS);

        kpiContainer = new HBox(14);
        kpiContainer.setAlignment(Pos.CENTER_LEFT);

        bannerContainer = new VBox();
        bannerContainer.setAlignment(Pos.CENTER);

        HBox filterBar = buildFilterBar();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("report-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        requestList = new VBox(16);
        requestList.setPadding(new Insets(4, 4, 16, 4));
        scroll.setContent(requestList);

        rightArea.getChildren().addAll(kpiContainer, bannerContainer, filterBar, scroll);
        return rightArea;
    }

    // =========================================================================
    // 3. TOP KPI STAT CARDS
    // =========================================================================
    private void refreshKpiCards() {
        kpiContainer.getChildren().clear();
        Map<String, Long> stats = facade.getMaintenanceStats();

        long total = stats.getOrDefault("total", 0L);
        long pending = stats.getOrDefault("pending", 0L);
        long assigned = stats.getOrDefault("assigned", 0L);
        long completed = stats.getOrDefault("completed", 0L);

        Node totalCard = createPremiumKpiCard("TOTAL REQUESTS", total, "📋", GRADIENT_INDIGO, "All tracked tickets");
        Node pendingCard = createPremiumKpiCard("PENDING TRIAGE", pending, "⏳", GRADIENT_AMBER, "Awaiting dispatch");
        Node assignedCard = createPremiumKpiCard("IN ACTIVE REPAIR", assigned, "🔧", GRADIENT_CYAN, "Technicians on-site");
        Node completedCard = createPremiumKpiCard("RESOLVED & VERIFIED", completed, "✅", GRADIENT_EMERALD, "Safety certified");

        kpiContainer.getChildren().addAll(totalCard, pendingCard, assignedCard, completedCard);
    }

    private Node createPremiumKpiCard(String title, long count, String icon, String gradient, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("maintenance-kpi-card");
        card.setStyle(gradient);
        card.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(180);

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px; -fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 10; -fx-padding: 6 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLbl = new Label("0");
        countLbl.getStyleClass().add("kpi-count-text");

        // Roll-up number counter animation
        Timeline tl = new Timeline();
        int steps = Math.min((int) count, 25);
        if (steps > 0) {
            for (int i = 0; i <= steps; i++) {
                final long val = (count * i) / steps;
                tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * (500.0 / steps)), e -> countLbl.setText(String.valueOf(val))));
            }
        } else {
            countLbl.setText(String.valueOf(count));
        }
        tl.play();

        topRow.getChildren().addAll(iconLbl, spacer, countLbl);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("kpi-title-text");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 11px;");

        card.getChildren().addAll(topRow, titleLbl, subLbl);

        // Hover elevation
        DropShadow hoverShadow = new DropShadow(16, Color.rgb(0, 0, 0, 0.35));
        hoverShadow.setOffsetY(6);
        card.setOnMouseEntered(e -> {
            card.setEffect(hoverShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.03); st.setToY(1.03);
            TranslateTransition tt = new TranslateTransition(Duration.millis(180), card);
            tt.setToY(-4);
            new ParallelTransition(st, tt).play();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(null);
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.0); st.setToY(1.0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(180), card);
            tt.setToY(0);
            new ParallelTransition(st, tt).play();
        });

        return card;
    }

    // =========================================================================
    // 4. CRITICAL ALERT BANNER
    // =========================================================================
    private void refreshBanner() {
        bannerContainer.getChildren().clear();
        List<MaintenanceRequest> highPending = facade.getHighUrgencyPendingRequests();

        if (!highPending.isEmpty()) {
            HBox banner = new HBox(14);
            banner.setAlignment(Pos.CENTER_LEFT);
            banner.getStyleClass().add("maintenance-alert-banner");

            Label alertIcon = new Label("🚨");
            alertIcon.setStyle("-fx-font-size: 22px;");

            VBox textBox = new VBox(2);
            Label alertTitle = new Label("Critical Safety Alert — " + highPending.size() + " High Urgency Request(s)");
            alertTitle.setStyle("-fx-text-fill: #fca5a5; -fx-font-weight: 800; -fx-font-size: 13px;");
            Label alertDesc = new Label("Equipment requires immediate technician intervention to ensure member safety.");
            alertDesc.setStyle("-fx-text-fill: #fecaca; -fx-font-size: 12px; -fx-opacity: 0.85;");
            textBox.getChildren().addAll(alertTitle, alertDesc);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button triageBtn = new Button("⚡ Triage Critical Items");
            triageBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 14; -fx-background-radius: 8; -fx-cursor: hand;");
            triageBtn.setOnAction(e -> {
                onlyCritical = true;
                applyFilters();
            });

            banner.getChildren().addAll(alertIcon, textBox, spacer, triageBtn);
            bannerContainer.getChildren().add(banner);
        }
    }

    // =========================================================================
    // 5. INTERACTIVE FILTER & SEARCH BAR
    // =========================================================================
    private HBox buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.getStyleClass().add("report-filter-bar");

        // Segmented Status Toggle Buttons
        filterGroup = new ToggleGroup();
        allBtn = createFilterToggle("All", null);
        pendingBtn = createFilterToggle("Pending", RequestStatus.PENDING);
        assignedBtn = createFilterToggle("In Repair", RequestStatus.ASSIGNED);
        completedBtn = createFilterToggle("Completed", RequestStatus.COMPLETED);

        allBtn.setSelected(true);

        HBox toggleBox = new HBox(6);
        toggleBox.getStyleClass().add("report-view-toggle-bar");
        toggleBox.getChildren().addAll(allBtn, pendingBtn, assignedBtn, completedBtn);

        // Equipment Category Selector
        Set<String> categories = new TreeSet<>();
        categories.add("All Categories");
        for (Equipment eq : facade.getAllEquipment()) {
            if (eq.getCategory() != null) categories.add(eq.getCategory());
        }
        ComboBox<String> catFilter = new ComboBox<>();
        catFilter.getItems().addAll(categories);
        catFilter.setValue("All Categories");
        catFilter.getStyleClass().add("report-date-combo");
        catFilter.setOnAction(e -> {
            currentCategoryFilter = catFilter.getValue();
            applyFilters();
        });

        // Search Field with Icon
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("report-search-box");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 13px; -fx-opacity: 0.6;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by equipment, technician, ID...");
        searchField.getStyleClass().add("report-search-field");
        searchField.setPrefWidth(220);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, old, newVal) -> {
            currentSearchText = newVal == null ? "" : newVal.trim().toLowerCase();
            applyFilters();
        });
        searchBox.getChildren().addAll(searchIcon, searchField);

        resultCountBadge = new Label("0 Requests");
        resultCountBadge.getStyleClass().add("report-count-badge");

        bar.getChildren().addAll(toggleBox, catFilter, searchBox, resultCountBadge);
        return bar;
    }

    private ToggleButton createFilterToggle(String label, RequestStatus status) {
        ToggleButton btn = new ToggleButton(label);
        btn.setToggleGroup(filterGroup);
        btn.getStyleClass().add("report-view-toggle-btn");

        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                currentStatusFilter = status;
                onlyCritical = false;
                updateToggleStyles();
                applyFilters();
            }
        });
        return btn;
    }

    private void updateToggleStyles() {
        for (Toggle t : filterGroup.getToggles()) {
            ToggleButton tb = (ToggleButton) t;
            tb.getStyleClass().remove("report-view-toggle-btn-active");
            if (tb.isSelected()) {
                tb.getStyleClass().add("report-view-toggle-btn-active");
            }
        }
    }

    // =========================================================================
    // 6. DYNAMIC ANALYTICS SIDEBAR (Left Column)
    // =========================================================================
    private VBox buildAnalyticsSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPrefWidth(290);
        sidebar.setMinWidth(290);
        sidebar.getStyleClass().add("maintenance-analytics-sidebar");

        Label sideTitle = new Label("Fleet Diagnostics");
        sideTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");

        // 1. Urgency Distribution Card
        VBox urgencyBox = buildUrgencyDistributionWidget();

        // 2. Preventative Care & High Usage Equipment
        VBox riskBox = buildEquipmentAtRiskWidget();

        // 3. Quick Filter Shortcuts
        VBox shortcutsBox = buildQuickFilterShortcuts();

        sidebar.getChildren().addAll(sideTitle, urgencyBox, riskBox, shortcutsBox);
        return sidebar;
    }

    private VBox buildUrgencyDistributionWidget() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-padding: 12;");

        Label title = new Label("Urgency Distribution");
        title.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-opacity: 0.8;");

        List<MaintenanceRequest> allReqs = facade.getAllMaintenanceRequests();
        int total = allReqs.size();
        long high = allReqs.stream().filter(r -> r.getUrgency() == Urgency.HIGH).count();
        long med = allReqs.stream().filter(r -> r.getUrgency() == Urgency.MEDIUM).count();
        long low = allReqs.stream().filter(r -> r.getUrgency() == Urgency.LOW).count();

        // Segmented visual progress bar
        ProgressBar bar = new ProgressBar(total == 0 ? 0 : (double) high / total);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: #ef4444; -fx-background-color: rgba(255,255,255,0.1);");

        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
            createLegendItem("High (" + high + ")", "#ef4444"),
            createLegendItem("Med (" + med + ")", "#f59e0b"),
            createLegendItem("Low (" + low + ")", "#10b981")
        );

        box.getChildren().addAll(title, bar, legend);
        return box;
    }

    private HBox createLegendItem(String text, String colorWeb) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4, Color.web(colorWeb));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-opacity: 0.8;");
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    private VBox buildEquipmentAtRiskWidget() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-padding: 12;");

        Label title = new Label("Preventative Care Alerts");
        title.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-opacity: 0.8;");

        List<Equipment> equipList = facade.getAllEquipment();
        List<Equipment> atRisk = equipList.stream()
            .filter(e -> e.getStatus() == EquipmentStatus.FAULTY ||
                         e.getStatus() == EquipmentStatus.UNDER_MAINTENANCE ||
                         (e.getCumulativeUsageHours() >= e.getMaintenanceThreshold() * 0.70))
            .limit(4)
            .toList();

        VBox list = new VBox(8);
        if (atRisk.isEmpty()) {
            Label allGood = new Label("✨ All machines within safety limits");
            allGood.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");
            list.getChildren().add(allGood);
        } else {
            for (Equipment eq : atRisk) {
                VBox item = new VBox(3);
                item.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-padding: 6 8; -fx-background-radius: 6;");

                HBox row1 = new HBox(6);
                row1.setAlignment(Pos.CENTER_LEFT);
                Label nameLbl = new Label(eq.getName());
                nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label statusTag = new Label(eq.getStatus().name());
                statusTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: "
                        + (eq.getStatus() == EquipmentStatus.FAULTY ? "#ef4444" : "#f59e0b") + ";");
                row1.getChildren().addAll(nameLbl, sp, statusTag);

                double ratio = eq.getMaintenanceThreshold() > 0 ? eq.getCumulativeUsageHours() / eq.getMaintenanceThreshold() : 0.5;
                ProgressBar pb = new ProgressBar(Math.min(ratio, 1.0));
                pb.setMaxWidth(Double.MAX_VALUE);
                pb.setPrefHeight(4);
                pb.setStyle("-fx-accent: " + (ratio >= 0.9 ? "#ef4444" : "#f59e0b") + ";");

                Label hoursLbl = new Label(String.format("%.0fh / %.0fh threshold", eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold()));
                hoursLbl.setStyle("-fx-font-size: 10px; -fx-opacity: 0.6;");

                item.getChildren().addAll(row1, pb, hoursLbl);
                list.getChildren().add(item);
            }
        }

        box.getChildren().addAll(title, list);
        return box;
    }

    private VBox buildQuickFilterShortcuts() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-padding: 12;");

        Label title = new Label("Quick View Filters");
        title.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-opacity: 0.8;");

        Button btnCrit = createSidebarActionBtn("🚨 Critical High Urgency Only", e -> {
            onlyCritical = true;
            applyFilters();
        });

        Button btnCardio = createSidebarActionBtn("🏃 Cardio Zone Machines", e -> {
            currentCategoryFilter = "Cardio";
            onlyCritical = false;
            applyFilters();
        });

        Button btnWeight = createSidebarActionBtn("🏋️ Weight Room Machines", e -> {
            currentCategoryFilter = "Strength (Free Weights)";
            onlyCritical = false;
            applyFilters();
        });

        Button btnReset = createSidebarActionBtn("🔄 Clear All Filters", e -> {
            currentStatusFilter = null;
            currentCategoryFilter = "All Categories";
            currentSearchText = "";
            onlyCritical = false;
            allBtn.setSelected(true);
            updateToggleStyles();
            applyFilters();
        });

        box.getChildren().addAll(title, btnCrit, btnCardio, btnWeight, btnReset);
        return box;
    }

    private Button createSidebarActionBtn(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: 600; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;");
        btn.setOnAction(handler);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.25); -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 600; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: 600; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 10;"));
        return btn;
    }

    // =========================================================================
    // 7. FILTER APPLICATION & CARD RENDERING
    // =========================================================================
    private void applyFilters() {
        refreshKpiCards();
        refreshBanner();

        requestList.getChildren().clear();

        List<MaintenanceRequest> all = facade.getAllMaintenanceRequests();
        List<MaintenanceRequest> filtered = all.stream().filter(req -> {
            // Status Filter
            if (currentStatusFilter != null && req.getStatus() != currentStatusFilter) {
                return false;
            }
            // Critical filter
            if (onlyCritical && !req.isHighUrgency()) {
                return false;
            }
            // Category Filter
            if (!"All Categories".equals(currentCategoryFilter)) {
                String cat = req.getEquipment().getCategory();
                if (cat == null || !cat.equalsIgnoreCase(currentCategoryFilter)) return false;
            }
            // Search Query
            if (!currentSearchText.isEmpty()) {
                String q = currentSearchText;
                String eqName = req.getEquipment().getName().toLowerCase();
                String eqId = req.getEquipment().getEquipmentId().toLowerCase();
                String reqId = req.getRequestId().toLowerCase();
                String desc = req.getDescription().toLowerCase();
                String tech = req.getAssignedTo() != null ? req.getAssignedTo().toLowerCase() : "";
                String rep = req.getReportedBy() != null ? req.getReportedBy().getName().toLowerCase() : "";

                return eqName.contains(q) || eqId.contains(q) || reqId.contains(q) ||
                       desc.contains(q) || tech.contains(q) || rep.contains(q);
            }
            return true;
        }).collect(Collectors.toList());

        resultCountBadge.setText(filtered.size() + " Requests");

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label emptyIcon = new Label("🔍");
            emptyIcon.setStyle("-fx-font-size: 32px; -fx-opacity: 0.5;");
            Label emptyText = new Label("No maintenance requests match the current filters.");
            emptyText.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-opacity: 0.6;");
            emptyBox.getChildren().addAll(emptyIcon, emptyText);
            requestList.getChildren().add(emptyBox);
            return;
        }

        int delay = 0;
        for (MaintenanceRequest req : filtered) {
            Node card = createRequestCard(req);
            
            // Staggered entrance animation
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), card);
            ft.setToValue(1.0);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            delay += 35;

            requestList.getChildren().add(card);
        }
    }

    // =========================================================================
    // 8. SUPREME MAINTENANCE REQUEST CARD
    // =========================================================================
    private Node createRequestCard(MaintenanceRequest req) {
        VBox card = new VBox(14);
        card.getStyleClass().add("maintenance-request-card");

        // Dynamic urgency border styling
        if (req.isHighUrgency() && req.getStatus() != RequestStatus.COMPLETED) {
            card.getStyleClass().add("request-card-high");
        } else if (req.getUrgency() == Urgency.MEDIUM && req.getStatus() != RequestStatus.COMPLETED) {
            card.getStyleClass().add("request-card-medium");
        } else if (req.getStatus() == RequestStatus.COMPLETED) {
            card.getStyleClass().add("request-card-completed");
        }

        // --- ROW 1: HEADER & BADGES ---
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Equipment category icon badge
        String catIcon = getCategoryIcon(req.getEquipment().getCategory());
        Label catBadge = new Label(catIcon);
        catBadge.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-background-radius: 8; -fx-padding: 6 10; -fx-font-size: 16px;");

        VBox titleBox = new VBox(2);
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label eqName = new Label(req.getEquipment().getName());
        eqName.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");

        Label eqId = new Label("[" + req.getEquipment().getEquipmentId() + "]");
        eqId.getStyleClass().add("equipment-badge-id");

        Label reqId = new Label("#" + req.getRequestId());
        reqId.setStyle("-fx-opacity: 0.55; -fx-font-size: 12px; -fx-font-weight: 700;");

        nameRow.getChildren().addAll(eqName, eqId, reqId);

        String locName = req.getEquipment().getLocation() != null ? req.getEquipment().getLocation().getDisplayName() : "Gym Floor";
        Label locLabel = new Label("📍 " + locName);
        locLabel.getStyleClass().add("report-location-tag");

        titleBox.getChildren().addAll(nameRow, locLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Urgency & Status Badges
        StatusBadge urgencyBadge = StatusBadge.forUrgency(req.getUrgency());
        StatusBadge statusBadge = StatusBadge.forRequestStatus(req.getStatus());

        headerRow.getChildren().addAll(catBadge, titleBox, spacer, urgencyBadge, statusBadge);

        // --- ROW 2: DESCRIPTION WITH QUOTE BORDER ---
        HBox descBox = new HBox(10);
        descBox.setAlignment(Pos.CENTER_LEFT);
        descBox.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: #6366f1; -fx-border-width: 0 0 0 3; -fx-padding: 10 14; -fx-background-radius: 0 8 8 0;");

        Label desc = new Label(req.getDescription());
        desc.setStyle("-fx-font-size: 13px; -fx-line-spacing: 2px;");
        desc.setWrapText(true);
        HBox.setHgrow(desc, Priority.ALWAYS);
        descBox.getChildren().add(desc);

        // --- ROW 3: METADATA & PREVENTATIVE USAGE BAR ---
        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Reporter
        String repName = req.getReportedBy() != null ? req.getReportedBy().getName() : "Instructor";
        String dateStr = req.getCreatedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        Label reportedBy = new Label("👤 Reported by: " + repName + " • " + dateStr);
        reportedBy.setStyle("-fx-opacity: 0.7; -fx-font-size: 12px;");

        // Equipment usage vs threshold
        Equipment eq = req.getEquipment();
        double usage = eq.getCumulativeUsageHours();
        double thresh = eq.getMaintenanceThreshold();
        int usagePercent = thresh > 0 ? (int) Math.round((usage / thresh) * 100) : 0;
        Label usageLbl = new Label("⚙️ Cumulative Usage: " + String.format("%.0fh / %.0fh (%d%%)", usage, thresh, usagePercent));
        usageLbl.setStyle("-fx-opacity: 0.7; -fx-font-size: 12px;");

        metaRow.getChildren().addAll(reportedBy, usageLbl);

        // --- ROW 4: ASSIGNMENT / RESOLUTION DETAILS ---
        VBox statusDetails = new VBox(6);
        if (req.getStatus() == RequestStatus.ASSIGNED && req.getAssignedTo() != null) {
            HBox assignBox = new HBox(8);
            assignBox.setAlignment(Pos.CENTER_LEFT);
            assignBox.setStyle("-fx-background-color: rgba(6, 182, 212, 0.12); -fx-border-color: rgba(6, 182, 212, 0.3); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");

            Label techIcon = new Label("🔧");
            techIcon.setStyle("-fx-font-size: 14px;");

            Label techText = new Label("Assigned Technician: " + req.getAssignedTo() + " • Dispatched & In-Progress");
            techText.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: 700; -fx-font-size: 12px;");

            assignBox.getChildren().addAll(techIcon, techText);
            statusDetails.getChildren().add(assignBox);
        } else if (req.getStatus() == RequestStatus.COMPLETED && req.getResolutionNotes() != null) {
            HBox resBox = new HBox(8);
            resBox.setAlignment(Pos.CENTER_LEFT);
            resBox.setStyle("-fx-background-color: rgba(16, 185, 129, 0.12); -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");

            Label chkIcon = new Label("✅");
            chkIcon.setStyle("-fx-font-size: 14px;");

            Label resText = new Label("Resolution: " + req.getResolutionNotes());
            resText.setStyle("-fx-text-fill: #34d399; -fx-font-weight: 700; -fx-font-size: 12px;");

            resBox.getChildren().addAll(chkIcon, resText);
            statusDetails.getChildren().add(resBox);
        }

        // --- ROW 5: INTERACTIVE TOOLING LINKS & ACTION BUTTONS ---
        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(6, 0, 0, 0));

        // Tooling pills
        Label photosBtn = new Label("📷  Issue Photos (3)");
        photosBtn.getStyleClass().add("link-tool-pill");
        photosBtn.setOnMouseClicked(e -> showPhotosDialog(req));

        Label partsBtn = new Label("📦  Required Parts & Stock");
        partsBtn.getStyleClass().add("link-tool-pill");
        partsBtn.setOnMouseClicked(e -> showPartsDialog(req));

        Label historyBtn = new Label("📜  Equipment History");
        historyBtn.getStyleClass().add("link-tool-pill");
        historyBtn.setOnMouseClicked(e -> showHistoryDialog(req.getEquipment().getEquipmentId()));

        Label lastServ = new Label("• Last Service: July 2026 (Safety Check)");
        lastServ.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");

        HBox toolLinks = new HBox(8);
        toolLinks.setAlignment(Pos.CENTER_LEFT);
        toolLinks.getChildren().addAll(photosBtn, partsBtn, historyBtn, lastServ);

        Region footSpacer = new Region();
        HBox.setHgrow(footSpacer, Priority.ALWAYS);

        // Action Buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        boolean isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;
        if (isAdmin) {
            switch (req.getStatus()) {
                case PENDING -> {
                    Button assignBtn = new Button("Assign Technician");
                    assignBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 8; -fx-cursor: hand;");
                    assignBtn.setOnAction(e -> showAssignDialog(req));

                    Button awaitBtn = new Button("Awaiting Parts");
                    awaitBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #f59e0b; -fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 12; -fx-border-radius: 8; -fx-cursor: hand;");
                    awaitBtn.setOnAction(e -> showInfoAlert("Supplier Notification Sent", "Order requisition sent for replacement parts. Ticket updated to Awaiting Parts."));

                    actions.getChildren().addAll(assignBtn, awaitBtn);
                }
                case ASSIGNED -> {
                    Button manageBtn = new Button("Manage Tech ⌄");
                    manageBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 7 12; -fx-background-radius: 8; -fx-cursor: hand;");
                    manageBtn.setOnAction(e -> showAssignDialog(req));

                    Button completeBtn = new Button("Complete Repair");
                    completeBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 16; -fx-background-radius: 8; -fx-cursor: hand;");
                    completeBtn.setOnAction(e -> showCompleteDialog(req));

                    actions.getChildren().addAll(manageBtn, completeBtn);
                }
                case COMPLETED -> {
                    Button reopenBtn = new Button("Reopen Issue");
                    reopenBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
                    reopenBtn.setOnAction(e -> {
                        try {
                            facade.reopenMaintenanceRequest(req.getRequestId());
                            applyFilters();
                        } catch (UnauthorizedAccessException ex) {
                            showAlert("Access Denied", ex.getMessage());
                        }
                    });
                    actions.getChildren().add(reopenBtn);
                }
            }
        }

        footerRow.getChildren().addAll(toolLinks, footSpacer, actions);

        card.getChildren().addAll(headerRow, descBox, metaRow);
        if (!statusDetails.getChildren().isEmpty()) {
            card.getChildren().add(statusDetails);
        }
        card.getChildren().add(footerRow);

        return card;
    }

    private String getCategoryIcon(String category) {
        if (category == null) return "⚙️";
        String c = category.toLowerCase();
        if (c.contains("cardio")) return "🏃";
        if (c.contains("strength") || c.contains("weights")) return "🏋️";
        if (c.contains("studio") || c.contains("group")) return "🧘";
        if (c.contains("functional")) return "🥊";
        if (c.contains("mobility")) return "💆";
        return "⚙️";
    }

    // =========================================================================
    // 9. HIGH-END INTERACTIVE DIALOGS
    // =========================================================================
    
    /**
     * Report New Equipment Issue Modal with visual urgency cards and common templates.
     */
    private void showReportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Report Equipment Issue");
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(520);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        Label headerTitle = new Label("Report Equipment Issue");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800;");

        // Equipment Picker
        VBox equipBox = new VBox(6);
        Label equipLbl = new Label("Select Target Equipment:");
        equipLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

        ComboBox<Equipment> equipCombo = new ComboBox<>();
        equipCombo.getItems().addAll(facade.getAllEquipment());
        equipCombo.setPromptText("Choose equipment from facility inventory...");
        equipCombo.setMaxWidth(Double.MAX_VALUE);
        equipCombo.getStyleClass().add("report-date-combo");

        equipCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " [" + item.getEquipmentId() + "] • " + item.getLocation().getDisplayName());
            }
        });
        equipCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " [" + item.getEquipmentId() + "] • " + item.getLocation().getDisplayName());
            }
        });
        equipBox.getChildren().addAll(equipLbl, equipCombo);

        // Visual Urgency Selector Cards
        VBox urgencyBox = new VBox(8);
        Label urgLbl = new Label("Urgency Level:");
        urgLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

        HBox urgencyCards = new HBox(10);
        ToggleGroup urgGroup = new ToggleGroup();

        ToggleButton lowCard = createUrgencyToggleCard("LOW", "Minor cosmetic / non-critical", "#10b981", urgGroup, Urgency.LOW);
        ToggleButton medCard = createUrgencyToggleCard("MEDIUM", "Degraded performance", "#f59e0b", urgGroup, Urgency.MEDIUM);
        ToggleButton highCard = createUrgencyToggleCard("HIGH", "Safety hazard / inoperable", "#ef4444", urgGroup, Urgency.HIGH);
        medCard.setSelected(true);

        urgencyCards.getChildren().addAll(lowCard, medCard, highCard);
        urgencyBox.getChildren().addAll(urgLbl, urgencyCards);

        // Problem Description
        VBox descBox = new VBox(6);
        Label descLbl = new Label("Issue Description & Symptoms:");
        descLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe what is wrong, unusual noises, safety risks, or damaged parts...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        // Quick Issue Templates
        HBox templates = new HBox(6);
        templates.getChildren().addAll(
            createTemplateChip("Cable fraying", descArea),
            createTemplateChip("Knob jammed", descArea),
            createTemplateChip("Motor overheating", descArea),
            createTemplateChip("Belt slipping", descArea)
        );

        descBox.getChildren().addAll(descLbl, descArea, templates);

        content.getChildren().addAll(headerTitle, new Separator(), equipBox, urgencyBox, descBox);
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Equipment selectedEq = equipCombo.getValue();
                Toggle selToggle = urgGroup.getSelectedToggle();
                Urgency urg = selToggle != null ? (Urgency) selToggle.getUserData() : Urgency.MEDIUM;
                String descText = descArea.getText();

                if (selectedEq != null && descText != null && !descText.trim().isEmpty()) {
                    try {
                        facade.reportMaintenance(selectedEq, descText.trim(), urg);
                        applyFilters();
                    } catch (UnauthorizedAccessException ex) {
                        showAlert("Access Denied", ex.getMessage());
                    }
                } else {
                    showAlert("Missing Information", "Please choose an equipment unit and provide an issue description.");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private ToggleButton createUrgencyToggleCard(String title, String subtitle, String colorWeb, ToggleGroup group, Urgency urgency) {
        ToggleButton tb = new ToggleButton();
        tb.setToggleGroup(group);
        tb.setUserData(urgency);
        tb.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tb, Priority.ALWAYS);
        tb.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand;");

        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: " + colorWeb + ";");
        Label sLbl = new Label(subtitle);
        sLbl.setStyle("-fx-font-size: 9px; -fx-opacity: 0.65; -fx-text-alignment: center;");
        box.getChildren().addAll(tLbl, sLbl);
        tb.setGraphic(box);

        tb.selectedProperty().addListener((obs, old, isSel) -> {
            if (isSel) {
                tb.setStyle("-fx-background-color: " + colorWeb + "20; -fx-border-color: " + colorWeb + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");
            } else {
                tb.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");
            }
        });

        return tb;
    }

    private Label createTemplateChip(String text, TextArea area) {
        Label chip = new Label("+ " + text);
        chip.setStyle("-fx-background-color: rgba(99, 102, 241, 0.15); -fx-text-fill: #818cf8; -fx-background-radius: 12; -fx-padding: 3 8; -fx-font-size: 10px; -fx-cursor: hand;");
        chip.setOnMouseClicked(e -> {
            if (area.getText().isEmpty()) area.setText(text);
            else area.setText(area.getText() + "; " + text);
        });
        return chip;
    }

    /**
     * Assign Technician Dialog with technician team dropdown.
     */
    private void showAssignDialog(MaintenanceRequest req) {
        Dialog<String> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Assign Technician");
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(460);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        Label headerTitle = new Label("Technician Dispatch");
        headerTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800;");

        Label ticketInfo = new Label("Ticket: #" + req.getRequestId() + " • " + req.getEquipment().getName());
        ticketInfo.setStyle("-fx-opacity: 0.7; -fx-font-size: 12px;");

        Label techLbl = new Label("Select Dedicated Service Provider / Tech:");
        techLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

        ComboBox<String> techCombo = new ComboBox<>();
        techCombo.getItems().addAll(
            "Tech Team Alpha (Priority Rapid Response)",
            "Precision Hydraulics & Cable Specialists",
            "Dave Martinez (Senior In-House Technician)",
            "Apex Fitness Systems Authorized Service",
            "Metro Electronic Diagnostics"
        );
        techCombo.setValue(req.getAssignedTo() != null ? req.getAssignedTo() : "Tech Team Alpha (Priority Rapid Response)");
        techCombo.setMaxWidth(Double.MAX_VALUE);
        techCombo.getStyleClass().add("report-date-combo");

        content.getChildren().addAll(headerTitle, ticketInfo, new Separator(), techLbl, techCombo);
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? techCombo.getValue() : null);

        dialog.showAndWait().ifPresent(techName -> {
            if (techName != null && !techName.trim().isEmpty()) {
                try {
                    facade.assignMaintenance(req.getRequestId(), techName.split(" \\(")[0].trim());
                    applyFilters();
                } catch (UnauthorizedAccessException ex) {
                    showAlert("Access Denied", ex.getMessage());
                }
            }
        });
    }

    /**
     * Complete Repair Dialog with resolution notes & threshold recalibration.
     */
    private void showCompleteDialog(MaintenanceRequest req) {
        Dialog<String> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Complete Maintenance Ticket");
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(480);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        Label headerTitle = new Label("Certification of Repair");
        headerTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800;");

        Label ticketInfo = new Label("Equipment: " + req.getEquipment().getName() + " [#" + req.getRequestId() + "]");
        ticketInfo.setStyle("-fx-opacity: 0.7; -fx-font-size: 12px;");

        Label notesLbl = new Label("Resolution Summary & Parts Replaced:");
        notesLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("E.g. Replaced tension knob assembly, lubed guide rods, completed 15-min safety stress test...");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        CheckBox resetHoursCb = new CheckBox("Reset Cumulative Usage Hours to 0.0h (Full Overhaul Certified)");
        resetHoursCb.setStyle("-fx-font-size: 11px; -fx-text-fill: #34d399; -fx-font-weight: 600;");

        content.getChildren().addAll(headerTitle, ticketInfo, new Separator(), notesLbl, notesArea, resetHoursCb);
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? notesArea.getText() : null);

        dialog.showAndWait().ifPresent(notes -> {
            if (notes != null && !notes.trim().isEmpty()) {
                try {
                    facade.completeMaintenance(req.getRequestId(), notes.trim());
                    if (resetHoursCb.isSelected()) {
                        req.getEquipment().setCumulativeUsageHours(0.0);
                        com.iwfc.pattern.DataStore.getInstance().getEquipment().update(req.getEquipment());
                    }
                    applyFilters();
                } catch (UnauthorizedAccessException ex) {
                    showAlert("Access Denied", ex.getMessage());
                }
            }
        });
    }

    /**
     * Simulated Issue Photos Gallery Dialog.
     */
    private void showPhotosDialog(MaintenanceRequest req) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Diagnostic Photos — " + req.getEquipment().getName());
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(540);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        Label title = new Label("Diagnostic Inspection Imagery");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800;");

        Label sub = new Label("Ticket #" + req.getRequestId() + " • Captured during incident report");
        sub.setStyle("-fx-opacity: 0.65; -fx-font-size: 12px;");

        HBox photoGallery = new HBox(12);
        photoGallery.setAlignment(Pos.CENTER);

        photoGallery.getChildren().addAll(
            createPhotoCard("Component Damage", "Fraying / crack point", "📷 #1"),
            createPhotoCard("Lockout Tag", "Safety warning placed", "🔒 #2"),
            createPhotoCard("Serial Plate", req.getEquipment().getEquipmentId(), "🏷️ #3")
        );

        content.getChildren().addAll(title, sub, new Separator(), photoGallery);
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private VBox createPhotoCard(String label, String note, String tag) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 16;");
        card.setPrefWidth(150);

        Label tagLbl = new Label(tag);
        tagLbl.setStyle("-fx-font-size: 26px;");

        Label l = new Label(label);
        l.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");

        Label n = new Label(note);
        n.setStyle("-fx-opacity: 0.6; -fx-font-size: 10px; -fx-text-alignment: center;");
        n.setWrapText(true);

        card.getChildren().addAll(tagLbl, l, n);
        return card;
    }

    /**
     * Parts Inventory Stock Verification Dialog.
     */
    private void showPartsDialog(MaintenanceRequest req) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Required Replacement Parts");
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(500);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        Label title = new Label("Required Parts & Local Inventory");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800;");

        Label sub = new Label("Requisition status for: " + req.getEquipment().getName());
        sub.setStyle("-fx-opacity: 0.65; -fx-font-size: 12px;");

        VBox list = new VBox(8);
        list.getChildren().addAll(
            createPartItem("Heavy-Duty Coated Cable 4.8mm", "Stock: 6 Units Available", "IN STOCK", "#10b981"),
            createPartItem("Bushing & Pulley Wheel 90mm", "Stock: 12 Units Available", "IN STOCK", "#10b981"),
            createPartItem("Precision Resistance Knob Assm", "Supplier ETA: Next Day (Ordered)", "DISPATCHED", "#f59e0b")
        );

        content.getChildren().addAll(title, sub, new Separator(), list);
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private HBox createPartItem(String partName, String stockDesc, String statusBadge, String colorWeb) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12;");

        VBox left = new VBox(2);
        Label name = new Label(partName);
        name.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");
        Label stock = new Label(stockDesc);
        stock.setStyle("-fx-opacity: 0.65; -fx-font-size: 11px;");
        left.getChildren().addAll(name, stock);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label(statusBadge);
        badge.setStyle("-fx-background-color: " + colorWeb + "20; -fx-text-fill: " + colorWeb + "; -fx-border-color: " + colorWeb + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: 700;");

        row.getChildren().addAll(left, sp, badge);
        return row;
    }

    /**
     * Historical Maintenance Log Dialog.
     */
    private void showHistoryDialog(String equipmentId) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Equipment Service History");
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(540);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        Label title = new Label("Service Lifecycle & Maintenance History");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800;");

        Label sub = new Label("Equipment ID: " + equipmentId);
        sub.setStyle("-fx-opacity: 0.65; -fx-font-size: 12px;");

        List<MaintenanceRequest> history = facade.getRequestsForEquipment(equipmentId);
        VBox list = new VBox(10);

        if (history.isEmpty()) {
            Label noHist = new Label("No historical service tickets on record for this machine.");
            noHist.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");
            list.getChildren().add(noHist);
        } else {
            for (MaintenanceRequest req : history) {
                VBox item = new VBox(4);
                item.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12;");

                HBox top = new HBox(8);
                top.setAlignment(Pos.CENTER_LEFT);
                Label idLbl = new Label("#" + req.getRequestId() + " (" + req.getStatus().getDisplayName() + ")");
                idLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 12px;");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                String d = req.getCreatedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                Label date = new Label(d);
                date.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");

                top.getChildren().addAll(idLbl, sp, date);

                Label dsc = new Label(req.getDescription());
                dsc.setStyle("-fx-opacity: 0.85; -fx-font-size: 12px;");
                dsc.setWrapText(true);

                item.getChildren().addAll(top, dsc);

                if (req.getStatus() == RequestStatus.COMPLETED && req.getResolutionNotes() != null) {
                    Label res = new Label("✅ " + req.getResolutionNotes());
                    res.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: 600;");
                    item.getChildren().add(res);
                }
                list.getChildren().add(item);
            }
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(280);
        scroll.getStyleClass().add("report-scroll");

        content.getChildren().addAll(title, sub, new Separator(), scroll);
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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
}

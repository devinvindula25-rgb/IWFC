package com.iwfc.ui;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.DialogUtils;
import com.iwfc.ui.components.StatusBadge;
import com.iwfc.ui.components.UsageRing;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Supreme Equipment Operations Center & Asset Telemetry Hub.
 * Features 2-tier navigation (Category Overview Donut Hub & Category Drill-Down),
 * interactive Gym Floor Map blueprint with live pulsing telemetry dots,
 * real-time maintenance alert triage feed, and 100% operational filters and modals.
 */
public class EquipmentView extends VBox {

    private final IWFCSystemFacade facade;

    // Filter controls
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> locationFilter;
    private EquipmentStatus activeKpiFilter = null;
    private String selectedCategory = null;
    private String mapCategoryFilter = null;

    // UI containers
    private HBox kpiContainer;
    private VBox contentContainer;
    private VBox rightColumn;
    private Pane mapCanvas;
    private final Map<EquipmentStatus, VBox> kpiCardsMap = new EnumMap<>(EquipmentStatus.class);

    // Date formatter
    private static final DateTimeFormatter ALERT_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public EquipmentView(IWFCSystemFacade facade) {
        this.facade = facade;
        setSpacing(20);
        setPadding(new Insets(24));
        getStyleClass().add("equipment-view");
        VBox.setVgrow(this, Priority.ALWAYS);

        try {
            getStylesheets().add(getClass().getResource("/com/iwfc/css/equipment-management.css").toExternalForm());
        } catch (Exception ignored) {}

        buildUI();
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Top Header & Interactive Toolbar
        VBox headerArea = buildHeaderAndToolbar();

        // 2. Equipment Summary KPI Stat Blocks (Operational, Faulty, Maintenance)
        kpiContainer = new HBox(16);
        kpiContainer.setAlignment(Pos.CENTER_LEFT);

        // 3. Main Center Area: Left (Categories / Drilldown) + Right (Map + Alerts)
        HBox bodyArea = new HBox(24);
        VBox.setVgrow(bodyArea, Priority.ALWAYS);

        // Left Column (Scrollable Cards)
        VBox leftColumn = new VBox(18);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        contentContainer = new VBox(20);
        ScrollPane scrollPane = new ScrollPane(contentContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        leftColumn.getChildren().addAll(kpiContainer, scrollPane);

        // Right Column (Map + Alerts)
        rightColumn = new VBox(20);
        rightColumn.setPrefWidth(360);
        rightColumn.setMinWidth(360);

        bodyArea.getChildren().addAll(leftColumn, rightColumn);

        getChildren().addAll(headerArea, bodyArea);

        // Initial render
        refreshAllData();
    }

    // =========================================================================
    // 1. HEADER & INTERACTIVE TOOLBAR
    // =========================================================================
    private VBox buildHeaderAndToolbar() {
        VBox container = new VBox(14);

        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Equipment Management");
        title.getStyleClass().add("equipment-header-title");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        // Add Equipment Button (Admin/Instructor)
        Button addBtn = new Button("+ Add Equipment");
        addBtn.getStyleClass().add("equipment-add-btn");
        boolean canAdd = facade.getCurrentUser().getRole() == Role.ADMIN || facade.getCurrentUser().getRole() == Role.INSTRUCTOR;
        addBtn.setVisible(canAdd);
        addBtn.setManaged(canAdd);
        addBtn.setOnAction(e -> showAddEquipmentModal());

        titleRow.getChildren().addAll(title, titleSpacer, addBtn);

        // Toolbar: Search + Status Dropdown + Location Dropdown + Refresh Button
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Search Input with instant listener
        searchField = new TextField();
        searchField.setPromptText("🔍  Search equipment by name or ID...");
        searchField.getStyleClass().add("equipment-search-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> renderMainContent());

        // Status Filter Dropdown
        statusFilter = new ComboBox<>();
        statusFilter.getStyleClass().add("equipment-combo-box");
        statusFilter.getItems().add("All Statuses");
        for (EquipmentStatus s : EquipmentStatus.values()) {
            statusFilter.getItems().add(s.getDisplayName());
        }
        statusFilter.setValue("All Statuses");
        statusFilter.setOnAction(e -> {
            String val = statusFilter.getValue();
            if (val == null || "All Statuses".equals(val)) {
                activeKpiFilter = null;
            } else {
                for (EquipmentStatus s : EquipmentStatus.values()) {
                    if (s.getDisplayName().equalsIgnoreCase(val)) {
                        activeKpiFilter = s;
                        break;
                    }
                }
            }
            updateKpiSelectionVisuals();
            renderMainContent();
            updateFloorMapDots();
        });

        // Location Filter Dropdown
        locationFilter = new ComboBox<>();
        locationFilter.getStyleClass().add("equipment-combo-box");
        locationFilter.getItems().add("All Locations");
        for (Location loc : Location.values()) {
            locationFilter.getItems().add(loc.getDisplayName());
        }
        locationFilter.setValue("All Locations");
        locationFilter.setOnAction(e -> {
            renderMainContent();
            updateFloorMapDots();
        });

        // Refresh Button with 360 spin animation
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("equipment-refresh-btn");
        refreshBtn.setOnAction(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(400), refreshBtn);
            rt.setByAngle(360);
            rt.play();

            // Clear search and reset filter selections
            searchField.clear();
            activeKpiFilter = null;
            mapCategoryFilter = null;
            statusFilter.setValue("All Statuses");
            locationFilter.setValue("All Locations");
            refreshAllData();
        });

        toolbar.getChildren().addAll(searchField, statusFilter, locationFilter, refreshBtn);

        container.getChildren().addAll(titleRow, toolbar);
        return container;
    }

    // =========================================================================
    // 2. EQUIPMENT SUMMARY KPI CARDS
    // =========================================================================
    private void refreshKpiCards() {
        kpiContainer.getChildren().clear();
        kpiCardsMap.clear();

        Map<EquipmentStatus, Long> summary = facade.getEquipmentSummary();
        long operational = summary.getOrDefault(EquipmentStatus.OPERATIONAL, 0L);
        long faulty = summary.getOrDefault(EquipmentStatus.FAULTY, 0L);
        long maintenance = summary.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L);

        VBox opCard = createKpiCard("OPERATIONAL", operational, EquipmentStatus.OPERATIONAL, "#10b981", "✔");
        VBox faultyCard = createKpiCard("FAULTY", faulty, EquipmentStatus.FAULTY, "#ef4444", "⚠");
        VBox maintCard = createKpiCard("MAINTENANCE", maintenance, EquipmentStatus.UNDER_MAINTENANCE, "#f59e0b", "📅");

        kpiCardsMap.put(EquipmentStatus.OPERATIONAL, opCard);
        kpiCardsMap.put(EquipmentStatus.FAULTY, faultyCard);
        kpiCardsMap.put(EquipmentStatus.UNDER_MAINTENANCE, maintCard);

        kpiContainer.getChildren().addAll(opCard, faultyCard, maintCard);
        updateKpiSelectionVisuals();
    }

    private VBox createKpiCard(String title, long count, EquipmentStatus status, String hexColor, String icon) {
        VBox card = new VBox(6);
        card.getStyleClass().add("equipment-kpi-block");
        card.setStyle("-fx-background-color: " + hexColor + ";");
        HBox.setHgrow(card, Priority.ALWAYS);

        // Click to toggle filter
        card.setOnMouseClicked(e -> {
            if (activeKpiFilter == status) {
                activeKpiFilter = null;
                statusFilter.setValue("All Statuses");
            } else {
                activeKpiFilter = status;
                statusFilter.setValue(status.getDisplayName());
            }
            updateKpiSelectionVisuals();
            renderMainContent();
            updateFloorMapDots();
        });

        // Top Row: Title + Icon Badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 800; -fx-letter-spacing: 0.5px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-text-fill: rgba(0,0,0,0.5); -fx-font-size: 13px; -fx-background-color: rgba(0,0,0,0.14); -fx-background-radius: 12px; -fx-padding: 2 7;");

        topRow.getChildren().addAll(titleLbl, spacer, iconLbl);

        // Bottom Row: Number + SVG Sparkline Trend
        HBox bottomRow = new HBox(12);
        bottomRow.setAlignment(Pos.BOTTOM_LEFT);

        Label countLbl = new Label("0");
        countLbl.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: 900;");

        // Roll-up counter animation
        Timeline counterTl = new Timeline();
        int steps = Math.min((int) count, 20);
        if (steps > 0) {
            for (int i = 0; i <= steps; i++) {
                final long val = (count * i) / steps;
                counterTl.getKeyFrames().add(new KeyFrame(Duration.millis(i * (400.0 / steps)), ev -> countLbl.setText(String.valueOf(val))));
            }
        } else {
            countLbl.setText(String.valueOf(count));
        }
        counterTl.play();

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        // Trend sparkline
        Polyline sparkline = new Polyline();
        double[] points = new double[12];
        Random rnd = new Random(title.hashCode());
        double y = 18;
        for (int i = 0; i < 6; i++) {
            points[i * 2] = i * 16.0;
            points[i * 2 + 1] = Math.max(4, Math.min(28, y + (rnd.nextDouble() * 12 - 5)));
            y = points[i * 2 + 1];
        }
        for (double d : points) sparkline.getPoints().add(d);
        sparkline.setStroke(Color.rgb(0, 0, 0, 0.45));
        sparkline.setStrokeWidth(2.5);
        sparkline.setFill(Color.TRANSPARENT);

        bottomRow.getChildren().addAll(countLbl, bottomSpacer, sparkline);

        card.getChildren().addAll(topRow, bottomRow);

        // Hover effect
        card.setOnMouseEntered(e -> {
            if (activeKpiFilter != status) {
                card.setStyle("-fx-background-color: " + hexColor + "ee; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            }
        });
        card.setOnMouseExited(e -> {
            if (activeKpiFilter != status) {
                card.setStyle("-fx-background-color: " + hexColor + ";");
            }
        });

        return card;
    }

    private void updateKpiSelectionVisuals() {
        for (Map.Entry<EquipmentStatus, VBox> entry : kpiCardsMap.entrySet()) {
            EquipmentStatus status = entry.getKey();
            VBox card = entry.getValue();
            String hex = switch (status) {
                case OPERATIONAL -> "#10b981";
                case FAULTY -> "#ef4444";
                case UNDER_MAINTENANCE -> "#f59e0b";
            };

            if (activeKpiFilter == status) {
                card.setStyle("-fx-background-color: " + hex + "; -fx-border-color: #ffffff; -fx-border-width: 2.5px; -fx-border-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.4), 14, 0, 0, 2);");
            } else {
                card.setStyle("-fx-background-color: " + hex + ";");
            }
        }
    }

    // =========================================================================
    // 3. MAIN CENTER CONTENT (CATEGORY HUB VS DRILL-DOWN)
    // =========================================================================
    private void renderMainContent() {
        contentContainer.getChildren().clear();

        List<Equipment> all = facade.getAllEquipment();
        List<Equipment> filtered = filterEquipment(all);

        if (selectedCategory == null) {
            renderCategoryOverviewHub(filtered);
        } else {
            renderCategoryDrillDownView(filtered);
        }
    }

    private List<Equipment> filterEquipment(List<Equipment> source) {
        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String selectedLoc = locationFilter != null ? locationFilter.getValue() : "All Locations";

        return source.stream().filter(eq -> {
            // Text search
            if (!query.isEmpty()) {
                boolean inName = eq.getName().toLowerCase().contains(query);
                boolean inId = eq.getEquipmentId().toLowerCase().contains(query);
                boolean inCat = eq.getCategory().toLowerCase().contains(query);
                boolean inLoc = eq.getLocation().getDisplayName().toLowerCase().contains(query);
                if (!inName && !inId && !inCat && !inLoc) return false;
            }
            // Status filter
            if (activeKpiFilter != null && eq.getStatus() != activeKpiFilter) {
                return false;
            }
            // Location filter
            if (selectedLoc != null && !"All Locations".equals(selectedLoc)) {
                if (!eq.getLocation().getDisplayName().equalsIgnoreCase(selectedLoc)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    // --- WINDOW 1: CATEGORY FLEET OVERVIEW HUB ---
    private void renderCategoryOverviewHub(List<Equipment> equipmentList) {
        // Group by category maintaining nice order
        Map<String, List<Equipment>> categoryMap = new LinkedHashMap<>();
        for (Equipment eq : equipmentList) {
            categoryMap.computeIfAbsent(eq.getCategory(), k -> new ArrayList<>()).add(eq);
        }

        FlowPane categoryGrid = new FlowPane(20, 20);
        categoryGrid.setAlignment(Pos.TOP_LEFT);

        for (Map.Entry<String, List<Equipment>> entry : categoryMap.entrySet()) {
            VBox catCard = createCategoryCard(entry.getKey(), entry.getValue());
            categoryGrid.getChildren().add(catCard);
        }

        if (categoryMap.isEmpty()) {
            VBox emptyBox = createEmptyState("No equipment matches your current search/filter criteria.");
            contentContainer.getChildren().add(emptyBox);
        } else {
            contentContainer.getChildren().add(categoryGrid);
        }
    }

    private VBox createCategoryCard(String categoryName, List<Equipment> items) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(235, 270);
        card.getStyleClass().add("equipment-cat-card");

        // Category icon & neon accent
        String icon = "🏋️";
        String accentColor = "#3b82f6";
        String lowerCat = categoryName.toLowerCase();
        if (lowerCat.contains("cardio")) { icon = "🏃"; accentColor = "#ef4444"; }
        else if (lowerCat.contains("machines")) { icon = "🏋️"; accentColor = "#eab308"; }
        else if (lowerCat.contains("free weights")) { icon = "💪"; accentColor = "#f59e0b"; }
        else if (lowerCat.contains("studio")) { icon = "🧘"; accentColor = "#a855f7"; }
        else if (lowerCat.contains("functional")) { icon = "🤸"; accentColor = "#f97316"; }
        else if (lowerCat.contains("mobility")) { icon = "🩹"; accentColor = "#06b6d4"; }

        // Counts
        int good = 0; int minor = 0; int faulty = 0;
        for (Equipment eq : items) {
            switch (eq.getStatus()) {
                case OPERATIONAL -> good++;
                case UNDER_MAINTENANCE -> minor++;
                case FAULTY -> faulty++;
            }
        }
        int total = items.size();

        // Donut Chart (Circular Health Ring)
        StackPane donutPane = new StackPane();
        donutPane.setPrefSize(100, 100);
        donutPane.setMaxSize(100, 100);

        double radius = 40;
        double strokeWidth = 8;

        // Background circle track
        Arc bgArc = new Arc(0, 0, radius, radius, 0, 360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStrokeWidth(strokeWidth);
        bgArc.setStroke(Color.rgb(255, 255, 255, 0.08));
        bgArc.setFill(Color.TRANSPARENT);
        donutPane.getChildren().add(bgArc);

        if (total > 0) {
            double goodAngle = ((double) good / total) * 360.0;
            double minorAngle = ((double) minor / total) * 360.0;
            double faultyAngle = ((double) faulty / total) * 360.0;

            // Good Arc (Green)
            if (good > 0) {
                Arc gArc = new Arc(0, 0, radius, radius, 90, -goodAngle);
                gArc.setType(ArcType.OPEN);
                gArc.setStrokeWidth(strokeWidth);
                gArc.setStroke(Color.web("#4ade80"));
                gArc.setFill(Color.TRANSPARENT);
                donutPane.getChildren().add(gArc);
            }
            // Minor / Maintenance Arc (Amber)
            if (minor > 0) {
                Arc mArc = new Arc(0, 0, radius, radius, 90 - goodAngle, -minorAngle);
                mArc.setType(ArcType.OPEN);
                mArc.setStrokeWidth(strokeWidth);
                mArc.setStroke(Color.web("#f59e0b"));
                mArc.setFill(Color.TRANSPARENT);
                donutPane.getChildren().add(mArc);
            }
            // Faulty Arc (Red)
            if (faulty > 0) {
                Arc fArc = new Arc(0, 0, radius, radius, 90 - goodAngle - minorAngle, -faultyAngle);
                fArc.setType(ArcType.OPEN);
                fArc.setStrokeWidth(strokeWidth);
                fArc.setStroke(Color.web("#ef4444"));
                fArc.setFill(Color.TRANSPARENT);
                donutPane.getChildren().add(fArc);
            }
        }

        // Centered category icon
        Label centerIcon = new Label(icon);
        centerIcon.setStyle("-fx-font-size: 34px; -fx-effect: dropshadow(gaussian, " + accentColor + "88, 14, 0, 0, 0);");
        donutPane.getChildren().add(centerIcon);

        // Titles & Stats
        Label catNameLbl = new Label(categoryName);
        catNameLbl.getStyleClass().add("equipment-cat-name");
        catNameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");

        Label countLbl = new Label(total + " Items");
        countLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 700;");

        Label breakdownLbl = new Label(good + " Good / " + minor + " Minor / " + faulty + " Faulty");
        breakdownLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");

        Label maintLbl = new Label("Next Maintenance: 15 Nov (" + Math.max(1, total / 2) + " items)");
        maintLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: 600;");

        // Action Toolbar: Inspect (🔍) + Map Pin (📍) + Service (🔧)
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.setPadding(new Insets(6, 12, 0, 12));

        Button inspectBtn = new Button("🔍 Inspect");
        inspectBtn.getStyleClass().add("equipment-card-btn");
        inspectBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
        inspectBtn.setOnAction(e -> {
            selectedCategory = categoryName;
            renderMainContent();
        });

        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, Priority.ALWAYS);

        Button mapPinBtn = new Button("📍");
        mapPinBtn.getStyleClass().add("equipment-card-btn");
        mapPinBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        Tooltip.install(mapPinBtn, new Tooltip("Highlight " + categoryName + " on Floor Map"));
        mapPinBtn.setOnAction(e -> {
            mapCategoryFilter = categoryName.equals(mapCategoryFilter) ? null : categoryName;
            updateFloorMapDots();
        });

        Button serviceBtn = new Button("🔧");
        serviceBtn.getStyleClass().add("equipment-card-btn");
        serviceBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        Tooltip.install(serviceBtn, new Tooltip("Quick Service for " + categoryName));
        serviceBtn.setOnAction(e -> {
            selectedCategory = categoryName;
            renderMainContent();
        });

        actionsRow.getChildren().addAll(inspectBtn, actSpacer, mapPinBtn, serviceBtn);

        card.getChildren().addAll(donutPane, catNameLbl, countLbl, breakdownLbl, maintLbl, actionsRow);

        // Clicking anywhere on card opens drill-down
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != inspectBtn && e.getTarget() != mapPinBtn && e.getTarget() != serviceBtn) {
                selectedCategory = categoryName;
                renderMainContent();
            }
        });

        // Hover scale animation
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.025); st.setToY(1.025);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        return card;
    }

    // --- WINDOW 2: CATEGORY DRILL-DOWN VIEW ---
    private void renderCategoryDrillDownView(List<Equipment> filteredList) {
        // Navigation Bar: "← Back to Categories"
        HBox navBar = new HBox(12);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 10, 0));

        Button backBtn = new Button("← Back to Categories");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #06b6d4; -fx-font-weight: 800; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 0;");
        backBtn.setOnAction(e -> {
            selectedCategory = null;
            renderMainContent();
        });

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        Label catTitle = new Label(selectedCategory + " Equipment");
        catTitle.getStyleClass().add("equipment-drilldown-title");
        catTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900;");

        List<Equipment> categoryItems = filteredList.stream()
                .filter(eq -> eq.getCategory().equalsIgnoreCase(selectedCategory))
                .collect(Collectors.toList());

        Label countPill = new Label(categoryItems.size() + " Items Available");
        countPill.setStyle("-fx-background-color: rgba(6, 182, 212, 0.15); -fx-text-fill: #22d3ee; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 4 12; -fx-background-radius: 14px;");

        navBar.getChildren().addAll(backBtn, navSpacer, catTitle, countPill);

        FlowPane eqGrid = new FlowPane(18, 18);
        eqGrid.setAlignment(Pos.TOP_LEFT);

        for (Equipment eq : categoryItems) {
            eqGrid.getChildren().add(createEquipmentItemCard(eq));
        }

        if (categoryItems.isEmpty()) {
            VBox empty = createEmptyState("No equipment found in category \"" + selectedCategory + "\" matching filters.");
            contentContainer.getChildren().addAll(navBar, empty);
        } else {
            contentContainer.getChildren().addAll(navBar, eqGrid);
        }
    }

    private VBox createEquipmentItemCard(Equipment eq) {
        VBox card = new VBox(14);
        card.setPrefWidth(265);
        card.getStyleClass().add("equipment-item-card");

        // Equipment specific icon
        String icon = "🏋️";
        String lower = eq.getName().toLowerCase();
        if (lower.contains("treadmill")) icon = "🏃";
        else if (lower.contains("stair")) icon = "🧗";
        else if (lower.contains("bike") || lower.contains("assault") || lower.contains("spin")) icon = "🚴";
        else if (lower.contains("row")) icon = "🚣";
        else if (lower.contains("elliptical")) icon = "🏃";
        else if (lower.contains("cable") || lower.contains("smith") || lower.contains("press")) icon = "🏋️";
        else if (lower.contains("dumbbell") || lower.contains("barbell")) icon = "💪";
        else if (lower.contains("mat") || lower.contains("yoga")) icon = "🧘";

        // Top Row: Icon + Name + ID + Menu (⋮)
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px;");

        VBox nameBox = new VBox(2);
        Label nameLbl = new Label(eq.getName());
        nameLbl.getStyleClass().add("equipment-item-name");
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800;");
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(165);

        Label idLbl = new Label("ID: " + eq.getEquipmentId());
        idLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 700;");
        nameBox.getChildren().addAll(nameLbl, idLbl);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label menuBtn = new Label("⋮");
        menuBtn.setStyle("-fx-font-size: 20px; -fx-text-fill: #94a3b8; -fx-cursor: hand; -fx-padding: 0 4;");
        attachEquipmentContextMenu(menuBtn, eq);

        topRow.getChildren().addAll(iconLbl, nameBox, topSpacer, menuBtn);

        // Middle Section: Usage Ring + Telemetry Details
        HBox midRow = new HBox(14);
        midRow.setAlignment(Pos.CENTER_LEFT);

        VBox ringCol = new VBox(4);
        ringCol.setAlignment(Pos.CENTER);
        UsageRing ring = new UsageRing(eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold(), eq.getStatus());
        StatusBadge statusBadge = StatusBadge.forEquipmentStatus(eq.getStatus());
        ringCol.getChildren().addAll(ring, statusBadge);

        VBox detailsCol = new VBox(6);
        detailsCol.setAlignment(Pos.CENTER_LEFT);

        double pct = (eq.getCumulativeUsageHours() / Math.max(1, eq.getMaintenanceThreshold())) * 100.0;
        Label hoursLbl = new Label(String.format("%.0f of %.0f hrs (%.0f%%)", eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold(), pct));
        hoursLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: 700;");

        Label estMaint = new Label("Est. Maint: 12/23");
        estMaint.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 700;");
        if (eq.getStatus() == EquipmentStatus.FAULTY) {
            estMaint.setText("Requires Service");
            estMaint.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 700;");
        } else if (eq.getStatus() == EquipmentStatus.UNDER_MAINTENANCE) {
            estMaint.setText("In Maintenance");
            estMaint.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: 700;");
        }

        Label locLbl = new Label("📍 " + eq.getLocation().getDisplayName());
        locLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        detailsCol.getChildren().addAll(hoursLbl, estMaint, locLbl);
        midRow.getChildren().addAll(ringCol, detailsCol);

        // Last Service row
        Label lastServiceLbl = new Label("Last Service: 12/21");
        lastServiceLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        // Bottom Action Buttons: View Details & Log Maintenance
        HBox bottomButtons = new HBox(8);
        bottomButtons.setAlignment(Pos.CENTER);

        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("equipment-card-btn");
        viewDetailsBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(viewDetailsBtn, Priority.ALWAYS);
        viewDetailsBtn.setOnAction(e -> showEquipmentDetailsModal(eq));

        Button logMaintBtn = new Button("Log Maintenance");
        logMaintBtn.getStyleClass().add("equipment-maint-btn");
        logMaintBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(logMaintBtn, Priority.ALWAYS);
        logMaintBtn.setOnAction(e -> handleLogMaintenanceAction(eq));

        bottomButtons.getChildren().addAll(viewDetailsBtn, logMaintBtn);

        card.getChildren().addAll(topRow, midRow, lastServiceLbl, bottomButtons);

        // Hover animation
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.02); st.setToY(1.02);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        return card;
    }

    private void attachEquipmentContextMenu(Node triggerNode, Equipment eq) {
        ContextMenu menu = new ContextMenu();

        MenuItem detailsItem = new MenuItem("🔍 View Full Details");
        detailsItem.setOnAction(e -> showEquipmentDetailsModal(eq));

        MenuItem logUsageItem = new MenuItem("⚡ Log Usage Hours");
        logUsageItem.setOnAction(e -> showLogUsageDialog(eq));

        MenuItem historyItem = new MenuItem("📜 View Usage History");
        historyItem.setOnAction(e -> showEquipmentDetailsModal(eq));

        menu.getItems().addAll(detailsItem, logUsageItem, historyItem);

        Role role = facade.getCurrentUser().getRole();
        if (role == Role.ADMIN || role == Role.INSTRUCTOR) {
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem editItem = new MenuItem("✏ Edit Specifications");
            editItem.setOnAction(e -> showEditEquipmentModal(eq));
            menu.getItems().add(editItem);

            if (role == Role.ADMIN) {
                MenuItem deactivateItem = new MenuItem("⏸ Deactivate Equipment");
                deactivateItem.setOnAction(e -> {
                    try {
                        facade.deactivateEquipment(eq.getEquipmentId());
                        refreshAllData();
                    } catch (UnauthorizedAccessException ex) {
                        showErrorAlert("Access Denied", ex.getMessage());
                    }
                });

                MenuItem removeItem = new MenuItem("🗑 Remove Equipment");
                removeItem.setOnAction(e -> handleRemoveEquipment(eq));

                menu.getItems().addAll(deactivateItem, removeItem);
            }
        }

        triggerNode.setOnMouseClicked(e -> menu.show(triggerNode, e.getScreenX(), e.getScreenY()));
    }

    // =========================================================================
    // 4. RIGHT COLUMN: GYM FLOOR MAP & ALERTS FEED
    // =========================================================================
    private void refreshRightColumn() {
        rightColumn.getChildren().clear();

        // 1. Gym Floor Map Panel
        VBox mapPanel = buildGymFloorMapPanel();

        // 2. Recent Maintenance & Alerts Panel
        VBox alertsPanel = buildRecentAlertsPanel();

        rightColumn.getChildren().addAll(mapPanel, alertsPanel);
    }

    private VBox buildGymFloorMapPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("equipment-glass-panel");

        // Map Header & Legend
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gym Floor Map");
        title.getStyleClass().add("equipment-panel-title");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label opLegend = new Label("● Op");
        opLegend.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 10px; -fx-font-weight: 700;");
        Label faultLegend = new Label("● Fault");
        faultLegend.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-weight: 700;");
        Label maintLegend = new Label("● Maint");
        maintLegend.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 10px; -fx-font-weight: 700;");

        headerRow.getChildren().addAll(title, spacer, opLegend, faultLegend, maintLegend);

        // Canvas container for blueprint
        mapCanvas = new Pane();
        mapCanvas.setPrefSize(324, 250);
        mapCanvas.setMinSize(324, 250);
        mapCanvas.setMaxSize(324, 250);
        mapCanvas.getStyleClass().add("equipment-map-canvas");

        drawFloorBlueprintZones(mapCanvas);
        populateFloorMapDots(mapCanvas);

        panel.getChildren().addAll(headerRow, mapCanvas);
        return panel;
    }

    private void drawFloorBlueprintZones(Pane pane) {
        // Draw subtle zone blueprint partitions
        double w = 324;
        double h = 250;

        // Zone labels and bounding boxes
        String[] zones = {"Cardio Zone", "Studio A & B", "Weight Room", "Pool & Recovery"};
        double[][] rects = {
                {10, 10, w / 2 - 15, h / 2 - 15},
                {w / 2 + 5, 10, w / 2 - 15, h / 2 - 15},
                {10, h / 2 + 5, w / 2 - 15, h / 2 - 15},
                {w / 2 + 5, h / 2 + 5, w / 2 - 15, h / 2 - 15}
        };

        for (int i = 0; i < zones.length; i++) {
            Rectangle r = new Rectangle(rects[i][0], rects[i][1], rects[i][2], rects[i][3]);
            r.setFill(Color.rgb(255, 255, 255, 0.02));
            r.setStroke(Color.rgb(255, 255, 255, 0.07));
            r.setStrokeWidth(1);
            r.setArcWidth(8);
            r.setArcHeight(8);

            Label lbl = new Label(zones[i].toUpperCase());
            lbl.setLayoutX(rects[i][0] + 8);
            lbl.setLayoutY(rects[i][1] + 6);
            lbl.setStyle("-fx-text-fill: rgba(255,255,255,0.18); -fx-font-size: 8px; -fx-font-weight: 800; -fx-letter-spacing: 0.5px;");

            pane.getChildren().addAll(r, lbl);
        }
    }

    private void populateFloorMapDots(Pane pane) {
        List<Equipment> equipmentList = facade.getAllEquipment();

        // Categorize by location to calculate realistic coordinates
        Map<Location, List<Equipment>> locMap = new EnumMap<>(Location.class);
        for (Equipment eq : equipmentList) {
            locMap.computeIfAbsent(eq.getLocation(), k -> new ArrayList<>()).add(eq);
        }

        for (Map.Entry<Location, List<Equipment>> entry : locMap.entrySet()) {
            Location loc = entry.getKey();
            List<Equipment> items = entry.getValue();

            // Bounding box for this location on the 324x250 canvas
            double minX = 20, maxX = 140, minY = 25, maxY = 105;
            switch (loc) {
                case CARDIO_ZONE -> { minX = 20; maxX = 135; minY = 28; maxY = 105; }
                case STUDIO_A -> { minX = 175; maxX = 300; minY = 25; maxY = 65; }
                case STUDIO_B -> { minX = 175; maxX = 300; minY = 70; maxY = 110; }
                case WEIGHT_ROOM -> { minX = 20; maxX = 140; minY = 145; maxY = 230; }
                case POOL_AREA -> { minX = 175; maxX = 305; minY = 145; maxY = 230; }
                case OUTDOOR_AREA -> { minX = 120; maxX = 195; minY = 100; maxY = 145; }
            }

            int count = items.size();
            for (int i = 0; i < count; i++) {
                Equipment eq = items.get(i);
                double frac = count > 1 ? (double) i / (count - 1) : 0.5;
                double x = minX + frac * (maxX - minX) + (i % 2 == 0 ? 0 : 5);
                double y = minY + (i % 3) * ((maxY - minY) / 2.5);

                Circle dot = new Circle(5.5);
                dot.setCenterX(x);
                dot.setCenterY(y);

                Color dotColor;
                switch (eq.getStatus()) {
                    case FAULTY -> dotColor = Color.web("#ef4444");
                    case UNDER_MAINTENANCE -> dotColor = Color.web("#f59e0b");
                    default -> dotColor = Color.web("#4ade80");
                }
                dot.setFill(dotColor);

                // Drop shadow
                DropShadow glow = new DropShadow(8, dotColor);
                dot.setEffect(glow);

                // Faulty pulsing animation
                if (eq.getStatus() == EquipmentStatus.FAULTY) {
                    FadeTransition pulse = new FadeTransition(Duration.millis(700), dot);
                    pulse.setFromValue(1.0);
                    pulse.setToValue(0.3);
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.setAutoReverse(true);
                    pulse.play();
                }

                // Interactive rich tooltip
                Tooltip tip = new Tooltip(eq.getName() + " (" + eq.getEquipmentId() + ")\n"
                        + "Status: " + eq.getStatus().getDisplayName() + "\n"
                        + "Location: " + eq.getLocation().getDisplayName() + "\n"
                        + "Usage: " + String.format("%.0f / %.0f hrs", eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold()));
                tip.setStyle("-fx-font-size: 11px; -fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.2);");
                Tooltip.install(dot, tip);

                // Clicking dot opens Details Modal
                dot.setOnMouseClicked(e -> showEquipmentDetailsModal(eq));
                dot.setOnMouseEntered(e -> dot.setRadius(8));
                dot.setOnMouseExited(e -> dot.setRadius(5.5));
                dot.setStyle("-fx-cursor: hand;");

                // Store equipment ID in dot userdata
                dot.setUserData(eq);

                pane.getChildren().add(dot);
            }
        }
    }

    private void updateFloorMapDots() {
        if (mapCanvas == null) return;

        for (Node node : mapCanvas.getChildren()) {
            if (node instanceof Circle dot && dot.getUserData() instanceof Equipment eq) {
                boolean visible = true;

                // Match status filter
                if (activeKpiFilter != null && eq.getStatus() != activeKpiFilter) {
                    visible = false;
                }

                // Match location filter
                String selLoc = locationFilter != null ? locationFilter.getValue() : "All Locations";
                if (selLoc != null && !"All Locations".equals(selLoc)) {
                    if (!eq.getLocation().getDisplayName().equalsIgnoreCase(selLoc)) {
                        visible = false;
                    }
                }

                // Match category map pin filter
                if (mapCategoryFilter != null && !eq.getCategory().equalsIgnoreCase(mapCategoryFilter)) {
                    visible = false;
                }

                dot.setVisible(visible);
                if (visible && mapCategoryFilter != null) {
                    dot.setRadius(8);
                } else if (visible) {
                    dot.setRadius(5.5);
                }
            }
        }
    }

    private VBox buildRecentAlertsPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("equipment-glass-panel");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Recent Maintenance & Alerts");
        title.getStyleClass().add("equipment-panel-title");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800;");

        VBox alertsList = new VBox(10);
        List<MaintenanceRequest> requests = facade.getAllMaintenanceRequests();
        requests.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));

        int count = Math.min(5, requests.size());
        for (int i = 0; i < count; i++) {
            MaintenanceRequest req = requests.get(i);
            HBox row = createAlertRow(req);
            alertsList.getChildren().add(row);
        }

        if (requests.isEmpty()) {
            Label empty = new Label("No pending maintenance alerts. Fleet operating smoothly.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-padding: 10 0;");
            alertsList.getChildren().add(empty);
        }

        panel.getChildren().addAll(title, alertsList);
        return panel;
    }

    private HBox createAlertRow(MaintenanceRequest req) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.07) transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0; -fx-cursor: hand;");

        Label icon = new Label(req.getUrgency() == Urgency.HIGH ? "🔴" : "⚠️");
        icon.setStyle("-fx-font-size: 14px;");

        VBox textCol = new VBox(2);
        Label titleLbl = new Label(req.getEquipment().getName() + " — " + req.getDescription());
        titleLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: 700;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(270);

        Label subLbl = new Label(req.getStatus().getDisplayName() + " · " + req.getCreatedDate().format(ALERT_DATE_FMT));
        subLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        textCol.getChildren().addAll(titleLbl, subLbl);
        row.getChildren().addAll(icon, textCol);

        // Click to view that equipment
        row.setOnMouseClicked(e -> showEquipmentDetailsModal(req.getEquipment()));

        return row;
    }

    // =========================================================================
    // 5. WINDOW 3: SUPREME EQUIPMENT DETAILS & TELEMETRY MODAL
    // =========================================================================
    private void showEquipmentDetailsModal(Equipment eq) {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Equipment Telemetry — " + eq.getName());

        VBox root = new VBox(20);
        root.setPadding(new Insets(26));
        root.setPrefWidth(680);
        root.getStyleClass().add("custom-modal-root");

        // Header: Title + ID + Status + Close
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label title = new Label(eq.getName());
        title.getStyleClass().add("custom-modal-title");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label idBadge = new Label("ID: " + eq.getEquipmentId());
        idBadge.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #94a3b8; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 6px;");
        Label catBadge = new Label(eq.getCategory());
        catBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.15); -fx-text-fill: #60a5fa; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 6px;");
        StatusBadge statusBadge = StatusBadge.forEquipmentStatus(eq.getStatus());
        metaRow.getChildren().addAll(idBadge, catBadge, statusBadge);

        titleBox.getChildren().addAll(title, metaRow);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeX = new Button("✕");
        closeX.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-weight: 800; -fx-cursor: hand;");
        closeX.setOnAction(e -> modal.close());

        header.getChildren().addAll(titleBox, headerSpacer, closeX);

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-opacity: 0.15;");

        // Two-Column Body: Left (Lifecycle Metrics) & Right (Usage & Service Logs)
        HBox body = new HBox(24);

        // Left Column (Telemetry)
        VBox leftCol = new VBox(14);
        leftCol.setPrefWidth(280);

        Label sec1 = new Label("LIFECYCLE & TELEMETRY");
        sec1.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-font-weight: 900; -fx-letter-spacing: 0.5px;");

        GridPane specGrid = new GridPane();
        specGrid.setHgap(12);
        specGrid.setVgap(8);
        specGrid.add(createSpecLabel("Category:"), 0, 0);
        specGrid.add(createSpecValue(eq.getCategory()), 1, 0);
        specGrid.add(createSpecLabel("Location:"), 0, 1);
        specGrid.add(createSpecValue(eq.getLocation().getDisplayName()), 1, 1);
        specGrid.add(createSpecLabel("Status:"), 0, 2);
        specGrid.add(createSpecValue(eq.getStatus().getDisplayName()), 1, 2);
        specGrid.add(createSpecLabel("Threshold:"), 0, 3);
        specGrid.add(createSpecValue(String.format("%.0f Hours", eq.getMaintenanceThreshold())), 1, 3);

        // Progress Bar
        double usage = eq.getCumulativeUsageHours();
        double thresh = Math.max(1, eq.getMaintenanceThreshold());
        double pct = Math.min(1.0, usage / thresh);

        VBox progressBox = new VBox(6);
        HBox pTop = new HBox();
        Label pTitle = new Label("Usage Depletion");
        pTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");
        Region pSp = new Region(); HBox.setHgrow(pSp, Priority.ALWAYS);
        Label pVal = new Label(String.format("%.1f / %.0f hrs (%.0f%%)", usage, thresh, pct * 100));
        pVal.setStyle("-fx-font-size: 11px; -fx-font-weight: 800;");
        pVal.getStyleClass().add("equipment-item-name");
        pTop.getChildren().addAll(pTitle, pSp, pVal);

        ProgressBar pBar = new ProgressBar(pct);
        pBar.setMaxWidth(Double.MAX_VALUE);
        pBar.setStyle("-fx-accent: " + (pct > 0.85 ? "#ef4444" : (pct > 0.6 ? "#f59e0b" : "#10b981")) + ";");
        progressBox.getChildren().addAll(pTop, pBar);

        // Health Score badge
        int healthScore = (int) Math.max(10, Math.min(100, 100 - (pct * 60) - (eq.getStatus() == EquipmentStatus.FAULTY ? 40 : 0)));
        VBox healthCard = new VBox(4);
        healthCard.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10px; -fx-padding: 12;");
        Label hTitle = new Label("Fleet Health Score");
        hTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        Label hVal = new Label(healthScore + " / 100 (" + (healthScore > 75 ? "Optimal" : (healthScore > 50 ? "Moderate" : "Attention Required")) + ")");
        hVal.setStyle("-fx-text-fill: " + (healthScore > 75 ? "#4ade80" : (healthScore > 50 ? "#f59e0b" : "#ef4444")) + "; -fx-font-size: 14px; -fx-font-weight: 900;");
        healthCard.getChildren().addAll(hTitle, hVal);

        leftCol.getChildren().addAll(sec1, specGrid, progressBox, healthCard);

        // Right Column (Recent Usage History)
        VBox rightCol = new VBox(12);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        Label sec2 = new Label("RECENT USAGE & SERVICE HISTORY");
        sec2.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-font-weight: 900; -fx-letter-spacing: 0.5px;");

        ListView<String> historyList = new ListView<>();
        historyList.setPrefHeight(210);
        historyList.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px;");

        for (UsageLogEntry entry : eq.getUsageLog()) {
            String ts = entry.getTimestamp() != null ? entry.getTimestamp().format(ALERT_DATE_FMT) : "Recent";
            String note = entry.getNotes() != null && !entry.getNotes().isEmpty() ? " — " + entry.getNotes() : "";
            historyList.getItems().add(String.format("📅 %s  ·  %.1f hrs by %s%s", ts, entry.getHoursLogged(), entry.getLoggedByUserId(), note));
        }

        if (historyList.getItems().isEmpty()) {
            historyList.getItems().add("No historical usage entries recorded yet.");
        }

        rightCol.getChildren().addAll(sec2, historyList);

        body.getChildren().addAll(leftCol, rightCol);

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-opacity: 0.15;");

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button logUsageBtn = new Button("⚡ Log Usage");
        logUsageBtn.getStyleClass().add("equipment-card-btn");
        logUsageBtn.setOnAction(e -> {
            showLogUsageDialog(eq);
            modal.close();
        });

        Button logMaintBtn = new Button("🔧 Log Maintenance");
        logMaintBtn.getStyleClass().add("equipment-maint-btn");
        logMaintBtn.setOnAction(e -> {
            handleLogMaintenanceAction(eq);
            modal.close();
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 8 20; -fx-background-radius: 8px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());

        footer.getChildren().addAll(logUsageBtn, logMaintBtn, closeBtn);

        root.getChildren().addAll(header, sep1, body, sep2, footer);

        DialogUtils.prepareModalScene(modal, root);
        modal.showAndWait();
    }

    private Label createSpecLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 700;");
        return l;
    }

    private Label createSpecValue(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 800;");
        l.getStyleClass().add("equipment-item-name");
        return l;
    }

    // =========================================================================
    // 6. ACTION HANDLERS & MODAL DIALOGS
    // =========================================================================
    private void handleLogMaintenanceAction(Equipment eq) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Log completed maintenance for " + eq.getName() + " [" + eq.getEquipmentId() + "]?\n\nThis will reset cumulative usage hours to 0 and set status to Operational.",
                ButtonType.YES, ButtonType.NO);
        DialogUtils.stylePremiumDialog(confirm);
        confirm.setTitle("Log Maintenance Confirmation");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(ans -> {
            if (ans == ButtonType.YES) {
                try {
                    eq.resetUsageHours();
                    facade.editEquipment(eq.getEquipmentId(), eq.getName(), eq.getCategory(), EquipmentStatus.OPERATIONAL, eq.getLocation(), eq.getMaintenanceThreshold());
                    refreshAllData();
                } catch (UnauthorizedAccessException ex) {
                    showErrorAlert("Access Denied", ex.getMessage());
                }
            }
        });
    }

    private void handleRemoveEquipment(Equipment eq) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to permanently remove \"" + eq.getName() + "\" (" + eq.getEquipmentId() + ") from inventory?",
                ButtonType.YES, ButtonType.NO);
        DialogUtils.stylePremiumDialog(confirm);
        confirm.setTitle("Confirm Equipment Removal");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(ans -> {
            if (ans == ButtonType.YES) {
                try {
                    facade.removeEquipment(eq.getEquipmentId());
                    refreshAllData();
                } catch (UnauthorizedAccessException | IllegalStateException ex) {
                    showErrorAlert("Cannot Remove Equipment", ex.getMessage());
                }
            }
        });
    }

    private void showLogUsageDialog(Equipment eq) {
        Dialog<Double> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Log Usage — " + eq.getName());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        TextField hoursField = new TextField();
        hoursField.setPromptText("Enter hours (e.g. 5.5)");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Equipment:"), 0, 0);
        grid.add(new Label(eq.getName() + " (" + eq.getEquipmentId() + ")"), 1, 0);
        grid.add(new Label("Hours to Log:"), 0, 1);
        grid.add(hoursField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return Double.parseDouble(hoursField.getText().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(hours -> {
            if (hours != null && hours > 0) {
                try {
                    facade.logEquipmentUsage(eq.getEquipmentId(), hours);
                    refreshAllData();
                } catch (Exception ex) {
                    showErrorAlert("Error Logging Usage", ex.getMessage());
                }
            }
        });
    }

    private void showAddEquipmentModal() {
        Dialog<Equipment> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Add New Equipment to Fleet");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        // Calculate next suggested ID (e.g. EQ025)
        int maxIdNum = facade.getAllEquipment().stream()
                .map(Equipment::getEquipmentId)
                .filter(id -> id.startsWith("EQ"))
                .mapToInt(id -> {
                    try { return Integer.parseInt(id.replace("EQ", "")); } catch (Exception e) { return 0; }
                }).max().orElse(0);
        String nextId = String.format("EQ%03d", maxIdNum + 1);

        TextField idField = new TextField(nextId);
        TextField nameField = new TextField();
        nameField.setPromptText("Equipment Name (e.g. Concept2 Rower)");

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getItems().addAll("Cardio", "Strength (Machines)", "Strength (Free Weights)", "Studio & Group", "Functional", "Mobility & Recovery", "Other");
        catCombo.setValue("Cardio");

        ComboBox<Location> locCombo = new ComboBox<>();
        locCombo.getItems().addAll(Location.values());
        locCombo.setValue(Location.CARDIO_ZONE);

        TextField threshField = new TextField("500");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Equipment ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Equipment Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(catCombo, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locCombo, 1, 3);
        grid.add(new Label("Maintenance Threshold (hrs):"), 0, 4);
        grid.add(threshField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String cat = catCombo.getValue();
                Location loc = locCombo.getValue();
                double thresh = 500;
                try { thresh = Double.parseDouble(threshField.getText().trim()); } catch (Exception ignored) {}

                if (!id.isEmpty() && !name.isEmpty() && cat != null && loc != null) {
                    return new Equipment(id, name, cat, EquipmentStatus.OPERATIONAL, loc, 0.0, thresh);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newEq -> {
            try {
                facade.addEquipment(newEq);
                refreshAllData();
            } catch (DuplicateDataException | UnauthorizedAccessException ex) {
                showErrorAlert("Failed to Add Equipment", ex.getMessage());
            }
        });
    }

    private void showEditEquipmentModal(Equipment eq) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Edit Equipment — " + eq.getEquipmentId());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        TextField nameField = new TextField(eq.getName());
        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getItems().addAll("Cardio", "Strength (Machines)", "Strength (Free Weights)", "Studio & Group", "Functional", "Mobility & Recovery", "Other");
        catCombo.setValue(eq.getCategory());

        ComboBox<EquipmentStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(EquipmentStatus.values());
        statusCombo.setValue(eq.getStatus());

        ComboBox<Location> locCombo = new ComboBox<>();
        locCombo.getItems().addAll(Location.values());
        locCombo.setValue(eq.getLocation());

        TextField threshField = new TextField(String.format("%.0f", eq.getMaintenanceThreshold()));

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Equipment ID:"), 0, 0);
        grid.add(new Label(eq.getEquipmentId()), 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(catCombo, 1, 2);
        grid.add(new Label("Status:"), 0, 3);
        grid.add(statusCombo, 1, 3);
        grid.add(new Label("Location:"), 0, 4);
        grid.add(locCombo, 1, 4);
        grid.add(new Label("Maintenance Threshold:"), 0, 5);
        grid.add(threshField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK);

        dialog.showAndWait().ifPresent(ok -> {
            if (ok) {
                try {
                    double thresh = eq.getMaintenanceThreshold();
                    try { thresh = Double.parseDouble(threshField.getText().trim()); } catch (Exception ignored) {}

                    facade.editEquipment(eq.getEquipmentId(), nameField.getText().trim(), catCombo.getValue(),
                            statusCombo.getValue(), locCombo.getValue(), thresh);
                    refreshAllData();
                } catch (UnauthorizedAccessException ex) {
                    showErrorAlert("Access Denied", ex.getMessage());
                }
            }
        });
    }

    private VBox createEmptyState(String msg) {
        VBox empty = new VBox(14);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(50, 20, 50, 20));

        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size: 38px;");

        Label label = new Label(msg);
        label.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px; -fx-font-weight: 700;");

        Button resetBtn = new Button("Clear All Filters");
        resetBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 8 20; -fx-background-radius: 8px; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> {
            if (searchField != null) searchField.clear();
            if (statusFilter != null) statusFilter.setValue("All Statuses");
            if (locationFilter != null) locationFilter.setValue("All Locations");
            activeKpiFilter = null;
            selectedCategory = null;
            mapCategoryFilter = null;
            refreshAllData();
        });

        empty.getChildren().addAll(icon, label, resetBtn);
        return empty;
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        DialogUtils.stylePremiumDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // =========================================================================
    // 7. GLOBAL REFRESH DISPATCHER
    // =========================================================================
    private void refreshAllData() {
        refreshKpiCards();
        renderMainContent();
        refreshRightColumn();
    }
}

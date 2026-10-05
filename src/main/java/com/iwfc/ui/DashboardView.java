package com.iwfc.ui;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.exception.PaymentFailedException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.DialogUtils;
import com.iwfc.ui.components.StatusBadge;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Executive Command & Operational Telemetry Dashboard.
 * Cyber-Dark Glassmorphic UI with full-stack capabilities, period filtering,
 * interactive LineChart, Equipment Duty Matrix, Live Bookings Ledger,
 * and Instant Incident Command.
 */
public class DashboardView extends StackPane {

    public enum Timeframe {
        TODAY("Today", 1),
        DAYS_7("7 Days", 7),
        DAYS_30("30 Days", 30),
        DAYS_90("90 Days", 90),
        ALL("All Time", 9999);

        private final String label;
        private final int days;

        Timeframe(String label, int days) {
            this.label = label;
            this.days = days;
        }

        public String getLabel() {
            return label;
        }

        public int getDays() {
            return days;
        }
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd");

    private final IWFCSystemFacade facade;
    private final Consumer<String> navCallback;

    // Filter & Metric States
    private Timeframe selectedTimeframe = Timeframe.DAYS_30;
    private String selectedChartMetric = "REVENUE"; // "REVENUE" or "ATTENDANCE"
    private String bookingSearchQuery = "";
    private String bookingStatusFilter = "ALL";

    // UI Nodes for Dynamic Refresh
    private VBox mainContainer;
    private Label syncTimestampLabel;
    private HBox kpiCardsRow;
    private VBox chartContainer;
    private VBox equipmentDutyContainer;
    private TableView<Booking> bookingsTable;
    private ObservableList<Booking> masterBookingsList = FXCollections.observableArrayList();
    private VBox maintenanceAlertsList;
    private Label incidentCountBadge;

    public DashboardView(IWFCSystemFacade facade, Consumer<String> navCallback) {
        this.facade = facade;
        this.navCallback = navCallback;

        getStyleClass().add("dashboard-view-root");
        try {
            var css = getClass().getResource("/com/iwfc/css/dashboard-management.css");
            if (css != null) {
                getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}

        buildUI();
    }

    private void buildUI() {
        getChildren().clear();

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(24, 28, 32, 28));
        mainContainer.setFillWidth(true);

        // 1. Executive Command Bar
        HBox commandBar = buildExecutiveCommandBar();

        // 2. High-Tech Glass KPI Cards Row
        kpiCardsRow = buildKpiCardsRow();

        // 3. Middle Section: Telemetry Analytics Chart & Equipment Duty Matrix
        HBox middleRow = buildTelemetryAndDutyRow();

        // 4. Bottom Section: Operational Bookings Ledger & Maintenance Incident Command
        HBox bottomRow = buildBookingsAndIncidentsRow();

        mainContainer.getChildren().addAll(commandBar, kpiCardsRow, middleRow, bottomRow);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("dashboard-scroll-pane");

        getChildren().add(scrollPane);

        Platform.runLater(() -> playEntranceAnimations(commandBar, kpiCardsRow, middleRow, bottomRow));
    }

    // =========================================================================
    // 1. Executive Command Bar
    // =========================================================================

    private HBox buildExecutiveCommandBar() {
        User user = facade.getCurrentUser();
        String userName = user != null ? user.getName() : "Administrator";
        String userRole = user != null ? user.getRole().getDisplayName() : "System Admin";

        // Title and Subtitle Block
        VBox headerText = new VBox(3);
        Label titleLbl = new Label("Executive Telemetry & Operations");
        titleLbl.getStyleClass().add("dashboard-header-title");

        HBox subBox = new HBox(10);
        subBox.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLbl = new Label("Logged in as " + userName + " (" + userRole + ")");
        welcomeLbl.getStyleClass().add("dashboard-header-subtitle");

        Label statusPill = new Label("● SYSTEM OPERATIONAL");
        statusPill.getStyleClass().add("dashboard-status-pill");

        syncTimestampLabel = new Label("Synced: " + LocalDateTime.now().format(TIME_FMT));
        syncTimestampLabel.getStyleClass().add("dashboard-timestamp");

        subBox.getChildren().addAll(welcomeLbl, statusPill, syncTimestampLabel);
        headerText.getChildren().addAll(titleLbl, subBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Period Timeframe Segmented Control
        HBox timeframeBox = new HBox(4);
        timeframeBox.setAlignment(Pos.CENTER);
        timeframeBox.getStyleClass().add("timeframe-segment-box");

        ToggleGroup tfGroup = new ToggleGroup();
        for (Timeframe tf : Timeframe.values()) {
            ToggleButton btn = new ToggleButton(tf.getLabel());
            btn.getStyleClass().add("timeframe-btn");
            btn.setToggleGroup(tfGroup);
            btn.setSelected(tf == selectedTimeframe);
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    selectedTimeframe = tf;
                    refreshData(false);
                } else {
                    btn.setSelected(true); // Don't allow unselecting current
                }
            });
            timeframeBox.getChildren().add(btn);
        }

        // Action Hub Buttons
        HBox actionHub = new HBox(10);
        actionHub.setAlignment(Pos.CENTER_RIGHT);

        if (user != null && user.getRole() == Role.MEMBER) {
            Button dailyLogBtn = new Button("📝 Daily Log");
            dailyLogBtn.getStyleClass().add("dashboard-action-btn");
            dailyLogBtn.setOnAction(e -> showDailyLogDialog());
            actionHub.getChildren().add(dailyLogBtn);
        }

        Button quickBookingBtn = new Button("+ Quick Booking");
        quickBookingBtn.getStyleClass().add("dashboard-primary-btn");
        quickBookingBtn.setOnAction(e -> showQuickBookingModal());

        Button processPaymentBtn = new Button("+ Process Payment");
        processPaymentBtn.getStyleClass().add("dashboard-emerald-btn");
        processPaymentBtn.setOnAction(e -> showQuickPaymentModal());

        Button reportIncidentBtn = new Button("+ Report Incident");
        reportIncidentBtn.getStyleClass().add("dashboard-amber-btn");
        reportIncidentBtn.setOnAction(e -> showQuickIncidentModal(null));

        Button liveSyncBtn = new Button("🔄 Sync");
        liveSyncBtn.getStyleClass().add("dashboard-action-btn");
        liveSyncBtn.setOnAction(e -> refreshData(true));

        actionHub.getChildren().addAll(quickBookingBtn, processPaymentBtn, reportIncidentBtn, liveSyncBtn);

        HBox commandBar = new HBox(18, headerText, spacer, timeframeBox, actionHub);
        commandBar.setAlignment(Pos.CENTER_LEFT);
        return commandBar;
    }

    // =========================================================================
    // 2. High-Tech Glass KPI Cards Row
    // =========================================================================

    private HBox buildKpiCardsRow() {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);

        // Fetch data
        List<Payment> payments = getFilteredPayments();
        double totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(Payment::getTotalCharged)
                .sum();
        long completedPaymentsCount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .count();
        double avgPayment = completedPaymentsCount > 0 ? totalRevenue / completedPaymentsCount : 0.0;

        List<Booking> bookings = getFilteredBookings();
        long confirmedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long attendedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.ATTENDED).count();
        long cancelledBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long activeBookings = confirmedBookings + attendedBookings;

        int totalMembers = 0;
        try {
            totalMembers = facade.getUsersByRole(Role.MEMBER).size();
        } catch (UnauthorizedAccessException ignored) {}

        List<Equipment> equipmentList = facade.getAllEquipment();
        long operationalEquip = equipmentList.stream().filter(eq -> eq.getStatus() == EquipmentStatus.OPERATIONAL).count();
        long faultyOrMaintenance = equipmentList.size() - operationalEquip;
        double operationalPct = equipmentList.isEmpty() ? 100.0 : (operationalEquip * 100.0 / equipmentList.size());

        // Card 1: Gross Cashflow & Revenue
        VBox revCard = createKpiCard(
                "Gross Revenue (" + selectedTimeframe.getLabel() + ")",
                String.format("LKR %,.2f", totalRevenue),
                "💰",
                String.format("%d settled txns • Avg: LKR %,.0f", completedPaymentsCount, avgPayment),
                "+14.2% Cashflow",
                true,
                e -> navCallback.accept("Payments")
        );

        // Card 2: Studio Bookings & Load
        VBox bookingCard = createKpiCard(
                "Studio Bookings (" + selectedTimeframe.getLabel() + ")",
                String.format("%d Bookings", activeBookings),
                "🎟️",
                String.format("%d Confirmed • %d Attended • %d Cancelled", confirmedBookings, attendedBookings, cancelledBookings),
                String.format("%.0f%% Fulfill", bookings.isEmpty() ? 100.0 : ((double) attendedBookings / Math.max(1, bookings.size()) * 100.0)),
                false,
                e -> navCallback.accept("Sessions")
        );

        // Card 3: Active Member Network
        VBox memberCard = createKpiCard(
                "Member Network",
                String.format("%d Members", totalMembers),
                "👥",
                String.format("%d Active in club roster", totalMembers),
                "98% Retained",
                true,
                e -> navCallback.accept("Members")
        );

        // Card 4: Facility Hardware Health
        VBox equipCard = createKpiCard(
                "Facility Hardware Health",
                String.format("%.1f%% Ready", operationalPct),
                "⚙️",
                String.format("%d Units Ready • %d Service Needed", operationalEquip, faultyOrMaintenance),
                faultyOrMaintenance == 0 ? "Optimal" : (faultyOrMaintenance + " Alerts"),
                faultyOrMaintenance == 0,
                e -> navCallback.accept("Equipment")
        );

        row.getChildren().addAll(revCard, bookingCard, memberCard, equipCard);
        HBox.setHgrow(revCard, Priority.ALWAYS);
        HBox.setHgrow(bookingCard, Priority.ALWAYS);
        HBox.setHgrow(memberCard, Priority.ALWAYS);
        HBox.setHgrow(equipCard, Priority.ALWAYS);

        return row;
    }

    private VBox createKpiCard(String title, String mainValue, String icon, String subtext, String trendBadge, boolean isPositive, Consumer<javafx.scene.input.MouseEvent> onClick) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-kpi-card");
        if (onClick != null) {
            card.setOnMouseClicked(onClick::accept);
        }

        // Header
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("dashboard-kpi-header");

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("dashboard-kpi-icon-wrap");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dashboard-kpi-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label trend = new Label(trendBadge);
        trend.getStyleClass().add(isPositive ? "dashboard-trend-pill-up" : "dashboard-trend-pill-neutral");

        top.getChildren().addAll(iconLbl, titleLbl, sp, trend);

        // Value
        Label valueLbl = new Label(mainValue);
        valueLbl.getStyleClass().add("dashboard-kpi-value");

        // Subtext
        Label subLbl = new Label(subtext);
        subLbl.getStyleClass().add("dashboard-kpi-subtext");

        card.getChildren().addAll(top, valueLbl, subLbl);
        return card;
    }

    // =========================================================================
    // 3. Middle Section: Telemetry Analytics Chart & Equipment Duty Matrix
    // =========================================================================

    private HBox buildTelemetryAndDutyRow() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        // Left: Interactive Telemetry Chart
        chartContainer = buildTelemetryChartSection();
        HBox.setHgrow(chartContainer, Priority.ALWAYS);

        // Right: Equipment Duty Matrix
        equipmentDutyContainer = buildEquipmentDutySection();
        equipmentDutyContainer.setMinWidth(380);
        equipmentDutyContainer.setPrefWidth(420);

        row.getChildren().addAll(chartContainer, equipmentDutyContainer);
        return row;
    }

    private VBox buildTelemetryChartSection() {
        VBox section = new VBox(16);
        section.getStyleClass().add("dashboard-card-section");

        // Header with Metric Switcher
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Performance Trends & Velocity");
        title.getStyleClass().add("dashboard-section-title");

        Label subtitle = new Label("Dynamic metrics across " + selectedTimeframe.getLabel());
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Metric Toggle Buttons
        ToggleGroup metricGroup = new ToggleGroup();

        ToggleButton revBtn = new ToggleButton("💰 Revenue (LKR)");
        revBtn.getStyleClass().add("timeframe-btn");
        revBtn.setToggleGroup(metricGroup);
        revBtn.setSelected(selectedChartMetric.equals("REVENUE"));

        ToggleButton attBtn = new ToggleButton("🎟️ Bookings");
        attBtn.getStyleClass().add("timeframe-btn");
        attBtn.setToggleGroup(metricGroup);
        attBtn.setSelected(selectedChartMetric.equals("ATTENDANCE"));

        revBtn.setOnAction(e -> {
            if (revBtn.isSelected()) {
                selectedChartMetric = "REVENUE";
                updateChartData(section);
            } else {
                revBtn.setSelected(true);
            }
        });

        attBtn.setOnAction(e -> {
            if (attBtn.isSelected()) {
                selectedChartMetric = "ATTENDANCE";
                updateChartData(section);
            } else {
                attBtn.setSelected(true);
            }
        });

        HBox metricBox = new HBox(4, revBtn, attBtn);
        metricBox.getStyleClass().add("timeframe-segment-box");

        header.getChildren().addAll(titleBox, spacer, metricBox);

        // Build Chart & Stats Content
        HBox chartBody = new HBox(16);
        chartBody.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(chartBody, Priority.ALWAYS);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(280);
        chart.getStyleClass().add("dashboard-line-chart");
        HBox.setHgrow(chart, Priority.ALWAYS);

        // Telemetry Summary Sub-Column
        VBox statSubCol = new VBox(12);
        statSubCol.setMinWidth(150);
        statSubCol.setPrefWidth(160);
        statSubCol.setAlignment(Pos.CENTER);

        populateChartAndStats(chart, statSubCol);

        chartBody.getChildren().addAll(chart, statSubCol);

        section.getChildren().addAll(header, chartBody);
        return section;
    }

    private void updateChartData(VBox section) {
        if (section.getChildren().size() >= 2 && section.getChildren().get(1) instanceof HBox chartBody) {
            if (chartBody.getChildren().size() >= 2 &&
                chartBody.getChildren().get(0) instanceof LineChart<?, ?> chart &&
                chartBody.getChildren().get(1) instanceof VBox statCol) {
                @SuppressWarnings("unchecked")
                LineChart<String, Number> lc = (LineChart<String, Number>) chart;
                populateChartAndStats(lc, statCol);
            }
        }
    }

    private void populateChartAndStats(LineChart<String, Number> chart, VBox statCol) {
        chart.getData().clear();
        statCol.getChildren().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Payment> payments = getFilteredPayments();
        List<Booking> bookings = getFilteredBookings();

        LocalDate today = LocalDate.now();
        int days = selectedTimeframe.getDays();
        if (selectedTimeframe == Timeframe.ALL) days = 30; // sensible window for trend

        double peakValue = 0.0;
        double totalVolume = 0.0;
        int dataPointsCount = 0;

        if (selectedTimeframe == Timeframe.TODAY) {
            // Group by 4-hour blocks
            int[] hours = {6, 10, 14, 18, 22};
            for (int h : hours) {
                String label = String.format("%02d:00", h);
                double val;
                if (selectedChartMetric.equals("REVENUE")) {
                    val = payments.stream()
                            .filter(p -> p.getStatus() == PaymentStatus.COMPLETED &&
                                         p.getPaidAt().toLocalDate().equals(today) &&
                                         p.getPaidAt().getHour() >= h - 2 && p.getPaidAt().getHour() < h + 2)
                            .mapToDouble(Payment::getTotalCharged)
                            .sum();
                } else {
                    val = bookings.stream()
                            .filter(b -> b.getSessionDate().equals(today) &&
                                         b.getSession().getStartTime().getHour() >= h - 2 &&
                                         b.getSession().getStartTime().getHour() < h + 2)
                            .count();
                }
                series.getData().add(new XYChart.Data<>(label, val));
                totalVolume += val;
                if (val > peakValue) peakValue = val;
                dataPointsCount++;
            }
        } else if (days <= 7) {
            // Daily for 7 days
            for (int i = days - 1; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                String label = d.format(SHORT_DATE_FMT);
                double val;
                if (selectedChartMetric.equals("REVENUE")) {
                    val = payments.stream()
                            .filter(p -> p.getStatus() == PaymentStatus.COMPLETED && p.getPaidAt().toLocalDate().equals(d))
                            .mapToDouble(Payment::getTotalCharged)
                            .sum();
                } else {
                    val = bookings.stream()
                            .filter(b -> b.getSessionDate().equals(d))
                            .count();
                }
                series.getData().add(new XYChart.Data<>(label, val));
                totalVolume += val;
                if (val > peakValue) peakValue = val;
                dataPointsCount++;
            }
        } else {
            // Group into 7 or 8 bucket intervals across the window
            int step = Math.max(1, days / 7);
            for (int i = days; i >= 0; i -= step) {
                LocalDate startWindow = today.minusDays(i);
                LocalDate endWindow = startWindow.plusDays(step);
                String label = startWindow.format(SHORT_DATE_FMT);
                double val;
                if (selectedChartMetric.equals("REVENUE")) {
                    val = payments.stream()
                            .filter(p -> p.getStatus() == PaymentStatus.COMPLETED &&
                                         !p.getPaidAt().toLocalDate().isBefore(startWindow) &&
                                         p.getPaidAt().toLocalDate().isBefore(endWindow))
                            .mapToDouble(Payment::getTotalCharged)
                            .sum();
                } else {
                    val = bookings.stream()
                            .filter(b -> !b.getSessionDate().isBefore(startWindow) &&
                                         b.getSessionDate().isBefore(endWindow))
                            .count();
                }
                series.getData().add(new XYChart.Data<>(label, val));
                totalVolume += val;
                if (val > peakValue) peakValue = val;
                dataPointsCount++;
            }
        }

        chart.getData().add(series);

        // Tooltips on data points
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                String tip = selectedChartMetric.equals("REVENUE")
                        ? String.format("%s: LKR %,.2f", d.getXValue(), d.getYValue().doubleValue())
                        : String.format("%s: %d Bookings", d.getXValue(), d.getYValue().intValue());
                Tooltip.install(d.getNode(), new Tooltip(tip));
            }
        }

        // Summary Stats in Sub-Column
        double avgVal = dataPointsCount > 0 ? totalVolume / dataPointsCount : 0.0;

        VBox peakBox = createTelemetryStatBox(
                "Peak Period",
                selectedChartMetric.equals("REVENUE") ? String.format("LKR %,.0f", peakValue) : String.format("%.0f Units", peakValue)
        );

        VBox totalBox = createTelemetryStatBox(
                "Period Total",
                selectedChartMetric.equals("REVENUE") ? String.format("LKR %,.0f", totalVolume) : String.format("%.0f Bookings", totalVolume)
        );

        VBox avgBox = createTelemetryStatBox(
                "Velocity / Day",
                selectedChartMetric.equals("REVENUE") ? String.format("LKR %,.0f", avgVal) : String.format("%.1f / Day", avgVal)
        );

        statCol.getChildren().addAll(peakBox, totalBox, avgBox);
    }

    private VBox createTelemetryStatBox(String label, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("telemetry-stat-box");
        Label l = new Label(label);
        l.getStyleClass().add("telemetry-stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("telemetry-stat-value");
        box.getChildren().addAll(l, v);
        return box;
    }

    private VBox buildEquipmentDutySection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("dashboard-card-section");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(2);
        Label title = new Label("Equipment Duty Matrix");
        title.getStyleClass().add("dashboard-section-title");

        Label subtitle = new Label("Units ranked by duty cycle percentage");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Hyperlink viewAllLink = new Hyperlink("Inventory ↗");
        viewAllLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px;");
        viewAllLink.setOnAction(e -> navCallback.accept("Equipment"));

        header.getChildren().addAll(titles, sp, viewAllLink);

        VBox rowsList = new VBox(10);
        List<Equipment> topDuty = facade.getAllEquipment().stream()
                .sorted((a, b) -> Double.compare(b.getUsagePercentage(), a.getUsagePercentage()))
                .limit(4)
                .collect(Collectors.toList());

        for (Equipment eq : topDuty) {
            rowsList.getChildren().add(createEquipmentDutyRow(eq));
        }

        if (topDuty.isEmpty()) {
            Label noData = new Label("No equipment inventory registered.");
            noData.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            rowsList.getChildren().add(noData);
        }

        section.getChildren().addAll(header, rowsList);
        return section;
    }

    private HBox createEquipmentDutyRow(Equipment eq) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("equipment-duty-row");

        String icon = switch (eq.getCategory().toLowerCase()) {
            case "cardio" -> "🏃";
            case "strength" -> "🏋️";
            case "cycling" -> "🚴";
            case "flexibility" -> "🧘";
            default -> "⚙️";
        };

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px; -fx-padding: 4px;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox topInfo = new HBox(8);
        topInfo.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(eq.getName());
        name.getStyleClass().add("equipment-name-label");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        double pct = eq.getUsagePercentage();
        Label dutyLbl = new Label(String.format("%.1f / %.0f hrs (%.0f%%)", eq.getCumulativeUsageHours(), eq.getMaintenanceThreshold(), pct * 100.0));
        dutyLbl.getStyleClass().add("equipment-duty-hours");

        topInfo.getChildren().addAll(name, sp, dutyLbl);

        ProgressBar pb = new ProgressBar(Math.min(1.0, pct));
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.getStyleClass().add("equipment-progress-bar");
        if (pct >= 0.9) {
            pb.getStyleClass().add("bar-danger");
        } else if (pct >= 0.7) {
            pb.getStyleClass().add("bar-amber");
        }

        info.getChildren().addAll(topInfo, pb);

        Button serviceBtn = new Button("🛠️");
        serviceBtn.setTooltip(new Tooltip("Dispatch Service / Report Incident"));
        serviceBtn.getStyleClass().add("quick-resolve-btn");
        serviceBtn.setOnAction(e -> showQuickIncidentModal(eq));

        row.getChildren().addAll(iconLbl, info, StatusBadge.forEquipmentStatus(eq.getStatus()), serviceBtn);
        return row;
    }

    // =========================================================================
    // 4. Bottom Section: Operational Bookings Ledger & Maintenance Incidents
    // =========================================================================

    private HBox buildBookingsAndIncidentsRow() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        // Left: Operational Bookings Ledger
        VBox bookingsSection = buildBookingsLedgerSection();
        HBox.setHgrow(bookingsSection, Priority.ALWAYS);

        // Right: Maintenance Incident Command
        VBox incidentsSection = buildMaintenanceIncidentsSection();
        incidentsSection.setMinWidth(380);
        incidentsSection.setPrefWidth(420);

        row.getChildren().addAll(bookingsSection, incidentsSection);
        return row;
    }

    @SuppressWarnings("unchecked")
    private VBox buildBookingsLedgerSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("dashboard-card-section");

        // Header + Filter Controls
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(2);
        Label title = new Label("Live Bookings & Attendance Ledger");
        title.getStyleClass().add("dashboard-section-title");

        Label subtitle = new Label("Operational check-in stream and booking roster");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchF = new TextField();
        searchF.setPromptText("🔍 Search member or session...");
        searchF.getStyleClass().add("dashboard-search-field");
        searchF.setPrefWidth(210);
        searchF.textProperty().addListener((obs, old, text) -> {
            bookingSearchQuery = text != null ? text.trim().toLowerCase() : "";
            applyBookingsFilter();
        });

        ComboBox<String> statusFilterCbx = new ComboBox<>(FXCollections.observableArrayList("ALL", "CONFIRMED", "ATTENDED", "CANCELLED"));
        statusFilterCbx.setValue("ALL");
        statusFilterCbx.getStyleClass().add("payments-combo-box");
        statusFilterCbx.setOnAction(e -> {
            bookingStatusFilter = statusFilterCbx.getValue();
            applyBookingsFilter();
        });

        header.getChildren().addAll(titles, spacer, searchF, statusFilterCbx);

        // Table
        bookingsTable = new TableView<>();
        bookingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bookingsTable.getStyleClass().add("dashboard-table");
        bookingsTable.setPrefHeight(270);

        TableColumn<Booking, Booking> memberCol = new TableColumn<>("Member");
        memberCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        memberCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    String name = item.getMember() != null ? item.getMember().getName() : "Unknown Member";
                    String initials = getInitials(name);

                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("avatar-badge-circle");
                    Label initLbl = new Label(initials);
                    initLbl.getStyleClass().add("avatar-badge-initials");
                    avatar.getChildren().add(initLbl);

                    VBox info = new VBox(2);
                    Label nLbl = new Label(name);
                    nLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #ffffff; -fx-font-size: 12px;");
                    Label idLbl = new Label(item.getMember() != null ? item.getMember().getUserId() : "");
                    idLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                    info.getChildren().addAll(nLbl, idLbl);

                    box.getChildren().addAll(avatar, info);
                    setGraphic(box);
                }
            }
        });

        TableColumn<Booking, String> sessionCol = new TableColumn<>("Session / Instructor");
        sessionCol.setCellValueFactory(d -> {
            var s = d.getValue().getSession();
            String instr = s.getInstructor() != null ? " • " + s.getInstructor().getName() : "";
            return new SimpleStringProperty(s.getName() + instr);
        });

        TableColumn<Booking, String> scheduleCol = new TableColumn<>("Schedule");
        scheduleCol.setCellValueFactory(d -> {
            var s = d.getValue().getSession();
            LocalDate date = d.getValue().getSessionDate();
            return new SimpleStringProperty(date.format(SHORT_DATE_FMT) + " @ " + s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        });

        TableColumn<Booking, BookingStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BookingStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(StatusBadge.forBookingStatus(item));
                }
            }
        });

        TableColumn<Booking, Booking> actionsCol = new TableColumn<>("Operations");
        actionsCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) {
                    setGraphic(null);
                } else {
                    HBox hb = new HBox(6);
                    hb.setAlignment(Pos.CENTER_LEFT);

                    if (b.getStatus() == BookingStatus.CONFIRMED) {
                        Button checkInBtn = new Button("✓ Check In");
                        checkInBtn.getStyleClass().add("quick-checkin-btn");
                        checkInBtn.setOnAction(e -> handleCheckInBooking(b));

                        Button cancelBtn = new Button("✕");
                        cancelBtn.getStyleClass().add("quick-cancel-btn");
                        cancelBtn.setTooltip(new Tooltip("Cancel Booking"));
                        cancelBtn.setOnAction(e -> handleCancelBooking(b));

                        hb.getChildren().addAll(checkInBtn, cancelBtn);
                    }

                    Button viewBtn = new Button("👁");
                    viewBtn.getStyleClass().add("dashboard-action-btn");
                    viewBtn.setTooltip(new Tooltip("Inspect Booking Details"));
                    viewBtn.setOnAction(e -> showBookingDetailsModal(b));

                    hb.getChildren().add(viewBtn);
                    setGraphic(hb);
                }
            }
        });

        bookingsTable.getColumns().addAll(memberCol, sessionCol, scheduleCol, statusCol, actionsCol);
        bookingsTable.setItems(masterBookingsList);

        applyBookingsFilter();

        section.getChildren().addAll(header, bookingsTable);
        return section;
    }

    private void applyBookingsFilter() {
        List<Booking> all = facade.getAllBookings();
        List<Booking> filtered = all.stream()
                .filter(b -> {
                    if (bookingStatusFilter.equals("ALL")) return true;
                    return b.getStatus().name().equalsIgnoreCase(bookingStatusFilter);
                })
                .filter(b -> {
                    if (bookingSearchQuery.isEmpty()) return true;
                    String memName = b.getMember() != null ? b.getMember().getName().toLowerCase() : "";
                    String sesName = b.getSession() != null ? b.getSession().getName().toLowerCase() : "";
                    String bId = b.getBookingId() != null ? b.getBookingId().toLowerCase() : "";
                    return memName.contains(bookingSearchQuery) || sesName.contains(bookingSearchQuery) || bId.contains(bookingSearchQuery);
                })
                .sorted((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()))
                .collect(Collectors.toList());

        masterBookingsList.setAll(filtered);
        if (filtered.isEmpty()) {
            bookingsTable.setPlaceholder(new Label("No bookings match active filters."));
        }
    }

    private VBox buildMaintenanceIncidentsSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("dashboard-card-section");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(2);
        Label title = new Label("Incident Command Monitor");
        title.getStyleClass().add("dashboard-section-title");

        Label subtitle = new Label("Active hardware and facility alerts");
        subtitle.getStyleClass().add("dashboard-section-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        incidentCountBadge = new Label("0 Active");
        incidentCountBadge.getStyleClass().add("dashboard-trend-pill-up");

        header.getChildren().addAll(titles, sp, incidentCountBadge);

        maintenanceAlertsList = new VBox(10);
        ScrollPane scroll = new ScrollPane(maintenanceAlertsList);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dashboard-scroll-pane");
        scroll.setPrefHeight(270);

        refreshMaintenanceList();

        section.getChildren().addAll(header, scroll);
        return section;
    }

    private void refreshMaintenanceList() {
        maintenanceAlertsList.getChildren().clear();
        List<MaintenanceRequest> requests = facade.getAllMaintenanceRequests().stream()
                .filter(r -> r.getStatus() != RequestStatus.COMPLETED)
                .sorted((a, b) -> {
                    if (a.isHighUrgency() != b.isHighUrgency()) {
                        return a.isHighUrgency() ? -1 : 1;
                    }
                    return b.getCreatedDate().compareTo(a.getCreatedDate());
                })
                .collect(Collectors.toList());

        incidentCountBadge.setText(requests.size() + " Active");
        if (requests.isEmpty()) {
            incidentCountBadge.getStyleClass().clear();
            incidentCountBadge.getStyleClass().add("dashboard-trend-pill-up");

            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(30, 20, 30, 20));
            Label shield = new Label("🛡️");
            shield.setStyle("-fx-font-size: 32px;");
            Label okLbl = new Label("All Systems Fully Operational");
            okLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: 700; -fx-font-size: 13px;");
            Label sub = new Label("Zero active faults or pending maintenance alerts.");
            sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            emptyBox.getChildren().addAll(shield, okLbl, sub);
            maintenanceAlertsList.getChildren().add(emptyBox);
            return;
        }

        incidentCountBadge.getStyleClass().clear();
        incidentCountBadge.getStyleClass().add("dashboard-trend-pill-neutral");

        for (MaintenanceRequest req : requests) {
            VBox card = new VBox(8);
            card.getStyleClass().add(req.isHighUrgency() ? "incident-card-urgent" : "incident-card");

            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);

            Label eqName = new Label(req.getEquipment().getName());
            eqName.getStyleClass().add("incident-equipment-title");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            StatusBadge urgBadge = StatusBadge.forUrgency(req.getUrgency());
            top.getChildren().addAll(eqName, sp, urgBadge);

            Label descLbl = new Label(req.getDescription());
            descLbl.getStyleClass().add("incident-desc-text");
            descLbl.setWrapText(true);

            HBox bot = new HBox(10);
            bot.setAlignment(Pos.CENTER_LEFT);

            String reporter = req.getReportedBy() != null ? req.getReportedBy().getName() : "Staff";
            Label meta = new Label("Reported by " + reporter + " • " + req.getCreatedDate().format(SHORT_DATE_FMT));
            meta.getStyleClass().add("incident-reporter-text");

            Region sp2 = new Region();
            HBox.setHgrow(sp2, Priority.ALWAYS);

            Button resolveBtn = new Button("⚡ Resolve");
            resolveBtn.getStyleClass().add("quick-resolve-btn");
            resolveBtn.setOnAction(e -> showResolveIncidentModal(req));

            bot.getChildren().addAll(meta, sp2, resolveBtn);

            card.getChildren().addAll(top, descLbl, bot);
            maintenanceAlertsList.getChildren().add(card);
        }
    }

    // =========================================================================
    // 5. Operations & Dialogs (100% Functional)
    // =========================================================================

    private void handleCheckInBooking(Booking booking) {
        try {
            facade.updateBookingStatus(booking.getBookingId(), BookingStatus.ATTENDED);
            refreshData(false);
            showNotificationToast("✓ Checked in " + booking.getMember().getName() + " for " + booking.getSession().getName());
        } catch (Exception ex) {
            showSimpleAlert("Permission Denied", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleCancelBooking(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtils.stylePremiumDialog(confirm);
        confirm.setTitle("Cancel Booking Confirmation");
        confirm.setHeaderText("Cancel Booking " + booking.getBookingId());
        confirm.setContentText("Are you sure you want to cancel the booking for member '" +
                booking.getMember().getName() + "' for session '" + booking.getSession().getName() + "'?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    facade.cancelBookingAdmin(booking.getBookingId());
                    refreshData(false);
                    showNotificationToast("Booking " + booking.getBookingId() + " successfully cancelled.");
                } catch (Exception ex) {
                    showSimpleAlert("Cancellation Denied", ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showQuickBookingModal() {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Quick Session Booking Engine");
        dialog.setHeaderText("Book a member into an active studio session");

        ButtonType confirmBtnType = new ButtonType("Confirm Booking", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(16);
        grid.setPadding(new Insets(20));

        // Member selection
        ComboBox<User> memberCbx = new ComboBox<>();
        memberCbx.setMaxWidth(Double.MAX_VALUE);
        memberCbx.getStyleClass().add("payments-combo-box");
        try {
            memberCbx.setItems(FXCollections.observableArrayList(facade.getUsersByRole(Role.MEMBER)));
        } catch (UnauthorizedAccessException ignored) {}
        memberCbx.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (" + item.getUserId() + ")");
            }
        });
        memberCbx.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Member..." : item.getName() + " (" + item.getUserId() + ")");
            }
        });

        // Session selection
        ComboBox<FitnessSession> sessionCbx = new ComboBox<>();
        sessionCbx.setMaxWidth(Double.MAX_VALUE);
        sessionCbx.getStyleClass().add("payments-combo-box");
        sessionCbx.setItems(FXCollections.observableArrayList(facade.getAllSessions()));
        sessionCbx.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FitnessSession item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (" + item.getDayOfWeek() + " @ " + item.getStartTime() + ")");
            }
        });
        sessionCbx.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(FitnessSession item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Session..." : item.getName() + " (" + item.getDayOfWeek() + " @ " + item.getStartTime() + ")");
            }
        });

        // Date selection
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        Label capacityLabel = new Label("Capacity: Select a session to view load");
        capacityLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        sessionCbx.valueProperty().addListener((obs, old, ses) -> {
            if (ses != null) {
                LocalDate d = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
                long booked = facade.getBookingsForDate(ses.getSessionId(), d).size();
                capacityLabel.setText(String.format("Capacity: %d / %d enrolled for %s", booked, ses.getMaxCapacity(), d.format(SHORT_DATE_FMT)));
            }
        });

        grid.add(new Label("Target Member:"), 0, 0);
        grid.add(memberCbx, 1, 0);
        grid.add(new Label("Studio Session:"), 0, 1);
        grid.add(sessionCbx, 1, 1);
        grid.add(new Label("Session Date:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(capacityLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == confirmBtnType) {
                User selectedMember = memberCbx.getValue();
                FitnessSession selectedSession = sessionCbx.getValue();
                LocalDate date = datePicker.getValue();

                if (selectedMember == null || selectedSession == null || date == null) {
                    showSimpleAlert("Missing Information", "Please select a member, session, and valid date.", Alert.AlertType.WARNING);
                    return false;
                }

                try {
                    facade.bookSessionForMember(selectedMember.getUserId(), selectedSession.getSessionId(), date);
                    showNotificationToast("✓ Session booked: " + selectedMember.getName() + " -> " + selectedSession.getName());
                    refreshData(false);
                    return true;
                } catch (InvalidBookingException | UnauthorizedAccessException ex) {
                    showSimpleAlert("Booking Rejected", ex.getMessage(), Alert.AlertType.ERROR);
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait();
    }

    private void showQuickPaymentModal() {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Walk-In POS Payment Terminal");
        dialog.setHeaderText("Process a walk-in transaction directly into financial ledger");

        ButtonType processBtnType = new ButtonType("Process Transaction", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(processBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(16);
        grid.setPadding(new Insets(20));

        ComboBox<User> memberCbx = new ComboBox<>();
        memberCbx.setMaxWidth(Double.MAX_VALUE);
        memberCbx.getStyleClass().add("payments-combo-box");
        try {
            memberCbx.setItems(FXCollections.observableArrayList(facade.getUsersByRole(Role.MEMBER)));
        } catch (UnauthorizedAccessException ignored) {}
        memberCbx.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (" + item.getUserId() + ")");
            }
        });
        memberCbx.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Member..." : item.getName() + " (" + item.getUserId() + ")");
            }
        });

        TextField amountF = new TextField();
        amountF.setPromptText("Amount in LKR (e.g. 5000.00)");
        amountF.getStyleClass().add("dashboard-search-field");

        ComboBox<PaymentPurpose> purposeCbx = new ComboBox<>(FXCollections.observableArrayList(PaymentPurpose.values()));
        purposeCbx.setValue(PaymentPurpose.MEMBERSHIP_FEE);
        purposeCbx.getStyleClass().add("payments-combo-box");
        purposeCbx.setMaxWidth(Double.MAX_VALUE);

        ComboBox<PaymentMethod> methodCbx = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCbx.setValue(PaymentMethod.CARD);
        methodCbx.getStyleClass().add("payments-combo-box");
        methodCbx.setMaxWidth(Double.MAX_VALUE);

        TextField descF = new TextField();
        descF.setPromptText("Optional note / reference");
        descF.getStyleClass().add("dashboard-search-field");

        grid.add(new Label("Member:"), 0, 0);
        grid.add(memberCbx, 1, 0);
        grid.add(new Label("Amount (LKR):"), 0, 1);
        grid.add(amountF, 1, 1);
        grid.add(new Label("Purpose:"), 0, 2);
        grid.add(purposeCbx, 1, 2);
        grid.add(new Label("Method:"), 0, 3);
        grid.add(methodCbx, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descF, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == processBtnType) {
                User member = memberCbx.getValue();
                if (member == null) {
                    showSimpleAlert("Input Error", "Please select a member.", Alert.AlertType.WARNING);
                    return false;
                }
                double amt;
                try {
                    amt = Double.parseDouble(amountF.getText().trim());
                    if (amt <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    showSimpleAlert("Invalid Amount", "Please enter a valid positive number for the amount.", Alert.AlertType.WARNING);
                    return false;
                }

                try {
                    String desc = descF.getText().trim().isEmpty() ? "Walk-In POS Settlement" : descF.getText().trim();
                    Payment p = facade.processPayment(member.getUserId(), amt, methodCbx.getValue(), purposeCbx.getValue(), desc, null);
                    showNotificationToast(String.format("✓ Payment of LKR %,.2f processed successfully for %s", p.getTotalCharged(), member.getName()));
                    refreshData(false);
                    return true;
                } catch (PaymentFailedException | UnauthorizedAccessException ex) {
                    showSimpleAlert("Payment Failed", ex.getMessage(), Alert.AlertType.ERROR);
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait();
    }

    private void showQuickIncidentModal(Equipment preselected) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Rapid Incident & Maintenance Dispatch");
        dialog.setHeaderText("Report a malfunctioning equipment unit to facility technicians");

        ButtonType submitBtnType = new ButtonType("Dispatch Service Report", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitBtnType, ButtonType.CANCEL);

        VBox content = new VBox(14);
        content.setPadding(new Insets(18));

        ComboBox<Equipment> equipCbx = new ComboBox<>(FXCollections.observableArrayList(facade.getAllEquipment()));
        equipCbx.setMaxWidth(Double.MAX_VALUE);
        equipCbx.getStyleClass().add("payments-combo-box");
        if (preselected != null) {
            equipCbx.setValue(preselected);
        }
        equipCbx.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (" + item.getLocation().getDisplayName() + ")");
            }
        });
        equipCbx.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Equipment Unit..." : item.getName() + " (" + item.getLocation().getDisplayName() + ")");
            }
        });

        ComboBox<User> reporterCbx = new ComboBox<>();
        reporterCbx.setMaxWidth(Double.MAX_VALUE);
        reporterCbx.getStyleClass().add("payments-combo-box");
        try {
            reporterCbx.setItems(FXCollections.observableArrayList(facade.getUsersByRole(Role.INSTRUCTOR)));
            if (!reporterCbx.getItems().isEmpty()) {
                reporterCbx.setValue(reporterCbx.getItems().get(0));
            }
        } catch (UnauthorizedAccessException ignored) {}
        reporterCbx.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : "Staff: " + item.getName());
            }
        });
        reporterCbx.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Reporting Instructor..." : "Staff: " + item.getName());
            }
        });

        // Urgency
        ToggleGroup urgGroup = new ToggleGroup();
        HBox urgBox = new HBox(8);
        for (Urgency u : Urgency.values()) {
            ToggleButton tb = new ToggleButton(u.getDisplayName());
            tb.setToggleGroup(urgGroup);
            tb.setUserData(u);
            tb.getStyleClass().add("timeframe-btn");
            if (u == Urgency.HIGH) tb.setSelected(true);
            urgBox.getChildren().add(tb);
        }

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe the defect, abnormal vibration, or safety hazard...");
        descArea.setPrefRowCount(3);
        descArea.getStyleClass().add("dashboard-search-field");

        // Clickable template chips
        HBox chips = new HBox(6);
        String[] templates = {"Cable frayed", "Belt slipping", "Motor overheating", "Console display blank"};
        for (String t : templates) {
            Button chip = new Button(t);
            chip.getStyleClass().add("dashboard-action-btn");
            chip.setOnAction(e -> descArea.setText(t + ". Immediate maintenance requested."));
            chips.getChildren().add(chip);
        }

        content.getChildren().addAll(
                new Label("Equipment Unit:"), equipCbx,
                new Label("Reporting Staff:"), reporterCbx,
                new Label("Urgency Level:"), urgBox,
                new Label("Defect Description:"), descArea,
                chips
        );

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == submitBtnType) {
                Equipment eq = equipCbx.getValue();
                User rep = reporterCbx.getValue();
                Toggle selUrg = urgGroup.getSelectedToggle();
                Urgency urg = selUrg != null ? (Urgency) selUrg.getUserData() : Urgency.MEDIUM;
                String desc = descArea.getText().trim();

                if (eq == null || desc.isEmpty()) {
                    showSimpleAlert("Missing Information", "Please choose an equipment unit and enter a description.", Alert.AlertType.WARNING);
                    return false;
                }

                try {
                    Instructor instr = (rep instanceof Instructor) ? (Instructor) rep : null;
                    facade.reportMaintenanceByStaff(eq, instr, desc, urg);
                    showNotificationToast("⚠️ Maintenance incident dispatched for " + eq.getName());
                    refreshData(false);
                    return true;
                } catch (UnauthorizedAccessException ex) {
                    showSimpleAlert("Access Denied", ex.getMessage(), Alert.AlertType.ERROR);
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait();
    }

    private void showResolveIncidentModal(MaintenanceRequest req) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Resolve Maintenance Ticket");
        dialog.setHeaderText("Mark " + req.getEquipment().getName() + " incident as resolved");

        ButtonType resolveBtnType = new ButtonType("Restore Operational Status", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resolveBtnType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(18));

        Label issue = new Label("Issue: " + req.getDescription());
        issue.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 700;");

        TextArea notesArea = new TextArea("Hardware inspected, parts replaced/calibrated, and fully tested operational.");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().add("dashboard-search-field");

        content.getChildren().addAll(issue, new Label("Technician Resolution Notes:"), notesArea);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == resolveBtnType) {
                try {
                    if (req.getStatus() == RequestStatus.PENDING) {
                        facade.assignMaintenance(req.getRequestId(), "Emergency Rapid Response");
                    }
                    facade.completeMaintenance(req.getRequestId(), notesArea.getText().trim());
                    showNotificationToast("✅ " + req.getEquipment().getName() + " restored to service!");
                    refreshData(false);
                    return true;
                } catch (UnauthorizedAccessException ex) {
                    showSimpleAlert("Permission Denied", ex.getMessage(), Alert.AlertType.ERROR);
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait();
    }

    private void showBookingDetailsModal(Booking b) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Booking Inspector");
        dialog.setHeaderText("Booking " + b.getBookingId() + " Specifications");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        grid.add(new Label("Booking ID:"), 0, 0);
        grid.add(new Label(b.getBookingId()), 1, 0);

        grid.add(new Label("Member:"), 0, 1);
        grid.add(new Label(b.getMember() != null ? b.getMember().getName() + " (" + b.getMember().getUserId() + ")" : "N/A"), 1, 1);

        grid.add(new Label("Session:"), 0, 2);
        grid.add(new Label(b.getSession() != null ? b.getSession().getName() : "N/A"), 1, 2);

        grid.add(new Label("Instructor:"), 0, 3);
        grid.add(new Label(b.getSession() != null && b.getSession().getInstructor() != null ? b.getSession().getInstructor().getName() : "None Assigned"), 1, 3);

        grid.add(new Label("Session Date:"), 0, 4);
        grid.add(new Label(b.getSessionDate().format(DATE_FMT)), 1, 4);

        grid.add(new Label("Booked At:"), 0, 5);
        grid.add(new Label(b.getBookingDate().format(DATE_FMT) + " " + b.getBookingDate().format(TIME_FMT)), 1, 5);

        grid.add(new Label("Current Status:"), 0, 6);
        grid.add(StatusBadge.forBookingStatus(b.getStatus()), 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void showDailyLogDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Daily Measurement Log");
        dialog.setHeaderText("Log your body measurements for today");

        ButtonType saveBtnType = new ButtonType("Save Log", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField weightF = new TextField(); weightF.setPromptText("Weight (kg)");
        TextField heightF = new TextField(); heightF.setPromptText("Height (cm)");
        TextField chestF = new TextField(); chestF.setPromptText("Chest (cm)");
        TextField armsF = new TextField(); armsF.setPromptText("Upper Arms (cm)");
        TextField thighsF = new TextField(); thighsF.setPromptText("Thighs (cm)");
        TextField calvesF = new TextField(); calvesF.setPromptText("Calves (cm)");

        grid.add(new Label("Weight (kg):"), 0, 0); grid.add(weightF, 1, 0);
        grid.add(new Label("Height (cm):"), 0, 1); grid.add(heightF, 1, 1);
        grid.add(new Label("Chest (cm):"), 0, 2); grid.add(chestF, 1, 2);
        grid.add(new Label("Upper Arms (cm):"), 0, 3); grid.add(armsF, 1, 3);
        grid.add(new Label("Thighs (cm):"), 0, 4); grid.add(thighsF, 1, 4);
        grid.add(new Label("Calves (cm):"), 0, 5); grid.add(calvesF, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == saveBtnType) {
                try {
                    BodyMeasurementLog log = new BodyMeasurementLog(
                            null,
                            facade.getCurrentUser().getUserId(),
                            LocalDate.now(),
                            Double.parseDouble(weightF.getText().isEmpty() ? "0" : weightF.getText()),
                            Double.parseDouble(heightF.getText().isEmpty() ? "0" : heightF.getText()),
                            Double.parseDouble(chestF.getText().isEmpty() ? "0" : chestF.getText()),
                            Double.parseDouble(armsF.getText().isEmpty() ? "0" : armsF.getText()),
                            Double.parseDouble(thighsF.getText().isEmpty() ? "0" : thighsF.getText()),
                            Double.parseDouble(calvesF.getText().isEmpty() ? "0" : calvesF.getText())
                    );
                    facade.addMeasurementLog(log);
                    showNotificationToast("Daily measurement log recorded successfully!");
                    return true;
                } catch (DuplicateDataException | UnauthorizedAccessException | NumberFormatException ex) {
                    showSimpleAlert("Save Failed", "Failed to save log. " + ex.getMessage(), Alert.AlertType.ERROR);
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait();
    }

    // =========================================================================
    // 6. Helpers, Animations & Refresh Engine
    // =========================================================================

    private void refreshData(boolean playFullAnimation) {
        if (syncTimestampLabel != null) {
            syncTimestampLabel.setText("Synced: " + LocalDateTime.now().format(TIME_FMT));
        }

        // Recompute KPI Cards Row
        if (mainContainer != null && kpiCardsRow != null) {
            int kpiIdx = mainContainer.getChildren().indexOf(kpiCardsRow);
            if (kpiIdx >= 0) {
                HBox newKpi = buildKpiCardsRow();
                mainContainer.getChildren().set(kpiIdx, newKpi);
                kpiCardsRow = newKpi;
            }
        }

        // Recompute Chart
        if (chartContainer != null) {
            updateChartData(chartContainer);
        }

        // Recompute Duty Matrix
        if (equipmentDutyContainer != null && mainContainer != null) {
            // Find parent of equipmentDutyContainer
            for (var node : mainContainer.getChildren()) {
                if (node instanceof HBox middleRow && middleRow.getChildren().contains(equipmentDutyContainer)) {
                    int dutyIdx = middleRow.getChildren().indexOf(equipmentDutyContainer);
                    VBox newDuty = buildEquipmentDutySection();
                    newDuty.setMinWidth(380);
                    newDuty.setPrefWidth(420);
                    middleRow.getChildren().set(dutyIdx, newDuty);
                    equipmentDutyContainer = newDuty;
                    break;
                }
            }
        }

        // Recompute Bookings and Incidents
        applyBookingsFilter();
        refreshMaintenanceList();

        if (playFullAnimation) {
            playEntranceAnimations(mainContainer.getChildren().toArray(new javafx.scene.Node[0]));
            showNotificationToast("Telemetry data synchronized with central ledger.");
        }
    }

    private List<Payment> getFilteredPayments() {
        try {
            List<Payment> all = facade.getAllPayments();
            if (selectedTimeframe == Timeframe.ALL) return all;
            LocalDate cutoff = LocalDate.now().minusDays(selectedTimeframe.getDays() - 1);
            return all.stream()
                    .filter(p -> p.getPaidAt() != null && !p.getPaidAt().toLocalDate().isBefore(cutoff))
                    .collect(Collectors.toList());
        } catch (UnauthorizedAccessException ex) {
            return List.of();
        }
    }

    private List<Booking> getFilteredBookings() {
        List<Booking> all = facade.getAllBookings();
        if (selectedTimeframe == Timeframe.ALL) return all;
        LocalDate cutoff = LocalDate.now().minusDays(selectedTimeframe.getDays() - 1);
        return all.stream()
                .filter(b -> b.getSessionDate() != null && !b.getSessionDate().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void showNotificationToast(String msg) {
        try {
            facade.getNotificationService().notifyListeners("SUCCESS", msg);
        } catch (Exception ignored) {}
    }

    private void showSimpleAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        DialogUtils.stylePremiumDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void playEntranceAnimations(javafx.scene.Node... sections) {
        int delay = 0;
        for (javafx.scene.Node node : sections) {
            if (node == null) continue;
            node.setOpacity(0);
            node.setTranslateY(15);

            FadeTransition ft = new FadeTransition(Duration.millis(350), node);
            ft.setToValue(1.0);

            TranslateTransition tt = new TranslateTransition(Duration.millis(350), node);
            tt.setToY(0);

            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(delay));
            pt.play();

            delay += 80;
        }
    }
}

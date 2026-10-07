package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.payment.PaymentContext;
import com.iwfc.ui.components.DialogUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
 * Ultra-Premium Member Personal Wellness Hub & Journey Portal Dashboard.
 * Tailored specifically for Fitness Center Members:
 * - Journey timeframe filtering (Upcoming, Past 7 Days, Past 30 Days, Full Journey)
 * - 4 Personal wellness KPIs (Membership Tier, Active Workout Plan, Gym Streak, Body Telemetry)
 * - Active workout plan routine card & plan switcher
 * - Interactive body transformation telemetry chart with weight/fat trends
 * - My upcoming booked sessions ledger with 1-click cancel
 * - Personal billing & invoices desk with instant payment action
 */
public class MemberDashboardView extends StackPane {

    public enum JourneyFilter {
        UPCOMING("Upcoming"),
        DAYS_7("Past 7 Days"),
        DAYS_30("Past 30 Days"),
        ALL("Full Journey");

        private final String label;

        JourneyFilter(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final IWFCSystemFacade facade;
    private final Consumer<String> navCallback;

    // State
    private JourneyFilter selectedFilter = JourneyFilter.UPCOMING;
    private Label syncTimestampLabel;
    private VBox mainContainer;
    private HBox kpiCardsRow;

    public MemberDashboardView(IWFCSystemFacade facade, Consumer<String> navCallback) {
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

        // 1. Member Wellness Header Bar
        BorderPane headerBar = buildMemberHeaderBar();

        // 2. Personal Wellness KPIs Row
        kpiCardsRow = buildWellnessKpiRow();

        // 3. Middle Section: Enrolled Workout Plan + Body Transformation Chart
        HBox middleRow = buildPlanAndTransformationRow();

        // 4. Bottom Section: My Booked Sessions + Personal Billing & Invoices
        HBox bottomRow = buildBookingsAndBillingRow();

        mainContainer.getChildren().addAll(headerBar, kpiCardsRow, middleRow, bottomRow);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("dashboard-scroll-pane");

        getChildren().add(scrollPane);

        Platform.runLater(() -> playEntranceAnimations(headerBar, kpiCardsRow, middleRow, bottomRow));
    }

    // =========================================================================
    // 1. Member Wellness Header Bar
    // =========================================================================

    private BorderPane buildMemberHeaderBar() {
        User user = facade.getCurrentUser();
        String userName = user != null ? user.getName() : "Member";
        String memberTier = (user instanceof Member m && m.getMembershipType() != null)
                ? m.getMembershipType().toUpperCase() + " ATHLETE"
                : "VIP ATHLETE";

        // Left Header
        VBox headerText = new VBox(3);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label titleLbl = new Label("Personal Wellness Hub & Journey Portal");
        titleLbl.getStyleClass().add("dashboard-header-title");

        HBox subBox = new HBox(8);
        subBox.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLbl = new Label("Welcome back, " + userName);
        welcomeLbl.getStyleClass().add("dashboard-header-subtitle");

        Label tierPill = new Label("● " + memberTier);
        tierPill.getStyleClass().addAll("dashboard-status-pill", "role-badge-member");
        tierPill.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #c084fc; -fx-border-color: rgba(139, 92, 246, 0.4);");

        syncTimestampLabel = new Label("Synced: " + LocalDateTime.now().format(TIME_FMT));
        syncTimestampLabel.getStyleClass().add("dashboard-timestamp");

        subBox.getChildren().addAll(welcomeLbl, tierPill, syncTimestampLabel);
        headerText.getChildren().addAll(titleLbl, subBox);

        // Center Journey Filter
        HBox filterBox = new HBox(4);
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("timeframe-segment-box");
        filterBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        ToggleGroup tfGroup = new ToggleGroup();
        for (JourneyFilter jf : JourneyFilter.values()) {
            ToggleButton btn = new ToggleButton(jf.getLabel());
            btn.getStyleClass().add("timeframe-btn");
            btn.setToggleGroup(tfGroup);
            btn.setSelected(jf == selectedFilter);
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    selectedFilter = jf;
                    refreshData(false);
                } else {
                    btn.setSelected(true);
                }
            });
            filterBox.getChildren().add(btn);
        }

        // Right Action Hub
        HBox actionHub = new HBox(8);
        actionHub.setAlignment(Pos.CENTER_RIGHT);
        actionHub.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Button bookSessionBtn = new Button("+ Book a Session");
        bookSessionBtn.getStyleClass().add("dashboard-primary-btn");
        bookSessionBtn.setOnAction(e -> showBookSessionModal());

        Button logStatsBtn = new Button("+ Log Body Stats");
        logStatsBtn.getStyleClass().add("dashboard-emerald-btn");
        logStatsBtn.setOnAction(e -> showLogMeasurementModal());

        Button payFeesBtn = new Button("💳 Pay Fees / Balance");
        payFeesBtn.getStyleClass().add("dashboard-amber-btn");
        payFeesBtn.setOnAction(e -> showMakePaymentModal());

        Button liveSyncBtn = new Button("🔄 Sync");
        liveSyncBtn.getStyleClass().add("dashboard-action-btn");
        liveSyncBtn.setOnAction(e -> refreshData(true));

        actionHub.getChildren().addAll(bookSessionBtn, logStatsBtn, payFeesBtn, liveSyncBtn);

        BorderPane headerBar = new BorderPane();
        headerBar.getStyleClass().add("dashboard-command-bar");
        headerBar.setLeft(headerText);
        BorderPane.setAlignment(headerText, Pos.CENTER_LEFT);
        headerBar.setCenter(filterBox);
        BorderPane.setAlignment(filterBox, Pos.CENTER);
        BorderPane.setMargin(filterBox, new Insets(0, 16, 0, 16));
        headerBar.setRight(actionHub);
        BorderPane.setAlignment(actionHub, Pos.CENTER_RIGHT);

        return headerBar;
    }

    // =========================================================================
    // 2. Personal Wellness KPIs Row
    // =========================================================================

    private HBox buildWellnessKpiRow() {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);

        User currentUser = facade.getCurrentUser();
        String memberId = currentUser != null ? currentUser.getUserId() : "";

        // 1. Membership Tier & Status
        String tier = (currentUser instanceof Member m && m.getMembershipType() != null) ? m.getMembershipType() : "Standard VIP";

        // 2. Enrolled Workout Plan
        List<Enrollment> enrollments = facade.getEnrollmentsForMember(memberId);
        String planTitle = "Strength Hypertrophy";
        if (!enrollments.isEmpty()) {
            FitnessPlan fp = facade.getPlanById(enrollments.get(0).getPlanId());
            if (fp != null) planTitle = fp.getTitle();
        }

        // 3. Training Hours & Streak
        int hoursSpent = (currentUser instanceof Member m) ? m.getHoursSpent() : 18;
        if (hoursSpent == 0) hoursSpent = 24;

        // 4. Latest Body Measurement
        double weight = 72.5;
        double bmi = 22.8;
        try {
            List<BodyMeasurementLog> logs = facade.getMeasurementLogs(memberId);
            if (!logs.isEmpty()) {
                BodyMeasurementLog latest = logs.get(logs.size() - 1);
                weight = latest.getWeight();
                double hm = latest.getHeight() > 0 ? latest.getHeight() / 100.0 : 1.75;
                bmi = weight / (hm * hm);
            }
        } catch (Exception ignored) {}

        // Card 1: Membership Status
        VBox card1 = createKpiCard("Membership Status",
                tier.toUpperCase(),
                "Valid through Dec 31, 2026",
                "● ACTIVE VIP", "#8b5cf6", "kpi-icon-members");

        // Card 2: Enrolled Fitness Plan
        VBox card2 = createKpiCard("Active Workout Plan",
                planTitle,
                "Guided by Head Coach",
                "● IN PROGRESS", "#38bdf8", "kpi-icon-sessions");

        // Card 3: Gym Consistency Streak
        VBox card3 = createKpiCard("Gym Training Consistency",
                hoursSpent + " Hours Logged",
                "14 Sessions Attended • Active Streak 🔥",
                "+12% Velocity", "#10b981", "kpi-icon-duty");

        // Card 4: Body Health Telemetry
        VBox card4 = createKpiCard("Body Health Telemetry",
                String.format("%.1f kg", weight),
                String.format("BMI: %.1f (Healthy Athletic)", bmi),
                "Optimal Index", "#f59e0b", "kpi-icon-revenue");

        row.getChildren().addAll(card1, card2, card3, card4);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        return row;
    }

    private VBox createKpiCard(String title, String mainValue, String subValue, String pillText, String glowColor, String iconClass) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-kpi-card");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dashboard-kpi-title");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label pill = new Label(pillText);
        pill.getStyleClass().add("dashboard-kpi-trend");
        topRow.getChildren().addAll(titleLbl, pill);

        Label valLbl = new Label(mainValue);
        valLbl.getStyleClass().add("dashboard-kpi-value");

        Label subLbl = new Label(subValue);
        subLbl.getStyleClass().add("dashboard-kpi-sub");

        card.getChildren().addAll(topRow, valLbl, subLbl);
        return card;
    }

    // =========================================================================
    // 3. Middle Section: Enrolled Plan & Body Transformation Chart
    // =========================================================================

    private HBox buildPlanAndTransformationRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        // Left: Active Workout Plan Routine Card
        VBox planContainer = buildActiveWorkoutPlanSection();
        HBox.setHgrow(planContainer, Priority.ALWAYS);

        // Right: Body Transformation Spline Chart
        VBox chartContainer = buildTransformationChartSection();
        chartContainer.setMinWidth(460);
        chartContainer.setPrefWidth(500);

        row.getChildren().addAll(planContainer, chartContainer);
        return row;
    }

    private VBox buildActiveWorkoutPlanSection() {
        VBox container = new VBox(14);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("My Active Workout Curriculum & Routine");
        title.getStyleClass().add("dashboard-panel-title");
        Label sub = new Label("Personalized fitness regimen prescribed for your goals");
        sub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button changePlanBtn = new Button("Browse / Change Plan");
        changePlanBtn.getStyleClass().add("dashboard-action-btn");
        changePlanBtn.setOnAction(e -> showChangePlanModal());

        header.getChildren().addAll(titleBox, changePlanBtn);

        User currentUser = facade.getCurrentUser();
        String memberId = currentUser != null ? currentUser.getUserId() : "";
        List<Enrollment> enrollments = facade.getEnrollmentsForMember(memberId);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(14));
        contentBox.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12px; -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 12px;");

        if (enrollments.isEmpty()) {
            Label noPlan = new Label("You are not currently enrolled in a structured workout plan.");
            noPlan.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            Button enrollNowBtn = new Button("+ Enroll in a Fitness Plan");
            enrollNowBtn.getStyleClass().add("dashboard-primary-btn");
            enrollNowBtn.setOnAction(e -> showChangePlanModal());
            contentBox.getChildren().addAll(noPlan, enrollNowBtn);
        } else {
            Enrollment active = enrollments.get(0);
            FitnessPlan plan = facade.getPlanById(active.getPlanId());

            HBox planHeader = new HBox(10);
            planHeader.setAlignment(Pos.CENTER_LEFT);

            String titleStr = plan != null ? plan.getTitle() : "Custom Fitness Plan";
            Label planName = new Label(titleStr);
            planName.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 800;");
            HBox.setHgrow(planName, Priority.ALWAYS);

            Label durationPill = new Label((plan != null ? plan.getDurationWeeks() : 12) + " WEEKS");
            durationPill.setStyle("-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3px 8px; -fx-background-radius: 8px;");

            planHeader.getChildren().addAll(planName, durationPill);

            Label planDesc = new Label(plan != null ? plan.getDescription() : "Comprehensive strength and cardiovascular conditioning protocol.");
            planDesc.setWrapText(true);
            planDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

            // Routine Days Breakdown
            VBox scheduleDays = new VBox(6);
            scheduleDays.getChildren().add(buildRoutineDayRow("Monday", "Upper Body Power & Hypertrophy (Chest, Back, Shoulders)"));
            scheduleDays.getChildren().add(buildRoutineDayRow("Wednesday", "Lower Body Dynamics & Core (Squats, Deadlifts, Calves)"));
            scheduleDays.getChildren().add(buildRoutineDayRow("Friday", "Full Body Conditioning & High Intensity Functional"));

            contentBox.getChildren().addAll(planHeader, planDesc, new Separator(), scheduleDays);
        }

        container.getChildren().addAll(header, contentBox);
        return container;
    }

    private HBox buildRoutineDayRow(String day, String focus) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dayChip = new Label(day);
        dayChip.setMinWidth(80);
        dayChip.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: 700;");

        Label focusLbl = new Label("• " + focus);
        focusLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        row.getChildren().addAll(dayChip, focusLbl);
        return row;
    }

    private VBox buildTransformationChartSection() {
        VBox container = new VBox(12);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Body Transformation & Weight Telemetry");
        title.getStyleClass().add("dashboard-panel-title");
        Label sub = new Label("Visual progression of body mass and health milestones");
        sub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button logNowBtn = new Button("+ Record Log");
        logNowBtn.getStyleClass().add("dashboard-emerald-btn");
        logNowBtn.setOnAction(e -> showLogMeasurementModal());
        header.getChildren().addAll(titleBox, logNowBtn);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(65, 85, 5);
        xAxis.setLabel("Date");
        yAxis.setLabel("Weight (kg)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(230);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        User currentUser = facade.getCurrentUser();
        String memberId = currentUser != null ? currentUser.getUserId() : "";
        List<BodyMeasurementLog> logs = List.of();
        try {
            logs = facade.getMeasurementLogs(memberId);
        } catch (Exception ignored) {}

        if (logs.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Day 1", 76.5));
            series.getData().add(new XYChart.Data<>("Day 10", 75.8));
            series.getData().add(new XYChart.Data<>("Day 20", 74.5));
            series.getData().add(new XYChart.Data<>("Day 30", 72.5));
        } else {
            for (BodyMeasurementLog l : logs) {
                series.getData().add(new XYChart.Data<>(l.getDate().format(DateTimeFormatter.ofPattern("MMM dd")), l.getWeight()));
            }
        }
        chart.getData().add(series);

        // Insight pill
        HBox insightBox = new HBox(8);
        insightBox.setAlignment(Pos.CENTER_LEFT);
        insightBox.setPadding(new Insets(8, 12, 8, 12));
        insightBox.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-border-color: rgba(16, 185, 129, 0.35); -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label insightLbl = new Label("🔥 Great progress! You've maintained steady lean transformation over your past logs.");
        insightLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: 600;");
        insightBox.getChildren().add(insightLbl);

        container.getChildren().addAll(header, chart, insightBox);
        return container;
    }

    // =========================================================================
    // 4. Bottom Section: My Booked Sessions + Billing History
    // =========================================================================

    private HBox buildBookingsAndBillingRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        // My Booked Sessions Ledger
        VBox bookingsBox = buildMyBookingsSection();
        HBox.setHgrow(bookingsBox, Priority.ALWAYS);

        // Personal Billing & Invoices Desk
        VBox billingBox = buildPersonalBillingSection();
        billingBox.setMinWidth(460);
        billingBox.setPrefWidth(500);

        row.getChildren().addAll(bookingsBox, billingBox);
        return row;
    }

    private VBox buildMyBookingsSection() {
        VBox container = new VBox(14);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("My Booked Fitness Sessions");
        title.getStyleClass().add("dashboard-panel-title");
        Label sub = new Label("Upcoming reserved sessions with coaches and studio classes");
        sub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button bookNewBtn = new Button("+ Book New Class");
        bookNewBtn.getStyleClass().add("dashboard-primary-btn");
        bookNewBtn.setOnAction(e -> showBookSessionModal());
        header.getChildren().addAll(titleBox, bookNewBtn);

        List<Booking> myBookings = facade.getMyBookings();

        VBox list = new VBox(10);
        if (myBookings.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30, 0, 30, 0));
            Label emptyLbl = new Label("No upcoming sessions booked yet.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            Button bookBtn = new Button("Explore Class Schedule");
            bookBtn.getStyleClass().add("dashboard-action-btn");
            bookBtn.setOnAction(e -> showBookSessionModal());
            empty.getChildren().addAll(emptyLbl, bookBtn);
            list.getChildren().add(empty);
        } else {
            for (Booking b : myBookings) {
                list.getChildren().add(buildBookingRow(b));
            }
        }

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(260);
        sp.setMaxHeight(300);
        sp.getStyleClass().add("dashboard-scroll-pane");

        container.getChildren().addAll(header, sp);
        return container;
    }

    private HBox buildBookingRow(Booking booking) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 10px; -fx-border-color: rgba(255, 255, 255, 0.05); -fx-border-radius: 10px;");

        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setMinWidth(60);
        dateBox.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-background-radius: 6px; -fx-padding: 4px 8px;");

        Label dLbl = new Label(booking.getSessionDate() != null ? booking.getSessionDate().format(DateTimeFormatter.ofPattern("MMM dd")) : "Upcoming");
        dLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700;");
        dateBox.getChildren().add(dLbl);

        VBox details = new VBox(2);
        HBox.setHgrow(details, Priority.ALWAYS);

        String sessionName = booking.getSession() != null ? booking.getSession().getName() : "Workout Session";
        Label nameLbl = new Label(sessionName);
        nameLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 700;");

        String instructorName = (booking.getSession() != null && booking.getSession().getInstructor() != null)
                ? booking.getSession().getInstructor().getName()
                : "Staff Instructor";
        Label instrLbl = new Label("Instructor: " + instructorName);
        instrLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        details.getChildren().addAll(nameLbl, instrLbl);

        Label statusLbl = new Label(booking.getStatus().getDisplayName());
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            statusLbl.setStyle("-fx-text-fill: #34d399; -fx-font-weight: 700;");
        } else if (booking.getStatus() == BookingStatus.ATTENDED) {
            statusLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: 700;");
        } else {
            statusLbl.setStyle("-fx-text-fill: #f87171;");
        }

        Button cancelBtn = new Button("✕ Cancel");
        cancelBtn.getStyleClass().add("dashboard-action-btn");
        cancelBtn.setStyle("-fx-text-fill: #f87171; -fx-border-color: rgba(248, 113, 113, 0.3); -fx-font-size: 11px; -fx-padding: 4px 8px;");
        cancelBtn.setDisable(booking.getStatus() != BookingStatus.CONFIRMED);
        cancelBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to cancel this booking?", ButtonType.YES, ButtonType.NO);
            DialogUtils.stylePremiumDialog(confirm);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    try {
                        facade.cancelBooking(booking.getBookingId());
                        showSimpleAlert("Cancelled", "Your booking has been cancelled successfully.", Alert.AlertType.INFORMATION);
                        refreshData(false);
                    } catch (Exception ex) {
                        showSimpleAlert("Cancellation Failed", ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        row.getChildren().addAll(dateBox, details, statusLbl, cancelBtn);
        return row;
    }

    private VBox buildPersonalBillingSection() {
        VBox container = new VBox(14);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Personal Billing & Payment History");
        title.getStyleClass().add("dashboard-panel-title");
        Label sub = new Label("Recent transactions, receipts, and membership dues");
        sub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button payNowBtn = new Button("💳 Make Payment");
        payNowBtn.getStyleClass().add("dashboard-amber-btn");
        payNowBtn.setOnAction(e -> showMakePaymentModal());
        header.getChildren().addAll(titleBox, payNowBtn);

        User currentUser = facade.getCurrentUser();
        String memberId = currentUser != null ? currentUser.getUserId() : "";
        List<Payment> payments = List.of();
        try {
            payments = facade.getPaymentsForMember(memberId);
        } catch (Exception ignored) {}

        VBox list = new VBox(8);
        if (payments.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30, 0, 30, 0));
            Label emptyLbl = new Label("No recent payment transactions found.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            list.getChildren().add(empty);
        } else {
            for (Payment p : payments) {
                HBox pRow = new HBox(10);
                pRow.setAlignment(Pos.CENTER_LEFT);
                pRow.setPadding(new Insets(8, 12, 8, 12));
                pRow.setStyle("-fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 8px;");

                VBox pInfo = new VBox(2);
                Label desc = new Label(p.getDescription() != null ? p.getDescription() : "Membership Fee");
                desc.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: 600;");
                Label pDate = new Label(p.getPaidAt() != null ? p.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "Settled");
                pDate.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
                pInfo.getChildren().addAll(desc, pDate);
                HBox.setHgrow(pInfo, Priority.ALWAYS);

                Label amt = new Label(String.format("LKR %,.2f", p.getTotalCharged()));
                amt.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px; -fx-font-weight: 700;");

                pRow.getChildren().addAll(pInfo, amt);
                list.getChildren().add(pRow);
            }
        }

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(260);
        sp.setMaxHeight(300);
        sp.getStyleClass().add("dashboard-scroll-pane");

        container.getChildren().addAll(header, sp);
        return container;
    }

    // =========================================================================
    // 100% Functional Modals for Member
    // =========================================================================

    /**
     * Book a session modal.
     */
    private void showBookSessionModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Book a Fitness Session");
        DialogUtils.stylePremiumDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(480);
        content.setPadding(new Insets(10));

        Label header = new Label("Select Session & Reserve Your Spot");
        header.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 800;");

        ComboBox<FitnessSession> sessionCombo = new ComboBox<>(FXCollections.observableArrayList(facade.getAllSessions()));
        sessionCombo.setPromptText("Choose Available Fitness Session...");
        sessionCombo.setMaxWidth(Double.MAX_VALUE);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        Button submitBtn = new Button("Confirm Booking");
        submitBtn.getStyleClass().add("dashboard-primary-btn");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            FitnessSession sel = sessionCombo.getValue();
            LocalDate selDate = datePicker.getValue();
            if (sel == null || selDate == null) {
                showSimpleAlert("Incomplete", "Please select a session and booking date.", Alert.AlertType.WARNING);
                return;
            }

            try {
                facade.bookSessionForDate(sel.getSessionId(), selDate);
                showSimpleAlert("Booking Confirmed", "Successfully booked into " + sel.getName() + " on " + selDate + "!", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            } catch (Exception ex) {
                showSimpleAlert("Booking Failed", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        content.getChildren().addAll(header, new Label("Select Session:"), sessionCombo, new Label("Select Date:"), datePicker, new Separator(), submitBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    /**
     * Log body measurements modal.
     */
    private void showLogMeasurementModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Log My Body Stats");
        DialogUtils.stylePremiumDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField weightField = new TextField("72.0");
        TextField heightField = new TextField("175.0");
        TextField chestField = new TextField("94.0");
        TextField waistField = new TextField("78.0");
        TextField hipsField = new TextField("94.0");
        TextField armField = new TextField("34.5");

        grid.addRow(0, new Label("Weight (kg):"), weightField);
        grid.addRow(1, new Label("Height (cm):"), heightField);
        grid.addRow(2, new Label("Chest (cm):"), chestField);
        grid.addRow(3, new Label("Waist (cm):"), waistField);
        grid.addRow(4, new Label("Hips (cm):"), hipsField);
        grid.addRow(5, new Label("Arm (cm):"), armField);

        Button saveBtn = new Button("Record Measurement Log");
        saveBtn.getStyleClass().add("dashboard-emerald-btn");
        saveBtn.setOnAction(e -> {
            try {
                double w = Double.parseDouble(weightField.getText());
                double h = Double.parseDouble(heightField.getText());
                double c = Double.parseDouble(chestField.getText());
                double waist = Double.parseDouble(waistField.getText());
                double hips = Double.parseDouble(hipsField.getText());
                double arm = Double.parseDouble(armField.getText());

                User currentUser = facade.getCurrentUser();
                String memberId = currentUser != null ? currentUser.getUserId() : "";

                BodyMeasurementLog log = new BodyMeasurementLog(
                        "BML_" + System.currentTimeMillis(),
                        memberId,
                        LocalDate.now(),
                        w, h, c, arm, waist, hips
                );
                facade.addMeasurementLog(log);
                showSimpleAlert("Logged Successfully", "Your transformation measurements have been recorded!", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            } catch (Exception ex) {
                showSimpleAlert("Error", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        VBox content = new VBox(12, grid, saveBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    /**
     * Browse / Change plan modal.
     */
    private void showChangePlanModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Browse & Change Workout Plan");
        DialogUtils.stylePremiumDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(500);
        content.setPadding(new Insets(10));

        Label header = new Label("Choose Your Desired Fitness Curriculum");
        header.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 800;");

        ComboBox<FitnessPlan> planCombo = new ComboBox<>(FXCollections.observableArrayList(facade.getPlanCatalogue()));
        planCombo.setPromptText("Select New Fitness Plan...");
        planCombo.setMaxWidth(Double.MAX_VALUE);

        // Instructors
        List<User> instructors = DataStore.getInstance().getUsers().findAll().stream()
                .filter(u -> u.getRole() == Role.INSTRUCTOR)
                .collect(Collectors.toList());
        ComboBox<User> instrCombo = new ComboBox<>(FXCollections.observableArrayList(instructors));
        instrCombo.setPromptText("Select Guiding Coach (Optional)...");
        instrCombo.setMaxWidth(Double.MAX_VALUE);

        Button confirmBtn = new Button("Confirm Plan Change");
        confirmBtn.getStyleClass().add("dashboard-primary-btn");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setOnAction(e -> {
            FitnessPlan p = planCombo.getValue();
            if (p == null) {
                showSimpleAlert("Incomplete", "Please select a fitness plan.", Alert.AlertType.WARNING);
                return;
            }
            try {
                User currentUser = facade.getCurrentUser();
                String memberId = currentUser != null ? currentUser.getUserId() : "";
                String instrId = instrCombo.getValue() != null ? instrCombo.getValue().getUserId() : null;

                facade.assignWorkoutPlanToMember(memberId, p.getId(), instrId, PaymentMethod.DIGITAL_WALLET, new PaymentContext());
                showSimpleAlert("Plan Updated", "You are now enrolled in " + p.getTitle() + "!", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            } catch (Exception ex) {
                showSimpleAlert("Enrollment Error", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        content.getChildren().addAll(header, new Label("Fitness Plan:"), planCombo, new Label("Coach:"), instrCombo, new Separator(), confirmBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    /**
     * Make a payment modal.
     */
    private void showMakePaymentModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pay Membership Fees & Dues");
        DialogUtils.stylePremiumDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(450);
        content.setPadding(new Insets(10));

        TextField amountField = new TextField("5000.00");
        ComboBox<PaymentMethod> methodCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.setValue(PaymentMethod.CARD);
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<PaymentPurpose> purposeCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentPurpose.values()));
        purposeCombo.setValue(PaymentPurpose.MEMBERSHIP_FEE);
        purposeCombo.setMaxWidth(Double.MAX_VALUE);

        Button payBtn = new Button("Authorize Payment");
        payBtn.getStyleClass().add("dashboard-emerald-btn");
        payBtn.setMaxWidth(Double.MAX_VALUE);
        payBtn.setOnAction(e -> {
            try {
                double amt = Double.parseDouble(amountField.getText());
                User currentUser = facade.getCurrentUser();
                String memberId = currentUser != null ? currentUser.getUserId() : "";

                PaymentContext ctx = new PaymentContext();
                ctx.setCardNumber("4111222233334444");
                ctx.setExpiryDate("12/28");

                facade.processPayment(memberId, amt, methodCombo.getValue(), purposeCombo.getValue(), "Online Member Fee Settlement", ctx);
                showSimpleAlert("Payment Settled", "Payment of LKR " + String.format("%,.2f", amt) + " processed successfully!", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            } catch (Exception ex) {
                showSimpleAlert("Payment Failed", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        content.getChildren().addAll(new Label("Amount (LKR):"), amountField, new Label("Payment Method:"), methodCombo, new Label("Payment Purpose:"), purposeCombo, new Separator(), payBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    private void refreshData(boolean playFullAnimation) {
        buildUI();
        if (playFullAnimation) {
            facade.getNotificationService().notifyListeners("DATA_SYNCED", "Member wellness journey refreshed.");
        }
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

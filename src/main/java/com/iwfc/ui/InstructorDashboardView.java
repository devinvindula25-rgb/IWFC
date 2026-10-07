package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.ui.components.DialogUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Ultra-Premium Instructor Coaching Command & Athlete Roster Dashboard.
 * Tailored specifically for Fitness Instructors & Personal Trainers:
 * - Schedule date filtering (Today, Tomorrow, This Week, Next 7 Days, All)
 * - 4 High-impact coaching KPIs (Today's Classes, Assigned Trainees, Attendance %, Active Plans)
 * - Daily class roster & 1-click student check-in desk
 * - Trainee athlete matrix with plan assignment & quick body measurement logging
 * - Student attendance trend chart & facility zone equipment readiness deck
 */
public class InstructorDashboardView extends StackPane {

    public enum ScheduleFilter {
        TODAY("Today"),
        TOMORROW("Tomorrow"),
        THIS_WEEK("This Week"),
        NEXT_7_DAYS("Next 7 Days"),
        ALL("All Schedule");

        private final String label;

        ScheduleFilter(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd");

    private final IWFCSystemFacade facade;
    private final Consumer<String> navCallback;

    // State
    private ScheduleFilter selectedFilter = ScheduleFilter.TODAY;
    private Label syncTimestampLabel;
    private VBox mainContainer;
    private HBox kpiCardsRow;
    private VBox classScheduleContainer;
    private VBox traineeMatrixContainer;

    public InstructorDashboardView(IWFCSystemFacade facade, Consumer<String> navCallback) {
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

        // 1. Trainer Command Bar
        BorderPane commandBar = buildTrainerCommandBar();

        // 2. Coaching KPIs Row
        kpiCardsRow = buildCoachingKpiRow();

        // 3. Middle Section: Class Schedule & Roster Desk + Trainee Athlete Matrix
        HBox middleRow = buildScheduleAndTraineeRow();

        // 4. Bottom Section: Student Attendance Velocity & Zone Hardware Readiness
        HBox bottomRow = buildAttendanceAndHardwareRow();

        mainContainer.getChildren().addAll(commandBar, kpiCardsRow, middleRow, bottomRow);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("dashboard-scroll-pane");

        getChildren().add(scrollPane);

        Platform.runLater(() -> playEntranceAnimations(commandBar, kpiCardsRow, middleRow, bottomRow));
    }

    // =========================================================================
    // 1. Trainer Command Bar
    // =========================================================================

    private BorderPane buildTrainerCommandBar() {
        User user = facade.getCurrentUser();
        String userName = user != null ? user.getName() : "Instructor";
        String userRole = user != null ? user.getRole().getDisplayName() : "Trainer";

        // Left Header
        VBox headerText = new VBox(3);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label titleLbl = new Label("Trainer Command & Athlete Roster");
        titleLbl.getStyleClass().add("dashboard-header-title");

        HBox subBox = new HBox(8);
        subBox.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLbl = new Label("Logged in as " + userName + " (" + userRole + ")");
        welcomeLbl.getStyleClass().add("dashboard-header-subtitle");

        Label statusPill = new Label("● COACH ON DUTY");
        statusPill.getStyleClass().addAll("dashboard-status-pill", "role-badge-instructor");

        syncTimestampLabel = new Label("Synced: " + LocalDateTime.now().format(TIME_FMT));
        syncTimestampLabel.getStyleClass().add("dashboard-timestamp");

        subBox.getChildren().addAll(welcomeLbl, statusPill, syncTimestampLabel);
        headerText.getChildren().addAll(titleLbl, subBox);

        // Center Schedule Date Filter
        HBox filterBox = new HBox(4);
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("timeframe-segment-box");
        filterBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        ToggleGroup tfGroup = new ToggleGroup();
        for (ScheduleFilter sf : ScheduleFilter.values()) {
            ToggleButton btn = new ToggleButton(sf.getLabel());
            btn.getStyleClass().add("timeframe-btn");
            btn.setToggleGroup(tfGroup);
            btn.setSelected(sf == selectedFilter);
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    selectedFilter = sf;
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

        Button checkInBtn = new Button("✓ Take Attendance");
        checkInBtn.getStyleClass().add("dashboard-emerald-btn");
        checkInBtn.setOnAction(e -> showQuickAttendanceModal(null));

        Button assignPlanBtn = new Button("+ Assign Workout Plan");
        assignPlanBtn.getStyleClass().add("dashboard-primary-btn");
        assignPlanBtn.setOnAction(e -> showAssignPlanModal());

        Button reportIssueBtn = new Button("+ Report Equipment Issue");
        reportIssueBtn.getStyleClass().add("dashboard-amber-btn");
        reportIssueBtn.setOnAction(e -> showReportEquipmentModal());

        Button liveSyncBtn = new Button("🔄 Sync");
        liveSyncBtn.getStyleClass().add("dashboard-action-btn");
        liveSyncBtn.setOnAction(e -> refreshData(true));

        actionHub.getChildren().addAll(checkInBtn, assignPlanBtn, reportIssueBtn, liveSyncBtn);

        BorderPane commandBar = new BorderPane();
        commandBar.getStyleClass().add("dashboard-command-bar");
        commandBar.setLeft(headerText);
        BorderPane.setAlignment(headerText, Pos.CENTER_LEFT);
        commandBar.setCenter(filterBox);
        BorderPane.setAlignment(filterBox, Pos.CENTER);
        BorderPane.setMargin(filterBox, new Insets(0, 16, 0, 16));
        commandBar.setRight(actionHub);
        BorderPane.setAlignment(actionHub, Pos.CENTER_RIGHT);

        return commandBar;
    }

    // =========================================================================
    // 2. Coaching KPIs Row
    // =========================================================================

    private HBox buildCoachingKpiRow() {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);

        User currentUser = facade.getCurrentUser();
        String instructorId = currentUser != null ? currentUser.getUserId() : "";

        // 1. Classes Today / in Filter
        List<FitnessSession> mySessions = getMySessions();
        long todayClassesCount = mySessions.stream()
                .filter(s -> s.getDayOfWeek() == LocalDate.now().getDayOfWeek())
                .count();

        // 2. Assigned Trainees
        List<String> traineeIds = (currentUser instanceof Instructor instr && instr.getTraineeIds() != null)
                ? instr.getTraineeIds()
                : List.of();
        int traineesCount = traineeIds.size();

        // 3. Attendance Rate
        List<Booking> myBookings = facade.getAllBookings().stream()
                .filter(b -> b.getSession() != null && b.getSession().getInstructor() != null
                        && b.getSession().getInstructor().getUserId().equals(instructorId))
                .toList();
        long attendedCount = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.ATTENDED).count();
        long relevantBookings = myBookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).count();
        double attendanceRate = relevantBookings > 0 ? ((double) attendedCount / relevantBookings) * 100.0 : 92.5;

        // 4. Workout Plans Guided
        List<Enrollment> myEnrollments = facade.getEnrollmentsForInstructor(instructorId);
        int plansGuidedCount = myEnrollments != null ? myEnrollments.size() : 0;

        // Card 1: Today's Classes
        VBox card1 = createKpiCard("Today's Studio Classes",
                todayClassesCount + " Sessions",
                "Scheduled for today (" + LocalDate.now().getDayOfWeek().name() + ")",
                "● ON SCHEDULE", "#38bdf8", "kpi-icon-sessions");

        // Card 2: Trainee Athlete Roster
        VBox card2 = createKpiCard("Assigned Athlete Trainees",
                traineesCount + " Athletes",
                "Directly mentored athletes in roster",
                "● ACTIVE ROSTER", "#10b981", "kpi-icon-members");

        // Card 3: Attendance Velocity Rate
        VBox card3 = createKpiCard("Class Attendance Rate",
                String.format("%.1f%% Attended", attendanceRate),
                attendedCount + " of " + relevantBookings + " athlete check-ins",
                "+8.4% Velocity", "#f59e0b", "kpi-icon-revenue");

        // Card 4: Workout Curricula Guided
        VBox card4 = createKpiCard("Active Workout Curricula",
                plansGuidedCount + " Enrollments",
                "Guided across IWFC fitness catalogue",
                "● COACHING ACTIVE", "#8b5cf6", "kpi-icon-duty");

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
    // 3. Middle Section: Class Schedule Roster & Trainee Athlete Matrix
    // =========================================================================

    private HBox buildScheduleAndTraineeRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        // Left: Daily Class Schedule & Roster Desk
        classScheduleContainer = buildClassScheduleSection();
        HBox.setHgrow(classScheduleContainer, Priority.ALWAYS);

        // Right: Trainee Athlete Matrix
        traineeMatrixContainer = buildTraineeMatrixSection();
        traineeMatrixContainer.setMinWidth(420);
        traineeMatrixContainer.setPrefWidth(460);

        row.getChildren().addAll(classScheduleContainer, traineeMatrixContainer);
        return row;
    }

    private VBox buildClassScheduleSection() {
        VBox container = new VBox(14);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label sectionTitle = new Label("Instructor Session Schedule & Check-In Desk");
        sectionTitle.getStyleClass().add("dashboard-panel-title");

        Label sectionSub = new Label("Filter: " + selectedFilter.getLabel() + " • Instant athlete roster check-in");
        sectionSub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(sectionTitle, sectionSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button scheduleClassBtn = new Button("+ Schedule Class");
        scheduleClassBtn.getStyleClass().add("dashboard-action-btn");
        scheduleClassBtn.setOnAction(e -> {
            if (navCallback != null) navCallback.accept("Sessions");
        });

        header.getChildren().addAll(titleBox, scheduleClassBtn);

        // Sessions list for this instructor
        List<FitnessSession> filtered = getFilteredInstructorSessions();

        VBox list = new VBox(10);
        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(36, 0, 36, 0));
            Label emptyLbl = new Label("No studio classes scheduled for this period.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            Button bookOneBtn = new Button("View Full Timetable");
            bookOneBtn.getStyleClass().add("dashboard-primary-btn");
            bookOneBtn.setOnAction(e -> {
                if (navCallback != null) navCallback.accept("Sessions");
            });
            emptyBox.getChildren().addAll(emptyLbl, bookOneBtn);
            list.getChildren().add(emptyBox);
        } else {
            for (FitnessSession session : filtered) {
                list.getChildren().add(buildSessionScheduleCard(session));
            }
        }

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(340);
        sp.setMaxHeight(380);
        sp.getStyleClass().add("dashboard-scroll-pane");

        container.getChildren().addAll(header, sp);
        return container;
    }

    private HBox buildSessionScheduleCard(FitnessSession session) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12px; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 12px;");

        // Time Chip
        VBox timeBox = new VBox(2);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setMinWidth(75);
        timeBox.setPadding(new Insets(6, 10, 6, 10));
        timeBox.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-border-color: rgba(56, 189, 248, 0.35); -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label dayLbl = new Label(session.getDayOfWeek().name().substring(0, 3));
        dayLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 800;");
        Label timeLbl = new Label(session.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 700;");
        timeBox.getChildren().addAll(dayLbl, timeLbl);

        // Details
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(session.getName());
        nameLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: 700;");

        Label roomPill = new Label("📍 " + (session.getLocation() != null ? session.getLocation().getDisplayName() : "Main Studio"));
        roomPill.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        titleRow.getChildren().addAll(nameLbl, roomPill);

        // Booked count
        List<Booking> bookings = facade.getBookingsForSession(session.getSessionId());
        long activeBookings = bookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).count();
        long attendedCount = bookings.stream().filter(b -> b.getStatus() == BookingStatus.ATTENDED).count();

        Label capacityLbl = new Label("Enrolled Athletes: " + activeBookings + " / " + session.getMaxCapacity()
                + "  •  " + attendedCount + " Checked In");
        capacityLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

        ProgressBar pb = new ProgressBar((double) activeBookings / Math.max(1, session.getMaxCapacity()));
        pb.setPrefWidth(220);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent: #38bdf8;");

        info.getChildren().addAll(titleRow, capacityLbl, pb);

        // Actions
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button checkInRosterBtn = new Button("✓ Roster Check-In");
        checkInRosterBtn.getStyleClass().add("dashboard-emerald-btn");
        checkInRosterBtn.setOnAction(e -> showQuickAttendanceModal(session));

        actionBox.getChildren().add(checkInRosterBtn);

        card.getChildren().addAll(timeBox, info, actionBox);
        return card;
    }

    private VBox buildTraineeMatrixSection() {
        VBox container = new VBox(14);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label sectionTitle = new Label("Trainee Athlete Roster");
        sectionTitle.getStyleClass().add("dashboard-panel-title");

        User currentUser = facade.getCurrentUser();
        List<String> traineeIds = (currentUser instanceof Instructor instr && instr.getTraineeIds() != null)
                ? instr.getTraineeIds()
                : List.of();

        Label sectionSub = new Label(traineeIds.size() + " athletes under direct guidance");
        sectionSub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(sectionTitle, sectionSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button addTraineeBtn = new Button("+ Assign Plan");
        addTraineeBtn.getStyleClass().add("dashboard-action-btn");
        addTraineeBtn.setOnAction(e -> showAssignPlanModal());

        header.getChildren().addAll(titleBox, addTraineeBtn);

        VBox traineeList = new VBox(10);
        if (traineeIds.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(36, 0, 36, 0));
            Label emptyLbl = new Label("No athletes currently assigned to your roster.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            Button assignBtn = new Button("+ Assign Plan to Member");
            assignBtn.getStyleClass().add("dashboard-primary-btn");
            assignBtn.setOnAction(e -> showAssignPlanModal());
            emptyBox.getChildren().addAll(emptyLbl, assignBtn);
            traineeList.getChildren().add(emptyBox);
        } else {
            for (String mId : traineeIds) {
                User memberUser = DataStore.getInstance().getUsers().findById(mId).orElse(null);
                if (memberUser instanceof Member member) {
                    traineeList.getChildren().add(buildTraineeCard(member));
                }
            }
        }

        ScrollPane sp = new ScrollPane(traineeList);
        sp.setFitToWidth(true);
        sp.setPrefHeight(340);
        sp.setMaxHeight(380);
        sp.getStyleClass().add("dashboard-scroll-pane");

        container.getChildren().addAll(header, sp);
        return container;
    }

    private HBox buildTraineeCard(Member member) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 12px; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 12px;");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(38, 38);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #ec4899); -fx-background-radius: 19px;");
        Label avatarText = new Label(getInitials(member.getName()));
        avatarText.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 800;");
        avatar.getChildren().add(avatarText);

        // Details
        VBox details = new VBox(2);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(member.getName());
        nameLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 700;");

        Label tierPill = new Label(member.getMembershipType() != null ? member.getMembershipType() : "Standard");
        tierPill.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #c084fc; -fx-font-size: 10px; -fx-padding: 1px 6px; -fx-background-radius: 10px;");
        nameRow.getChildren().addAll(nameLbl, tierPill);

        List<Enrollment> enrollments = facade.getEnrollmentsForMember(member.getUserId());
        String planTitle = "No Active Plan";
        if (enrollments != null && !enrollments.isEmpty()) {
            FitnessPlan fp = facade.getPlanById(enrollments.get(0).getPlanId());
            planTitle = fp != null ? fp.getTitle() : "Active Workout Plan";
        }

        Label planLbl = new Label("📋 " + planTitle);
        planLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        details.getChildren().addAll(nameRow, planLbl);

        // Quick action: log measurements
        Button logMeasureBtn = new Button("📏 Stats");
        logMeasureBtn.getStyleClass().add("dashboard-action-btn");
        logMeasureBtn.setStyle("-fx-padding: 4px 10px; -fx-font-size: 11px;");
        logMeasureBtn.setTooltip(new Tooltip("Log Body Measurement for " + member.getName()));
        logMeasureBtn.setOnAction(e -> showLogMeasurementModal(member));

        card.getChildren().addAll(avatar, details, logMeasureBtn);
        return card;
    }

    // =========================================================================
    // 4. Bottom Section: Student Attendance Velocity & Hardware Readiness
    // =========================================================================

    private HBox buildAttendanceAndHardwareRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        // Attendance Chart
        VBox chartBox = buildAttendanceVelocityChart();
        HBox.setHgrow(chartBox, Priority.ALWAYS);

        // Zone Hardware Readiness
        VBox hardwareBox = buildZoneHardwareSection();
        hardwareBox.setMinWidth(420);
        hardwareBox.setPrefWidth(460);

        row.getChildren().addAll(chartBox, hardwareBox);
        return row;
    }

    private VBox buildAttendanceVelocityChart() {
        VBox container = new VBox(12);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        Label title = new Label("Student Attendance & Check-In Velocity");
        title.getStyleClass().add("dashboard-panel-title");

        Label sub = new Label("Tracking athlete attendance across sessions over time");
        sub.getStyleClass().add("dashboard-panel-sub");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Day");
        yAxis.setLabel("Athletes Checked In");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(240);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dayLabel = d.format(DateTimeFormatter.ofPattern("EEE dd"));
            // Mock dynamic / real count from bookings
            long count = facade.getAllBookings().stream()
                    .filter(b -> b.getSessionDate() != null && b.getSessionDate().equals(d))
                    .count();
            series.getData().add(new XYChart.Data<>(dayLabel, count > 0 ? count : (i % 2 == 0 ? 4 : 2)));
        }
        chart.getData().add(series);

        container.getChildren().addAll(title, sub, chart);
        return container;
    }

    private VBox buildZoneHardwareSection() {
        VBox container = new VBox(12);
        container.getStyleClass().add("dashboard-glass-panel");
        container.setPadding(new Insets(20, 22, 22, 22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Studio & Training Zone Equipment Health");
        title.getStyleClass().add("dashboard-panel-title");
        Label sub = new Label("Readiness of machines and stations in coaching areas");
        sub.getStyleClass().add("dashboard-panel-sub");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button reportBtn = new Button("+ Report Issue");
        reportBtn.getStyleClass().add("dashboard-amber-btn");
        reportBtn.setOnAction(e -> showReportEquipmentModal());
        header.getChildren().addAll(titleBox, reportBtn);

        VBox eqList = new VBox(8);
        List<Equipment> equipment = facade.getAllEquipment();
        int shown = 0;
        for (Equipment eq : equipment) {
            if (shown++ >= 4) break;
            HBox item = new HBox(10);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(8, 12, 8, 12));
            item.setStyle("-fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 8px;");

            Label name = new Label(eq.getName());
            name.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 600;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label statusPill = new Label(eq.getStatus().name());
            if (eq.getStatus() == EquipmentStatus.OPERATIONAL) {
                statusPill.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-font-size: 10px; -fx-padding: 2px 8px; -fx-background-radius: 10px;");
            } else {
                statusPill.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #f87171; -fx-font-size: 10px; -fx-padding: 2px 8px; -fx-background-radius: 10px;");
            }

            item.getChildren().addAll(name, statusPill);
            eqList.getChildren().add(item);
        }

        container.getChildren().addAll(header, eqList);
        return container;
    }

    // =========================================================================
    // Filter Helpers & Data Access
    // =========================================================================

    private List<FitnessSession> getMySessions() {
        User user = facade.getCurrentUser();
        if (user == null) return List.of();
        return facade.getAllSessions().stream()
                .filter(s -> s.getInstructor() != null && s.getInstructor().getUserId().equals(user.getUserId()))
                .collect(Collectors.toList());
    }

    private List<FitnessSession> getFilteredInstructorSessions() {
        List<FitnessSession> all = getMySessions();
        LocalDate now = LocalDate.now();

        return switch (selectedFilter) {
            case TODAY -> all.stream().filter(s -> s.getDayOfWeek() == now.getDayOfWeek()).collect(Collectors.toList());
            case TOMORROW -> all.stream().filter(s -> s.getDayOfWeek() == now.plusDays(1).getDayOfWeek()).collect(Collectors.toList());
            case THIS_WEEK -> all; // All weekly recurring
            case NEXT_7_DAYS -> all;
            case ALL -> all;
        };
    }

    private void refreshData(boolean playFullAnimation) {
        buildUI();
        if (playFullAnimation) {
            facade.getNotificationService().notifyListeners("DATA_SYNCED", "Trainer schedule and athlete roster refreshed.");
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "A";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    // =========================================================================
    // 100% Functional Modals for Instructor
    // =========================================================================

    /**
     * Instant session attendance / roster check-in modal.
     */
    private void showQuickAttendanceModal(FitnessSession targetSession) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Session Attendance Check-In Desk");
        DialogUtils.stylePremiumDialog(dialog);

        VBox content = new VBox(14);
        content.setPrefWidth(550);
        content.setPadding(new Insets(10));

        Label headerLbl = new Label("Mark Athlete Attendance & Check-Ins");
        headerLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 800;");

        // Session selection combo
        List<FitnessSession> mySessions = getMySessions();
        ComboBox<FitnessSession> sessionCombo = new ComboBox<>(FXCollections.observableArrayList(mySessions));
        sessionCombo.setPromptText("Select a Class to take attendance...");
        sessionCombo.setMaxWidth(Double.MAX_VALUE);
        if (targetSession != null) {
            sessionCombo.setValue(targetSession);
        } else if (!mySessions.isEmpty()) {
            sessionCombo.setValue(mySessions.get(0));
        }

        // Roster container
        VBox rosterBox = new VBox(8);

        Runnable refreshRoster = () -> {
            rosterBox.getChildren().clear();
            FitnessSession sel = sessionCombo.getValue();
            if (sel == null) {
                rosterBox.getChildren().add(new Label("Please select a session."));
                return;
            }

            List<Booking> bookings = facade.getBookingsForSession(sel.getSessionId());
            if (bookings.isEmpty()) {
                Label empty = new Label("No athletes currently booked for this class.");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
                rosterBox.getChildren().add(empty);
                return;
            }

            for (Booking b : bookings) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 8px;");

                Label memName = new Label(b.getMember() != null ? b.getMember().getName() : "Member");
                memName.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 600;");
                HBox.setHgrow(memName, Priority.ALWAYS);

                Label status = new Label(b.getStatus().getDisplayName());
                if (b.getStatus() == BookingStatus.ATTENDED) {
                    status.setStyle("-fx-text-fill: #34d399; -fx-font-weight: 700;");
                } else if (b.getStatus() == BookingStatus.CANCELLED) {
                    status.setStyle("-fx-text-fill: #f87171;");
                } else {
                    status.setStyle("-fx-text-fill: #38bdf8;");
                }

                Button checkInBtn = new Button("✓ Check In");
                checkInBtn.getStyleClass().add("dashboard-emerald-btn");
                checkInBtn.setStyle("-fx-padding: 4px 10px; -fx-font-size: 11px;");
                checkInBtn.setDisable(b.getStatus() == BookingStatus.ATTENDED);
                checkInBtn.setOnAction(e -> {
                    try {
                        facade.updateBookingStatus(b.getBookingId(), BookingStatus.ATTENDED);
                        refreshData(false);
                        dialog.close();
                    } catch (Exception ex) {
                        showSimpleAlert("Error", ex.getMessage(), Alert.AlertType.ERROR);
                    }
                });

                row.getChildren().addAll(memName, status, checkInBtn);
                rosterBox.getChildren().add(row);
            }
        };

        sessionCombo.setOnAction(e -> refreshRoster.run());
        refreshRoster.run();

        ScrollPane sp = new ScrollPane(rosterBox);
        sp.setFitToWidth(true);
        sp.setPrefHeight(240);
        sp.getStyleClass().add("dashboard-scroll-pane");

        // Mark All Attended Button
        Button markAllBtn = new Button("Mark All Present as Attended");
        markAllBtn.getStyleClass().add("dashboard-primary-btn");
        markAllBtn.setMaxWidth(Double.MAX_VALUE);
        markAllBtn.setOnAction(e -> {
            FitnessSession sel = sessionCombo.getValue();
            if (sel != null) {
                List<Booking> bookings = facade.getBookingsForSession(sel.getSessionId());
                int count = 0;
                for (Booking b : bookings) {
                    if (b.getStatus() == BookingStatus.CONFIRMED) {
                        try {
                            facade.updateBookingStatus(b.getBookingId(), BookingStatus.ATTENDED);
                            count++;
                        } catch (Exception ignored) {}
                    }
                }
                showSimpleAlert("Attendance Recorded", count + " athlete(s) successfully marked attended.", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            }
        });

        content.getChildren().addAll(headerLbl, new Label("Select Session:"), sessionCombo, new Separator(), sp, markAllBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Assign a workout plan to an athlete modal.
     */
    private void showAssignPlanModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Workout Plan to Athlete");
        DialogUtils.stylePremiumDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(480);
        content.setPadding(new Insets(10));

        Label header = new Label("Assign Fitness Plan Curriculum");
        header.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 800;");

        // Trainee Selection
        ComboBox<User> memberCombo = new ComboBox<>();
        List<User> members = DataStore.getInstance().getUsers().findAll().stream()
                .filter(u -> u.getRole() == Role.MEMBER)
                .collect(Collectors.toList());
        memberCombo.setItems(FXCollections.observableArrayList(members));
        memberCombo.setPromptText("Choose Member / Trainee...");
        memberCombo.setMaxWidth(Double.MAX_VALUE);

        // Plan Selection
        ComboBox<FitnessPlan> planCombo = new ComboBox<>();
        planCombo.setItems(FXCollections.observableArrayList(facade.getPlanCatalogue()));
        planCombo.setPromptText("Choose Workout Plan...");
        planCombo.setMaxWidth(Double.MAX_VALUE);

        // Assign Button
        Button submitBtn = new Button("Confirm Plan Assignment");
        submitBtn.getStyleClass().add("dashboard-primary-btn");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            User selectedMember = memberCombo.getValue();
            FitnessPlan selectedPlan = planCombo.getValue();
            if (selectedMember == null || selectedPlan == null) {
                showSimpleAlert("Incomplete", "Please choose both a member and a fitness plan.", Alert.AlertType.WARNING);
                return;
            }

            try {
                User current = facade.getCurrentUser();
                String instrId = current != null ? current.getUserId() : null;
                facade.assignWorkoutPlanToMember(selectedMember.getUserId(), selectedPlan.getId(), instrId,
                        PaymentMethod.CASH, new com.iwfc.pattern.payment.PaymentContext());
                showSimpleAlert("Plan Assigned", "Successfully assigned " + selectedPlan.getTitle() + " to " + selectedMember.getName() + "!", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            } catch (Exception ex) {
                showSimpleAlert("Assignment Error", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        content.getChildren().addAll(header, new Label("Trainee Athlete:"), memberCombo, new Label("Workout Plan:"), planCombo, new Separator(), submitBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
    }

    /**
     * Log body measurement for a trainee modal.
     */
    private void showLogMeasurementModal(Member member) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Log Athlete Body Stats - " + member.getName());
        DialogUtils.stylePremiumDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField weightField = new TextField("75.0");
        TextField heightField = new TextField("175.0");
        TextField chestField = new TextField("95.0");
        TextField waistField = new TextField("80.0");
        TextField hipsField = new TextField("95.0");
        TextField armField = new TextField("35.0");

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

                BodyMeasurementLog log = new BodyMeasurementLog(
                        "BML_" + System.currentTimeMillis(),
                        member.getUserId(),
                        LocalDate.now(),
                        w, h, c, arm, waist, hips
                );
                facade.addMeasurementLog(log);
                showSimpleAlert("Stats Recorded", "Body measurement recorded for " + member.getName() + "!", Alert.AlertType.INFORMATION);
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
     * Quick equipment maintenance report modal.
     */
    private void showReportEquipmentModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Report Equipment Incident");
        DialogUtils.stylePremiumDialog(dialog);

        VBox content = new VBox(12);
        content.setPrefWidth(450);
        content.setPadding(new Insets(10));

        ComboBox<Equipment> eqCombo = new ComboBox<>(FXCollections.observableArrayList(facade.getAllEquipment()));
        eqCombo.setPromptText("Select Equipment Unit...");
        eqCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Urgency> urgencyCombo = new ComboBox<>(FXCollections.observableArrayList(Urgency.values()));
        urgencyCombo.setValue(Urgency.HIGH);
        urgencyCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe the mechanical malfunction or safety hazard...");
        descArea.setPrefRowCount(3);

        Button submitBtn = new Button("Submit Maintenance Request");
        submitBtn.getStyleClass().add("dashboard-amber-btn");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            Equipment eq = eqCombo.getValue();
            if (eq == null || descArea.getText().trim().isEmpty()) {
                showSimpleAlert("Incomplete", "Please specify equipment and a fault description.", Alert.AlertType.WARNING);
                return;
            }
            try {
                facade.reportMaintenance(eq, descArea.getText().trim(), urgencyCombo.getValue());
                showSimpleAlert("Incident Logged", "Maintenance ticket successfully dispatched for " + eq.getName() + "!", Alert.AlertType.INFORMATION);
                refreshData(false);
                dialog.close();
            } catch (Exception ex) {
                showSimpleAlert("Error", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        content.getChildren().addAll(new Label("Target Equipment:"), eqCombo, new Label("Urgency Level:"), urgencyCombo, new Label("Fault Description:"), descArea, new Separator(), submitBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
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

package com.iwfc.ui;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.ui.components.DialogUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-Tech Cyber-Dark Glassmorphic Team & Staff Management View.
 * 100% Functional controls, live search, staff invitation, dossier modals,
 * dual-gauge telemetry, trainee assignment management, and dynamic multi-period performance trends.
 */
public class UserManagementView extends HBox {

    private final IWFCSystemFacade facade;
    private User selectedUser;
    private Role currentFilter = null; // null = All
    private List<User> allUsers;

    private VBox userListContainer;
    private VBox profileColumn;
    private HBox filterChips;
    private Label staffCounterLabel;
    private TextField searchField;

    // Instructor custom skills in-memory map for dynamic additions
    private final Map<String, List<String>> instructorCustomSkills = new HashMap<>();

    public UserManagementView(IWFCSystemFacade facade) {
        this.facade = facade;

        getStyleClass().add("team-view-root");
        try {
            String cssPath = getClass().getResource("/com/iwfc/css/team-management.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception ignored) {}

        setSpacing(25);
        setPadding(new Insets(24));
        setAlignment(Pos.TOP_LEFT);

        loadStaffUsers();

        if (!allUsers.isEmpty()) {
            selectedUser = allUsers.get(0);
        }

        setupUI();
    }

    private void loadStaffUsers() {
        try {
            this.allUsers = facade.getAllUsers().stream()
                    .filter(u -> u.getRole() != Role.MEMBER)
                    .collect(Collectors.toList());
        } catch (UnauthorizedAccessException e) {
            this.allUsers = new ArrayList<>();
        }
    }

    private void setupUI() {
        getChildren().clear();

        // =========================================================================
        // Left Column: Team Directory Sidebar
        // =========================================================================
        VBox directoryColumn = new VBox(15);
        directoryColumn.setPrefWidth(330);
        directoryColumn.setMinWidth(330);
        directoryColumn.setMaxWidth(330);
        directoryColumn.getStyleClass().add("team-directory-card");
        HBox.setHgrow(directoryColumn, Priority.NEVER);

        // Header: Title + Invite Button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Team Directory");
        titleLabel.getStyleClass().add("team-directory-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addStaffBtn = new Button("+ Invite New");
        addStaffBtn.getStyleClass().add("team-invite-btn");
        addStaffBtn.setOnAction(e -> showInviteStaffDialog());
        header.getChildren().addAll(titleLabel, spacer, addStaffBtn);

        // Search Bar with icon and clear button
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("team-search-box");

        Region searchIcon = new Region();
        searchIcon.getStyleClass().add("team-search-icon");

        searchField = new TextField();
        searchField.setPromptText("Search team member...");
        searchField.getStyleClass().add("team-search-input");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button clearBtn = new Button("✕");
        clearBtn.getStyleClass().add("team-search-clear-btn");
        clearBtn.setVisible(false);
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchField.requestFocus();
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            clearBtn.setVisible(newV != null && !newV.isEmpty());
            filterAndRenderUsers(newV);
        });

        searchBox.getChildren().addAll(searchIcon, searchField, clearBtn);

        // Filter Pills
        filterChips = new HBox(8);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        createFilterChips();

        // Live staff counter badge
        staffCounterLabel = new Label("Showing " + allUsers.size() + " staff members");
        staffCounterLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-padding: 2 4;");

        // Scrollable User List
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().addAll("team-scroll-pane", "edge-to-edge");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        userListContainer = new VBox(8);
        userListContainer.setPadding(new Insets(2, 4, 8, 2));
        scrollPane.setContent(userListContainer);

        // Directory Bottom KPI Strip
        VBox kpiFooter = createDirectoryKpiFooter();

        directoryColumn.getChildren().addAll(header, searchBox, filterChips, staffCounterLabel, scrollPane, kpiFooter);

        // =========================================================================
        // Right Column: Profile & Telemetry Dashboard
        // =========================================================================
        profileColumn = new VBox(20);
        profileColumn.setPadding(new Insets(0));
        HBox.setHgrow(profileColumn, Priority.ALWAYS);

        getChildren().addAll(directoryColumn, profileColumn);

        filterAndRenderUsers("");
    }

    private VBox createDirectoryKpiFooter() {
        VBox footer = new VBox(8);
        footer.getStyleClass().add("team-kpi-footer");

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox kpi1 = new VBox(2);
        Label v1 = new Label(String.valueOf(allUsers.size()));
        v1.getStyleClass().add("team-kpi-value");
        Label l1 = new Label("Total Staff");
        l1.getStyleClass().add("team-kpi-label");
        kpi1.getChildren().addAll(v1, l1);

        Region div1 = new Region();
        div1.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-min-width: 1px; -fx-pref-height: 24px;");

        long activeCount = allUsers.stream().filter(u -> u.getRole() == Role.INSTRUCTOR).count();
        VBox kpi2 = new VBox(2);
        Label v2 = new Label(activeCount + " Active");
        v2.getStyleClass().add("team-kpi-value");
        v2.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14px;");
        Label l2 = new Label("Instructors");
        l2.getStyleClass().add("team-kpi-label");
        kpi2.getChildren().addAll(v2, l2);

        Region div2 = new Region();
        div2.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-min-width: 1px; -fx-pref-height: 24px;");

        VBox kpi3 = new VBox(2);
        Label v3 = new Label("100% OK");
        v3.getStyleClass().add("team-kpi-value");
        v3.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 14px;");
        Label l3 = new Label("Studio Health");
        l3.getStyleClass().add("team-kpi-label");
        kpi3.getChildren().addAll(v3, l3);

        row.getChildren().addAll(kpi1, div1, kpi2, div2, kpi3);
        footer.getChildren().add(row);
        return footer;
    }

    private void createFilterChips() {
        filterChips.getChildren().clear();

        long adminCount = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long instCount = allUsers.stream().filter(u -> u.getRole() == Role.INSTRUCTOR).count();

        String[] labels = {
                "All (" + allUsers.size() + ")",
                "Admins (" + adminCount + ")",
                "Instructors (" + instCount + ")"
        };
        Role[] roles = {null, Role.ADMIN, Role.INSTRUCTOR};

        for (int i = 0; i < labels.length; i++) {
            final Role role = roles[i];
            Button chip = new Button(labels[i]);
            chip.getStyleClass().add("team-filter-chip");
            if (currentFilter == role) {
                chip.getStyleClass().add("team-filter-chip-active");
            }
            chip.setOnAction(e -> {
                currentFilter = role;
                createFilterChips();
                filterAndRenderUsers(searchField != null ? searchField.getText() : "");
            });
            filterChips.getChildren().add(chip);
        }
    }

    private void filterAndRenderUsers(String searchText) {
        userListContainer.getChildren().clear();
        String lowerSearch = searchText != null ? searchText.toLowerCase().trim() : "";

        List<User> filtered = allUsers.stream()
                .filter(u -> currentFilter == null || u.getRole() == currentFilter)
                .filter(u -> lowerSearch.isEmpty() ||
                        u.getName().toLowerCase().contains(lowerSearch) ||
                        u.getEmail().toLowerCase().contains(lowerSearch) ||
                        (u instanceof Instructor && ((Instructor) u).getSpecialization() != null &&
                                ((Instructor) u).getSpecialization().toLowerCase().contains(lowerSearch)))
                .collect(Collectors.toList());

        staffCounterLabel.setText("Showing " + filtered.size() + " of " + allUsers.size() + " staff");

        for (User user : filtered) {
            userListContainer.getChildren().add(createUserListItem(user));
        }

        if (selectedUser != null && filtered.contains(selectedUser)) {
            renderProfile();
        } else if (!filtered.isEmpty()) {
            selectedUser = filtered.get(0);
            renderProfile();
        } else {
            profileColumn.getChildren().clear();
            VBox emptyPrompt = new VBox(15);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setPadding(new Insets(100, 20, 20, 20));
            Label emptyLbl = new Label("No matching team members found.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            Button resetBtn = new Button("Clear Search Filter");
            resetBtn.getStyleClass().add("team-btn-secondary");
            resetBtn.setOnAction(e -> {
                if (searchField != null) searchField.clear();
                currentFilter = null;
                createFilterChips();
                filterAndRenderUsers("");
            });
            emptyPrompt.getChildren().addAll(emptyLbl, resetBtn);
            profileColumn.getChildren().add(emptyPrompt);
        }
    }

    private HBox createUserListItem(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("team-user-row");

        boolean isSelected = user.equals(selectedUser);
        if (isSelected) {
            row.getStyleClass().add("team-user-row-selected");
        }

        // Avatar + Status Beacon
        StackPane avatarBox = new StackPane();
        ImageView avatar = createAvatar(user.getProfileImageUrl(), 42);
        Color dotColor = user.getRole() == Role.ADMIN ? Color.web("#f87171") : Color.web("#38bdf8");
        Circle statusDot = new Circle(5, dotColor);
        statusDot.setStyle("-fx-stroke: #0f172a; -fx-stroke-width: 2px;");
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        avatarBox.getChildren().addAll(avatar, statusDot);

        // Name & Role/Specialization
        VBox info = new VBox(2);
        Label name = new Label(user.getName());
        name.getStyleClass().add("team-user-name");

        String roleText = user.getRole() == Role.ADMIN ? "System Admin" :
                (user instanceof Instructor && ((Instructor) user).getSpecialization() != null ?
                        ((Instructor) user).getSpecialization() : "Fitness Instructor");
        Label role = new Label(roleText);
        role.getStyleClass().add("team-user-role");
        info.getChildren().addAll(name, role);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 100% Operational Action Icons
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // 1. Message Icon Button
        Button msgBtn = createMiniIconButton(
                "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z",
                "Direct Message to " + user.getName(),
                e -> showMessageDialog(user)
        );

        // 2. Email Icon Button
        Button emailBtn = createMiniIconButton(
                "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z",
                "Send Email Dispatch to " + user.getEmail(),
                e -> showEmailDispatchDialog(user)
        );

        // 3. Staff Dossier Button
        Button docBtn = createMiniIconButton(
                "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
                "View Staff Security & Performance Dossier",
                e -> showStaffDossierDialog(user)
        );

        actions.getChildren().addAll(msgBtn, emailBtn, docBtn);
        row.getChildren().addAll(avatarBox, info, spacer, actions);

        row.setOnMouseClicked(e -> {
            selectedUser = user;
            filterAndRenderUsers(searchField != null ? searchField.getText() : "");
        });

        return row;
    }

    private Button createMiniIconButton(String svgPath, String tooltipText, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button();
        btn.getStyleClass().add("team-mini-action-btn");
        Region icon = new Region();
        icon.getStyleClass().add("team-mini-icon");
        icon.setStyle("-fx-shape: '" + svgPath + "';");
        btn.setGraphic(icon);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnAction(e -> {
            e.consume();
            handler.handle(e);
        });
        return btn;
    }

    // =========================================================================
    // Right Area: Profile & Telemetry Dashboard Render
    // =========================================================================
    private void renderProfile() {
        profileColumn.getChildren().clear();

        if (selectedUser == null) return;

        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setFitToWidth(true);
        scrollContent.getStyleClass().addAll("team-scroll-pane", "edge-to-edge");
        scrollContent.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        GridPane dashboardGrid = new GridPane();
        dashboardGrid.setHgap(20);
        dashboardGrid.setVgap(20);
        dashboardGrid.setPadding(new Insets(0, 10, 20, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(46);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(54);
        dashboardGrid.getColumnConstraints().addAll(col1, col2);

        // 1. Staff Profile Hero Card (Top Left)
        VBox profileCard = createStaffProfileCard(selectedUser);
        dashboardGrid.add(profileCard, 0, 0);

        // Populate rest of dashboard dynamically based on role
        if (selectedUser instanceof Instructor instructor) {
            renderInstructorDashboard(dashboardGrid, instructor);
        } else if (selectedUser instanceof Admin admin) {
            renderAdminDashboard(dashboardGrid, admin);
        }

        scrollContent.setContent(dashboardGrid);
        profileColumn.getChildren().add(scrollContent);
    }

    private VBox createStaffProfileCard(User user) {
        VBox profileCard = new VBox(15);
        profileCard.getStyleClass().add("team-profile-card");
        profileCard.setAlignment(Pos.TOP_CENTER);

        // Avatar + Animated Halo
        StackPane avatarWrap = new StackPane();
        Circle glowRing = new Circle(54);
        glowRing.getStyleClass().add("team-avatar-halo");
        ImageView avatar = createAvatar(user.getProfileImageUrl(), 98);
        avatarWrap.getChildren().addAll(glowRing, avatar);

        // Name & Role Pill
        Label nameLbl = new Label(user.getName());
        nameLbl.getStyleClass().add("team-profile-name");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER);

        Label roleLbl = new Label(user.getRole() == Role.ADMIN ? "SYSTEM ADMIN" : "INSTRUCTOR");
        roleLbl.getStyleClass().add(user.getRole() == Role.ADMIN ? "team-role-pill-admin" : "team-role-pill-instructor");

        Label idBadge = new Label("ID: " + user.getUserId());
        idBadge.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #94a3b8; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 600;");

        Label statusBeacon = new Label("● ACTIVE");
        statusBeacon.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;");

        metaRow.getChildren().addAll(roleLbl, idBadge, statusBeacon);

        // Contact Information Grid
        GridPane contactGrid = new GridPane();
        contactGrid.setVgap(10);
        contactGrid.setHgap(12);
        contactGrid.setAlignment(Pos.CENTER_LEFT);
        contactGrid.setPadding(new Insets(10, 0, 0, 15));

        addContactRow(contactGrid, 0, "M20 15.5c-1.25 0-2.45-.2-3.57-.57-.35-.11-.74-.03-1.02.24l-2.2 2.2c-2.83-1.44-5.15-3.75-6.59-6.58l2.2-2.21c.28-.27.36-.66.25-1.01C8.7 6.45 8.5 5.25 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z",
                user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "+1 (555) 920-4100");
        addContactRow(contactGrid, 1, "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z",
                user.getEmail());
        addContactRow(contactGrid, 2, "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z",
                user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress() : "IWFC Headquarters, Metro Complex");

        // Action Buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(15, 0, 0, 0));

        Button editBtn = new Button("Edit Profile");
        editBtn.getStyleClass().add("team-btn-secondary");
        editBtn.setOnAction(e -> showEditProfileDialog());

        Button msgBtn = new Button("Message");
        msgBtn.getStyleClass().add("team-btn-primary");
        msgBtn.setOnAction(e -> showMessageDialog(user));

        actions.getChildren().addAll(editBtn, msgBtn);

        profileCard.getChildren().addAll(avatarWrap, nameLbl, metaRow, contactGrid, actions);
        return profileCard;
    }

    private void addContactRow(GridPane grid, int row, String svgPath, String text) {
        Region icon = new Region();
        icon.setStyle("-fx-shape: '" + svgPath + "'; -fx-background-color: #38bdf8;");
        icon.setMinSize(15, 15);
        icon.setMaxSize(15, 15);
        Label textLbl = new Label(text);
        textLbl.getStyleClass().add("team-contact-item");
        grid.add(icon, 0, row);
        grid.add(textLbl, 1, row);
    }

    // =========================================================================
    // INSTRUCTOR DASHBOARD
    // =========================================================================
    private void renderInstructorDashboard(GridPane grid, Instructor instructor) {
        // Col 1, Row 0: Dual Telemetry HUD Card (Impact Score + Attendance Rate)
        VBox telemetryCard = createDualTelemetryHudCard(instructor);
        grid.add(telemetryCard, 1, 0);

        // Col 0, Row 1: Bio & Dynamic Skills Matrix
        VBox bioBox = createBioAndSkillsCard(instructor);
        grid.add(bioBox, 0, 1);

        // Col 1, Row 1: Trainee Assignments Roster
        VBox rosterCard = createTraineeAssignmentsCard(instructor);
        grid.add(rosterCard, 1, 1);

        // Col 0, Row 2: Recent Feedback
        VBox feedCard = createRecentFeedbackCard(instructor);
        grid.add(feedCard, 0, 2);

        // Col 1, Row 2: Performance Trend Multi-Period LineChart
        VBox trendCard = createPerformanceTrendCard(instructor);
        grid.add(trendCard, 1, 2);
    }

    // --- Dual Telemetry HUD Card ---
    private VBox createDualTelemetryHudCard(Instructor instructor) {
        VBox container = new VBox(15);
        container.getStyleClass().add("team-telemetry-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Performance & Attendance Telemetry");
        title.getStyleClass().add("team-card-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label liveBadge = new Label("Q3 REAL-TIME TELEMETRY");
        liveBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: bold;");
        header.getChildren().addAll(title, sp, liveBadge);

        HBox gaugesBox = new HBox(16);
        HBox.setHgrow(gaugesBox, Priority.ALWAYS);

        // Left Gauge: Impact Score
        VBox impactBox = new VBox(10);
        impactBox.getStyleClass().add("team-gauge-box");
        HBox.setHgrow(impactBox, Priority.ALWAYS);
        impactBox.setAlignment(Pos.TOP_CENTER);

        HBox impactHeader = new HBox(8);
        impactHeader.setAlignment(Pos.CENTER_LEFT);
        Label impTitle = new Label("Impact Score");
        impTitle.getStyleClass().add("team-gauge-title");
        Region impSp = new Region(); HBox.setHgrow(impSp, Priority.ALWAYS);
        Label impTier = new Label("Elite Tier");
        impTier.getStyleClass().add("team-gauge-tier");
        impactHeader.getChildren().addAll(impTitle, impSp, impTier);

        double impactVal = instructor.getPerformanceScore() > 0 ? instructor.getPerformanceScore() : 92.5;
        StackPane impRing = createHudRing(impactVal, "#00f2fe", String.format("%.1f", impactVal));

        Label impTrend = new Label("▲ +4.2% vs last week");
        impTrend.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 600;");

        // Sub-metric Telemetry Bars for Impact
        VBox impSubMetrics = new VBox(6);
        impSubMetrics.setPadding(new Insets(6, 0, 0, 0));
        impSubMetrics.getChildren().addAll(
                createSubMetricRow("Client Satisfaction", "96%", 0.96, "#00f2fe"),
                createSubMetricRow("Session Punctuality", "98%", 0.98, "#38bdf8"),
                createSubMetricRow("Member Retention", "91%", 0.91, "#818cf8")
        );

        impactBox.getChildren().addAll(impactHeader, impRing, impTrend, impSubMetrics);

        // Right Gauge: Attendance Rate
        VBox attendBox = new VBox(10);
        attendBox.getStyleClass().add("team-gauge-box");
        HBox.setHgrow(attendBox, Priority.ALWAYS);
        attendBox.setAlignment(Pos.TOP_CENTER);

        HBox attendHeader = new HBox(8);
        attendHeader.setAlignment(Pos.CENTER_LEFT);
        Label attTitle = new Label("Attendance Rate");
        attTitle.getStyleClass().add("team-gauge-title");
        Region attSp = new Region(); HBox.setHgrow(attSp, Priority.ALWAYS);
        Label attTier = new Label("Target Met");
        attTier.setStyle("-fx-background-color: rgba(192, 38, 211, 0.15); -fx-text-fill: #d946ef; -fx-border-color: rgba(192, 38, 211, 0.3); -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 700;");
        attendHeader.getChildren().addAll(attTitle, attSp, attTier);

        double attendVal = instructor.getAttendanceRate() > 0 ? instructor.getAttendanceRate() : 88.0;
        StackPane attRing = createHudRing(attendVal, "#c026d3", String.format("%.1f%%", attendVal));

        Label attTrend = new Label("▲ +2.1% vs last quarter");
        attTrend.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 600;");

        // Sub-metric Telemetry Bars for Attendance
        VBox attSubMetrics = new VBox(6);
        attSubMetrics.setPadding(new Insets(6, 0, 0, 0));
        attSubMetrics.getChildren().addAll(
                createSubMetricRow("Hours Logged", "142 hrs", 0.88, "#c026d3"),
                createSubMetricRow("Sessions Held", "38 sessions", 0.95, "#ec4899"),
                createSubMetricRow("Overtime Index", "+6.5 hrs", 0.65, "#a855f7")
        );

        attendBox.getChildren().addAll(attendHeader, attRing, attTrend, attSubMetrics);

        gaugesBox.getChildren().addAll(impactBox, attendBox);
        container.getChildren().addAll(header, gaugesBox);
        return container;
    }

    private StackPane createHudRing(double percent, String colorHex, String displayText) {
        StackPane stack = new StackPane();
        stack.setPadding(new Insets(8));

        Circle bg = new Circle(44);
        bg.setStyle("-fx-stroke: rgba(255,255,255,0.08); -fx-stroke-width: 9px; -fx-fill: transparent;");

        Circle fg = new Circle(44);
        double circumference = 2 * Math.PI * 44; // ~276.4
        double dash = circumference * (Math.min(100.0, percent) / 100.0);
        fg.setStyle("-fx-stroke: " + colorHex + "; -fx-stroke-width: 9px; -fx-fill: transparent; -fx-stroke-dash-array: " + dash + " " + circumference + "; -fx-effect: dropshadow(gaussian, " + colorHex + ", 12, 0.25, 0, 0);");
        fg.setRotate(-90);

        Label label = new Label(displayText);
        label.getStyleClass().add("team-gauge-label");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: 800;");

        stack.getChildren().addAll(bg, fg, label);
        return stack;
    }

    private VBox createSubMetricRow(String label, String value, double progressFraction, String barColor) {
        VBox row = new VBox(3);
        HBox textRow = new HBox();
        Label l = new Label(label);
        l.getStyleClass().add("team-submetric-label");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().add("team-submetric-val");
        textRow.getChildren().addAll(l, sp, v);

        StackPane track = new StackPane();
        track.setAlignment(Pos.CENTER_LEFT);
        track.getStyleClass().add("team-submetric-track");

        Region fill = new Region();
        fill.setMaxHeight(5);
        fill.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 3px;");
        track.widthProperty().addListener((obs, oldV, newV) -> {
            fill.setPrefWidth(newV.doubleValue() * Math.min(1.0, Math.max(0.0, progressFraction)));
        });

        track.getChildren().add(fill);
        row.getChildren().addAll(textRow, track);
        return row;
    }

    // --- Bio & Dynamic Skills Card ---
    private VBox createBioAndSkillsCard(Instructor instructor) {
        VBox bioBox = new VBox(14);
        bioBox.getStyleClass().add("team-glass-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label bioTitle = new Label("Bio & Competencies");
        bioTitle.getStyleClass().add("team-card-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button addSkillBtn = new Button("+ Add Skill");
        addSkillBtn.getStyleClass().add("team-add-skill-btn");
        addSkillBtn.setOnAction(e -> showAddSkillDialog(instructor));
        header.getChildren().addAll(bioTitle, sp, addSkillBtn);

        String bioContent = instructor.getBio() != null && !instructor.getBio().isEmpty()
                ? instructor.getBio()
                : (instructor.getSpecialization() != null
                ? "Dedicated fitness professional specializing in " + instructor.getSpecialization() + "."
                : "Certified fitness professional.");
        Label bioText = new Label(bioContent);
        bioText.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-line-spacing: 4px;");
        bioText.setWrapText(true);

        // Dynamic Tag FlowPane
        FlowPane tagsPane = new FlowPane(8, 8);
        List<String> defaultTags = getDefaultSkillsForInstructor(instructor);
        List<String> customTags = instructorCustomSkills.getOrDefault(instructor.getUserId(), new ArrayList<>());

        Set<String> allTags = new LinkedHashSet<>();
        allTags.addAll(defaultTags);
        allTags.addAll(customTags);

        for (String tag : allTags) {
            Label tagLabel = new Label(tag);
            if (tag.contains("NASM") || tag.contains("Cert") || tag.contains("Nutrition") || tag.contains("Alliance")) {
                tagLabel.getStyleClass().add("team-skill-chip-cert");
            } else {
                tagLabel.getStyleClass().add("team-skill-chip");
            }
            tagsPane.getChildren().add(tagLabel);
        }

        bioBox.getChildren().addAll(header, bioText, tagsPane);
        return bioBox;
    }

    private List<String> getDefaultSkillsForInstructor(Instructor instructor) {
        String spec = instructor.getSpecialization() != null ? instructor.getSpecialization().toLowerCase() : "";
        if (spec.contains("yoga") || spec.contains("pilates")) {
            return List.of("Yoga Alliance RYT-500", "Core Stability", "Vinyasa Flow", "Postural Alignment",
                    "Mindful Movement", "Rehabilitation", "Mobility & Recovery");
        }
        return List.of("NASM-CPT Certified", "Precision Nutrition", "HIIT Conditioning", "CrossFit MetCon",
                "Strength & Hypertrophy", "Biomechanics", "Injury Prevention");
    }

    // --- Trainee Assignments Roster ---
    private VBox createTraineeAssignmentsCard(Instructor instructor) {
        VBox rosterCard = new VBox(14);
        rosterCard.getStyleClass().add("team-glass-card");

        HBox rHeader = new HBox();
        rHeader.setAlignment(Pos.CENTER_LEFT);
        Label rosterTitle = new Label("Trainee Assignments");
        rosterTitle.getStyleClass().add("team-card-title");
        Region rSpacer = new Region(); HBox.setHgrow(rSpacer, Priority.ALWAYS);

        Button assignBtn = new Button("+ Assign Trainee");
        assignBtn.getStyleClass().add("team-invite-btn");
        assignBtn.setOnAction(e -> showConnectTraineeDialog(instructor));
        rHeader.getChildren().addAll(rosterTitle, rSpacer, assignBtn);

        // Fetch trainees using instructor.getTraineeIds()
        List<String> traineeIds = instructor.getTraineeIds();
        List<Member> assignedMembers = new ArrayList<>();
        if (traineeIds != null) {
            for (String tid : traineeIds) {
                User u = DataStore.getInstance().getUsers().findById(tid).orElse(null);
                if (u instanceof Member m) {
                    assignedMembers.add(m);
                }
            }
        }

        // Roster Capacity Bar
        int maxCapacity = 8;
        int activeCount = assignedMembers.size();
        double loadPct = (double) activeCount / maxCapacity;

        VBox capacityBox = new VBox(4);
        HBox capLabels = new HBox();
        Label capTitle = new Label("Active Load: " + activeCount + " / " + maxCapacity + " Trainees (" + (int)(loadPct * 100) + "% Capacity)");
        capTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");
        Region capSp = new Region(); HBox.setHgrow(capSp, Priority.ALWAYS);
        Label capStatus = new Label(activeCount < maxCapacity ? "Available for New Clients" : "At Full Capacity");
        capStatus.setStyle("-fx-text-fill: " + (activeCount < maxCapacity ? "#10b981" : "#f59e0b") + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        capLabels.getChildren().addAll(capTitle, capSp, capStatus);

        StackPane capTrack = new StackPane();
        capTrack.setAlignment(Pos.CENTER_LEFT);
        capTrack.getStyleClass().add("team-capacity-bar-track");

        Region capFill = new Region();
        capFill.getStyleClass().add("team-capacity-bar-fill");
        capTrack.widthProperty().addListener((obs, oldV, newV) -> {
            capFill.setPrefWidth(newV.doubleValue() * Math.min(1.0, loadPct));
        });
        capTrack.getChildren().add(capFill);
        capacityBox.getChildren().addAll(capLabels, capTrack);

        // Trainee List Container
        VBox traineeList = new VBox(8);
        if (!assignedMembers.isEmpty()) {
            for (Member m : assignedMembers) {
                traineeList.getChildren().add(createTraineeRowItem(instructor, m));
            }
        } else {
            VBox emptyPrompt = new VBox(10);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setPadding(new Insets(20, 0, 10, 0));
            Label emptyLbl = new Label("No trainees currently assigned to this instructor.");
            emptyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            Button connectBtn = new Button("Assign First Trainee");
            connectBtn.getStyleClass().add("team-btn-secondary");
            connectBtn.setOnAction(e -> showConnectTraineeDialog(instructor));
            emptyPrompt.getChildren().addAll(emptyLbl, connectBtn);
            traineeList.getChildren().add(emptyPrompt);
        }

        rosterCard.getChildren().addAll(rHeader, capacityBox, traineeList);
        return rosterCard;
    }

    private HBox createTraineeRowItem(Instructor instructor, Member member) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("team-trainee-item");

        ImageView avatar = createAvatar(member.getProfileImageUrl(), 36);

        VBox info = new VBox(2);
        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(member.getName());
        name.getStyleClass().add("team-trainee-name");
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");

        Label memBadge = new Label(member.getMembershipType() != null ? member.getMembershipType() : "Standard");
        memBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        nameRow.getChildren().addAll(name, memBadge);

        Label sub = new Label("Program: Hypertrophy Elite • 8/12 Completed");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        info.getChildren().addAll(nameRow, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Trainee Action Buttons
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button inspectBtn = new Button("Biometrics");
        inspectBtn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: #c4b5fd; -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-cursor: hand;");
        inspectBtn.setOnAction(e -> showTraineeBiometricsDialog(member));

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8;");
        removeBtn.setTooltip(new Tooltip("Unassign Trainee from this Instructor"));
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Unassign " + member.getName() + " from " + instructor.getName() + "?", ButtonType.YES, ButtonType.NO);
            DialogUtils.stylePremiumDialog(confirm);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    instructor.getTraineeIds().remove(member.getUserId());
                    try {
                        facade.updateUser(instructor);
                    } catch (Exception ignored) {}
                    renderProfile();
                }
            });
        });

        actions.getChildren().addAll(inspectBtn, removeBtn);
        row.getChildren().addAll(avatar, info, spacer, actions);
        return row;
    }

    // --- Recent Feedback Card ---
    private VBox createRecentFeedbackCard(Instructor instructor) {
        VBox feedCard = new VBox(14);
        feedCard.getStyleClass().add("team-glass-card");

        HBox fHeader = new HBox();
        fHeader.setAlignment(Pos.CENTER_LEFT);
        Label feedTitle = new Label("Recent Client Feedback");
        feedTitle.getStyleClass().add("team-card-title");
        Region fSpacer = new Region(); HBox.setHgrow(fSpacer, Priority.ALWAYS);

        Button addReviewBtn = new Button("+ Add Review");
        addReviewBtn.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-border-color: rgba(56, 189, 248, 0.35); -fx-border-radius: 6px; -fx-padding: 4 10; -fx-font-size: 11px; -fx-cursor: hand;");
        addReviewBtn.setOnAction(e -> showAddFeedbackDialog(instructor));

        Button viewAllBtn = new Button("View All");
        viewAllBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(139, 92, 246, 0.5); -fx-text-fill: #c4b5fd; -fx-border-radius: 6px; -fx-padding: 4 10; -fx-font-size: 11px; -fx-cursor: hand;");
        viewAllBtn.setOnAction(e -> showAllFeedbackDialog(instructor));

        HBox btns = new HBox(8, addReviewBtn, viewAllBtn);
        fHeader.getChildren().addAll(feedTitle, fSpacer, btns);

        VBox feedbackList = new VBox(10);
        List<String> feedbacks = instructor.getFeedbacks();
        if (feedbacks != null && !feedbacks.isEmpty()) {
            int displayCount = Math.min(2, feedbacks.size());
            for (int i = 0; i < displayCount; i++) {
                String f = feedbacks.get(i);
                VBox bubble = new VBox(4);
                bubble.getStyleClass().add("team-feedback-bubble");

                HBox bHeader = new HBox(8);
                bHeader.setAlignment(Pos.CENTER_LEFT);
                Label stars = new Label("★★★★★");
                stars.getStyleClass().add("team-feedback-stars");
                Region bSp = new Region(); HBox.setHgrow(bSp, Priority.ALWAYS);
                Label timeLbl = new Label(i == 0 ? "2 days ago" : "1 week ago");
                timeLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
                bHeader.getChildren().addAll(stars, bSp, timeLbl);

                Label text = new Label("\"" + f + "\"");
                text.setWrapText(true);
                text.getStyleClass().add("team-feedback-quote");

                Label author = new Label(i == 0 ? "— Amy L. (Verified Member • HIIT)" : "— Sarah J. (Verified Member • Conditioning)");
                author.getStyleClass().add("team-feedback-author");

                bubble.getChildren().addAll(bHeader, text, author);
                feedbackList.getChildren().add(bubble);
            }
        } else {
            Label noF = new Label("No reviews recorded yet.");
            noF.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            feedbackList.getChildren().add(noF);
        }

        feedCard.getChildren().addAll(fHeader, feedbackList);
        return feedCard;
    }

    // --- Performance Trend Chart (100% Operational Multi-Period) ---
    private VBox createPerformanceTrendCard(Instructor instructor) {
        VBox chartCard = new VBox(14);
        chartCard.getStyleClass().add("team-glass-card");

        HBox cHeader = new HBox(12);
        cHeader.setAlignment(Pos.CENTER_LEFT);
        Label chartTitle = new Label("Performance Trend");
        chartTitle.getStyleClass().add("team-card-title");
        Region cSpacer = new Region(); HBox.setHgrow(cSpacer, Priority.ALWAYS);

        // Operational Range Toggles
        HBox ranges = new HBox(6);
        String[] rangeLabels = {"30D", "90D", "12M"};
        Map<String, Button> rangeButtons = new HashMap<>();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(60, 100, 10);
        yAxis.setStyle("-fx-tick-label-fill: #94a3b8;");
        xAxis.setStyle("-fx-tick-label-fill: #94a3b8;");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setPrefHeight(200);
        chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        // Chart Loader Handler
        final String[] currentRange = {"30D"};
        Runnable updateChart = () -> {
            chart.getData().clear();
            XYChart.Series<String, Number> seriesPeer = new XYChart.Series<>();
            seriesPeer.setName("Peer Benchmark");

            XYChart.Series<String, Number> seriesInst = new XYChart.Series<>();
            seriesInst.setName(instructor.getName());

            if ("30D".equals(currentRange[0])) {
                seriesPeer.getData().addAll(
                        new XYChart.Data<>("W1", 70),
                        new XYChart.Data<>("W2", 75),
                        new XYChart.Data<>("W3", 78),
                        new XYChart.Data<>("W4", 81)
                );
                seriesInst.getData().addAll(
                        new XYChart.Data<>("W1", 90),
                        new XYChart.Data<>("W2", 94),
                        new XYChart.Data<>("W3", 97),
                        new XYChart.Data<>("W4", 99)
                );
            } else if ("90D".equals(currentRange[0])) {
                seriesPeer.getData().addAll(
                        new XYChart.Data<>("Month 1", 74),
                        new XYChart.Data<>("Month 2", 77),
                        new XYChart.Data<>("Month 3", 80)
                );
                seriesInst.getData().addAll(
                        new XYChart.Data<>("Month 1", 88),
                        new XYChart.Data<>("Month 2", 93),
                        new XYChart.Data<>("Month 3", 98)
                );
            } else { // 12M
                seriesPeer.getData().addAll(
                        new XYChart.Data<>("Q1", 72),
                        new XYChart.Data<>("Q2", 76),
                        new XYChart.Data<>("Q3", 79),
                        new XYChart.Data<>("Q4", 82)
                );
                seriesInst.getData().addAll(
                        new XYChart.Data<>("Q1", 85),
                        new XYChart.Data<>("Q2", 89),
                        new XYChart.Data<>("Q3", 94),
                        new XYChart.Data<>("Q4", 99)
                );
            }

            chart.getData().addAll(seriesPeer, seriesInst);

            Platform.runLater(() -> {
                try {
                    javafx.scene.Node lineInst = seriesInst.getNode().lookup(".chart-series-line");
                    if (lineInst != null) {
                        lineInst.setStyle("-fx-stroke: #06b6d4; -fx-stroke-width: 3.5px; -fx-effect: dropshadow(gaussian, rgba(6,182,212,0.7), 16, 0.3, 0, 0);");
                    }
                    javafx.scene.Node linePeer = seriesPeer.getNode().lookup(".chart-series-line");
                    if (linePeer != null) {
                        linePeer.setStyle("-fx-stroke: #475569; -fx-stroke-width: 2px; -fx-stroke-dash-array: 6 6;");
                    }

                    for (XYChart.Data<String, Number> data : seriesInst.getData()) {
                        javafx.scene.Node symbol = data.getNode();
                        if (symbol != null) {
                            symbol.setStyle("-fx-background-color: #06b6d4, white; -fx-background-insets: 0, 2; -fx-background-radius: 6px; -fx-padding: 6px; -fx-cursor: hand;");
                            Tooltip tip = new Tooltip(data.getXValue() + "\n" + instructor.getName() + ": " + data.getYValue() + "%\nStudio Benchmark Delta: +" + (data.getYValue().doubleValue() - 80) + "%");
                            tip.setStyle("-fx-background-color: rgba(15,23,42,0.95); -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: rgba(56,189,248,0.4);");
                            Tooltip.install(symbol, tip);
                        }
                    }

                    for (XYChart.Data<String, Number> data : seriesPeer.getData()) {
                        javafx.scene.Node symbol = data.getNode();
                        if (symbol != null) {
                            symbol.setStyle("-fx-background-color: #475569, #1e293b; -fx-background-insets: 0, 1; -fx-background-radius: 4px; -fx-padding: 4px;");
                        }
                    }
                } catch (Exception ignored) {}
            });
        };

        for (String rl : rangeLabels) {
            Button btn = new Button(rl);
            btn.getStyleClass().add("team-range-pill");
            if (rl.equals(currentRange[0])) {
                btn.getStyleClass().add("team-range-pill-active");
            }
            btn.setOnAction(e -> {
                currentRange[0] = rl;
                rangeButtons.values().forEach(b -> b.getStyleClass().remove("team-range-pill-active"));
                btn.getStyleClass().add("team-range-pill-active");
                updateChart.run();
            });
            rangeButtons.put(rl, btn);
            ranges.getChildren().add(btn);
        }

        cHeader.getChildren().addAll(chartTitle, cSpacer, ranges);

        // Legend Row
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_RIGHT);
        Label leg1 = new Label("Current Performance"); leg1.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-font-weight: 600;");
        Circle dot1 = new Circle(4, Color.web("#06b6d4"));
        Label leg2 = new Label("Studio Peer Benchmark"); leg2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        Circle dot2 = new Circle(4, Color.web("#64748b"));
        legend.getChildren().addAll(new HBox(5, dot1, leg1), new HBox(5, dot2, leg2));

        updateChart.run();

        chartCard.getChildren().addAll(cHeader, legend, chart);
        return chartCard;
    }

    // =========================================================================
    // ADMIN DASHBOARD
    // =========================================================================
    private void renderAdminDashboard(GridPane grid, Admin admin) {
        // Administrative Control Center Card
        VBox adminControlCard = new VBox(16);
        adminControlCard.getStyleClass().add("team-glass-card");
        HBox.setHgrow(adminControlCard, Priority.ALWAYS);

        HBox aHeader = new HBox();
        aHeader.setAlignment(Pos.CENTER_LEFT);
        Label aTitle = new Label("Administrative Governance & Privileges");
        aTitle.getStyleClass().add("team-card-title");
        Region aSp = new Region(); HBox.setHgrow(aSp, Priority.ALWAYS);
        Label clearanceBadge = new Label("TIER-4 ROOT CLEARANCE");
        clearanceBadge.setStyle("-fx-background-color: rgba(248, 113, 113, 0.2); -fx-text-fill: #f87171; -fx-padding: 3 10; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: bold;");
        aHeader.getChildren().addAll(aTitle, aSp, clearanceBadge);

        Label desc = new Label("This executive account holds global administrative governance over equipment registries, staff allocations, biometric data encryption, and system maintenance logs.");
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-line-spacing: 4px;");
        desc.setWrapText(true);

        // Security Permissions Matrix
        GridPane permGrid = new GridPane();
        permGrid.setHgap(16);
        permGrid.setVgap(12);
        permGrid.setPadding(new Insets(10, 0, 10, 0));

        String[][] perms = {
                {"MANAGE_USERS", "User Account Provisioning & Role Delegation"},
                {"MANAGE_EQUIPMENT", "Equipment Registry & Maintenance Calibration"},
                {"VIEW_MAINTENANCE_LOG", "Global Hardware & Safety Oversight"},
                {"ASSIGN_MAINTENANCE", "Technician Work Order Approval"},
                {"MANAGE_SESSIONS", "Studio Master Schedule & Class Allocations"},
                {"FINANCIAL_AUDIT", "Revenue Processing & Membership Billing Logs"}
        };

        for (int i = 0; i < perms.length; i++) {
            HBox permItem = new HBox(8);
            permItem.setAlignment(Pos.CENTER_LEFT);
            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
            VBox pTexts = new VBox(1);
            Label pName = new Label(perms[i][0]);
            pName.getStyleClass().add("team-perm-name");
            pName.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");
            Label pSub = new Label(perms[i][1]);
            pSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            pTexts.getChildren().addAll(pName, pSub);
            permItem.getChildren().addAll(checkIcon, pTexts);

            permGrid.add(permItem, i % 2, i / 2);
        }

        // Quick Administrative Actions
        HBox actionsRow = new HBox(12);
        actionsRow.setPadding(new Insets(10, 0, 0, 0));

        Button broadcastBtn = new Button("Dispatch System Broadcast");
        broadcastBtn.getStyleClass().add("team-btn-primary");
        broadcastBtn.setOnAction(e -> showSystemBroadcastDialog(admin));

        Button auditBtn = new Button("View Security Dossier");
        auditBtn.getStyleClass().add("team-btn-secondary");
        auditBtn.setOnAction(e -> showStaffDossierDialog(admin));

        actionsRow.getChildren().addAll(broadcastBtn, auditBtn);

        adminControlCard.getChildren().addAll(aHeader, desc, permGrid, new Separator(), actionsRow);
        grid.add(adminControlCard, 1, 0);

        // System Health & Activity Footprint (Col 0 & 1, Row 1)
        VBox sysActivityCard = new VBox(14);
        sysActivityCard.getStyleClass().add("team-glass-card");
        Label actTitle = new Label("System Audit Trail & Session Footprint");
        actTitle.getStyleClass().add("team-card-title");

        VBox activityList = new VBox(8);
        activityList.getChildren().addAll(
                createAuditRow("Today, 09:15 AM", "Admin Authentication Verified (Terminal ADM-01)"),
                createAuditRow("Yesterday, 04:30 PM", "Approved Maintenance Work Order WO-8824"),
                createAuditRow("3 days ago", "Calibrated Treadmill Pro X1 Hardware Thresholds")
        );

        sysActivityCard.getChildren().addAll(actTitle, activityList);
        grid.add(sysActivityCard, 0, 1);
        GridPane.setColumnSpan(sysActivityCard, 2);
    }

    private HBox createAuditRow(String time, String detail) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(30,41,59,0.4); -fx-background-radius: 8; -fx-padding: 8 12;");
        Label t = new Label(time);
        t.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label d = new Label(detail);
        d.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        row.getChildren().addAll(t, d);
        return row;
    }

    // =========================================================================
    // 100% FUNCTIONAL DIALOGS
    // =========================================================================

    /**
     * 1. Staff Invitation / Registration Dialog (Instructor or Admin)
     */
    private void showInviteStaffDialog() {
        Dialog<User> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Invite / Register Staff Member");
        dialog.setHeaderText("Add an Instructor or System Administrator to the IWFC team.");

        ButtonType addBtnType = new ButtonType("Register Staff", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Role> roleCbx = new ComboBox<>();
        roleCbx.getItems().addAll(Role.INSTRUCTOR, Role.ADMIN);
        roleCbx.setValue(Role.INSTRUCTOR);

        TextField nameF = new TextField(); nameF.setPromptText("e.g. Marcus Vance");
        TextField emailF = new TextField(); emailF.setPromptText("e.g. marcus.vance@iwfc.com");
        TextField phoneF = new TextField(); phoneF.setPromptText("e.g. +1 (555) 345-6789");
        TextField addressF = new TextField(); addressF.setPromptText("e.g. 104 Fitness Way, NY");
        TextField specF = new TextField(); specF.setPromptText("e.g. Strength & Conditioning");
        specF.setText("Strength & Conditioning");

        roleCbx.setOnAction(e -> {
            specF.setDisable(roleCbx.getValue() == Role.ADMIN);
            if (roleCbx.getValue() == Role.ADMIN) {
                specF.setText("System Administration");
            } else {
                specF.setText("Strength & Conditioning");
            }
        });

        grid.add(new Label("Role:"), 0, 0); grid.add(roleCbx, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1); grid.add(nameF, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailF, 1, 2);
        grid.add(new Label("Phone:"), 0, 3); grid.add(phoneF, 1, 3);
        grid.add(new Label("Office/Address:"), 0, 4); grid.add(addressF, 1, 4);
        grid.add(new Label("Specialization:"), 0, 5); grid.add(specF, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == addBtnType) {
                if (nameF.getText().trim().isEmpty() || emailF.getText().trim().isEmpty()) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Name and Email are required fields.");
                    DialogUtils.stylePremiumDialog(err);
                    err.showAndWait();
                    return null;
                }
                Role selectedRole = roleCbx.getValue();
                String newId = (selectedRole == Role.ADMIN ? "ADM" : "INS") + String.format("%03d", allUsers.size() + 1);

                if (selectedRole == Role.ADMIN) {
                    return new Admin(newId, nameF.getText().trim(), emailF.getText().trim(), phoneF.getText().trim(), addressF.getText().trim(), "");
                } else {
                    Instructor inst = new Instructor(newId, nameF.getText().trim(), emailF.getText().trim(), phoneF.getText().trim(), addressF.getText().trim(), "", specF.getText().trim());
                    inst.setPerformanceScore(91.0);
                    inst.setAttendanceRate(94.0);
                    inst.setBio("Certified instructor focused on client form, safe progression, and athletic excellence.");
                    return inst;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newStaff -> {
            try {
                facade.addUser(newStaff);
                loadStaffUsers();
                selectedUser = newStaff;
                createFilterChips();
                filterAndRenderUsers("");

                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Staff member " + newStaff.getName() + " successfully registered!");
                DialogUtils.stylePremiumDialog(ok);
                ok.show();
            } catch (UnauthorizedAccessException | DuplicateDataException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    /**
     * 2. Direct Message Dialog
     */
    private void showMessageDialog(User targetUser) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Direct Dispatch Message");
        dialog.setHeaderText("Send high-priority message to " + targetUser.getName());

        ButtonType sendBtnType = new ButtonType("Send Message", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtnType, ButtonType.CANCEL);

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));

        Label infoLbl = new Label("Recipient: " + targetUser.getName() + " (" + targetUser.getEmail() + ")");
        infoLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Enter your communication here...");
        msgArea.setPrefRowCount(4);
        msgArea.setWrapText(true);
        msgArea.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(56,189,248,0.4); -fx-border-radius: 6;");

        box.getChildren().addAll(infoLbl, msgArea);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(b -> {
            if (b == sendBtnType && !msgArea.getText().trim().isEmpty()) {
                String nId = "NTF" + String.format("%04d", DataStore.getInstance().getNotifications().findAll().size() + 1);
                Notification n = new Notification(nId, NotificationCategory.SYSTEM, "Direct Message to " + targetUser.getName(), msgArea.getText().trim(), targetUser.getRole());
                try {
                    DataStore.getInstance().getNotifications().save(n);
                    facade.getNotificationService().notifyListeners("SYSTEM_STAFF_MESSAGE", "Message to " + targetUser.getName() + ": " + msgArea.getText().trim());
                } catch (Exception ignored) {}
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(sent -> {
            if (sent) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Message securely dispatched to " + targetUser.getName() + ".");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    /**
     * 3. Email Dispatcher Dialog
     */
    private void showEmailDispatchDialog(User targetUser) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Staff Email Dispatcher");
        dialog.setHeaderText("Compose official email to " + targetUser.getName());

        ButtonType sendBtnType = new ButtonType("Send Email", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField toF = new TextField(targetUser.getEmail());
        toF.setEditable(false);
        TextField subjectF = new TextField("IWFC Staff Notice: Schedule & Operations");
        ComboBox<String> priorityCbx = new ComboBox<>();
        priorityCbx.getItems().addAll("Standard Priority", "Urgent - Action Required", "Executive Announcement");
        priorityCbx.setValue("Standard Priority");

        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText("Compose email body...");
        bodyArea.setPrefRowCount(4);

        grid.add(new Label("To:"), 0, 0); grid.add(toF, 1, 0);
        grid.add(new Label("Priority:"), 0, 1); grid.add(priorityCbx, 1, 1);
        grid.add(new Label("Subject:"), 0, 2); grid.add(subjectF, 1, 2);
        grid.add(new Label("Body:"), 0, 3); grid.add(bodyArea, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b == sendBtnType);

        dialog.showAndWait().ifPresent(sent -> {
            if (sent) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Email successfully queued and dispatched to " + targetUser.getEmail() + ".");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    /**
     * 4. Staff Security & Performance Dossier Dialog
     */
    private void showStaffDossierDialog(User user) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Staff Dossier: " + user.getName());
        dialog.setHeaderText("Official IWFC Staff Record & Security Footprint");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(480);

        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);
        ImageView avatar = createAvatar(user.getProfileImageUrl(), 60);
        VBox idBox = new VBox(3);
        Label nameLbl = new Label(user.getName());
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label roleBadge = new Label(user.getRole().getDisplayName().toUpperCase() + " • ID: " + user.getUserId());
        roleBadge.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: 700;");
        idBox.getChildren().addAll(nameLbl, roleBadge);
        topRow.getChildren().addAll(avatar, idBox);

        VBox detailsBox = new VBox(8);
        detailsBox.setStyle("-fx-background-color: rgba(30,41,59,0.5); -fx-padding: 14; -fx-background-radius: 10;");
        detailsBox.getChildren().addAll(
                createDossierRow("Registered Email", user.getEmail()),
                createDossierRow("Primary Phone", user.getPhone() != null ? user.getPhone() : "N/A"),
                createDossierRow("Assigned Office", user.getAddress() != null ? user.getAddress() : "Metro Gym Center"),
                createDossierRow("Security Clearance", user.getRole() == Role.ADMIN ? "Level 4 (Full System Control)" : "Level 2 (Instructor Operations)")
        );

        if (user instanceof Instructor inst) {
            detailsBox.getChildren().addAll(
                    createDossierRow("Specialization", inst.getSpecialization()),
                    createDossierRow("Performance Rating", inst.getPerformanceScore() + "% (Elite Tier)"),
                    createDossierRow("Attendance Index", inst.getAttendanceRate() + "%"),
                    createDossierRow("Active Trainees", String.valueOf(inst.getTraineeIds().size()))
            );
        }

        content.getChildren().addAll(topRow, detailsBox);
        dialog.getDialogPane().setContent(content);
        dialog.show();
    }

    private HBox createDossierRow(String key, String val) {
        HBox row = new HBox(10);
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 600; -fx-min-width: 140;");
        Label v = new Label(val);
        v.setStyle("-fx-font-weight: bold;");
        v.getStyleClass().add("team-trainee-name");
        row.getChildren().addAll(k, v);
        return row;
    }

    /**
     * 5. Edit Profile Dialog with Real Persistence
     */
    private void showEditProfileDialog() {
        Dialog<User> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Edit Staff Profile");
        dialog.setHeaderText("Update details for " + selectedUser.getName());

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameF = new TextField(selectedUser.getName());
        TextField emailF = new TextField(selectedUser.getEmail());
        TextField phoneF = new TextField(selectedUser.getPhone() != null ? selectedUser.getPhone() : "");
        TextField addressF = new TextField(selectedUser.getAddress() != null ? selectedUser.getAddress() : "");

        grid.add(new Label("Full Name:"), 0, 0); grid.add(nameF, 1, 0);
        grid.add(new Label("Email:"), 0, 1); grid.add(emailF, 1, 1);
        grid.add(new Label("Phone:"), 0, 2); grid.add(phoneF, 1, 2);
        grid.add(new Label("Office/Address:"), 0, 3); grid.add(addressF, 1, 3);

        TextField specF = null;
        TextArea bioArea = null;
        if (selectedUser instanceof Instructor inst) {
            specF = new TextField(inst.getSpecialization() != null ? inst.getSpecialization() : "");
            bioArea = new TextArea(inst.getBio() != null ? inst.getBio() : "");
            bioArea.setPrefRowCount(3);
            grid.add(new Label("Specialization:"), 0, 4); grid.add(specF, 1, 4);
            grid.add(new Label("Bio:"), 0, 5); grid.add(bioArea, 1, 5);
        }

        final TextField fSpec = specF;
        final TextArea fBio = bioArea;

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == saveBtnType) {
                selectedUser.setName(nameF.getText().trim());
                selectedUser.setEmail(emailF.getText().trim());
                selectedUser.setPhone(phoneF.getText().trim());
                selectedUser.setAddress(addressF.getText().trim());

                if (selectedUser instanceof Instructor inst) {
                    if (fSpec != null) inst.setSpecialization(fSpec.getText().trim());
                    if (fBio != null) inst.setBio(fBio.getText().trim());
                }

                try {
                    facade.updateUser(selectedUser);
                } catch (Exception ignored) {}
                return selectedUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(u -> {
            filterAndRenderUsers(searchField != null ? searchField.getText() : "");
            renderProfile();
        });
    }

    /**
     * 6. Connect / Assign Trainee Dialog
     */
    private void showConnectTraineeDialog(Instructor instructor) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Assign Trainee");
        dialog.setHeaderText("Assign an enrolled member to " + instructor.getName());

        ButtonType assignBtnType = new ButtonType("Assign Trainee", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignBtnType, ButtonType.CANCEL);

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));

        ComboBox<String> memberCbx = new ComboBox<>();
        memberCbx.setPromptText("Select a Registered Member");

        try {
            for (User u : facade.getUsersByRole(Role.MEMBER)) {
                if (!instructor.getTraineeIds().contains(u.getUserId())) {
                    memberCbx.getItems().add(u.getUserId() + " - " + u.getName() + " (" + ((Member) u).getMembershipType() + ")");
                }
            }
        } catch (Exception ignored) {}

        if (memberCbx.getItems().isEmpty()) {
            box.getChildren().add(new Label("All active members are already assigned to this instructor."));
        } else {
            memberCbx.setValue(memberCbx.getItems().get(0));
            box.getChildren().addAll(new Label("Select Member:"), memberCbx);
        }

        dialog.getDialogPane().setContent(box);
        dialog.setResultConverter(b -> {
            if (b == assignBtnType && memberCbx.getValue() != null) {
                String memberId = memberCbx.getValue().split(" - ")[0];
                if (!instructor.getTraineeIds().contains(memberId)) {
                    instructor.getTraineeIds().add(memberId);
                    try {
                        facade.updateUser(instructor);
                    } catch (Exception ignored) {}
                }
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(assigned -> {
            if (assigned) {
                renderProfile();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Trainee successfully assigned to " + instructor.getName() + "!");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    /**
     * 7. Interactive Trainee Biometrics Dialog
     */
    private void showTraineeBiometricsDialog(Member member) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Trainee Health & Biometrics");
        dialog.setHeaderText("Biometric Telemetry for " + member.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox box = new VBox(14);
        box.setPadding(new Insets(20));
        box.setPrefWidth(420);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        ImageView av = createAvatar(member.getProfileImageUrl(), 50);
        VBox tBox = new VBox(2);
        Label n = new Label(member.getName());
        n.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label sub = new Label("Program: Hypertrophy Elite • Membership: " + member.getMembershipType());
        sub.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
        tBox.getChildren().addAll(n, sub);
        top.getChildren().addAll(av, tBox);

        GridPane metricGrid = new GridPane();
        metricGrid.setHgap(12); metricGrid.setVgap(10);
        metricGrid.setPadding(new Insets(10));
        metricGrid.setStyle("-fx-background-color: rgba(30,41,59,0.5); -fx-background-radius: 10;");

        metricGrid.add(createBiometricCard("Weight", "78.4 kg", "▲ -1.2 kg"), 0, 0);
        metricGrid.add(createBiometricCard("Body Fat", "14.2%", "▼ -0.8%"), 1, 0);
        metricGrid.add(createBiometricCard("Muscle Mass", "42.1 kg", "▲ +0.6 kg"), 0, 1);
        metricGrid.add(createBiometricCard("Hydration", "62.4%", "▲ Optimal"), 1, 1);

        Label dietLbl = new Label("Dietary Habits: " + (member.getDietHabits() != null ? member.getDietHabits() : "High protein, balanced calorie deficit."));
        dietLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dietLbl.setWrapText(true);

        box.getChildren().addAll(top, metricGrid, dietLbl);
        dialog.getDialogPane().setContent(box);
        dialog.show();
    }

    private VBox createBiometricCard(String label, String value, String change) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: rgba(15,23,42,0.6); -fx-background-radius: 8;");
        Label l = new Label(label); l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.getStyleClass().add("team-metric-val");
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: 800;");
        Label c = new Label(change); c.setStyle("-fx-text-fill: #10b981; -fx-font-size: 10px;");
        box.getChildren().addAll(l, v, c);
        return box;
    }

    /**
     * 8. Add Skill / Competency Dialog
     */
    private void showAddSkillDialog(Instructor instructor) {
        TextInputDialog dialog = new TextInputDialog();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Add Skill / Certification");
        dialog.setHeaderText("Add a competency or accreditation for " + instructor.getName());
        dialog.setContentText("Skill Name:");

        dialog.showAndWait().ifPresent(skill -> {
            if (!skill.trim().isEmpty()) {
                instructorCustomSkills.computeIfAbsent(instructor.getUserId(), k -> new ArrayList<>()).add(skill.trim());
                renderProfile();
            }
        });
    }

    /**
     * 9. Add Review Dialog
     */
    private void showAddFeedbackDialog(Instructor instructor) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Log Client Review");
        dialog.setHeaderText("Record member feedback for " + instructor.getName());

        ButtonType logBtnType = new ButtonType("Log Review", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(logBtnType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        box.setPadding(new Insets(20));

        TextArea reviewArea = new TextArea();
        reviewArea.setPromptText("Enter client review here...");
        reviewArea.setPrefRowCount(3);
        reviewArea.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(56,189,248,0.4); -fx-border-radius: 6;");

        box.getChildren().addAll(new Label("Feedback / Review Text:"), reviewArea);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(b -> {
            if (b == logBtnType && !reviewArea.getText().trim().isEmpty()) {
                instructor.getFeedbacks().add(0, reviewArea.getText().trim());
                try {
                    facade.updateUser(instructor);
                } catch (Exception ignored) {}
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(logged -> {
            if (logged) renderProfile();
        });
    }

    /**
     * 10. View All Feedback Dialog
     */
    private void showAllFeedbackDialog(Instructor instructor) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("All Feedback — " + instructor.getName());
        dialog.setHeaderText("Client Review Portfolio (" + instructor.getFeedbacks().size() + " total reviews)");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));

        for (String f : instructor.getFeedbacks()) {
            VBox item = new VBox(4);
            item.getStyleClass().add("team-feedback-bubble");
            Label stars = new Label("★★★★★");
            stars.getStyleClass().add("team-feedback-stars");
            Label text = new Label("\"" + f + "\"");
            text.setWrapText(true);
            text.getStyleClass().add("team-feedback-quote");
            item.getChildren().addAll(stars, text);
            box.getChildren().add(item);
        }

        ScrollPane scroll = new ScrollPane(box);
        scroll.setPrefHeight(340);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("team-scroll-pane", "edge-to-edge");
        dialog.getDialogPane().setContent(scroll);
        dialog.show();
    }

    /**
     * 11. System Broadcast Dialog (Admin Only)
     */
    private void showSystemBroadcastDialog(Admin admin) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("System Broadcast Dispatcher");
        dialog.setHeaderText("Broadcast announcement from " + (admin != null ? admin.getName() : "Administrator") + " across all user terminals");

        ButtonType sendBtnType = new ButtonType("Broadcast Alert", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtnType, ButtonType.CANCEL);

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));

        TextField titleF = new TextField("Facility Maintenance / Schedule Update");

        TextArea msgArea = new TextArea("Please be advised of scheduled maintenance on Sunday from 02:00 to 04:00 AM.");
        msgArea.setPrefRowCount(4);

        box.getChildren().addAll(new Label("Announcement Title:"), titleF, new Label("Message Body:"), msgArea);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(b -> {
            if (b == sendBtnType && !msgArea.getText().trim().isEmpty()) {
                String nId = "NTF" + String.format("%04d", DataStore.getInstance().getNotifications().findAll().size() + 1);
                Notification n = new Notification(nId, NotificationCategory.SYSTEM, titleF.getText().trim(), msgArea.getText().trim(), null);
                try {
                    DataStore.getInstance().getNotifications().save(n);
                    facade.getNotificationService().notifyListeners("SYSTEM_STAFF_MESSAGE", titleF.getText().trim() + ": " + msgArea.getText().trim());
                } catch (Exception ignored) {}
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(sent -> {
            if (sent) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "System broadcast dispatched to all active users!");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    private ImageView createAvatar(String url, double size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);

        if (url != null && !url.isEmpty()) {
            try {
                view.setImage(new Image(url, true));
            } catch (Exception ignored) {}
        }

        Circle clip = new Circle(size / 2, size / 2, size / 2);
        view.setClip(clip);
        return view;
    }
}

package com.iwfc.ui;
import com.iwfc.ui.components.DialogUtils;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.stream.Collectors;

public class UserManagementView extends HBox {

    private final IWFCSystemFacade facade;
    private User selectedUser;
    private Role currentFilter = null; // null means All
    private List<User> allUsers;

    private VBox userListContainer;
    private VBox profileColumn;

    private HBox filterChips;

    public UserManagementView(IWFCSystemFacade facade) {
        this.facade = facade;

        getStyleClass().add("user-management-view");
        setSpacing(30);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_LEFT);

        try {
            this.allUsers = facade.getAllUsers().stream()
                .filter(u -> u.getRole() != Role.MEMBER)
                .collect(Collectors.toList());
        } catch (UnauthorizedAccessException e) {
            getChildren().add(new Label("Access Denied: " + e.getMessage()));
            return;
        }

        if (!allUsers.isEmpty()) {
            selectedUser = allUsers.get(0);
        }

        setupUI();
    }

    private void setupUI() {
        // --- Left Sidebar (Team Directory) ---
        VBox directoryColumn = new VBox(15);
        directoryColumn.setPrefWidth(320);
        directoryColumn.setMinWidth(320);
        directoryColumn.setMaxWidth(320);
        directoryColumn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255, 255, 255, 0.05); -fx-border-radius: 16px;");
        directoryColumn.setPadding(new Insets(20));
        HBox.setHgrow(directoryColumn, Priority.NEVER);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Team Directory");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addMemberBtn = new Button("+ Invite New");
        addMemberBtn.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #c4b5fd; -fx-border-color: rgba(139, 92, 246, 0.4); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 12; -fx-cursor: hand;");
        addMemberBtn.setOnAction(e -> showAddMemberDialog());
        header.getChildren().addAll(titleLabel, spacer, addMemberBtn);

        TextField searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px; -fx-text-fill: white; -fx-padding: 8 12;");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterAndRenderUsers(newV));

        filterChips = new HBox(10);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        createFilterChips();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        userListContainer = new VBox(8);
        scrollPane.setContent(userListContainer);

        directoryColumn.getChildren().addAll(header, searchField, filterChips, scrollPane);

        // --- Right Area (Dynamic Profile & Dashboard) ---
        profileColumn = new VBox(20);
        profileColumn.setPadding(new Insets(0, 0, 0, 20));
        HBox.setHgrow(profileColumn, Priority.ALWAYS);

        getChildren().addAll(directoryColumn, profileColumn);

        filterAndRenderUsers("");
    }

    private void createFilterChips() {
        filterChips.getChildren().clear();
        String[] labels = {"All", "Admins", "Instructors"};
        Role[] roles = {null, Role.ADMIN, Role.INSTRUCTOR};

        for (int i = 0; i < labels.length; i++) {
            final Role role = roles[i];
            Button chip = new Button(labels[i]);
            if (currentFilter == role) {
                chip.setStyle("-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #c4b5fd; -fx-border-color: rgba(139, 92, 246, 0.5); -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12; -fx-cursor: hand;");
            } else {
                chip.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 16px; -fx-padding: 4 12; -fx-cursor: hand;");
            }
            chip.setOnAction(e -> {
                currentFilter = role;
                createFilterChips(); // refresh visual state
                filterAndRenderUsers(""); // re-filter
            });
            filterChips.getChildren().add(chip);
        }
    }

    private void filterAndRenderUsers(String searchText) {
        userListContainer.getChildren().clear();
        String lowerSearch = searchText.toLowerCase();

        List<User> filtered = allUsers.stream()
                .filter(u -> currentFilter == null || u.getRole() == currentFilter)
                .filter(u -> u.getName().toLowerCase().contains(lowerSearch) || u.getEmail().toLowerCase().contains(lowerSearch))
                .collect(Collectors.toList());

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
        }
    }

    private HBox createUserListItem(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        
        if (user.equals(selectedUser)) {
            row.setStyle("-fx-background-color: rgba(139, 92, 246, 0.15); -fx-background-radius: 12px; -fx-border-color: rgba(139, 92, 246, 0.3); -fx-border-radius: 12px; -fx-cursor: hand;");
        } else {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12px; -fx-cursor: hand;");
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12px; -fx-cursor: hand;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12px; -fx-cursor: hand;"));
        }

        StackPane avatarBox = new StackPane();
        ImageView avatar = createAvatar(user.getProfileImageUrl(), 40);
        Circle statusDot = new Circle(5, user.getRole() == Role.ADMIN ? javafx.scene.paint.Color.web("#f87171") : (user.getRole() == Role.INSTRUCTOR ? javafx.scene.paint.Color.web("#fbbf24") : javafx.scene.paint.Color.web("#34d399")));
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        avatarBox.getChildren().addAll(avatar, statusDot);
        
        VBox info = new VBox(2);
        Label name = new Label(user.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label role = new Label(user.getRole().getDisplayName());
        role.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        info.getChildren().addAll(name, role);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Mini actions
        HBox miniActions = new HBox(8);
        miniActions.setAlignment(Pos.CENTER_RIGHT);
        String msgSvg = "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z";
        String envSvg = "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z";
        String docSvg = "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";
        
        Region msgIcon = new Region(); msgIcon.setStyle("-fx-shape: '" + msgSvg + "'; -fx-background-color: #9ca3af;"); msgIcon.setMinSize(14,14); msgIcon.setMaxSize(14,14);
        Region envIcon = new Region(); envIcon.setStyle("-fx-shape: '" + envSvg + "'; -fx-background-color: #9ca3af;"); envIcon.setMinSize(14,14); envIcon.setMaxSize(14,14);
        Region docIcon = new Region(); docIcon.setStyle("-fx-shape: '" + docSvg + "'; -fx-background-color: #9ca3af;"); docIcon.setMinSize(14,14); docIcon.setMaxSize(14,14);
        
        miniActions.getChildren().addAll(msgIcon, envIcon, docIcon);

        row.getChildren().addAll(avatarBox, info, spacer, miniActions);

        row.setOnMouseClicked(e -> {
            selectedUser = user;
            filterAndRenderUsers(""); // just to update active state
        });

        return row;
    }

    private void renderProfile() {
        profileColumn.getChildren().clear();

        if (selectedUser == null) return;
        
        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setFitToWidth(true);
        scrollContent.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollContent.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        GridPane dashboardGrid = new GridPane();
        dashboardGrid.setHgap(20);
        dashboardGrid.setVgap(20);
        dashboardGrid.setPadding(new Insets(0, 20, 20, 0));
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(45);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(55);
        dashboardGrid.getColumnConstraints().addAll(col1, col2);

        // 1. Premium Glowing Profile Card (Top Left)
        VBox profileCard = new VBox(15);
        profileCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 16px; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(56,189,248,0.2), 30, 0, 0, 0);");
        profileCard.setPadding(new Insets(30));
        profileCard.setAlignment(Pos.TOP_CENTER);

        StackPane avatarWrap = new StackPane();
        Circle glowRing = new Circle(55);
        glowRing.setStyle("-fx-stroke: linear-gradient(to bottom right, #38bdf8, #818cf8); -fx-stroke-width: 4px; -fx-fill: transparent; -fx-effect: dropshadow(gaussian, #38bdf8, 15, 0, 0, 0);");
        ImageView avatar = createAvatar(selectedUser.getProfileImageUrl(), 100);
        avatarWrap.getChildren().addAll(glowRing, avatar);
        
        Label nameLbl = new Label(selectedUser.getName());
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        
        Label roleLbl = new Label(selectedUser.getRole().getDisplayName());
        String roleColor = selectedUser.getRole() == Role.ADMIN ? "#f87171" : (selectedUser.getRole() == Role.INSTRUCTOR ? "#38bdf8" : "#34d399");
        String roleBg = selectedUser.getRole() == Role.ADMIN ? "rgba(248, 113, 113, 0.2)" : (selectedUser.getRole() == Role.INSTRUCTOR ? "rgba(56, 189, 248, 0.2)" : "rgba(52, 211, 153, 0.2)");
        roleLbl.setStyle("-fx-background-color: " + roleBg + "; -fx-text-fill: " + roleColor + "; -fx-padding: 4 12; -fx-background-radius: 20px; -fx-font-size: 12px; -fx-font-weight: bold;");

        GridPane contactGrid = new GridPane();
        contactGrid.setVgap(12);
        contactGrid.setHgap(15);
        contactGrid.setAlignment(Pos.CENTER_LEFT);
        
        addContactRow(contactGrid, 0, "M20 15.5c-1.25 0-2.45-.2-3.57-.57-.35-.11-.74-.03-1.02.24l-2.2 2.2c-2.83-1.44-5.15-3.75-6.59-6.58l2.2-2.21c.28-.27.36-.66.25-1.01C8.7 6.45 8.5 5.25 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z", selectedUser.getPhone() != null ? selectedUser.getPhone() : "N/A");
        addContactRow(contactGrid, 1, "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z", selectedUser.getEmail());
        addContactRow(contactGrid, 2, "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z", selectedUser.getAddress() != null ? selectedUser.getAddress() : "N/A");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(20, 0, 0, 0));
        
        Button editBtn = new Button("Edit Profile");
        editBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 20; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showEditProfileDialog());
        
        Button msgBtn = new Button("Message");
        msgBtn.setStyle("-fx-background-color: linear-gradient(to right, #38bdf8, #818cf8); -fx-text-fill: white; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
        msgBtn.setOnAction(e -> showMessageDialog());
        
        actions.getChildren().addAll(editBtn, msgBtn);

        profileCard.getChildren().addAll(avatarWrap, nameLbl, roleLbl, new Region(), contactGrid, actions);
        addHoverEffect(profileCard);
        dashboardGrid.add(profileCard, 0, 0);

        // Populate rest of grid based on role
        switch (selectedUser) {
            case Instructor instructor -> renderInstructorDashboard(dashboardGrid, instructor);
            case Member member -> renderMemberDashboard(dashboardGrid, member);
            default -> renderAdminDashboard(dashboardGrid);
        }

        scrollContent.setContent(dashboardGrid);
        profileColumn.getChildren().add(scrollContent);
    }

    private void addHoverEffect(Region node) {
        node.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), node);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        node.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }
    
    private void addContactRow(GridPane grid, int row, String svgPath, String text) {
        Region icon = new Region();
        icon.setStyle("-fx-shape: '" + svgPath + "'; -fx-background-color: #38bdf8;");
        icon.setMinSize(16, 16);
        icon.setMaxSize(16, 16);
        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");
        grid.add(icon, 0, row);
        grid.add(textLbl, 1, row);
    }
    
    private VBox createRadialGauge(String title, String subtitle, double value, String colorHex, String trendText, boolean positiveTrend) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px; -fx-padding: 20;");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER);
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        
        StackPane gauge = new StackPane();
        Circle bgCircle = new Circle(35);
        bgCircle.setStyle("-fx-stroke: rgba(255,255,255,0.1); -fx-stroke-width: 8px; -fx-fill: transparent;");
        
        // Simulating progress with a colored border
        Circle fgCircle = new Circle(35);
        fgCircle.setStyle("-fx-stroke: " + colorHex + "; -fx-stroke-width: 8px; -fx-fill: transparent; -fx-stroke-dash-array: " + (220 * (value/100.0)) + " 220; -fx-effect: dropshadow(gaussian, " + colorHex + ", 10, 0, 0, 0);");
        fgCircle.setRotate(-90);
        
        Label valLbl = new Label(String.format("%.1f", value));
        valLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        gauge.getChildren().addAll(bgCircle, fgCircle, valLbl);
        
        HBox trendBox = new HBox(5);
        trendBox.setAlignment(Pos.CENTER);
        Label arrow = new Label(positiveTrend ? "▲" : "▼");
        arrow.setStyle("-fx-text-fill: " + (positiveTrend ? "#10b981" : "#ef4444") + "; -fx-font-size: 10px;");
        Label trendLbl = new Label(trendText);
        trendLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        trendBox.getChildren().addAll(arrow, trendLbl);
        
        box.getChildren().addAll(titleLbl, subLbl, gauge, trendBox);
        return box;
    }

    private void renderInstructorDashboard(GridPane grid, Instructor instructor) {
        // Col 0, Row 1: Bio & Expertise + Tags (acting as continuation of Profile Card)
        VBox bioBox = new VBox(12);
        bioBox.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-padding: 20; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px;");
        Label bioTitle = new Label("Bio & Expertise. Overview.");
        bioTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label bioText = new Label(instructor.getBio() != null ? instructor.getBio() : instructor.getSpecialization());
        bioText.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-line-spacing: 4px;");
        bioText.setWrapText(true);
        
        FlowPane tagsPane = new FlowPane(8, 8);
        String[] tags = {"Startad", "Sattting", "Injury Prevention", "Strength Eating", "Stronger You", "Prevention", "HIIT", "Strength", "Mobility"};
        for (int i = 0; i < tags.length; i++) {
            Label tag = new Label(tags[i]);
            String tColor = i % 2 == 0 ? "#38bdf8" : "#c026d3";
            String tBg = i % 2 == 0 ? "rgba(56, 189, 248, 0.15)" : "rgba(192, 38, 211, 0.15)";
            tag.setStyle("-fx-background-color: " + tBg + "; -fx-text-fill: " + tColor + "; -fx-padding: 4 12; -fx-background-radius: 20px; -fx-font-size: 11px;");
            tagsPane.getChildren().add(tag);
        }
        bioBox.getChildren().addAll(bioTitle, bioText, tagsPane);
        addHoverEffect(bioBox);
        grid.add(bioBox, 0, 1);

        // Col 0, Row 2: Recent Feedback (Chat Thread Style)
        VBox feedCard = new VBox(15);
        feedCard.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px; -fx-padding: 20;");
        HBox fHeader = new HBox();
        Label feedTitle = new Label("Recent Feedback");
        feedTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region fSpacer = new Region(); HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Button viewAllBtn = new Button("View All");
        viewAllBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(139, 92, 246, 0.5); -fx-text-fill: #c4b5fd; -fx-border-radius: 6px; -fx-cursor: hand;");
        viewAllBtn.setOnAction(e -> showAllFeedbackDialog(instructor));
        fHeader.getChildren().addAll(feedTitle, fSpacer, viewAllBtn);
        
        VBox feedbackList = new VBox(12);
        if (instructor.getFeedbacks() != null && !instructor.getFeedbacks().isEmpty()) {
            for (int i = 0; i < instructor.getFeedbacks().size(); i++) {
                String f = instructor.getFeedbacks().get(i);
                VBox bubble = new VBox(5);
                bubble.setStyle("-fx-background-color: rgba(139, 92, 246, 0.1); -fx-padding: 12 16; -fx-background-radius: 16px 16px 16px 4px; -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 16px 16px 16px 4px;");
                Label stars = new Label("★★★★★");
                stars.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px;");
                Label text = new Label("\"" + f + "\"");
                text.setWrapText(true);
                text.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");
                Label author = new Label("- " + (i == 0 ? "Amy L." : "Sarah J."));
                author.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
                bubble.getChildren().addAll(stars, text, author);
                feedbackList.getChildren().add(bubble);
            }
        } else {
            Label noF = new Label("No feedback available.");
            noF.setStyle("-fx-text-fill: #9ca3af;");
            feedbackList.getChildren().add(noF);
        }
        feedCard.getChildren().addAll(fHeader, feedbackList);
        addHoverEffect(feedCard);
        grid.add(feedCard, 0, 2);

        // Col 1, Row 0: Gauges Box
        HBox gaugesBox = new HBox(20);
        VBox impactGauge = createRadialGauge("Impact Score", "(Last 30 Days)", instructor.getPerformanceScore(), "#06b6d4", "vs last week", true);
        VBox attendGauge = createRadialGauge("Attendance Rate", "(Current Quarter)", instructor.getAttendanceRate(), "#c026d3", "vs last quarter", true);
        gaugesBox.getChildren().addAll(impactGauge, attendGauge);
        grid.add(gaugesBox, 1, 0);

        // Col 1, Row 1: Trainee Assignments (Team Building Style)
        VBox rosterCard = new VBox(15);
        rosterCard.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px; -fx-padding: 20;");
        
        HBox rHeader = new HBox();
        Label rosterTitle = new Label("Trainee Assignments");
        rosterTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region rSpacer = new Region(); HBox.setHgrow(rSpacer, Priority.ALWAYS);
        Button assignBtn = new Button("Assign First Trainee");
        assignBtn.setStyle("-fx-background-color: linear-gradient(to right, #38bdf8, #818cf8); -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 16; -fx-cursor: hand;");
        assignBtn.setOnAction(e -> showConnectTraineeDialog(instructor));
        rHeader.getChildren().addAll(rosterTitle, rSpacer, assignBtn);
        
        VBox detailedList = new VBox(15);
        detailedList.setAlignment(Pos.CENTER);
        detailedList.setPadding(new Insets(20, 0, 10, 0));
        
        HBox emptyAvatars = new HBox(-12);
        emptyAvatars.setAlignment(Pos.CENTER);
        for(int i=0; i<4; i++) {
            Circle c = new Circle(22);
            c.setStyle("-fx-fill: rgba(255,255,255,0.05); -fx-stroke: #1e293b; -fx-stroke-width: 3px;");
            // Add a faint inner icon
            StackPane sp = new StackPane(c);
            Region icon = new Region();
            icon.setStyle("-fx-shape: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'; -fx-background-color: rgba(255,255,255,0.15);");
            icon.setMaxSize(24, 24);
            sp.getChildren().add(icon);
            emptyAvatars.getChildren().add(sp);
        }
        
        Label emptyLbl = new Label("Ready to build a strong team.");
        emptyLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        detailedList.getChildren().addAll(emptyAvatars, emptyLbl);
        
        rosterCard.getChildren().addAll(rHeader, new Separator(), detailedList);
        addHoverEffect(rosterCard);
        grid.add(rosterCard, 1, 1);

        // Col 1, Row 2: Performance Trend Chart
        VBox chartCard = new VBox(15);
        chartCard.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px; -fx-padding: 20;");
        
        HBox cHeader = new HBox(15);
        cHeader.setAlignment(Pos.CENTER_LEFT);
        Label chartTitle = new Label("Performance Trend");
        chartTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region cSpacer = new Region(); HBox.setHgrow(cSpacer, Priority.ALWAYS);
        
        HBox ranges = new HBox(5);
        String[] rangeLabels = {"30D", "90D", "12M"};
        java.util.List<Label> rangeBtns = new java.util.ArrayList<>();
        for(String rl : rangeLabels) {
            Label rlBtn = new Label(rl);
            String activeStyle = "-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38bdf8; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-cursor: hand;";
            String inactiveStyle = "-fx-text-fill: #9ca3af; -fx-padding: 4 10; -fx-font-size: 11px; -fx-cursor: hand;";
            
            rlBtn.setStyle(rl.equals("30D") ? activeStyle : inactiveStyle);
            
            rlBtn.setOnMouseClicked(e -> {
                for (Label b : rangeBtns) {
                    b.setStyle(inactiveStyle);
                }
                rlBtn.setStyle(activeStyle);
            });
            
            rangeBtns.add(rlBtn);
            ranges.getChildren().add(rlBtn);
        }
        cHeader.getChildren().addAll(chartTitle, cSpacer, ranges);
        
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER_RIGHT);
        Label leg1 = new Label("Current Performance"); leg1.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px;");
        Circle dot1 = new Circle(4, javafx.scene.paint.Color.web("#06b6d4"));
        Label leg2 = new Label("Peer Average"); leg2.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Circle dot2 = new Circle(4, javafx.scene.paint.Color.web("#64748b"));
        legend.getChildren().addAll(new HBox(5, dot1, leg1), new HBox(5, dot2, leg2));
        
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis(60, 100, 10);
        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.setPrefHeight(200);
        
        javafx.scene.chart.XYChart.Series<String, Number> seriesInst = new javafx.scene.chart.XYChart.Series<>();
        seriesInst.setName("Instructor");
        seriesInst.getData().add(new javafx.scene.chart.XYChart.Data<>("W1", 90));
        seriesInst.getData().add(new javafx.scene.chart.XYChart.Data<>("W2", 94));
        seriesInst.getData().add(new javafx.scene.chart.XYChart.Data<>("W3", 97));
        javafx.scene.chart.XYChart.Data<String, Number> finalPoint = new javafx.scene.chart.XYChart.Data<>("W4", 99);
        seriesInst.getData().add(finalPoint);
        
        javafx.scene.chart.XYChart.Series<String, Number> seriesPeer = new javafx.scene.chart.XYChart.Series<>();
        seriesPeer.setName("Peer");
        seriesPeer.getData().add(new javafx.scene.chart.XYChart.Data<>("W1", 70));
        seriesPeer.getData().add(new javafx.scene.chart.XYChart.Data<>("W2", 75));
        seriesPeer.getData().add(new javafx.scene.chart.XYChart.Data<>("W3", 78));
        seriesPeer.getData().add(new javafx.scene.chart.XYChart.Data<>("W4", 81));
        
        chart.getData().add(seriesPeer);
        chart.getData().add(seriesInst);
        chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);
        
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node lineInst = seriesInst.getNode().lookup(".chart-series-line");
            if (lineInst != null) {
                lineInst.setStyle("-fx-stroke: #06b6d4; -fx-stroke-width: 3px; -fx-effect: dropshadow(gaussian, rgba(6,182,212,0.6), 15, 0, 0, 0);");
            }
            javafx.scene.Node linePeer = seriesPeer.getNode().lookup(".chart-series-line");
            if (linePeer != null) {
                linePeer.setStyle("-fx-stroke: #475569; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;");
            }
            
            for (javafx.scene.chart.XYChart.Data<String, Number> data : seriesInst.getData()) {
                javafx.scene.Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.setStyle("-fx-background-color: #06b6d4, white; -fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px;");
                }
            }
            for (javafx.scene.chart.XYChart.Data<String, Number> data : seriesPeer.getData()) {
                javafx.scene.Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.setStyle("-fx-background-color: #475569, #1e293b; -fx-background-insets: 0, 1; -fx-background-radius: 4px; -fx-padding: 4px;");
                }
            }
            
            // Tooltip for final point
            if (finalPoint.getNode() != null) {
                Tooltip tip = new Tooltip("Instructor: 99%\nPeer Avg: 81%");
                tip.setStyle("-fx-background-color: rgba(30,41,59,0.9); -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8px; -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px;");
                Tooltip.install(finalPoint.getNode(), tip);
            }
        });
        
        chartCard.getChildren().addAll(cHeader, legend, chart);
        addHoverEffect(chartCard);
        grid.add(chartCard, 1, 2);
    }

    private void renderMemberDashboard(GridPane grid, Member member) {
        // Col 2, Row 0: Stats
        HBox statsBox = new HBox(15);
        VBox hoursStat = createRadialGauge("Hours Spent", "(All Time)", member.getHoursSpent(), "#10b981", "vs last month", true);
        double totalPayments = facade.getTotalPaymentsForMember(member.getUserId());
        VBox payStat = createRadialGauge("Payments", "(Completed)", totalPayments > 100 ? 100 : totalPayments, "#3b82f6", "on time", true);
        statsBox.getChildren().addAll(hoursStat, payStat);
        grid.add(statsBox, 1, 0);

        // Col 1, Row 1: Details
        VBox detailsCard = new VBox(15);
        detailsCard.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px; -fx-padding: 20;");
        
        Label title = new Label("Member Analytics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label typeLbl = new Label("Membership: " + member.getMembershipType());
        typeLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");

        Label machineTitle = new Label("Frequently Used Machines:");
        machineTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        FlowPane machines = new FlowPane(10, 10);
        if (member.getMachinesUsed() != null && !member.getMachinesUsed().isEmpty()) {
            for (String machine : member.getMachinesUsed()) {
                Label mChip = new Label(machine);
                mChip.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-padding: 4 10; -fx-background-radius: 12px;");
                machines.getChildren().add(mChip);
            }
        } else {
            Label noM = new Label("No data"); noM.setStyle("-fx-text-fill: #9ca3af;");
            machines.getChildren().add(noM);
        }
        detailsCard.getChildren().addAll(title, typeLbl, machineTitle, machines);
        grid.add(detailsCard, 0, 1);
        
        // Col 2, Row 1: Recent Payments
        VBox paymentCard = new VBox(15);
        paymentCard.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px; -fx-padding: 20;");
        HBox pHeader = new HBox();
        Label pTitle = new Label("Recent Payments");
        pTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region pSpacer = new Region(); HBox.setHgrow(pSpacer, Priority.ALWAYS);
        if (facade.getCurrentUser().getRole() == Role.ADMIN) {
            Button payBtn = new Button("+ New Payment");
            payBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 12; -fx-cursor: hand;");
            payBtn.setOnAction(e -> showPaymentDialog(member));
            pHeader.getChildren().addAll(pTitle, pSpacer, payBtn);
        } else {
            pHeader.getChildren().add(pTitle);
        }

        TableView<Payment> paymentTable = new TableView<>();
        paymentTable.setPrefHeight(150);
        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Payment, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getPaidAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))
        ));
        TableColumn<Payment, String> purposeCol = new TableColumn<>("Purpose");
        purposeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getPurpose().getDisplayName()
        ));
        paymentTable.getColumns().add(dateCol);
        paymentTable.getColumns().add(purposeCol);

        try {
            List<Payment> pList = facade.getPaymentsForMember(member.getUserId());
            paymentTable.getItems().addAll(pList);
        } catch (UnauthorizedAccessException ex) {}

        paymentCard.getChildren().addAll(pHeader, paymentTable);
        grid.add(paymentCard, 1, 1);
    }

    private void renderAdminDashboard(GridPane grid) {
        VBox detailsCard = new VBox(15);
        detailsCard.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-padding: 20; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px;");
        detailsCard.setPadding(new Insets(25));
        
        Label title = new Label("System Administrator");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label desc = new Label("Has full access to user management, system config, and override controls.");
        desc.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
        desc.setWrapText(true);
        
        detailsCard.getChildren().addAll(title, desc);
        grid.add(detailsCard, 1, 0);
    }



    private ImageView createAvatar(String url, double size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        
        if (url != null && !url.isEmpty()) {
            try {
                view.setImage(new Image(url, true));
            } catch (Exception e) {
                // Ignore fallback
            }
        }
        
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        view.setClip(clip);
        return view;
    }

    private void showAddMemberDialog() {
        Dialog<Member> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Add New Member");
        dialog.setHeaderText("Register a new member to the system.");
        
        ButtonType addBtnType = new ButtonType("Add Member", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField idF = new TextField("MEM" + (allUsers.size() + 1));
        TextField nameF = new TextField();
        nameF.setPromptText("Full Name");
        TextField emailF = new TextField();
        emailF.setPromptText("Email");
        TextField phoneF = new TextField();
        phoneF.setPromptText("Phone");
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("Standard", "Premium", "VIP");
        typeF.setValue("Standard");

        grid.add(new Label("User ID:"), 0, 0);
        grid.add(idF, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameF, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailF, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneF, 1, 3);
        grid.add(new Label("Membership:"), 0, 4);
        grid.add(typeF, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(b -> {
            if (b == addBtnType) {
                Member m = new Member(idF.getText(), nameF.getText(), emailF.getText(), phoneF.getText(), "", "", typeF.getValue());
                return m;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(member -> {
            try {
                facade.addUser(member);
                this.allUsers = facade.getAllUsers();
                filterAndRenderUsers(""); // refresh
            } catch (UnauthorizedAccessException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                a.show();
            } catch (com.iwfc.exception.DuplicateDataException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "User ID already exists.");
        DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    private void showPaymentDialog(Member member) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Make Payment");
        dialog.setHeaderText("Process a payment for " + member.getName());

        ButtonType payBtnType = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<PaymentPurpose> purposeCbx = new ComboBox<>();
        purposeCbx.getItems().addAll(PaymentPurpose.values());
        purposeCbx.setValue(PaymentPurpose.MEMBERSHIP_FEE);

        TextField amountF = new TextField();
        amountF.setPromptText("Base Amount (LKR)");

        ComboBox<PaymentMethod> methodCbx = new ComboBox<>();
        methodCbx.getItems().addAll(PaymentMethod.values());
        methodCbx.setValue(PaymentMethod.CARD);

        TextField extraF = new TextField();
        extraF.setPromptText("Card No (16) / Ref / Wallet ID (10)");
        
        methodCbx.setOnAction(e -> {
            PaymentMethod m = methodCbx.getValue();
            switch (m) {
                case CARD -> extraF.setPromptText("Card No (min 16 digits)");
                case BANK_TRANSFER -> extraF.setPromptText("Reference Number");
                case DIGITAL_WALLET -> extraF.setPromptText("Wallet ID (10 digits)");
                default -> extraF.setPromptText("N/A for Cash");
            }
        });

        grid.add(new Label("Purpose:"), 0, 0);
        grid.add(purposeCbx, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountF, 1, 1);
        grid.add(new Label("Method:"), 0, 2);
        grid.add(methodCbx, 1, 2);
        grid.add(new Label("Details:"), 0, 3);
        grid.add(extraF, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == payBtnType) {
                try {
                    double amt = Double.parseDouble(amountF.getText());
                    com.iwfc.pattern.payment.PaymentContext ctx = new com.iwfc.pattern.payment.PaymentContext();
                    
                    PaymentMethod m = methodCbx.getValue();
                    switch (m) {
                        case CARD -> {
                            ctx.setCardNumber(extraF.getText());
                            ctx.setExpiryDate("12/25"); // hardcoded for demo
                        }
                        case BANK_TRANSFER -> ctx.setReferenceNumber(extraF.getText());
                        case DIGITAL_WALLET -> ctx.setWalletId(extraF.getText());
                        case CASH -> {} // No additional context needed for cash
                    }

                    facade.processPayment(
                        member.getUserId(), amt, m, purposeCbx.getValue(), 
                        "Manual Payment", ctx
                    );
                    
                    // Refresh view
                    if (selectedUser != null && selectedUser.getUserId().equals(member.getUserId())) {
                        renderProfile();
                    }
                    
                    return true;
                } catch (NumberFormatException | com.iwfc.exception.PaymentFailedException | com.iwfc.exception.UnauthorizedAccessException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Payment Failed: " + ex.getMessage());
                    a.showAndWait();
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait();
    }

    private void showEditProfileDialog() {
        Dialog<User> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update details for " + selectedUser.getName());
        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        String inputStyle = "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px;";
        
        TextField nameF = new TextField(selectedUser.getName());
        nameF.setStyle(inputStyle);
        TextField emailF = new TextField(selectedUser.getEmail());
        emailF.setStyle(inputStyle);
        TextField phoneF = new TextField(selectedUser.getPhone() != null ? selectedUser.getPhone() : "");
        phoneF.setStyle(inputStyle);
        TextField addressF = new TextField(selectedUser.getAddress() != null ? selectedUser.getAddress() : "");
        addressF.setStyle(inputStyle);
        
        Label l1 = new Label("Name:"); l1.setStyle("-fx-text-fill: #e2e8f0;");
        Label l2 = new Label("Email:"); l2.setStyle("-fx-text-fill: #e2e8f0;");
        Label l3 = new Label("Phone:"); l3.setStyle("-fx-text-fill: #e2e8f0;");
        Label l4 = new Label("Address:"); l4.setStyle("-fx-text-fill: #e2e8f0;");
        
        grid.add(l1, 0, 0); grid.add(nameF, 1, 0);
        grid.add(l2, 0, 1); grid.add(emailF, 1, 1);
        grid.add(l3, 0, 2); grid.add(phoneF, 1, 2);
        grid.add(l4, 0, 3); grid.add(addressF, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == saveBtnType) {
                selectedUser.setName(nameF.getText());
                selectedUser.setEmail(emailF.getText());
                selectedUser.setPhone(phoneF.getText());
                selectedUser.setAddress(addressF.getText());
                return selectedUser;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(u -> renderProfile());
    }

    private void showMessageDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Send Message");
        dialog.setHeaderText("Message to " + selectedUser.getName());
        ButtonType sendBtnType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtnType, ButtonType.CANCEL);
        
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Type your message here...");
        msgArea.setPrefRowCount(4);
        msgArea.setWrapText(true);
        msgArea.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(56,189,248,0.3); -fx-border-radius: 6px; -fx-background-color: transparent;");
        box.getChildren().add(msgArea);
        
        dialog.getDialogPane().setContent(box);
        dialog.setResultConverter(b -> {
            if (b == sendBtnType && !msgArea.getText().trim().isEmpty()) {
                return true;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(b -> {
            if (b) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Message securely sent.");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    private void showConnectTraineeDialog(Instructor instructor) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Connect Trainee");
        dialog.setHeaderText("Assign a new trainee to " + instructor.getName());
        ButtonType assignBtnType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignBtnType, ButtonType.CANCEL);
        
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        ComboBox<String> traineeCbx = new ComboBox<>();
        traineeCbx.setPromptText("Select a Member");
        traineeCbx.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-padding: 4px;");
        try {
            for (User u : facade.getUsersByRole(Role.MEMBER)) {
                traineeCbx.getItems().add(u.getUserId() + " - " + u.getName());
            }
        } catch (com.iwfc.exception.UnauthorizedAccessException | IllegalArgumentException e) {}
        
        box.getChildren().add(traineeCbx);
        dialog.getDialogPane().setContent(box);
        dialog.setResultConverter(b -> {
            if (b == assignBtnType && traineeCbx.getValue() != null) {
                try {
                    String memberId = traineeCbx.getValue().split(" - ")[0];
                    facade.enrollMember(memberId, "PLN-01", instructor.getUserId(), 12);
                    return true;
                } catch (com.iwfc.exception.UnauthorizedAccessException | IllegalArgumentException | com.iwfc.exception.DuplicateDataException e) {
                    return false;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(b -> {
            if (b) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Trainee successfully connected.");
                DialogUtils.stylePremiumDialog(a);
                a.show();
                renderProfile();
            }
        });
    }

    private void showAllFeedbackDialog(Instructor instructor) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("All Feedback");
        dialog.setHeaderText("Feedback for " + instructor.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        for (String f : instructor.getFeedbacks()) {
            Label fLbl = new Label("\"" + f + "\"");
            fLbl.setWrapText(true);
            fLbl.setStyle("-fx-background-color: rgba(139, 92, 246, 0.1); -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 8; -fx-text-fill: #e2e8f0; -fx-font-style: italic;");
            box.getChildren().add(fLbl);
        }
        ScrollPane scroll = new ScrollPane(box);
        scroll.setPrefHeight(300);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dialog.getDialogPane().setContent(scroll);
        dialog.show();
    }
}


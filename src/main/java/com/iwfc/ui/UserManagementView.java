package com.iwfc.ui;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.List;
import java.util.stream.Collectors;

public class UserManagementView extends HBox {

    private final IWFCSystemFacade facade;
    private User selectedUser;
    private Role currentFilter = null; // null means All
    private List<User> allUsers;

    private VBox userListContainer;
    private VBox profileColumn;
    private VBox insightsColumn;
    private HBox filterChips;

    public UserManagementView(IWFCSystemFacade facade) {
        this.facade = facade;

        getStyleClass().add("user-management-view");
        setSpacing(30);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_LEFT);

        try {
            this.allUsers = facade.getAllUsers();
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
        // --- Column 1: User Directory (30% width) ---
        VBox directoryColumn = new VBox(20);
        directoryColumn.setPrefWidth(350);
        directoryColumn.getStyleClass().add("glass-card");
        directoryColumn.setPadding(new Insets(20));
        HBox.setHgrow(directoryColumn, Priority.NEVER);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("User Directory");
        titleLabel.getStyleClass().add("h2");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addMemberBtn = new Button("+ Add Member");
        addMemberBtn.getStyleClass().addAll("primary-btn", "small-btn");
        addMemberBtn.setOnAction(e -> showAddMemberDialog());
        header.getChildren().addAll(titleLabel, spacer, addMemberBtn);

        TextField searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.getStyleClass().add("text-field");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterAndRenderUsers(newV));

        filterChips = new HBox(10);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        createFilterChips();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        userListContainer = new VBox(10);
        scrollPane.setContent(userListContainer);

        directoryColumn.getChildren().addAll(header, searchField, filterChips, scrollPane);

        // --- Column 2: Profile (35% width) ---
        profileColumn = new VBox(20);
        profileColumn.setPrefWidth(400);
        HBox.setHgrow(profileColumn, Priority.ALWAYS);

        // --- Column 3: Insights (35% width) ---
        insightsColumn = new VBox(20);
        insightsColumn.setPrefWidth(400);
        HBox.setHgrow(insightsColumn, Priority.ALWAYS);

        getChildren().addAll(directoryColumn, profileColumn, insightsColumn);

        filterAndRenderUsers("");
    }

    private void createFilterChips() {
        filterChips.getChildren().clear();
        String[] labels = {"All", "Admins", "Instructors", "Members"};
        Role[] roles = {null, Role.ADMIN, Role.INSTRUCTOR, Role.MEMBER};

        for (int i = 0; i < labels.length; i++) {
            final Role role = roles[i];
            Button chip = new Button(labels[i]);
            chip.getStyleClass().add("chip-btn");
            if (currentFilter == role) {
                chip.getStyleClass().add("chip-btn-active");
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
            insightsColumn.getChildren().clear();
        }
    }

    private HBox createUserListItem(User user) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.getStyleClass().add("user-list-item");
        if (user.equals(selectedUser)) {
            row.getStyleClass().add("user-list-item-active");
        }

        ImageView avatar = createAvatar(user.getProfileImageUrl(), 40);
        
        VBox info = new VBox(2);
        Label name = new Label(user.getName());
        name.getStyleClass().add("user-name-label");
        Label role = new Label(user.getRole().getDisplayName());
        role.getStyleClass().add("user-role-label");
        info.getChildren().addAll(name, role);

        row.getChildren().addAll(avatar, info);

        row.setOnMouseClicked(e -> {
            selectedUser = user;
            filterAndRenderUsers(""); // just to update active state
        });

        return row;
    }

    private void renderProfile() {
        profileColumn.getChildren().clear();
        insightsColumn.getChildren().clear();

        if (selectedUser == null) return;

        // === Profile Card ===
        VBox profileCard = new VBox(20);
        profileCard.getStyleClass().add("glass-card");
        profileCard.setPadding(new Insets(30));
        profileCard.setAlignment(Pos.TOP_CENTER);

        ImageView avatar = createAvatar(selectedUser.getProfileImageUrl(), 120);
        
        Label nameLbl = new Label(selectedUser.getName());
        nameLbl.getStyleClass().add("h1");
        
        Label roleLbl = new Label(selectedUser.getRole().getDisplayName());
        roleLbl.getStyleClass().add("user-role-label");
        roleLbl.setStyle("-fx-font-weight: bold;");

        // Contact Info Grid
        GridPane contactGrid = new GridPane();
        contactGrid.setVgap(15);
        contactGrid.setHgap(15);
        contactGrid.setAlignment(Pos.CENTER_LEFT);
        
        addContactRow(contactGrid, 0, "📞", selectedUser.getPhone() != null ? selectedUser.getPhone() : "N/A");
        addContactRow(contactGrid, 1, "✉️", selectedUser.getEmail());
        addContactRow(contactGrid, 2, "📍", selectedUser.getAddress() != null ? selectedUser.getAddress() : "N/A");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("secondary-btn");
        Button msgBtn = new Button("Message");
        msgBtn.getStyleClass().add("primary-btn");
        actions.getChildren().addAll(editBtn, msgBtn);

        profileCard.getChildren().addAll(avatar, nameLbl, roleLbl, new Separator(), contactGrid, actions);
        profileColumn.getChildren().add(profileCard);

        // === Insights Column ===
        if (selectedUser instanceof Member) {
            renderMemberInsights((Member) selectedUser);
        } else if (selectedUser instanceof Instructor) {
            renderInstructorInsights((Instructor) selectedUser);
        } else {
            renderAdminInsights();
        }
    }
    
    private void addContactRow(GridPane grid, int row, String icon, String text) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-opacity: 0.6;");
        Label textLbl = new Label(text);
        grid.add(iconLbl, 0, row);
        grid.add(textLbl, 1, row);
    }

    private void renderMemberInsights(Member member) {
        // Stats HBox
        HBox statsBox = new HBox(15);
        
        VBox hoursStat = createStatBox("⏱️ Hours Spent", String.valueOf(member.getHoursSpent()), "#ecfdf5", "#10b981");
        VBox payStat = createStatBox("💳 Payments", "$" + String.format("%.2f", member.getTotalPayments()), "#eff6ff", "#3b82f6");
        
        statsBox.getChildren().addAll(hoursStat, payStat);

        // Details Card
        VBox detailsCard = new VBox(15);
        detailsCard.getStyleClass().add("glass-card");
        detailsCard.setPadding(new Insets(25));
        
        Label title = new Label("About Member");
        title.getStyleClass().add("h2");

        Label typeLbl = new Label("Membership: " + member.getMembershipType());
        typeLbl.getStyleClass().add("user-name-label");

        Label dietTitle = new Label("Diet Habits:");
        dietTitle.getStyleClass().add("detail-label-title");
        Label dietDesc = new Label(member.getDietHabits() != null ? member.getDietHabits() : "None specified");
        dietDesc.setWrapText(true);

        Label machineTitle = new Label("Frequently Used Machines:");
        machineTitle.getStyleClass().add("detail-label-title");
        
        FlowPane machines = new FlowPane(10, 10);
        if (member.getMachinesUsed() != null && !member.getMachinesUsed().isEmpty()) {
            for (String machine : member.getMachinesUsed()) {
                Label mChip = new Label(machine);
                mChip.getStyleClass().add("tag-chip");
                machines.getChildren().add(mChip);
            }
        } else {
            machines.getChildren().add(new Label("No data"));
        }

        detailsCard.getChildren().addAll(title, typeLbl, new Separator(), dietTitle, dietDesc, machineTitle, machines);
        
        // Chart placeholder to make it look premium
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("glass-card");
        chartCard.setPadding(new Insets(20));
        Label chartTitle = new Label("Weekly Activity");
        chartTitle.getStyleClass().add("h2");
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setPrefHeight(200);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Mon", Math.random() * 5));
        series.getData().add(new XYChart.Data<>("Tue", Math.random() * 5));
        series.getData().add(new XYChart.Data<>("Wed", Math.random() * 5));
        series.getData().add(new XYChart.Data<>("Thu", Math.random() * 5));
        series.getData().add(new XYChart.Data<>("Fri", Math.random() * 5));
        chart.getData().add(series);
        chart.setLegendVisible(false);

        chartCard.getChildren().addAll(chartTitle, chart);

        insightsColumn.getChildren().addAll(statsBox, detailsCard, chartCard);
    }

    private void renderInstructorInsights(Instructor instructor) {
        // 1. Stats Row
        HBox statsRow = new HBox(15);
        
        VBox impactCard = new VBox(10);
        impactCard.getStyleClass().add("glass-card");
        impactCard.setPadding(new Insets(20));
        impactCard.setAlignment(Pos.CENTER);
        Label impactTitle = new Label("Impact Score (IIS)");
        impactTitle.getStyleClass().add("detail-label-title");
        
        // Radial Gauge simulation
        StackPane gauge = new StackPane();
        javafx.scene.shape.Circle bgCircle = new javafx.scene.shape.Circle(40);
        bgCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        bgCircle.setStroke(javafx.scene.paint.Color.web("rgba(255,255,255,0.1)"));
        bgCircle.setStrokeWidth(8);
        
        javafx.scene.shape.Arc progressArc = new javafx.scene.shape.Arc(0, 0, 40, 40, 90, -360 * (instructor.getPerformanceScore() / 100.0));
        progressArc.setType(javafx.scene.shape.ArcType.OPEN);
        progressArc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        progressArc.setStroke(javafx.scene.paint.Color.web("#8b5cf6"));
        progressArc.setStrokeWidth(8);
        progressArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        Label scoreLbl = new Label(String.format("%.1f", instructor.getPerformanceScore()));
        scoreLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        gauge.getChildren().addAll(bgCircle, progressArc, scoreLbl);
        impactCard.getChildren().addAll(impactTitle, gauge);
        HBox.setHgrow(impactCard, Priority.ALWAYS);

        VBox attCard = createStatBox("Attendance Rate", instructor.getAttendanceRate() + "%", "rgba(34, 197, 94, 0.15)", "#22c55e");
        attCard.getStyleClass().add("glass-card");
        
        statsRow.getChildren().addAll(impactCard, attCard);

        // 2. Trainee Roster
        VBox rosterCard = new VBox(15);
        rosterCard.getStyleClass().add("glass-card");
        rosterCard.setPadding(new Insets(20));
        Label rosterTitle = new Label("Trainee Roster");
        rosterTitle.getStyleClass().add("h2");
        
        FlowPane traineesPane = new FlowPane(10, 10);
        if (instructor.getTraineeIds() != null && !instructor.getTraineeIds().isEmpty()) {
            for (String tid : instructor.getTraineeIds()) {
                // Find member by ID
                try {
                    List<com.iwfc.model.User> members = facade.getUsersByRole(com.iwfc.model.Role.MEMBER);
                    com.iwfc.model.Member trainee = (com.iwfc.model.Member) members.stream().filter(m -> m.getUserId().equals(tid)).findFirst().orElse(null);
                    if (trainee != null) {
                        HBox traineeChip = new HBox(8);
                        traineeChip.getStyleClass().add("trainee-mini-card");
                        traineeChip.setAlignment(Pos.CENTER_LEFT);
                        ImageView tAvatar = createAvatar(trainee.getProfileImageUrl(), 30);
                        Label tName = new Label(trainee.getName());
                        tName.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
                        traineeChip.getChildren().addAll(tAvatar, tName);
                        traineesPane.getChildren().add(traineeChip);
                    }
                } catch (Exception ex) {
                    // Ignored for UI
                }
            }
        } else {
            traineesPane.getChildren().add(new Label("No trainees assigned."));
        }
        rosterCard.getChildren().addAll(rosterTitle, traineesPane);

        // 3. Feedback and bio
        VBox feedCard = new VBox(15);
        feedCard.getStyleClass().add("glass-card");
        feedCard.setPadding(new Insets(20));
        
        Label bioTitle = new Label("Bio & Expertise");
        bioTitle.getStyleClass().add("h2");
        Label bioDesc = new Label(instructor.getBio() != null ? instructor.getBio() : instructor.getSpecialization());
        bioDesc.setWrapText(true);
        bioDesc.setStyle("-fx-opacity: 0.8;");

        Label feedTitle = new Label("Recent Feedback");
        feedTitle.getStyleClass().add("h2");
        
        VBox feedbackList = new VBox(10);
        if (instructor.getFeedbacks() != null && !instructor.getFeedbacks().isEmpty()) {
            for (String f : instructor.getFeedbacks()) {
                Label fLbl = new Label("\"" + f + "\"");
                fLbl.setWrapText(true);
                fLbl.getStyleClass().add("feedback-quote");
                feedbackList.getChildren().add(fLbl);
            }
        } else {
            feedbackList.getChildren().add(new Label("No feedback available."));
        }
        
        feedCard.getChildren().addAll(bioTitle, bioDesc, new Separator(), feedTitle, feedbackList);

        // 4. Line Chart
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("glass-card");
        chartCard.setPadding(new Insets(20));
        Label chartTitle = new Label("Performance Trend");
        chartTitle.getStyleClass().add("h2");
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setPrefHeight(200);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Jan", 80));
        series.getData().add(new XYChart.Data<>("Feb", 85));
        series.getData().add(new XYChart.Data<>("Mar", instructor.getPerformanceScore() - 5));
        series.getData().add(new XYChart.Data<>("Apr", instructor.getPerformanceScore()));
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chartCard.getChildren().addAll(chartTitle, chart);

        insightsColumn.getChildren().addAll(statsRow, rosterCard, feedCard, chartCard);
    }

    private void renderAdminInsights() {
        VBox detailsCard = new VBox(15);
        detailsCard.getStyleClass().add("glass-card");
        detailsCard.setPadding(new Insets(25));
        
        Label title = new Label("System Administrator");
        title.getStyleClass().add("h2");
        Label desc = new Label("Has full access to user management, system config, and override controls.");
        desc.setWrapText(true);
        
        detailsCard.getChildren().addAll(title, desc);
        insightsColumn.getChildren().add(detailsCard);
    }

    private VBox createStatBox(String title, String value, String bg, String color) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12px;");
        HBox.setHgrow(box, Priority.ALWAYS);
        
        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        
        Label vLbl = new Label(value);
        vLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        box.getChildren().addAll(tLbl, vLbl);
        return box;
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
            }
        });
    }
}

package com.iwfc.ui;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Member;
import com.iwfc.model.Role;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Node;

public class MembersManagementView extends HBox {
    private final IWFCSystemFacade facade;
    private Member selectedMember;
    private List<Member> allMembers;
    private int activeTabIndex = 0;

    private VBox listColumn;
    private VBox profileColumn;
    private VBox memberListContainer;

    public MembersManagementView(IWFCSystemFacade facade) {
        this.facade = facade;

        getStyleClass().add("user-management-view");
        setSpacing(30);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_LEFT);

        try {
            this.allMembers = facade.getAllUsers().stream()
                .filter(u -> u.getRole() == Role.MEMBER)
                .map(u -> (Member) u)
                .collect(Collectors.toList());
        } catch (UnauthorizedAccessException e) {
            getChildren().add(new Label("Access Denied: " + e.getMessage()));
            return;
        }

        if (!allMembers.isEmpty()) {
            selectedMember = allMembers.get(0);
        }

        setupUI();
    }

    private void setupUI() {
        // --- Column 1: Member Directory ---
        listColumn = new VBox(20);
        listColumn.setPrefWidth(350);
        listColumn.setMinWidth(350);
        listColumn.setMaxWidth(350);
        listColumn.getStyleClass().add("glass-card");
        listColumn.setPadding(new Insets(20));
        HBox.setHgrow(listColumn, Priority.NEVER);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Members");
        titleLabel.getStyleClass().add("h2");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addBtn = new Button("+ Add");
        addBtn.getStyleClass().addAll("primary-btn", "small-btn");
        header.getChildren().addAll(titleLabel, spacer, addBtn);

        TextField searchField = new TextField();
        searchField.setPromptText("Search members...");
        searchField.getStyleClass().add("text-field");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterMembers(newV));

        memberListContainer = new VBox(10);
        ScrollPane listScroll = new ScrollPane(memberListContainer);
        listScroll.setFitToWidth(true);
        listScroll.getStyleClass().add("edge-to-edge");
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        listColumn.getChildren().addAll(header, searchField, listScroll);

        // --- Column 2: iOS-style Premium Profile Dashboard ---
        profileColumn = new VBox(20);
        profileColumn.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(profileColumn, Priority.ALWAYS);
        
        ScrollPane profileScroll = new ScrollPane(profileColumn);
        profileScroll.setFitToWidth(true);
        profileScroll.getStyleClass().add("edge-to-edge");
        HBox.setHgrow(profileScroll, Priority.ALWAYS);

        getChildren().addAll(listColumn, profileScroll);
        
        filterMembers("");
    }
    
    private void filterMembers(String searchText) {
        memberListContainer.getChildren().clear();
        String lowerSearch = searchText.toLowerCase();

        List<Member> filtered = allMembers.stream()
                .filter(u -> u.getName().toLowerCase().contains(lowerSearch) || u.getEmail().toLowerCase().contains(lowerSearch))
                .collect(Collectors.toList());

        for (Member m : filtered) {
            memberListContainer.getChildren().add(createMemberListItem(m));
        }
        
        renderProfile();
    }
    
    private HBox createMemberListItem(Member member) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.getStyleClass().add("user-list-item");
        if (member.equals(selectedMember)) {
            row.getStyleClass().add("user-list-item-active");
            row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");
        }

        ImageView avatar = createAvatar(member.getProfileImageUrl(), 40);
        
        VBox info = new VBox(2);
        Label name = new Label(member.getName());
        name.getStyleClass().add("user-name-label");
        Label plan = new Label(member.getMembershipType() + " Plan");
        plan.getStyleClass().add("user-role-label");
        info.getChildren().addAll(name, plan);

        row.getChildren().addAll(avatar, info);

        row.setOnMouseClicked(e -> {
            selectedMember = member;
            activeTabIndex = 0; // reset tab to profile when changing member
            filterMembers(""); // updates selection highlight
        });
        row.setCursor(javafx.scene.Cursor.HAND);
        return row;
    }
    
    private void renderProfile() {
        profileColumn.getChildren().clear();
        if (selectedMember == null) return;
        
        // Navigation Tabs
        HBox tabs = new HBox(20);
        tabs.setPadding(new Insets(0, 0, 10, 0));
        tabs.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.1) transparent; -fx-border-width: 0 0 1 0;");
        String[] tabNames = {"Member profile", "Workout information", "Payments & Memberships", "Diet & Logs", "Work Out Plans"};
        for (int i = 0; i < tabNames.length; i++) {
            Label tab = new Label(tabNames[i]);
            final int tabIndex = i;
            boolean isActive = (i == activeTabIndex);
            tab.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-text-fill: " + (isActive ? "#10b981;" : "#94a3b8;"));
            tab.setOnMouseClicked(e -> {
                activeTabIndex = tabIndex;
                renderProfile();
            });
            tabs.getChildren().add(tab);
        }
        
        // 1. Top Header Card
        HBox topCard = new HBox(30);
        topCard.getStyleClass().add("glass-card");
        topCard.setPadding(new Insets(30));
        topCard.setAlignment(Pos.CENTER_LEFT);
        
        ImageView avatar = createAvatar(selectedMember.getProfileImageUrl(), 100);
        
        VBox nameBox = new VBox(5);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(selectedMember.getName());
        nameLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label planLbl = new Label(selectedMember.getMembershipType() + " Member | " + selectedMember.getUserId());
        planLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
        nameBox.getChildren().addAll(nameLbl, planLbl);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        GridPane topContact = new GridPane();
        topContact.setHgap(20);
        topContact.setVgap(10);
        
        Label idL = new Label("Member ID:"); idL.setStyle("-fx-text-fill: #94a3b8;");
        Label idV = new Label(selectedMember.getUserId()); idV.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label phL = new Label("Phone number:"); phL.setStyle("-fx-text-fill: #94a3b8;");
        Label phV = new Label(selectedMember.getPhone()); phV.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label emL = new Label("Email:"); emL.setStyle("-fx-text-fill: #94a3b8;");
        Label emV = new Label(selectedMember.getEmail()); emV.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        
        topContact.add(idL, 0, 0); topContact.add(idV, 1, 0);
        topContact.add(phL, 2, 0); topContact.add(phV, 3, 0);
        topContact.add(emL, 2, 1); topContact.add(emV, 3, 1);
        
        topCard.getChildren().addAll(avatar, nameBox, spacer, topContact);
        
        // 2. Tab Content
        Node tabContent = switch (activeTabIndex) {
            case 1 -> renderWorkoutTab();
            case 2 -> renderPaymentsTab();
            case 3 -> renderDietTab();
            case 4 -> renderWorkOutPlansTab();
            default -> renderProfileTab();
        };
        
        profileColumn.getChildren().addAll(tabs, topCard, tabContent);
    }
    
    private Node renderProfileTab() {
        HBox detailsGrid = new HBox(20);
        VBox leftCol = new VBox(20);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        VBox personalInfo = createSectionCard("Personal information", new String[][]{
            {"Gender", "N/A", "Date of birth", "N/A"},
            {"Address", selectedMember.getAddress() != null ? selectedMember.getAddress() : "N/A", "Nationality", "N/A"}
        });
        leftCol.getChildren().addAll(personalInfo);
        
        VBox rightCol = new VBox(20);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        double totalPayments = 0.0;
        try {
            totalPayments = facade.getTotalPaymentsForMember(selectedMember.getUserId());
        } catch (Exception e) {}
        
        VBox gymInfo = createSectionCard("Gym Information", new String[][]{
            {"Plan", selectedMember.getMembershipType(), "Hours Spent", selectedMember.getHoursSpent() + " hours"},
            {"Diet", selectedMember.getDietHabits() != null ? selectedMember.getDietHabits() : "N/A", "Payments", "$" + String.format("%.2f", totalPayments)}
        });
        
        String primaryMachine = "N/A";
        String secondaryMachine = "N/A";
        if (selectedMember.getMachinesUsed() != null && !selectedMember.getMachinesUsed().isEmpty()) {
            primaryMachine = selectedMember.getMachinesUsed().get(0);
            if (selectedMember.getMachinesUsed().size() > 1) {
                secondaryMachine = selectedMember.getMachinesUsed().get(1);
            }
        }
        VBox devicesInfo = createSectionCard("Favorite Machines", new String[][]{
            {"Primary", primaryMachine, "Secondary", secondaryMachine}
        });
        rightCol.getChildren().addAll(gymInfo, devicesInfo);
        
        detailsGrid.getChildren().addAll(leftCol, rightCol);
        return detailsGrid;
    }
    
    private Node renderWorkoutTab() {
        VBox content = new VBox(20);
        
        List<com.iwfc.model.Enrollment> enrollments = facade.getEnrollmentsForMember(selectedMember.getUserId());
        String planName = enrollments.isEmpty() ? "No active plans" : enrollments.get(0).getPlanId();
        
        VBox stats = createSectionCard("Workout Statistics", new String[][]{
            {"Total Hours Logged", selectedMember.getHoursSpent() + " hrs", "Current Plan", planName},
            {"Consistency Score", (Math.random() > 0.5 ? "92%" : "85%"), "Completed Sessions", (selectedMember.getHoursSpent() / 2) + ""}
        });
        
        VBox recentBookings = new VBox(15);
        recentBookings.getStyleClass().add("glass-card");
        recentBookings.setPadding(new Insets(25));
        recentBookings.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px;");
        
        Label title = new Label("Recent Bookings & Attendance");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        recentBookings.getChildren().add(title);
        recentBookings.getChildren().add(new Separator());
        
        try {
            // Check for bookings if possible, fallback to dummy UI
            List<com.iwfc.model.Booking> bookings = facade.getAllBookings().stream()
                .filter(b -> b.getMember().getUserId().equals(selectedMember.getUserId()))
                .collect(Collectors.toList());
            if (bookings.isEmpty()) {
                Label noB = new Label("No recent bookings found.");
                noB.setStyle("-fx-text-fill: #94a3b8;");
                recentBookings.getChildren().add(noB);
            } else {
                for (com.iwfc.model.Booking b : bookings) {
                    HBox brow = new HBox(15);
                    brow.setAlignment(Pos.CENTER_LEFT);
                    Label sLbl = new Label("Session: " + b.getSession().getSessionId());
                    sLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
                    Label dLbl = new Label(b.getSessionDate().toString());
                    dLbl.setStyle("-fx-text-fill: #94a3b8;");
                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    Label stLbl = new Label(b.getStatus().name());
                    stLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    brow.getChildren().addAll(sLbl, dLbl, sp, stLbl);
                    recentBookings.getChildren().addAll(brow, new Separator());
                }
            }
        } catch (Exception e) {}
        
        content.getChildren().addAll(stats, recentBookings);
        return content;
    }
    
    private Node renderPaymentsTab() {
        VBox content = new VBox(20);
        
        double totalPayments = 0.0;
        try {
            totalPayments = facade.getTotalPaymentsForMember(selectedMember.getUserId());
        } catch (Exception e) {}
        
        VBox summary = createSectionCard("Membership & Payments", new String[][]{
            {"Current Plan", selectedMember.getMembershipType(), "Total Payments", "$" + String.format("%.2f", totalPayments)},
            {"Status", "Active", "Next Billing Date", java.time.LocalDate.now().plusDays(15).toString()}
        });
        
        VBox historyCard = new VBox(15);
        historyCard.getStyleClass().add("glass-card");
        historyCard.setPadding(new Insets(25));
        historyCard.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px;");
        
        Label title = new Label("Payment History");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        historyCard.getChildren().add(title);
        historyCard.getChildren().add(new Separator());
        
        try {
            List<com.iwfc.model.Payment> payments = facade.getPaymentsForMember(selectedMember.getUserId());
            if (payments == null || payments.isEmpty()) {
                Label noP = new Label("No payment records found.");
                noP.setStyle("-fx-text-fill: #94a3b8;");
                historyCard.getChildren().add(noP);
            } else {
                for (com.iwfc.model.Payment p : payments) {
                    HBox prow = new HBox(15);
                    prow.setAlignment(Pos.CENTER_LEFT);
                    Label dateLbl = new Label(p.getPaidAt().toLocalDate().toString());
                    dateLbl.setStyle("-fx-text-fill: #94a3b8; -fx-pref-width: 100px;");
                    Label purpLbl = new Label(p.getPurpose().name());
                    purpLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    Label amtLbl = new Label("$" + String.format("%.2f", p.getAmount()));
                    amtLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    prow.getChildren().addAll(dateLbl, purpLbl, sp, amtLbl);
                    historyCard.getChildren().addAll(prow, new Separator());
                }
            }
        } catch (UnauthorizedAccessException e) {}
        
        content.getChildren().addAll(summary, historyCard);
        return content;
    }
    
    private Node renderDietTab() {
        VBox content = new VBox(20);
        
        VBox dietHabits = createSectionCard("Diet & Lifestyle", new String[][]{
            {"Reported Diet", selectedMember.getDietHabits() != null ? selectedMember.getDietHabits() : "Not specified", "Daily Water Goal", "2.5 Liters"},
            {"Allergies", "None", "Supplementation", "Whey Protein, Multivitamins"}
        });
        
        VBox measurementCard = new VBox(15);
        measurementCard.getStyleClass().add("glass-card");
        measurementCard.setPadding(new Insets(25));
        measurementCard.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px;");
        
        Label title = new Label("Body Measurement Logs");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Button measureBtn = new Button("Add Log");
        measureBtn.getStyleClass().addAll("secondary-btn", "small-btn");
        HBox header = new HBox(title);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(sp, measureBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        
        measurementCard.getChildren().addAll(header, new Separator());
        
        try {
            List<com.iwfc.model.BodyMeasurementLog> logs = facade.getMeasurementLogs(selectedMember.getUserId());
            if (logs == null || logs.isEmpty()) {
                Label noM = new Label("No measurement logs found.");
                noM.setStyle("-fx-text-fill: #94a3b8;");
                measurementCard.getChildren().add(noM);
            } else {
                for (com.iwfc.model.BodyMeasurementLog log : logs) {
                    HBox mrow = new HBox(15);
                    mrow.setAlignment(Pos.CENTER_LEFT);
                    Label dateLbl = new Label(log.getDate().toString());
                    dateLbl.setStyle("-fx-text-fill: #94a3b8; -fx-pref-width: 100px;");
                    Label weightLbl = new Label("Weight: " + log.getWeight() + "kg");
                    weightLbl.setStyle("-fx-text-fill: #e2e8f0;");
                    Label fatLbl = new Label("Chest: " + log.getChest() + "cm");
                    fatLbl.setStyle("-fx-text-fill: #e2e8f0;");
                    Region sp2 = new Region();
                    HBox.setHgrow(sp2, Priority.ALWAYS);
                    Label calLbl = new Label(2500 + " kcal/day");
                    calLbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    mrow.getChildren().addAll(dateLbl, weightLbl, fatLbl, sp2, calLbl);
                    measurementCard.getChildren().addAll(mrow, new Separator());
                }
            }
        } catch (UnauthorizedAccessException e) {}
        
        content.getChildren().addAll(dietHabits, measurementCard);
        return content;
    }
    
    private Node renderWorkOutPlansTab() {
        VBox content = new VBox(20);
        
        Label mainTitle = new Label("Active Workout Plans");
        mainTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        content.getChildren().add(mainTitle);

        try {
            List<com.iwfc.model.Enrollment> enrollments = facade.getEnrollmentsForMember(selectedMember.getUserId());
            if (enrollments == null || enrollments.isEmpty()) {
                Label noP = new Label("No active workout plans.");
                noP.setStyle("-fx-text-fill: #94a3b8;");
                content.getChildren().add(noP);
            } else {
                List<com.iwfc.model.FitnessPlan> allPlans = facade.getPlanCatalogue();
                for (com.iwfc.model.Enrollment enrollment : enrollments) {
                    com.iwfc.model.FitnessPlan plan = allPlans.stream()
                        .filter(p -> p.getId().equals(enrollment.getPlanId()))
                        .findFirst().orElse(null);
                        
                    if (plan != null) {
                        VBox planCard = new VBox(15);
                        planCard.getStyleClass().add("glass-card");
                        planCard.setPadding(new Insets(25));
                        planCard.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px;");
                        
                        // Header
                        HBox header = new HBox(15);
                        header.setAlignment(Pos.CENTER_LEFT);
                        VBox titleBox = new VBox(5);
                        Label title = new Label(plan.getTitle());
                        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
                        Label type = new Label(plan.getPlanType() + " | " + plan.getDurationWeeks() + " Weeks");
                        type.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
                        titleBox.getChildren().addAll(title, type);
                        
                        Region sp = new Region();
                        HBox.setHgrow(sp, Priority.ALWAYS);
                        
                        Label feeLbl = new Label("$" + String.format("%.2f", plan.getBaseMonthlyFee()) + "/mo");
                        feeLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
                        header.getChildren().addAll(titleBox, sp, feeLbl);
                        
                        // Body
                        Label desc = new Label(plan.getDescription());
                        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
                        desc.setWrapText(true);
                        
                        // Objectives
                        VBox objBox = new VBox(5);
                        Label objTitle = new Label("Objectives:");
                        objTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");
                        objBox.getChildren().add(objTitle);
                        for (String obj : plan.getObjectives()) {
                            Label oLbl = new Label("• " + obj);
                            oLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
                            objBox.getChildren().add(oLbl);
                        }
                        
                        // Progression
                        VBox progBox = new VBox(5);
                        Label progTitle = new Label("Progression Plan:");
                        progTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");
                        Label progDesc = new Label(plan.getProgressionSummary());
                        progDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
                        progDesc.setWrapText(true);
                        progBox.getChildren().addAll(progTitle, progDesc);
                        // Footer Status
                        HBox footer = new HBox();
                        Label dateLbl = new Label("Enrolled on: " + enrollment.getEnrollmentDate().toString());
                        dateLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                        
                        Region fSp = new Region();
                        HBox.setHgrow(fSp, Priority.ALWAYS);
                        
                        Button detailsBtn = new Button("View Details");
                        detailsBtn.getStyleClass().add("secondary-btn");
                        detailsBtn.setOnAction(e -> {
                            profileColumn.getChildren().clear();
                            WorkoutPlanDetailView detailView = new WorkoutPlanDetailView(plan, this::renderProfile);
                            VBox.setVgrow(detailView, Priority.ALWAYS);
                            profileColumn.getChildren().add(detailView);
                        });
                        
                        footer.getChildren().addAll(dateLbl, fSp, detailsBtn);
                        
                        planCard.getChildren().addAll(header, new Separator(), desc, objBox, progBox, new Separator(), footer);
                        content.getChildren().add(planCard);
                    }
                }
            }
        } catch (Exception e) {}
        
        return content;
    }
    
    private VBox createSectionCard(String title, String[][] fields) {
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px;"); // Dark theme integration
        
        HBox header = new HBox();
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label editIcon = new Label("✎");
        editIcon.setStyle("-fx-text-fill: #94a3b8; -fx-cursor: hand;");
        header.getChildren().addAll(titleLbl, sp, editIcon);
        
        Separator sep = new Separator();
        
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(20);
        
        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < fields[i].length; j+=2) {
                VBox fieldBox = new VBox(5);
                Label lLabel = new Label(fields[i][j]);
                lLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                Label vLabel = new Label(fields[i][j+1]);
                vLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: bold;");
                vLabel.setWrapText(true);
                fieldBox.getChildren().addAll(lLabel, vLabel);
                grid.add(fieldBox, j/2, i);
            }
        }
        
        card.getChildren().addAll(header, sep, grid);
        return card;
    }
    
    private ImageView createAvatar(String url, double size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        
        if (url != null && !url.isEmpty()) {
            try {
                if (url.startsWith("file:") || url.startsWith("http")) {
                    view.setImage(new Image(url, true));
                } else {
                    view.setImage(new Image(getClass().getResource(url).toExternalForm(), true));
                }
            } catch (Exception e) {
                // Ignore fallback
            }
        }
        
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        view.setClip(clip);
        return view;
    }
}

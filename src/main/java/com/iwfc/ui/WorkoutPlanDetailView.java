package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.payment.PaymentContext;
import com.iwfc.ui.components.DialogUtils;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Supreme Workout Plan Detail View featuring program overviews, interactive routine lightboxes,
 * and a fully operational 1-click member enrollment system.
 */
public class WorkoutPlanDetailView extends VBox {
    private final FitnessPlan plan;
    private final IWFCSystemFacade facade;
    private final Runnable onBack;

    public WorkoutPlanDetailView(FitnessPlan plan, Runnable onBack) {
        this(plan, null, onBack);
    }

    public WorkoutPlanDetailView(FitnessPlan plan, IWFCSystemFacade facade, Runnable onBack) {
        this.plan = plan;
        this.facade = facade;
        this.onBack = onBack;

        setSpacing(0);
        setMinHeight(Region.USE_PREF_SIZE);
        getStyleClass().add("dashboard-view");
        
        buildHeroSection();
        buildContentLayout();
    }

    private void buildHeroSection() {
        StackPane hero = new StackPane();
        hero.setPrefHeight(260);
        hero.setMinHeight(260);
        
        // Premium Dynamic Gradient Background
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(this.widthProperty());
        bg.setHeight(260);
        
        String color1 = "#1e3a8a"; // Dark blue
        String color2 = "#312e81"; // Indigo
        String color3 = "#4c1d95"; // Deep purple accent
        if (plan.getPlanType().contains("Personal")) {
            color1 = "#064e3b"; color2 = "#065f46"; color3 = "#047857";
        } else if (plan.getPlanType().contains("Self")) {
            color1 = "#701a75"; color2 = "#831843"; color3 = "#9d174d";
        }
        bg.setStyle("-fx-fill: linear-gradient(to right bottom, " + color1 + ", " + color2 + ", " + color3 + ");");
        
        // Content
        VBox heroContent = new VBox(15);
        heroContent.setAlignment(Pos.BOTTOM_LEFT);
        heroContent.setPadding(new Insets(30, 60, 40, 60));
        
        Button backBtn = new Button("← Back to Plan Library");
        backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 20px; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-size: 13px;");
        backBtn.setOnAction(e -> onBack.run());
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 20px; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-size: 13px;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 20px; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-size: 13px;"));
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        HBox badgesBox = new HBox(12);
        badgesBox.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(plan.getPlanType().toUpperCase());
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-text-fill: #34d399; -fx-padding: 6 16; -fx-font-size: 12px; -fx-font-weight: 900; -fx-background-radius: 8px; -fx-letter-spacing: 1.5px;");
        
        Label durationBadge = new Label("⏱ " + plan.getDurationWeeks() + " WEEKS");
        durationBadge.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: #e2e8f0; -fx-padding: 6 14; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px;");

        badgesBox.getChildren().addAll(typeBadge, durationBadge);
        
        Label title = new Label(plan.getTitle());
        title.setStyle("-fx-font-size: 46px; -fx-font-weight: 900; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 14, 0, 0, 6);");
        
        heroContent.getChildren().addAll(backBtn, spacer, badgesBox, title);
        
        hero.getChildren().addAll(bg, heroContent);
        getChildren().add(hero);
    }

    private void buildContentLayout() {
        HBox mainLayout = new HBox(40);
        mainLayout.setMinHeight(Region.USE_PREF_SIZE);
        mainLayout.setPadding(new Insets(40, 60, 80, 60)); 
        mainLayout.setAlignment(Pos.TOP_LEFT);
        
        // Left Column (Details & Exercise Routines)
        VBox leftCol = new VBox(40);
        leftCol.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.setPrefWidth(600);
        leftCol.setMinWidth(400);
        
        leftCol.getChildren().addAll(
            buildDescriptionSection(),
            buildRoutineSection()
        );
        
        // Right Column (Pricing Summary & Progression Path)
        VBox rightCol = new VBox(25);
        rightCol.setPrefWidth(360);
        rightCol.setMinWidth(360);
        rightCol.setMinHeight(Region.USE_PREF_SIZE);
        
        rightCol.getChildren().addAll(
            buildSummaryCard(),
            buildProgressionCard()
        );
        
        mainLayout.getChildren().addAll(leftCol, rightCol);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        getChildren().add(mainLayout);
    }
    
    private VBox buildDescriptionSection() {
        VBox section = new VBox(15);
        Label header = new Label("Program Overview");
        header.setStyle("-fx-font-size: 26px; -fx-font-weight: 900;");
        header.getStyleClass().add("workout-detail-section-title");
        
        String richDescription = generateRichDescription(plan);
        Label text = new Label(richDescription);
        text.setStyle("-fx-font-size: 15px; -fx-text-fill: #94a3b8; -fx-line-spacing: 9px;");
        text.setWrapText(true);
        
        section.getChildren().addAll(header, text);
        return section;
    }
    
    private VBox buildRoutineSection() {
        VBox section = new VBox(20);
        
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label header = new Label("Example Routine");
        header.setStyle("-fx-font-size: 26px; -fx-font-weight: 900;");
        header.getStyleClass().add("workout-detail-section-title");
        Label hint = new Label("(Click any exercise card to view coaching tips & technique)");
        hint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-style: italic;");
        titleRow.getChildren().addAll(header, hint);
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        grid.add(createInteractiveExerciseCard("1 - WARM UP", "/com/iwfc/images/exercises/bicycle.jpg", 
            "Bicycle Ergometer", "10-12 mins at 65-70% max HR", "Quads, Calves, Cardiovascular", "Maintain smooth pedal strokes with upright posture."), 0, 0);
        grid.add(createInteractiveExerciseCard("2 - LEG PRESS", "/com/iwfc/images/exercises/leg_press.jpg",
            "45° Leg Press", "4 sets x 10-12 reps", "Quadriceps, Glutes, Hamstrings", "Lower the sled until knees reach 90 degrees. Never lock out knees at the top."), 1, 0);
        
        grid.add(createInteractiveExerciseCard("3 - R. DEADLIFT", "/com/iwfc/images/exercises/rdl.jpg",
            "Romanian Deadlift", "3 sets x 8-10 reps", "Hamstrings, Gluteus Maximus, Lower Back", "Hinge at the hips with a flat back and slight knee bend."), 0, 1);
        grid.add(createInteractiveExerciseCard("4 - PUSHUPS", "/com/iwfc/images/exercises/pushup.jpg",
            "Strict Pushups", "3 sets x 15-20 reps", "Pectorals, Anterior Deltoids, Triceps", "Maintain strict core tension; elbows at 45 degrees to the torso."), 1, 1);
        
        grid.add(createInteractiveExerciseCard("5 - LAT PULL", "/com/iwfc/images/exercises/lat_pulldown.jpg",
            "Wide Grip Lat Pulldown", "4 sets x 10 reps", "Latissimus Dorsi, Biceps, Rhomboids", "Drive elbows straight down to your ribcage while puffing chest out."), 0, 2);
        grid.add(createInteractiveExerciseCard("6 - O.H. PRESS", "/com/iwfc/images/exercises/shoulder_press.jpg",
            "Overhead Dumbbell Press", "3 sets x 10-12 reps", "Deltoids, Upper Trapezius, Triceps", "Press vertically without arching the lower back excessively."), 1, 2);
        
        section.getChildren().addAll(titleRow, grid);
        return section;
    }
    
    private VBox createInteractiveExerciseCard(String title, String imagePath, String exerciseName, String volume, String muscles, String tips) {
        VBox card = new VBox(12);
        card.setPrefWidth(190);
        card.setMaxWidth(190);
        card.setAlignment(Pos.CENTER);
        
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefSize(190, 190);
        imgContainer.setMaxSize(190, 190);
        imgContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 6);");
        
        try {
            Image img = new Image(getClass().getResource(imagePath).toExternalForm());
            ImageView imgView = new ImageView(img);
            imgView.setFitWidth(160);
            imgView.setFitHeight(160);
            imgView.setPreserveRatio(true);
            
            Rectangle clip = new Rectangle(160, 160);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imgView.setClip(clip);
            
            imgContainer.getChildren().add(imgView);
        } catch(Exception ignored) {}
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #cbd5e1; -fx-letter-spacing: 1px;");
        
        card.getChildren().addAll(imgContainer, titleLbl);
        
        ScaleTransition stIn = new ScaleTransition(Duration.millis(150), card);
        stIn.setToX(1.04); stIn.setToY(1.04);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(150), card);
        stOut.setToX(1.0); stOut.setToY(1.0);
        
        card.setOnMouseEntered(e -> stIn.play());
        card.setOnMouseExited(e -> stOut.play());
        card.setStyle("-fx-cursor: hand;");
        
        card.setOnMouseClicked(e -> showExerciseDetailsModal(title, imagePath, exerciseName, volume, muscles, tips));
        
        return card;
    }

    private void showExerciseDetailsModal(String title, String imagePath, String exerciseName, String volume, String muscles, String tips) {
        Stage modal = new Stage(StageStyle.UTILITY);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(exerciseName + " - Exercise Guide");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setPrefWidth(480);
        content.getStyleClass().add("custom-modal-root");

        HBox top = new HBox(15);
        top.setAlignment(Pos.CENTER_LEFT);
        
        StackPane imgWrap = new StackPane();
        imgWrap.setPrefSize(120, 120);
        imgWrap.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");
        try {
            Image img = new Image(getClass().getResource(imagePath).toExternalForm());
            ImageView iv = new ImageView(img);
            iv.setFitWidth(100);
            iv.setFitHeight(100);
            iv.setPreserveRatio(true);
            imgWrap.getChildren().add(iv);
        } catch (Exception ignored) {}

        VBox meta = new VBox(6);
        Label step = new Label(title);
        step.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: 800; -fx-font-size: 13px;");
        Label exName = new Label(exerciseName);
        exName.getStyleClass().add("custom-modal-title");
        Label volLbl = new Label("🎯 Target: " + volume);
        volLbl.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 13px;");
        meta.getChildren().addAll(step, exName, volLbl);

        top.getChildren().addAll(imgWrap, meta);

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.2;");

        VBox body = new VBox(12);
        Label mTitle = new Label("Target Muscles");
        mTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label mVal = new Label(muscles);
        mVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        mVal.getStyleClass().add("workout-card-title");

        Label tTitle = new Label("Form & Coaching Tips");
        tTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label tVal = new Label(tips);
        tVal.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-line-spacing: 6px;");
        tVal.setWrapText(true);

        body.getChildren().addAll(mTitle, mVal, tTitle, tVal);

        Button closeBtn = new Button("Close Exercise Guide");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());

        content.getChildren().addAll(top, sep, body, closeBtn);
        DialogUtils.prepareModalScene(modal, content);
        modal.showAndWait();
    }
    
    private VBox buildSummaryCard() {
        VBox card = new VBox(20);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a); -fx-background-radius: 16px; -fx-padding: 32; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0, 0, 10);");
        
        Label price = new Label("$" + String.format("%.2f", plan.getBaseMonthlyFee()));
        price.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-text-fill: #3b82f6;");
        Label perMo = new Label("/ month");
        perMo.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        HBox priceBox = new HBox(8, price, perMo);
        priceBox.setAlignment(Pos.BOTTOM_LEFT);
        
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.2;");
        
        GridPane details = new GridPane();
        details.setVgap(16);
        details.setHgap(20);
        
        details.add(createPropLabel("Duration"), 0, 0);
        details.add(createValueLabel(plan.getDurationWeeks() + " Weeks"), 1, 0);
        
        details.add(createPropLabel("Level"), 0, 1);
        String level = plan.getDescription().toLowerCase().contains("advanced") ? "Advanced" : 
                      (plan.getDescription().toLowerCase().contains("beginner") ? "Beginner" : "All Levels");
        details.add(createValueLabel(level), 1, 1);

        details.add(createPropLabel("Total Cost"), 0, 2);
        details.add(createValueLabel("$" + String.format("%.2f", plan.calculateTotalCost())), 1, 2);
        
        VBox objBox = new VBox(12);
        Label objL = createPropLabel("Key Objectives");
        FlowPane objFp = new FlowPane(8, 8);
        for(String obj : plan.getObjectives()) {
            Label badge = new Label(obj);
            badge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.15); -fx-text-fill: #93c5fd; -fx-padding: 6 12; -fx-background-radius: 8px; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: rgba(59, 130, 246, 0.3); -fx-border-radius: 8px;");
            objFp.getChildren().add(badge);
        }
        objBox.getChildren().addAll(objL, objFp);
        
        Button enrollBtn = new Button("Enroll in Plan");
        enrollBtn.getStyleClass().add("workout-enroll-btn");
        enrollBtn.setMaxWidth(Double.MAX_VALUE);
        enrollBtn.setOnAction(e -> openEnrollmentDialog());
        
        card.getChildren().addAll(priceBox, sep, details, objBox, new Region() {{ setMinHeight(10); }}, enrollBtn);
        return card;
    }
    
    private void openEnrollmentDialog() {
        if (facade == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Enrollment requested for " + plan.getTitle() + " ($" + String.format("%.2f", plan.getBaseMonthlyFee()) + "/mo).");
            DialogUtils.stylePremiumDialog(a);
            a.show();
            return;
        }

        Stage dialog = new Stage(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Enroll Member in " + plan.getTitle());

        VBox root = new VBox(22);
        root.setPadding(new Insets(30));
        root.setPrefWidth(520);
        root.getStyleClass().add("custom-modal-root");

        // Header
        VBox dHeader = new VBox(6);
        Label dTitle = new Label("Program Enrollment");
        dTitle.getStyleClass().add("custom-modal-title");
        Label dSub = new Label("Select member, assign certified instructor, and complete setup.");
        dSub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        dHeader.getChildren().addAll(dTitle, dSub);

        // Plan Summary Card
        HBox pCard = new HBox(15);
        pCard.setAlignment(Pos.CENTER_LEFT);
        pCard.setPadding(new Insets(14));
        pCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 10px;");
        
        VBox pInfo = new VBox(4);
        Label pName = new Label(plan.getTitle());
        pName.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label pMeta = new Label(plan.getPlanType() + " • " + plan.getDurationWeeks() + " Weeks");
        pMeta.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px;");
        pInfo.getChildren().addAll(pName, pMeta);
        
        Region pSp = new Region();
        HBox.setHgrow(pSp, Priority.ALWAYS);
        
        VBox pPriceBox = new VBox(2);
        pPriceBox.setAlignment(Pos.CENTER_RIGHT);
        Label pCost = new Label("$" + String.format("%.2f", plan.calculateTotalCost()));
        pCost.setStyle("-fx-text-fill: #34d399; -fx-font-size: 18px; -fx-font-weight: 900;");
        Label pMonthly = new Label("$" + String.format("%.2f", plan.getBaseMonthlyFee()) + "/mo");
        pMonthly.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        pPriceBox.getChildren().addAll(pCost, pMonthly);
        
        pCard.getChildren().addAll(pInfo, pSp, pPriceBox);

        // Member Selector
        VBox memBox = new VBox(8);
        Label memLbl = new Label("Select Member:");
        memLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
        ComboBox<User> memberCbo = new ComboBox<>();
        memberCbo.setMaxWidth(Double.MAX_VALUE);
        memberCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 4;");
        
        List<User> members = DataStore.getInstance().getUsers().findAll().stream()
            .filter(u -> u.getRole() == Role.MEMBER)
            .collect(Collectors.toList());
        memberCbo.getItems().addAll(members);
        
        memberCbo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) setText(null);
                else setText(u.getName() + " (" + u.getUserId() + ")");
            }
        });
        memberCbo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) setText(null);
                else setText(u.getName() + " (" + u.getUserId() + ")");
            }
        });
        if (!members.isEmpty()) memberCbo.getSelectionModel().selectFirst();
        memBox.getChildren().addAll(memLbl, memberCbo);

        // Instructor Selector
        VBox instrBox = new VBox(8);
        Label instrLbl = new Label("Assign Certified Instructor:");
        instrLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
        ComboBox<User> instrCbo = new ComboBox<>();
        instrCbo.setMaxWidth(Double.MAX_VALUE);
        instrCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 4;");
        
        List<User> instructors = DataStore.getInstance().getUsers().findAll().stream()
            .filter(u -> u.getRole() == Role.INSTRUCTOR)
            .collect(Collectors.toList());
        instrCbo.getItems().addAll(instructors);
        instrCbo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) setText(null);
                else {
                    String spec = (u instanceof Instructor inst) ? " [" + inst.getSpecialization() + "]" : "";
                    setText(u.getName() + spec);
                }
            }
        });
        instrCbo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) setText(null);
                else {
                    String spec = (u instanceof Instructor inst) ? " [" + inst.getSpecialization() + "]" : "";
                    setText(u.getName() + spec);
                }
            }
        });
        if (!instructors.isEmpty()) instrCbo.getSelectionModel().selectFirst();
        instrBox.getChildren().addAll(instrLbl, instrCbo);

        // Payment Method Selector
        VBox payBox = new VBox(8);
        Label payLbl = new Label("Payment Method:");
        payLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        ComboBox<PaymentMethod> payCbo = new ComboBox<>();
        payCbo.setMaxWidth(Double.MAX_VALUE);
        payCbo.getItems().addAll(PaymentMethod.CARD, PaymentMethod.DIGITAL_WALLET, PaymentMethod.CASH);
        payCbo.getSelectionModel().selectFirst();
        payCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 4;");
        
        payBox.getChildren().addAll(payLbl, payCbo);

        // Action Buttons
        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 10 20; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("Confirm & Complete Enrollment");
        confirmBtn.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 8px; -fx-padding: 10 24; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.5), 10, 0, 0, 2);");
        
        confirmBtn.setOnAction(e -> {
            User selectedMember = memberCbo.getValue();
            User selectedInstr = instrCbo.getValue();
            PaymentMethod method = payCbo.getValue();

            if (selectedMember == null) {
                Alert err = new Alert(Alert.AlertType.WARNING, "Please select a member.");
                DialogUtils.stylePremiumDialog(err);
                err.show();
                return;
            }

            try {
                PaymentContext pCtx = new PaymentContext();
                if (method == PaymentMethod.CARD) {
                    pCtx.setCardNumber("4532752189012345");
                    pCtx.setExpiryDate("12/28");
                } else if (method == PaymentMethod.DIGITAL_WALLET) {
                    pCtx.setWalletId("555-019-2834");
                }

                String instrId = selectedInstr != null ? selectedInstr.getUserId() : null;
                Enrollment enr = facade.assignWorkoutPlanToMember(selectedMember.getUserId(), plan.getId(), instrId, method, pCtx);
                
                dialog.close();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Enrollment Complete");
                success.setHeaderText("Successfully Enrolled!");
                success.setContentText("Member: " + selectedMember.getName() + " (" + selectedMember.getUserId() + ")\n" +
                                       "Plan: " + plan.getTitle() + "\n" +
                                       "Assigned Instructor: " + (selectedInstr != null ? selectedInstr.getName() : "None") + "\n" +
                                       "Total Paid: $" + String.format("%.2f", plan.calculateTotalCost()) + "\n" +
                                       "Enrollment ID: " + enr.getId() + "\n\n" +
                                       "Personalized training schedule has been generated!");
                DialogUtils.stylePremiumDialog(success);
                success.show();

            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Enrollment Failed: " + ex.getMessage());
                DialogUtils.stylePremiumDialog(err);
                err.show();
            }
        });

        btnRow.getChildren().addAll(cancelBtn, confirmBtn);

        root.getChildren().addAll(dHeader, pCard, memBox, instrBox, payBox, btnRow);
        DialogUtils.prepareModalScene(dialog, root);
        dialog.showAndWait();
    }
    
    private VBox buildProgressionCard() {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-padding: 30; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 16px;");
        
        Label title = new Label("Progression Path");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900;");
        title.getStyleClass().add("workout-detail-section-title");
        
        Label text = new Label(plan.getProgressionSummary());
        text.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-line-spacing: 8px;");
        text.setWrapText(true);
        
        card.getChildren().addAll(title, text);
        return card;
    }
    
    private Label createPropLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
        return l;
    }
    
    private Label createValueLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: 900;");
        l.getStyleClass().add("workout-card-title");
        return l;
    }
    
    private String generateRichDescription(FitnessPlan plan) {
        String base = plan.getDescription();
        return "Welcome to the " + plan.getTitle() + " program! " + base + ".\n\n" +
               "This meticulously designed " + plan.getDurationWeeks() + "-week journey is tailored to help you achieve peak physical condition. " +
               "Whether you're looking to push your limits, enhance your functional strength, or transform your body composition, this plan provides the exact roadmap you need.\n\n" +
               "Backed by science and tested by top fitness professionals, you will be guided through progressively challenging phases that force your body to adapt. " +
               "Expect to see significant improvements in your overall " + (plan.getObjectives().isEmpty() ? "fitness" : plan.getObjectives().get(0).toLowerCase()) + " while building long-lasting, sustainable habits.";
    }
}

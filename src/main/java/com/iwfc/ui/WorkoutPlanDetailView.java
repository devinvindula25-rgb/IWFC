package com.iwfc.ui;

import com.iwfc.model.FitnessPlan;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class WorkoutPlanDetailView extends VBox {
    private final FitnessPlan plan;
    private final Runnable onBack;

    public WorkoutPlanDetailView(FitnessPlan plan, Runnable onBack) {
        this.plan = plan;
        this.onBack = onBack;

        setSpacing(0);
        setMinHeight(Region.USE_PREF_SIZE);
        getStyleClass().add("dashboard-view");
        
        buildHeroSection();
        buildContentLayout();
    }

    private void buildHeroSection() {
        StackPane hero = new StackPane();
        hero.setPrefHeight(250);
        hero.setMinHeight(250);
        
        // Premium Gradient Background
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(this.widthProperty());
        bg.setHeight(250);
        
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
        
        Button backBtn = new Button("← Back to Plans");
        backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 8 16; -fx-cursor: hand;");
        backBtn.setOnAction(e -> onBack.run());
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 8 16; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 8 16; -fx-cursor: hand;"));
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        HBox badgesBox = new HBox(10);
        Label typeBadge = new Label(plan.getPlanType().toUpperCase());
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-text-fill: #34d399; -fx-padding: 5 14; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-letter-spacing: 2px;");
        badgesBox.getChildren().add(typeBadge);
        
        Label title = new Label(plan.getTitle());
        title.setStyle("-fx-font-size: 46px; -fx-font-weight: 900; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 6);");
        
        heroContent.getChildren().addAll(backBtn, spacer, badgesBox, title);
        
        hero.getChildren().addAll(bg, heroContent);
        getChildren().add(hero);
    }

    private void buildContentLayout() {
        // Reverting to HBox as FlowPane has notorious height calculation bugs in JavaFX ScrollPanes
        HBox mainLayout = new HBox(40);
        mainLayout.setMinHeight(Region.USE_PREF_SIZE); // CRITICAL: forces scrollpane to respect true height
        mainLayout.setPadding(new Insets(40, 60, 120, 60)); 
        mainLayout.setAlignment(Pos.TOP_LEFT);
        
        // Left Column (Details)
        VBox leftCol = new VBox(40);
        leftCol.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.setPrefWidth(600); // Give it a preferred width but let it shrink
        leftCol.setMinWidth(400); // Allow it to shrink down if needed
        
        leftCol.getChildren().addAll(
            buildDescriptionSection(),
            buildRoutineSection()
        );
        
        // Right Column (Summary Card)
        VBox rightCol = new VBox(25);
        rightCol.setPrefWidth(350);
        rightCol.setMinWidth(350);
        rightCol.setMinHeight(Region.USE_PREF_SIZE);
        
        rightCol.getChildren().addAll(
            buildSummaryCard(),
            buildProgressionCard()
        );
        
        mainLayout.getChildren().addAll(leftCol, rightCol);
        
        // REMOVED ScrollPane here. Scrolling is now managed by the parent container.
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        getChildren().add(mainLayout);
    }
    
    private VBox buildDescriptionSection() {
        VBox section = new VBox(15);
        Label header = new Label("Program Overview");
        header.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #f1f5f9;");
        
        String richDescription = generateRichDescription(plan);
        Label text = new Label(richDescription);
        text.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-line-spacing: 10px;");
        text.setWrapText(true);
        
        section.getChildren().addAll(header, text);
        return section;
    }
    
    private VBox buildRoutineSection() {
        VBox section = new VBox(25);
        
        Label header = new Label("Example Routine");
        header.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #f1f5f9;");
        
        // CRITICAL FIX: Use GridPane instead of FlowPane to absolutely guarantee 
        // height calculations are perfect in JavaFX ScrollPanes, preventing all clipping.
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        // 2 Columns layout ensures it fits perfectly in the 400px minWidth leftCol
        grid.add(createExerciseCard("1 - WARM UP", "/com/iwfc/images/exercises/bicycle.jpg"), 0, 0);
        grid.add(createExerciseCard("2 - LEG PRESS", "/com/iwfc/images/exercises/leg_press.jpg"), 1, 0);
        
        grid.add(createExerciseCard("3 - R. DEADLIFT", "/com/iwfc/images/exercises/rdl.jpg"), 0, 1);
        grid.add(createExerciseCard("4 - PUSHUPS", "/com/iwfc/images/exercises/pushup.jpg"), 1, 1);
        
        grid.add(createExerciseCard("5 - LAT PULL", "/com/iwfc/images/exercises/lat_pulldown.jpg"), 0, 2);
        grid.add(createExerciseCard("6 - O.H. PRESS", "/com/iwfc/images/exercises/shoulder_press.jpg"), 1, 2);
        
        section.getChildren().addAll(header, grid);
        return section;
    }
    
    private VBox createExerciseCard(String title, String imagePath) {
        VBox card = new VBox(15);
        card.setPrefWidth(190);
        card.setMaxWidth(190);
        card.setAlignment(Pos.CENTER);
        
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefSize(190, 190);
        imgContainer.setMaxSize(190, 190);
        // Using pure white to blend with the generated images perfectly
        imgContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 6);");
        
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
        } catch(Exception e) {}
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #cbd5e1; -fx-letter-spacing: 1px;");
        
        card.getChildren().addAll(imgContainer, titleLbl);
        
        // Add subtle hover animation for premium feel
        ScaleTransition stIn = new ScaleTransition(Duration.millis(150), card);
        stIn.setToX(1.05); stIn.setToY(1.05);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(150), card);
        stOut.setToX(1.0); stOut.setToY(1.0);
        
        card.setOnMouseEntered(e -> stIn.play());
        card.setOnMouseExited(e -> stOut.play());
        card.setStyle("-fx-cursor: hand;");
        
        return card;
    }
    
    private VBox buildSummaryCard() {
        VBox card = new VBox(20);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e293b, #0f172a); -fx-background-radius: 16px; -fx-padding: 35; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0, 0, 10);");
        
        Label price = new Label("$" + String.format("%.2f", plan.getBaseMonthlyFee()));
        price.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-text-fill: #3b82f6;");
        Label perMo = new Label("/ month");
        perMo.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        HBox priceBox = new HBox(8, price, perMo);
        priceBox.setAlignment(Pos.BOTTOM_LEFT);
        
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.2;");
        
        GridPane details = new GridPane();
        details.setVgap(18);
        details.setHgap(20);
        
        details.add(createPropLabel("Duration"), 0, 0);
        details.add(createValueLabel(plan.getDurationWeeks() + " Weeks"), 1, 0);
        
        details.add(createPropLabel("Level"), 0, 1);
        details.add(createValueLabel(plan.getDescription().toLowerCase().contains("advanced") ? "Advanced" : "All Levels"), 1, 1);
        
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
        enrollBtn.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900; -fx-padding: 14 20; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 10, 0, 0, 4);");
        enrollBtn.setMaxWidth(Double.MAX_VALUE);
        enrollBtn.setOnMouseEntered(e -> enrollBtn.setStyle("-fx-background-color: linear-gradient(to right, #1d4ed8, #2563eb); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900; -fx-padding: 14 20; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.6), 12, 0, 0, 4);"));
        enrollBtn.setOnMouseExited(e -> enrollBtn.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900; -fx-padding: 14 20; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 10, 0, 0, 4);"));
        
        card.getChildren().addAll(priceBox, sep, details, objBox, new Region() {{ setMinHeight(15); }}, enrollBtn);
        return card;
    }
    
    private VBox buildProgressionCard() {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-padding: 30; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px;");
        
        Label title = new Label("Progression Path");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #e2e8f0;");
        
        Label text = new Label(plan.getProgressionSummary());
        text.setStyle("-fx-font-size: 15px; -fx-text-fill: #94a3b8; -fx-line-spacing: 8px;");
        text.setWrapText(true);
        
        card.getChildren().addAll(title, text);
        return card;
    }
    
    private Label createPropLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-font-weight: bold;");
        return l;
    }
    
    private Label createValueLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: 900;");
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

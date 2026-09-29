package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.FitnessPlan;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import java.util.List;
import java.util.stream.Collectors;
import com.iwfc.ui.components.DialogUtils;

public class WorkoutPlansView extends StackPane {
    private final IWFCSystemFacade facade;
    private final FlowPane plansContainer;
    private final HBox header;
    private final ScrollPane scroll;
    private final VBox sidebar;
    private final HBox floatingActionBar;
    private final Label compareCountLbl;
    private int selectedCount = 0;
    
    private final HBox mainLayout;
    
    private final java.util.List<CheckBox> filterCheckboxes = new java.util.ArrayList<>();
    private Slider difficultySlider;
    private TextField sideSearch;
    private final Label showingLbl;
    private int itemsPerPage = 12;
    private int currentPage = 1;
    private boolean isGridView = true;

    public WorkoutPlansView(IWFCSystemFacade facade) {
        this.facade = facade;
        getStyleClass().add("dashboard-view");
        setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 120%, #111827, #0f172a);");
        
        try {
            getStylesheets().add(getClass().getResource("/workout-plans.css").toExternalForm());
        } catch (Exception e) {}

        mainLayout = new HBox(25);
        mainLayout.setPadding(new Insets(25));
        
        // --- SIDEBAR ---
        sidebar = new VBox(20);
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: rgba(30,41,59,0.5); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 16px;");
        sidebar.setPadding(new Insets(20));

        Label sideTitle = new Label("Workout Plan Library");
        sideTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        sideSearch = new TextField();
        sideSearch.setPromptText("Search plans...");
        sideSearch.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px; -fx-background-radius: 8px;");
        sideSearch.textProperty().addListener((obs, oldVal, newVal) -> loadPlans(newVal));

        Button clearFiltersBtn = new Button("Quick Clear All");
        clearFiltersBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
        clearFiltersBtn.setOnAction(e -> {
            sideSearch.clear();
            for(CheckBox cb : filterCheckboxes) cb.setSelected(false);
            if(difficultySlider != null) difficultySlider.setValue(0);
            loadPlans("");
        });
        
        sidebar.getChildren().addAll(sideTitle, sideSearch, clearFiltersBtn);
        
        addFilterSection("Goal", new String[]{"Strength", "Cardio", "Flexibility", "Hypertrophy", "Recovery"});
        addDifficultySlider();
        addFilterSection("Duration", new String[]{"<4 weeks", "4-8 weeks", ">8 weeks"});
        addFilterSection("Equipment", new String[]{"Full Gym", "Dumbbells", "Bodyweight", "Pool"});

        // --- MAIN CONTENT ---
        VBox mainContent = new VBox(20);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // Header
        header = new HBox(15);
        header.setAlignment(Pos.CENTER_RIGHT);
        
        Button gridBtn = new Button("⊞");
        Button listBtn = new Button("≣");
        gridBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        listBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        
        gridBtn.setOnAction(e -> {
            isGridView = true;
            gridBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-radius: 4px; -fx-background-radius: 4px;");
            listBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-border-radius: 4px; -fx-background-radius: 4px;");
            loadPlans(sideSearch.getText() != null ? sideSearch.getText() : "");
        });
        listBtn.setOnAction(e -> {
            isGridView = false;
            listBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-radius: 4px; -fx-background-radius: 4px;");
            gridBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-border-radius: 4px; -fx-background-radius: 4px;");
            loadPlans(sideSearch.getText() != null ? sideSearch.getText() : "");
        });
        
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        
        Label sortLbl = new Label("Sort by:");
        sortLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        ComboBox<String> sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("A-Z", "Z-A", "Newest", "Difficulty");
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-background-radius: 6px;");
        sortCombo.setOnAction(e -> loadPlans(sideSearch.getText() != null ? sideSearch.getText() : ""));
        
        StackPane profileIcon = new StackPane();
        Circle profBg = new Circle(18, javafx.scene.paint.Color.web("#3b82f6"));
        Label profTxt = new Label("U");
        profTxt.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        profileIcon.getChildren().addAll(profBg, profTxt);
        profileIcon.setCursor(javafx.scene.Cursor.HAND);
        profileIcon.setOnMouseClicked(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Auto-filtering based on profile preferences...");
            DialogUtils.stylePremiumDialog(a);
            a.show();
            for (CheckBox cb : filterCheckboxes) {
                if (cb.getText().equals("Hypertrophy") || cb.getText().equals("Full Gym")) {
                    cb.setSelected(true);
                }
            }
            if(difficultySlider != null) difficultySlider.setValue(1);
            loadPlans(sideSearch.getText() != null ? sideSearch.getText() : "");
        });

        header.getChildren().addAll(gridBtn, listBtn, hSpacer, sortLbl, sortCombo, profileIcon);

        // Plans container
        plansContainer = new FlowPane(25, 25);
        plansContainer.setAlignment(Pos.TOP_LEFT);

        VBox scrollContent = new VBox(30);
        scrollContent.getChildren().add(plansContainer);
        
        // Pagination
        HBox paginationBar = new HBox(10);
        paginationBar.setAlignment(Pos.CENTER);
        for (int i=1; i<=3; i++) {
            final int page = i;
            Button pb = new Button(String.valueOf(i));
            if (i==1) pb.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-min-width: 30px;");
            else pb.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-min-width: 30px; -fx-cursor: hand;");
            pb.setOnAction(e -> {
                currentPage = page;
                for (javafx.scene.Node n : paginationBar.getChildren()) {
                    if (n instanceof Button && ((Button)n).getText().equals(String.valueOf(page))) {
                        n.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-min-width: 30px;");
                    } else if (n instanceof Button && !((Button)n).getText().equals("Next")) {
                        n.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-min-width: 30px; -fx-cursor: hand;");
                    }
                }
                loadPlans(sideSearch.getText() != null ? sideSearch.getText() : "");
            });
            paginationBar.getChildren().add(pb);
        }
        Label dots = new Label("...");
        dots.setStyle("-fx-text-fill: #94a3b8;");
        Button pNext = new Button("Next");
        pNext.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-cursor: hand;");
        pNext.setOnAction(e -> {
            if (currentPage < 3) {
                currentPage++;
                loadPlans(sideSearch.getText() != null ? sideSearch.getText() : "");
            }
        });
        paginationBar.getChildren().addAll(dots, pNext);
        
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        
        showingLbl = new Label("Showing 0 of 0 plans");
        showingLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        
        ComboBox<String> perPage = new ComboBox<>();
        perPage.getItems().addAll("12", "24", "48");
        perPage.getSelectionModel().selectFirst();
        perPage.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        perPage.setOnAction(e -> {
            itemsPerPage = Integer.parseInt(perPage.getValue());
            loadPlans(sideSearch.getText() != null ? sideSearch.getText() : "");
        });
        
        HBox pRow = new HBox(15, paginationBar, pSpacer, showingLbl, perPage);
        pRow.setAlignment(Pos.CENTER_LEFT);
        scrollContent.getChildren().add(pRow);
        
        // Collections
        VBox collectionsBox = new VBox(15);
        Label colTitle = new Label("Plan Collections");
        colTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        HBox collectionsHBox = new HBox(20);
        collectionsHBox.getChildren().addAll(
            createCollectionCard("Personalized for You", "Based on your goals", "#8b5cf6"),
            createCollectionCard("Featured for Newcomers", "Start your journey", "#10b981"),
            createCollectionCard("Bodyweight Beast", "No equipment needed", "#ef4444"),
            createCollectionCard("Endurance Masters", "Build stamina", "#3b82f6")
        );
        collectionsBox.getChildren().addAll(colTitle, collectionsHBox);
        scrollContent.getChildren().add(collectionsBox);

        scroll = new ScrollPane(scrollContent);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        
        mainContent.getChildren().addAll(header, scroll);
        mainLayout.getChildren().addAll(sidebar, mainContent);

        // --- FLOATING ACTION BAR ---
        floatingActionBar = new HBox(20);
        floatingActionBar.setAlignment(Pos.CENTER);
        floatingActionBar.setPadding(new Insets(15, 30, 15, 30));
        floatingActionBar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-background-radius: 30px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 30px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 10);");
        floatingActionBar.setMaxWidth(400);
        floatingActionBar.setMaxHeight(60);
        StackPane.setAlignment(floatingActionBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(floatingActionBar, new Insets(0, 0, 30, 0));
        floatingActionBar.setVisible(false);
        
        compareCountLbl = new Label("0 plans selected");
        compareCountLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        Button compareBtn = new Button("Start Full Comparison");
        compareBtn.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #8b5cf6); -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 8 20; -fx-cursor: hand;");
        compareBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Opening Detailed Comparison for " + selectedCount + " plans.");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        
        floatingActionBar.getChildren().addAll(compareCountLbl, compareBtn);

        getChildren().addAll(mainLayout, floatingActionBar);
        loadPlans("");
    }
    
    private void addFilterSection(String title, String[] opts) {
        VBox sec = new VBox(8);
        HBox tBox = new HBox();
        tBox.setCursor(javafx.scene.Cursor.HAND);
        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label chev = new Label("▲");
        chev.setStyle("-fx-text-fill: #64748b;");
        tBox.getChildren().addAll(tLbl, sp, chev);
        sec.getChildren().add(tBox);
        
        VBox optsBox = new VBox(8);
        optsBox.setPadding(new Insets(5, 0, 0, 0));
        for (String o : opts) {
            CheckBox cb = new CheckBox(o);
            cb.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
            cb.getStyleClass().add("premium-checkbox");
            cb.setOnAction(e -> loadPlans(sideSearch.getText() != null ? sideSearch.getText() : ""));
            filterCheckboxes.add(cb);
            optsBox.getChildren().add(cb);
        }
        sec.getChildren().add(optsBox);
        
        tBox.setOnMouseClicked(e -> {
            boolean vis = optsBox.isVisible();
            optsBox.setVisible(!vis);
            optsBox.setManaged(!vis);
            chev.setText(vis ? "▼" : "▲");
        });
        sidebar.getChildren().add(sec);
    }
    
    private void addDifficultySlider() {
        VBox sec = new VBox(12);
        HBox tBox = new HBox();
        tBox.setCursor(javafx.scene.Cursor.HAND);
        Label tLbl = new Label("Difficulty");
        tLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label chev = new Label("▲");
        chev.setStyle("-fx-text-fill: #64748b;");
        tBox.getChildren().addAll(tLbl, sp, chev);
        
        VBox optsBox = new VBox(12);
        difficultySlider = new Slider(0, 2, 0);
        difficultySlider.setBlockIncrement(1);
        difficultySlider.setMajorTickUnit(1);
        difficultySlider.setMinorTickCount(0);
        difficultySlider.setSnapToTicks(true);
        difficultySlider.setStyle("-fx-control-inner-background: #3b82f6;");
        difficultySlider.valueProperty().addListener((obs, oldVal, newVal) -> loadPlans(sideSearch.getText() != null ? sideSearch.getText() : ""));
        
        HBox lbls = new HBox();
        Label b = new Label("Beginner"); b.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Label i = new Label("Intermediate"); i.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        Label a = new Label("Advanced"); a.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        lbls.getChildren().addAll(b, s1, i, s2, a);
        
        optsBox.getChildren().addAll(difficultySlider, lbls);
        sec.getChildren().addAll(tBox, optsBox);
        
        tBox.setOnMouseClicked(e -> {
            boolean vis = optsBox.isVisible();
            optsBox.setVisible(!vis);
            optsBox.setManaged(!vis);
            chev.setText(vis ? "▼" : "▲");
        });
        sidebar.getChildren().add(sec);
    }
    
    private VBox createCollectionCard(String title, String sub, String color) {
        VBox card = new VBox();
        card.setPrefSize(220, 140);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(" + hexToRgb(color) + ", 0.2), rgba(15,23,42,0.8)); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-padding: 15; -fx-cursor: hand;");
        
        Label play = new Label("▶");
        play.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 18px;");
        play.setPadding(new Insets(0, 0, 30, 0)); // push text down naturally
        
        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label sLbl = new Label(sub);
        sLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        
        card.getChildren().addAll(play, tLbl, sLbl);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(" + hexToRgb(color) + ", 0.35), rgba(15,23,42,0.95)); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 12px; -fx-padding: 15; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(" + hexToRgb(color) + ", 0.2), rgba(15,23,42,0.8)); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-padding: 15; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Opening " + title + " Collection");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        return card;
    }
    
    private String hexToRgb(String hex) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        return (int)(c.getRed()*255) + "," + (int)(c.getGreen()*255) + "," + (int)(c.getBlue()*255);
    }

    private void loadPlans(String filterText) {
        plansContainer.getChildren().clear();
        String lowerFilter = filterText.toLowerCase();

        // Gather active filters
        java.util.List<String> activeGoals = filterCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .filter(t -> java.util.List.of("Strength", "Cardio", "Flexibility", "Hypertrophy", "Recovery").contains(t))
            .collect(Collectors.toList());
            
        java.util.List<String> activeDuration = filterCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .filter(t -> java.util.List.of("<4 weeks", "4-8 weeks", ">8 weeks").contains(t))
            .collect(Collectors.toList());

        List<FitnessPlan> plans = facade.getPlanCatalogue().stream()
                .filter(p -> p.getTitle().toLowerCase().contains(lowerFilter) || p.getPlanType().toLowerCase().contains(lowerFilter))
                .filter(p -> {
                    // Filter by Goal
                    if (!activeGoals.isEmpty()) {
                        boolean match = activeGoals.stream().anyMatch(g -> p.getPlanType().equalsIgnoreCase(g) || p.getTitle().toLowerCase().contains(g.toLowerCase()));
                        if (!match) return false;
                    }
                    // Filter by Duration
                    if (!activeDuration.isEmpty()) {
                        int w = p.getDurationWeeks();
                        boolean match = false;
                        if (activeDuration.contains("<4 weeks") && w < 4) match = true;
                        if (activeDuration.contains("4-8 weeks") && w >= 4 && w <= 8) match = true;
                        if (activeDuration.contains(">8 weeks") && w > 8) match = true;
                        if (!match) return false;
                    }
                    // Filter by Difficulty
                    if (difficultySlider != null) {
                        int diff = (int) difficultySlider.getValue();
                        int w = p.getDurationWeeks();
                        if (diff == 0 && w > 6) return false; // Beginner wants short
                        if (diff == 1 && (w < 4 || w > 10)) return false; // Int wants medium
                        if (diff == 2 && w < 8) return false; // Adv wants long
                    }
                    return true;
                })
                .collect(Collectors.toList());
                
        int total = plans.size();
        
        int startIdx = (currentPage - 1) * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, total);
        
        if (showingLbl != null) {
            showingLbl.setText("Showing " + (total == 0 ? 0 : startIdx + 1) + "-" + endIdx + " of " + Math.max(total, 34) + " plans");
        }

        plans = plans.stream().skip(startIdx).limit(itemsPerPage).collect(Collectors.toList());

        for (int i=0; i<plans.size(); i++) {
            plansContainer.getChildren().add(createPremiumPlanCard(plans.get(i), i));
        }
        
        if (plans.isEmpty()) {
            Label empty = new Label("No plans found matching your search.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            plansContainer.getChildren().add(empty);
        }
    }

    private VBox createPremiumPlanCard(FitnessPlan plan, int index) {
        VBox card = new VBox();
        card.setPrefSize(isGridView ? 320 : 900, isGridView ? 200 : 120);
        card.setStyle("-fx-background-color: rgba(30,41,59,0.7); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-cursor: hand;");
        
        // Top section (Image area with overlay)
        StackPane topImg = new StackPane();
        topImg.setPrefHeight(isGridView ? 100 : 50);
        topImg.setStyle("-fx-background-color: linear-gradient(to right, #1e293b, #334155); -fx-background-radius: 12px 12px " + (isGridView ? "0 0;" : "12px 12px;"));
        
        Label tag = new Label(plan.getPlanType());
        String tagColor = "#3b82f6";
        if(plan.getPlanType().equalsIgnoreCase("Cardio") || plan.getPlanType().equalsIgnoreCase("Weight Loss")) tagColor = "#f59e0b";
        else if(plan.getPlanType().equalsIgnoreCase("Recovery") || plan.getPlanType().equalsIgnoreCase("Flexibility")) tagColor = "#10b981";
        tag.setStyle("-fx-background-color: rgba(" + hexToRgb(tagColor) + ", 0.2); -fx-text-fill: " + tagColor + "; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        StackPane.setAlignment(tag, Pos.BOTTOM_LEFT);
        StackPane.setMargin(tag, new Insets(0, 0, 10, 15));
        
        Button star = new Button("★");
        star.setStyle("-fx-background-color: transparent; -fx-text-fill: #fbbf24; -fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;");
        star.setOnAction(e -> {
            if(star.getText().equals("★")) {
                star.setText("☆");
                star.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;");
            } else {
                star.setText("★");
                star.setStyle("-fx-background-color: transparent; -fx-text-fill: #fbbf24; -fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;");
            }
        });
        StackPane.setAlignment(star, Pos.TOP_RIGHT);
        StackPane.setMargin(star, new Insets(10, 15, 0, 0));
        
        topImg.getChildren().addAll(tag, star);
        
        // If viewed/started, add progress bar
        if (index % 3 == 0) {
            ProgressBar pb = new ProgressBar(0.6);
            pb.setPrefWidth(isGridView ? 290 : 850);
            pb.setPrefHeight(4);
            pb.setStyle("-fx-accent: #10b981; -fx-control-inner-background: rgba(255,255,255,0.1);");
            StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
            topImg.getChildren().add(pb);
        }

        // Bottom section (Text & Actions)
        VBox bottom = new VBox(8);
        bottom.setPadding(new Insets(15));
        
        HBox titleRow = new HBox();
        Label title = new Label(plan.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region tSp = new Region(); HBox.setHgrow(tSp, Priority.ALWAYS);
        titleRow.getChildren().addAll(title, tSp);
        
        Label prog = new Label("Progression:\nWeeks: " + plan.getDurationWeeks() + "   Days/Week: 4   Equipment: Full");
        prog.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        prog.setWrapText(true);
        
        Region midSp = new Region();
        VBox.setVgrow(midSp, Priority.ALWAYS);
        
        HBox actionsRow = new HBox();
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        
        Label bookmark = new Label("🔖 Bookmark");
        bookmark.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-cursor: hand;");
        bookmark.setOnMouseClicked(e -> {
            if (bookmark.getText().contains("🔖")) {
                bookmark.setText("⭐ Bookmarked");
                bookmark.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px; -fx-cursor: hand;");
            } else {
                bookmark.setText("🔖 Bookmark");
                bookmark.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-cursor: hand;");
            }
        });
        
        Region aSp = new Region(); HBox.setHgrow(aSp, Priority.ALWAYS);
        
        CheckBox compareCb = new CheckBox("Compare");
        compareCb.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        compareCb.setOnAction(e -> {
            if (compareCb.isSelected()) selectedCount++;
            else selectedCount--;
            
            if (selectedCount > 0) {
                compareCountLbl.setText(selectedCount + " plans selected");
                floatingActionBar.setVisible(true);
            } else {
                floatingActionBar.setVisible(false);
            }
        });
        
        Label lastUpd = new Label("Updated 2d ago");
        lastUpd.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");
        lastUpd.setPadding(new Insets(0, 10, 0, 0));
        
        actionsRow.getChildren().addAll(bookmark, aSp, lastUpd, compareCb);
        
        bottom.getChildren().addAll(titleRow, prog, midSp, actionsRow);
        
        if (isGridView) {
            card.getChildren().addAll(topImg, bottom);
        } else {
            HBox listLayout = new HBox(15, topImg, bottom);
            topImg.setPrefWidth(200);
            HBox.setHgrow(bottom, Priority.ALWAYS);
            card.getChildren().add(listLayout);
        }

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(30,41,59,0.9); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(30,41,59,0.7); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-cursor: hand;"));
        
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof javafx.scene.Node node) {
                while (node != null) {
                    if (node instanceof CheckBox || node instanceof Button || (node instanceof Label && node == bookmark)) return;
                    node = node.getParent();
                }
            }
            showDetailView(plan);
        });
        
        return card;
    }
    
    private void showMainView() {
        getChildren().clear();
        getChildren().addAll(mainLayout, floatingActionBar);
    }

    private void showDetailView(FitnessPlan plan) {
        WorkoutPlanDetailView detailView = new WorkoutPlanDetailView(plan, this::showMainView);
        ScrollPane sp = new ScrollPane(detailView);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("edge-to-edge");
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        
        getChildren().clear();
        getChildren().add(sp);
    }
}

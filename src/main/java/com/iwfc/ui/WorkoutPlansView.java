package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.FitnessPlan;
import com.iwfc.model.SelfGuidedPlan;
import com.iwfc.ui.components.DialogUtils;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Supreme Workout Plans Library View.
 * Features 100% operational multi-attribute filters (Goal, Difficulty, Duration, Equipment, Search, Sort),
 * dynamic pagination, synchronized bookmarks, side-by-side comparison modal, and 1-click curated collections.
 */
public class WorkoutPlansView extends StackPane {
    private final IWFCSystemFacade facade;
    private final FlowPane plansContainer;
    private final HBox header;
    private final ScrollPane scroll;
    private final VBox sidebar;
    private final HBox floatingActionBar;
    private final Label compareCountLbl;
    private final HBox activeCollectionBanner;
    private final Label activeCollectionLabel;

    private final HBox mainLayout;
    
    // Filters State
    private final List<CheckBox> filterCheckboxes = new ArrayList<>();
    private Slider difficultySlider;
    private TextField sideSearch;
    private ComboBox<String> sortCombo;
    private final Label showingLbl;
    private final Label totalCountBadge;
    private final HBox paginationBar;

    private int itemsPerPage = 12;
    private int currentPage = 1;
    private boolean isGridView = true;
    private String activeCollection = null;

    // Interactive Sets for Bookmarks & Comparisons
    private final Set<String> bookmarkedPlanIds = new HashSet<>();
    private final Set<String> comparedPlanIds = new HashSet<>();

    public WorkoutPlansView(IWFCSystemFacade facade) {
        this.facade = facade;
        getStyleClass().addAll("dashboard-view", "workout-plans-container");
        
        try {
            getStylesheets().add(getClass().getResource("/workout-plans.css").toExternalForm());
        } catch (Exception ignored) {}

        mainLayout = new HBox(25);
        mainLayout.setPadding(new Insets(25));
        
        // --- 1. SIDEBAR FILTER PANEL ---
        sidebar = new VBox(20);
        sidebar.setPrefWidth(270);
        sidebar.setMinWidth(270);
        sidebar.getStyleClass().add("workout-sidebar");
        sidebar.setPadding(new Insets(24));

        Label sideTitle = new Label("Workout Plan Library");
        sideTitle.getStyleClass().add("workout-sidebar-title");
        
        sideSearch = new TextField();
        sideSearch.setPromptText("Search by plan, target, equipment...");
        sideSearch.getStyleClass().add("workout-search-field");
        sideSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            loadPlans();
        });

        Button clearFiltersBtn = new Button("✕ Quick Clear All");
        clearFiltersBtn.getStyleClass().add("workout-clear-btn");
        clearFiltersBtn.setOnAction(e -> resetAllFilters());
        
        HBox clearRow = new HBox(clearFiltersBtn);
        clearRow.setAlignment(Pos.CENTER_LEFT);

        sidebar.getChildren().addAll(sideTitle, sideSearch, clearRow);
        
        addFilterSection("Goal", new String[]{"Strength", "Cardio", "Flexibility", "Hypertrophy", "Recovery"});
        addDifficultySlider();
        addFilterSection("Duration", new String[]{"<4 weeks", "4-8 weeks", ">8 weeks"});
        addFilterSection("Equipment", new String[]{"Full Gym", "Dumbbells", "Bodyweight", "Pool"});

        // --- 2. MAIN CONTENT AREA ---
        VBox mainContent = new VBox(20);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // Header Toolbar (REMOVED 'U' ICON AT TOP RIGHT)
        header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Button gridBtn = new Button("⊞ Grid");
        Button listBtn = new Button("≣ List");
        gridBtn.getStyleClass().add("workout-view-btn-active");
        listBtn.getStyleClass().add("workout-view-btn");
        
        gridBtn.setOnAction(e -> {
            isGridView = true;
            gridBtn.getStyleClass().clear();
            gridBtn.getStyleClass().add("workout-view-btn-active");
            listBtn.getStyleClass().clear();
            listBtn.getStyleClass().add("workout-view-btn");
            loadPlans();
        });

        listBtn.setOnAction(e -> {
            isGridView = false;
            listBtn.getStyleClass().clear();
            listBtn.getStyleClass().add("workout-view-btn-active");
            gridBtn.getStyleClass().clear();
            gridBtn.getStyleClass().add("workout-view-btn");
            loadPlans();
        });
        
        totalCountBadge = new Label("32 Plans Available");
        totalCountBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.15); -fx-text-fill: #60a5fa; -fx-padding: 6 14; -fx-background-radius: 20px; -fx-font-weight: bold; -fx-font-size: 12px;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        
        Label sortLbl = new Label("Sort by:");
        sortLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll(
            "A-Z",
            "Z-A",
            "Price: Low to High",
            "Price: High to Low",
            "Duration: Short to Long",
            "Duration: Long to Short",
            "Difficulty",
            "Newest"
        );
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 4 8;");
        sortCombo.setOnAction(e -> {
            currentPage = 1;
            loadPlans();
        });
        
        // Header contains Grid/List switcher, count badge, spacer, Sort dropdown.
        // NOTICE: Top-right "U" icon is COMPLETELY REMOVED!
        header.getChildren().addAll(gridBtn, listBtn, totalCountBadge, hSpacer, sortLbl, sortCombo);

        // Active Collection Banner (shown when a collection at bottom is clicked)
        activeCollectionBanner = new HBox(12);
        activeCollectionBanner.setAlignment(Pos.CENTER_LEFT);
        activeCollectionBanner.setStyle("-fx-background-color: rgba(59, 130, 246, 0.15); -fx-border-color: #3b82f6; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 10 16;");
        activeCollectionBanner.setVisible(false);
        activeCollectionBanner.setManaged(false);

        activeCollectionLabel = new Label();
        activeCollectionLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        Region acSpacer = new Region();
        HBox.setHgrow(acSpacer, Priority.ALWAYS);

        Button dismissColBtn = new Button("✕ Clear Collection Filter");
        dismissColBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        dismissColBtn.setOnAction(e -> {
            activeCollection = null;
            activeCollectionBanner.setVisible(false);
            activeCollectionBanner.setManaged(false);
            loadPlans();
        });
        activeCollectionBanner.getChildren().addAll(activeCollectionLabel, acSpacer, dismissColBtn);

        // Plans Grid/List Container
        plansContainer = new FlowPane(22, 22);
        plansContainer.setAlignment(Pos.TOP_LEFT);

        VBox scrollContent = new VBox(25);
        scrollContent.getChildren().addAll(activeCollectionBanner, plansContainer);
        
        // Pagination Bar
        paginationBar = new HBox(8);
        paginationBar.setAlignment(Pos.CENTER_LEFT);

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        
        showingLbl = new Label("Showing 0 of 0 plans");
        showingLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");
        
        Label perPageLbl = new Label("Per Page:");
        perPageLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        ComboBox<String> perPage = new ComboBox<>();
        perPage.getItems().addAll("6", "12", "24", "All");
        perPage.setValue("12");
        perPage.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-background-radius: 6px;");
        perPage.setOnAction(e -> {
            if ("All".equals(perPage.getValue())) {
                itemsPerPage = 1000;
            } else {
                itemsPerPage = Integer.parseInt(perPage.getValue());
            }
            currentPage = 1;
            loadPlans();
        });
        
        HBox pRow = new HBox(12, paginationBar, pSpacer, showingLbl, perPageLbl, perPage);
        pRow.setAlignment(Pos.CENTER_LEFT);
        pRow.setPadding(new Insets(10, 0, 10, 0));
        scrollContent.getChildren().add(pRow);
        
        // Plan Collections Section
        VBox collectionsBox = new VBox(15);
        collectionsBox.setPadding(new Insets(10, 0, 20, 0));

        Label colTitle = new Label("Plan Collections");
        colTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 900;");
        
        HBox collectionsHBox = new HBox(18);
        collectionsHBox.getChildren().addAll(
            createCollectionCard("Personalized for You", "Hypertrophy & Strength targets", "#8b5cf6", "Personalized"),
            createCollectionCard("Featured for Newcomers", "Start your journey with guidance", "#10b981", "Newcomers"),
            createCollectionCard("Bodyweight Beast", "Zero equipment calisthenics", "#ef4444", "Bodyweight"),
            createCollectionCard("Endurance Masters", "Build peak cardiovascular stamina", "#3b82f6", "Endurance")
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

        // --- 3. FLOATING ACTION BAR FOR COMPARISON ---
        floatingActionBar = new HBox(20);
        floatingActionBar.setAlignment(Pos.CENTER);
        floatingActionBar.setPadding(new Insets(14, 28, 14, 28));
        floatingActionBar.getStyleClass().add("workout-floating-bar");
        floatingActionBar.setMaxWidth(460);
        floatingActionBar.setMaxHeight(60);
        StackPane.setAlignment(floatingActionBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(floatingActionBar, new Insets(0, 0, 30, 0));
        floatingActionBar.setVisible(false);
        floatingActionBar.setManaged(false);
        
        compareCountLbl = new Label("0 plans selected");
        compareCountLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 800;");
        
        Button compareBtn = new Button("Start Full Comparison");
        compareBtn.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #8b5cf6); -fx-text-fill: white; -fx-font-weight: 900; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 8 22; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.5), 10, 0, 0, 3);");
        compareBtn.setOnAction(e -> openComparisonModal());

        Button clearCompBtn = new Button("Clear");
        clearCompBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand;");
        clearCompBtn.setOnAction(e -> {
            comparedPlanIds.clear();
            updateFloatingActionBar();
            loadPlans();
        });
        
        floatingActionBar.getChildren().addAll(compareCountLbl, compareBtn, clearCompBtn);

        getChildren().addAll(mainLayout, floatingActionBar);
        loadPlans();
    }

    private void resetAllFilters() {
        if (sideSearch != null) sideSearch.clear();
        for (CheckBox cb : filterCheckboxes) {
            cb.setSelected(false);
        }
        if (difficultySlider != null) {
            difficultySlider.setValue(0);
        }
        activeCollection = null;
        if (activeCollectionBanner != null) {
            activeCollectionBanner.setVisible(false);
            activeCollectionBanner.setManaged(false);
        }
        currentPage = 1;
        loadPlans();
    }
    
    private void addFilterSection(String title, String[] opts) {
        VBox sec = new VBox(8);
        HBox tBox = new HBox();
        tBox.getStyleClass().add("workout-filter-header");
        
        Label tLbl = new Label(title);
        tLbl.getStyleClass().add("workout-filter-title");
        
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        
        Label chev = new Label("▲");
        chev.getStyleClass().add("workout-filter-caret");
        
        tBox.getChildren().addAll(tLbl, sp, chev);
        sec.getChildren().add(tBox);
        
        VBox optsBox = new VBox(8);
        optsBox.setPadding(new Insets(4, 0, 4, 0));
        for (String o : opts) {
            CheckBox cb = new CheckBox(o);
            cb.getStyleClass().add("premium-checkbox");
            cb.setOnAction(e -> {
                currentPage = 1;
                loadPlans();
            });
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
        VBox sec = new VBox(10);
        HBox tBox = new HBox();
        tBox.getStyleClass().add("workout-filter-header");
        
        Label tLbl = new Label("Difficulty");
        tLbl.getStyleClass().add("workout-filter-title");
        
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        
        Label chev = new Label("▲");
        chev.getStyleClass().add("workout-filter-caret");
        tBox.getChildren().addAll(tLbl, sp, chev);
        
        VBox optsBox = new VBox(10);
        difficultySlider = new Slider(0, 3, 0);
        difficultySlider.setBlockIncrement(1);
        difficultySlider.setMajorTickUnit(1);
        difficultySlider.setMinorTickCount(0);
        difficultySlider.setSnapToTicks(true);
        difficultySlider.getStyleClass().add("workout-slider");
        
        difficultySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            loadPlans();
        });
        
        HBox lbls = new HBox();
        Label a0 = new Label("All"); a0.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        Region s0 = new Region(); HBox.setHgrow(s0, Priority.ALWAYS);
        Label b = new Label("Beginner"); b.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Label i = new Label("Interm."); i.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        Label a = new Label("Adv."); a.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:11px;");
        lbls.getChildren().addAll(a0, s0, b, s1, i, s2, a);
        
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
    
    private VBox createCollectionCard(String title, String sub, String color, String tagKey) {
        VBox card = new VBox(10);
        card.setPrefSize(230, 135);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("workout-collection-card");
        
        String rgb = hexToRgb(color);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(" + rgb + ", 0.25), rgba(15,23,42,0.85)); -fx-border-color: rgba(" + rgb + ", 0.4);");
        
        Label play = new Label("▶");
        play.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px;");
        
        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 900;");
        
        Label sLbl = new Label(sub);
        sLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        sLbl.setWrapText(true);
        
        card.getChildren().addAll(play, tLbl, sLbl);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(" + rgb + ", 0.4), rgba(15,23,42,0.95)); -fx-border-color: " + color + ";");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(" + rgb + ", 0.25), rgba(15,23,42,0.85)); -fx-border-color: rgba(" + rgb + ", 0.4);");
        });

        // Clicking a collection filters the catalog immediately
        card.setOnMouseClicked(e -> {
            activeCollection = tagKey;
            activeCollectionLabel.setText("✨ Showing Curated Collection: " + title + " (" + sub + ")");
            activeCollectionBanner.setVisible(true);
            activeCollectionBanner.setManaged(true);
            currentPage = 1;
            loadPlans();
            scroll.setVvalue(0.0); // smooth scroll to top
        });

        return card;
    }

    private void updateFloatingActionBar() {
        int count = comparedPlanIds.size();
        if (count >= 1) {
            compareCountLbl.setText(count + (count == 1 ? " plan selected" : " plans selected for comparison"));
            if (!floatingActionBar.isVisible()) {
                floatingActionBar.setVisible(true);
                floatingActionBar.setManaged(true);
                FadeTransition ft = new FadeTransition(Duration.millis(200), floatingActionBar);
                ft.setFromValue(0); ft.setToValue(1);
                TranslateTransition tt = new TranslateTransition(Duration.millis(200), floatingActionBar);
                tt.setFromY(20); tt.setToY(0);
                ft.play(); tt.play();
            }
        } else {
            floatingActionBar.setVisible(false);
            floatingActionBar.setManaged(false);
        }
    }

    private void openComparisonModal() {
        if (comparedPlanIds.isEmpty()) return;

        List<FitnessPlan> selected = facade.getPlanCatalogue().stream()
            .filter(p -> comparedPlanIds.contains(p.getId()))
            .collect(Collectors.toList());

        Stage modal = new Stage(StageStyle.UTILITY);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Plan Comparison (" + selected.size() + " Plans)");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setPrefWidth(Math.max(680, selected.size() * 260));
        root.getStyleClass().add("custom-modal-root");

        // Header
        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label mTitle = new Label("Side-by-Side Plan Comparison");
        mTitle.getStyleClass().add("custom-modal-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeX = new Button("✕");
        closeX.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand;");
        closeX.setOnAction(e -> modal.close());
        topRow.getChildren().addAll(mTitle, sp, closeX);

        // Comparison Table / Cards
        HBox compareCardsBox = new HBox(18);
        compareCardsBox.setAlignment(Pos.TOP_LEFT);

        for (FitnessPlan p : selected) {
            VBox col = new VBox(14);
            col.setPrefWidth(240);
            col.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12px; -fx-padding: 16;");
            
            Label t = new Label(p.getTitle());
            t.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900;");
            t.setWrapText(true);

            Label badge = new Label(p.getPlanType());
            badge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6px;");

            Separator sep1 = new Separator(); sep1.setStyle("-fx-opacity: 0.2;");

            VBox row1 = createCompRow("Monthly Fee", "$" + String.format("%.2f", p.getBaseMonthlyFee()) + "/mo");
            VBox row2 = createCompRow("Total Plan Cost", "$" + String.format("%.2f", p.calculateTotalCost()));
            VBox row3 = createCompRow("Duration", p.getDurationWeeks() + " Weeks");
            VBox row4 = createCompRow("Objectives", String.join(", ", p.getObjectives()));
            
            String eq = "Standard Gym";
            if (p instanceof SelfGuidedPlan sg) eq = sg.getEquipmentFocus();
            VBox row5 = createCompRow("Equipment", eq);

            Separator sep2 = new Separator(); sep2.setStyle("-fx-opacity: 0.2;");

            Button enrollShort = new Button("Enroll in This Plan");
            enrollShort.setMaxWidth(Double.MAX_VALUE);
            enrollShort.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 8 14; -fx-background-radius: 8px; -fx-cursor: hand;");
            enrollShort.setOnAction(e -> {
                modal.close();
                showDetailView(p);
            });

            col.getChildren().addAll(badge, t, sep1, row1, row2, row3, row4, row5, sep2, enrollShort);
            compareCardsBox.getChildren().add(col);
        }

        ScrollPane compScroll = new ScrollPane(compareCardsBox);
        compScroll.setFitToHeight(true);
        compScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        // Footer Row
        HBox fRow = new HBox(12);
        fRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearSel = new Button("Clear Selection");
        clearSel.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 18; -fx-cursor: hand;");
        clearSel.setOnAction(e -> {
            comparedPlanIds.clear();
            updateFloatingActionBar();
            loadPlans();
            modal.close();
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());

        fRow.getChildren().addAll(clearSel, closeBtn);

        root.getChildren().addAll(topRow, compScroll, fRow);
        DialogUtils.prepareModalScene(modal, root);
        modal.showAndWait();
    }

    private VBox createCompRow(String label, String value) {
        VBox v = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: 700;");
        val.setWrapText(true);
        v.getChildren().addAll(l, val);
        return v;
    }
    
    private String hexToRgb(String hex) {
        try {
            Color c = Color.web(hex);
            return (int)(c.getRed()*255) + "," + (int)(c.getGreen()*255) + "," + (int)(c.getBlue()*255);
        } catch (Exception e) {
            return "59,130,246";
        }
    }

    // =========================================================================
    // 100% OPERATIONAL FILTER & DATA ENGINE
    // =========================================================================

    private void loadPlans() {
        plansContainer.getChildren().clear();
        String query = (sideSearch != null && sideSearch.getText() != null) ? sideSearch.getText().trim().toLowerCase() : "";

        // Gather active filter options
        List<String> activeGoals = filterCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .filter(t -> List.of("Strength", "Cardio", "Flexibility", "Hypertrophy", "Recovery").contains(t))
            .collect(Collectors.toList());
            
        List<String> activeDurations = filterCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .filter(t -> List.of("<4 weeks", "4-8 weeks", ">8 weeks").contains(t))
            .collect(Collectors.toList());

        List<String> activeEquipment = filterCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .filter(t -> List.of("Full Gym", "Dumbbells", "Bodyweight", "Pool").contains(t))
            .collect(Collectors.toList());

        int diffSliderVal = difficultySlider != null ? (int) difficultySlider.getValue() : 0;

        List<FitnessPlan> filtered = facade.getPlanCatalogue().stream()
            // 1. Text Search Filter
            .filter(p -> {
                if (query.isEmpty()) return true;
                boolean inTitle = p.getTitle().toLowerCase().contains(query);
                boolean inDesc = p.getDescription().toLowerCase().contains(query);
                boolean inType = p.getPlanType().toLowerCase().contains(query);
                boolean inObj = p.getObjectives().stream().anyMatch(o -> o.toLowerCase().contains(query));
                boolean inEq = false;
                if (p instanceof SelfGuidedPlan sg) {
                    inEq = sg.getEquipmentFocus().toLowerCase().contains(query);
                }
                return inTitle || inDesc || inType || inObj || inEq;
            })
            // 2. Goal Checkboxes Filter
            .filter(p -> {
                if (activeGoals.isEmpty()) return true;
                return activeGoals.stream().anyMatch(g -> matchesGoal(p, g));
            })
            // 3. Difficulty Slider Filter (0 = All, 1 = Beginner, 2 = Intermediate, 3 = Advanced)
            .filter(p -> {
                if (diffSliderVal == 0) return true;
                int tier = getPlanDifficultyTier(p);
                return tier == diffSliderVal;
            })
            // 4. Duration Checkboxes Filter
            .filter(p -> {
                if (activeDurations.isEmpty()) return true;
                int w = p.getDurationWeeks();
                boolean match = false;
                if (activeDurations.contains("<4 weeks") && w <= 4) match = true;
                if (activeDurations.contains("4-8 weeks") && w >= 4 && w <= 8) match = true;
                if (activeDurations.contains(">8 weeks") && w > 8) match = true;
                return match;
            })
            // 5. Equipment Checkboxes Filter (NOW 100% OPERATIONAL)
            .filter(p -> {
                if (activeEquipment.isEmpty()) return true;
                return activeEquipment.stream().anyMatch(eq -> matchesEquipment(p, eq));
            })
            // 6. Curated Collections Filter
            .filter(p -> {
                if (activeCollection == null) return true;
                if ("Personalized".equals(activeCollection)) {
                    return matchesGoal(p, "Hypertrophy") || matchesGoal(p, "Strength");
                } else if ("Newcomers".equals(activeCollection)) {
                    return getPlanDifficultyTier(p) <= 1 || p.getDurationWeeks() <= 8;
                } else if ("Bodyweight".equals(activeCollection)) {
                    return matchesEquipment(p, "Bodyweight");
                } else if ("Endurance".equals(activeCollection)) {
                    return matchesGoal(p, "Cardio") || p.getTitle().toLowerCase().contains("marathon") || p.getTitle().toLowerCase().contains("spin") || p.getTitle().toLowerCase().contains("triathlon");
                }
                return true;
            })
            .collect(Collectors.toList());

        // Sort Engine
        String sortOption = (sortCombo != null && sortCombo.getValue() != null) ? sortCombo.getValue() : "A-Z";
        switch (sortOption) {
            case "A-Z" -> filtered.sort(Comparator.comparing(FitnessPlan::getTitle));
            case "Z-A" -> filtered.sort(Comparator.comparing(FitnessPlan::getTitle).reversed());
            case "Price: Low to High" -> filtered.sort(Comparator.comparingDouble(FitnessPlan::getBaseMonthlyFee));
            case "Price: High to Low" -> filtered.sort(Comparator.comparingDouble(FitnessPlan::getBaseMonthlyFee).reversed());
            case "Duration: Short to Long" -> filtered.sort(Comparator.comparingInt(FitnessPlan::getDurationWeeks));
            case "Duration: Long to Short" -> filtered.sort(Comparator.comparingInt(FitnessPlan::getDurationWeeks).reversed());
            case "Difficulty" -> filtered.sort(Comparator.comparingInt(this::getPlanDifficultyTier));
            case "Newest" -> filtered.sort(Comparator.comparing(FitnessPlan::getId).reversed());
        }

        int totalCount = filtered.size();
        totalCountBadge.setText(totalCount + " Plans Available");

        // Dynamic Pagination Calculation
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / itemsPerPage));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int startIdx = (currentPage - 1) * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, totalCount);

        showingLbl.setText("Showing " + (totalCount == 0 ? 0 : startIdx + 1) + "-" + endIdx + " of " + totalCount + " plans");
        renderPaginationControls(totalPages);

        List<FitnessPlan> pageItems = filtered.stream().skip(startIdx).limit(itemsPerPage).collect(Collectors.toList());

        for (int i = 0; i < pageItems.size(); i++) {
            plansContainer.getChildren().add(createPremiumPlanCard(pageItems.get(i), i));
        }
        
        if (pageItems.isEmpty()) {
            VBox empty = new VBox(15);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40, 20, 40, 20));
            
            Label emptyIcon = new Label("🔍");
            emptyIcon.setStyle("-fx-font-size: 36px;");

            Label emptyLbl = new Label("No plans match your selected filters.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-weight: bold;");

            Button resetBtn = new Button("Reset All Filters");
            resetBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 18; -fx-cursor: hand;");
            resetBtn.setOnAction(e -> resetAllFilters());

            empty.getChildren().addAll(emptyIcon, emptyLbl, resetBtn);
            plansContainer.getChildren().add(empty);
        }
    }

    private void renderPaginationControls(int totalPages) {
        paginationBar.getChildren().clear();

        // Prev Button
        Button prevBtn = new Button("«");
        prevBtn.getStyleClass().add("workout-page-btn");
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadPlans();
            }
        });
        paginationBar.getChildren().add(prevBtn);

        // Page Number Buttons
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, startPage + 4);
        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        if (startPage > 1) {
            Button p1 = new Button("1");
            p1.getStyleClass().add(currentPage == 1 ? "workout-page-btn-active" : "workout-page-btn");
            p1.setOnAction(e -> { currentPage = 1; loadPlans(); });
            paginationBar.getChildren().add(p1);
            if (startPage > 2) {
                Label dot = new Label("...");
                dot.setStyle("-fx-text-fill: #64748b;");
                paginationBar.getChildren().add(dot);
            }
        }

        for (int p = startPage; p <= endPage; p++) {
            final int page = p;
            Button pb = new Button(String.valueOf(p));
            pb.getStyleClass().add(page == currentPage ? "workout-page-btn-active" : "workout-page-btn");
            pb.setOnAction(e -> {
                currentPage = page;
                loadPlans();
            });
            paginationBar.getChildren().add(pb);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                Label dot = new Label("...");
                dot.setStyle("-fx-text-fill: #64748b;");
                paginationBar.getChildren().add(dot);
            }
            Button pLast = new Button(String.valueOf(totalPages));
            pLast.getStyleClass().add(currentPage == totalPages ? "workout-page-btn-active" : "workout-page-btn");
            pLast.setOnAction(e -> { currentPage = totalPages; loadPlans(); });
            paginationBar.getChildren().add(pLast);
        }

        // Next Button
        Button nextBtn = new Button("»");
        nextBtn.getStyleClass().add("workout-page-btn");
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadPlans();
            }
        });
        paginationBar.getChildren().add(nextBtn);
    }

    private boolean matchesGoal(FitnessPlan p, String goal) {
        String g = goal.toLowerCase();
        String title = p.getTitle().toLowerCase();
        String desc = p.getDescription().toLowerCase();
        boolean objMatch = p.getObjectives().stream().anyMatch(obj -> obj.toLowerCase().contains(g));
        if (objMatch) return true;
        if (title.contains(g) || desc.contains(g)) return true;

        if (goal.equals("Strength")) {
            return title.contains("power") || title.contains("lift") || title.contains("grip") || title.contains("crossfit") || desc.contains("lift") || desc.contains("strength");
        } else if (goal.equals("Cardio")) {
            return title.contains("hiit") || title.contains("spin") || title.contains("cycling") || title.contains("running") || title.contains("marathon") || title.contains("ironman") || title.contains("triathlon") || title.contains("boxing") || title.contains("zumba") || title.contains("5k") || desc.contains("cardio") || desc.contains("intervals");
        } else if (goal.equals("Flexibility")) {
            return title.contains("yoga") || title.contains("pilates") || title.contains("stretch") || title.contains("mobility") || title.contains("posture") || desc.contains("flexibility") || desc.contains("mobility");
        } else if (goal.equals("Hypertrophy")) {
            return title.contains("bodybuilding") || title.contains("hypertrophy") || title.contains("bootcamp") || desc.contains("muscle") || desc.contains("hypertrophy");
        } else if (goal.equals("Recovery")) {
            return title.contains("recovery") || title.contains("rehab") || title.contains("wellness") || title.contains("meditation") || title.contains("aqua") || title.contains("pre-natal") || desc.contains("recovery") || desc.contains("rehab");
        }
        return false;
    }

    private int getPlanDifficultyTier(FitnessPlan p) {
        String t = p.getTitle().toLowerCase();
        String d = p.getDescription().toLowerCase();
        if (t.contains("advanced") || t.contains("powerlifting") || t.contains("pro") || t.contains("ironman") || t.contains("triathlon") || t.contains("olympic") || t.contains("prep") || d.contains("advanced") || d.contains("contest") || d.contains("extreme")) {
            return 3; // Advanced
        }
        if (t.contains("intermediate") || t.contains("conditioning") || t.contains("crossfit") || t.contains("bootcamp") || t.contains("boxing") || t.contains("pilates") || t.contains("hiit") || d.contains("intermediate")) {
            return 2; // Intermediate
        }
        return 1; // Beginner / All Levels
    }

    private boolean matchesEquipment(FitnessPlan p, String eq) {
        String title = p.getTitle().toLowerCase();
        String desc = p.getDescription().toLowerCase();
        String req = "";
        if (p instanceof SelfGuidedPlan sg) {
            req = sg.getEquipmentFocus().toLowerCase();
        }

        if (eq.equals("Full Gym")) {
            return req.contains("barbell") || req.contains("all") || req.contains("gym") || title.contains("gym") || title.contains("powerlifting") || title.contains("olympic") || title.contains("stronglifts") || desc.contains("gym") || desc.contains("lifts");
        } else if (eq.equals("Dumbbells")) {
            return req.contains("dumbbell") || req.contains("kettlebell") || req.contains("grip") || title.contains("dumbbell") || title.contains("kettlebell") || title.contains("grip");
        } else if (eq.equals("Bodyweight")) {
            return req.contains("none") || req.contains("pullup") || req.contains("mat") || req.contains("band") || title.contains("bodyweight") || title.contains("calisthenics") || title.contains("home") || title.contains("yoga") || desc.contains("bodyweight") || desc.contains("home");
        } else if (eq.equals("Pool")) {
            return req.contains("pool") || req.contains("swim") || title.contains("aqua") || title.contains("triathlon") || desc.contains("pool") || desc.contains("water") || desc.contains("swim");
        }
        return false;
    }

    // =========================================================================
    // PLAN CARD COMPONENT (GRID & LIST VIEWS)
    // =========================================================================

    private VBox createPremiumPlanCard(FitnessPlan plan, int index) {
        VBox card = new VBox();
        boolean isSelected = comparedPlanIds.contains(plan.getId());
        card.getStyleClass().add(isSelected ? "workout-plan-card-selected" : "workout-plan-card");

        if (isGridView) {
            card.setPrefSize(320, 210);
            card.setMaxWidth(320);
        } else {
            card.setPrefSize(920, 130);
            card.setMaxWidth(Double.MAX_VALUE);
        }
        
        // Top Banner / Image Area
        StackPane topBanner = new StackPane();
        topBanner.setPrefHeight(isGridView ? 95 : 45);
        topBanner.getStyleClass().add("workout-card-banner");
        
        Label tag = new Label(plan.getPlanType().toUpperCase());
        String tagColor;
        if (plan.getPlanType().contains("Group")) tagColor = "#38bdf8";
        else if (plan.getPlanType().contains("Personal")) tagColor = "#34d399";
        else tagColor = "#fbbf24";

        tag.setStyle("-fx-background-color: rgba(" + hexToRgb(tagColor) + ", 0.22); -fx-text-fill: " + tagColor + "; -fx-padding: 4 10; -fx-background-radius: 8px; -fx-font-size: 10px; -fx-font-weight: 900;");
        StackPane.setAlignment(tag, Pos.BOTTOM_LEFT);
        StackPane.setMargin(tag, new Insets(0, 0, 8, 14));
        
        // Favorite / Star Button
        boolean isBookmarked = bookmarkedPlanIds.contains(plan.getId());
        Button star = new Button(isBookmarked ? "★" : "☆");
        star.setStyle("-fx-background-color: transparent; -fx-text-fill: " + (isBookmarked ? "#fbbf24" : "#94a3b8") + "; -fx-font-size: 18px; -fx-padding: 0; -fx-cursor: hand;");
        
        StackPane.setAlignment(star, Pos.TOP_RIGHT);
        StackPane.setMargin(star, new Insets(8, 14, 0, 0));
        
        topBanner.getChildren().addAll(tag, star);
        
        // Active Progress Bar (simulated on every third plan)
        if (index % 3 == 0) {
            ProgressBar pb = new ProgressBar(0.65);
            pb.setPrefWidth(isGridView ? 290 : 860);
            pb.setPrefHeight(4);
            pb.setStyle("-fx-accent: #10b981; -fx-control-inner-background: rgba(255,255,255,0.1);");
            StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
            topBanner.getChildren().add(pb);
        }

        // Bottom Details & Actions
        VBox bottom = new VBox(6);
        bottom.setPadding(new Insets(12, 14, 12, 14));
        
        HBox titleRow = new HBox();
        Label title = new Label(plan.getTitle());
        title.getStyleClass().add("workout-card-title");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 900;");
        Region tSp = new Region(); HBox.setHgrow(tSp, Priority.ALWAYS);
        Label price = new Label("$" + String.format("%.0f", plan.getBaseMonthlyFee()) + "/mo");
        price.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 13px; -fx-font-weight: 800;");
        titleRow.getChildren().addAll(title, tSp, price);
        
        Label prog = new Label("Progression: " + plan.getDurationWeeks() + " Wks • " + plan.getDescription());
        prog.getStyleClass().add("workout-card-prog");
        prog.setStyle("-fx-font-size: 11px;");
        prog.setMaxHeight(32);
        
        Region midSp = new Region();
        VBox.setVgrow(midSp, Priority.ALWAYS);
        
        HBox actionsRow = new HBox();
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        
        Label bookmark = new Label(isBookmarked ? "⭐ Bookmarked" : "🔖 Bookmark");
        bookmark.setStyle("-fx-text-fill: " + (isBookmarked ? "#fbbf24" : "#64748b") + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        
        // Synchronized Favorite / Bookmark toggle
        Runnable toggleBookmark = () -> {
            if (bookmarkedPlanIds.contains(plan.getId())) {
                bookmarkedPlanIds.remove(plan.getId());
                star.setText("☆");
                star.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-padding: 0; -fx-cursor: hand;");
                bookmark.setText("🔖 Bookmark");
                bookmark.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                bookmarkedPlanIds.add(plan.getId());
                star.setText("★");
                star.setStyle("-fx-background-color: transparent; -fx-text-fill: #fbbf24; -fx-font-size: 18px; -fx-padding: 0; -fx-cursor: hand;");
                bookmark.setText("⭐ Bookmarked");
                bookmark.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
            }
        };

        star.setOnAction(e -> toggleBookmark.run());
        bookmark.setOnMouseClicked(e -> toggleBookmark.run());
        
        Region aSp = new Region();
        HBox.setHgrow(aSp, Priority.ALWAYS);
        
        CheckBox compareCb = new CheckBox("Compare");
        compareCb.setSelected(isSelected);
        compareCb.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");
        compareCb.setOnAction(e -> {
            if (compareCb.isSelected()) {
                comparedPlanIds.add(plan.getId());
                card.getStyleClass().clear();
                card.getStyleClass().add("workout-plan-card-selected");
            } else {
                comparedPlanIds.remove(plan.getId());
                card.getStyleClass().clear();
                card.getStyleClass().add("workout-plan-card");
            }
            updateFloatingActionBar();
        });
        
        Label lastUpd = new Label("Updated 2d ago");
        lastUpd.getStyleClass().add("workout-card-meta");
        lastUpd.setStyle("-fx-font-size: 10px;");
        lastUpd.setPadding(new Insets(0, 10, 0, 0));
        
        actionsRow.getChildren().addAll(bookmark, aSp, lastUpd, compareCb);
        
        bottom.getChildren().addAll(titleRow, prog, midSp, actionsRow);
        
        if (isGridView) {
            card.getChildren().addAll(topBanner, bottom);
        } else {
            HBox listLayout = new HBox(15, topBanner, bottom);
            topBanner.setPrefWidth(220);
            HBox.setHgrow(bottom, Priority.ALWAYS);
            card.getChildren().add(listLayout);
        }

        // Navigate to Plan Detail View on Card Click
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof javafx.scene.Node node) {
                while (node != null && node != card) {
                    if (node instanceof CheckBox || node instanceof Button || (node instanceof Label && node == bookmark)) {
                        return; // Prevent navigating when toggling checkboxes or bookmarks
                    }
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
        loadPlans();
    }

    private void showDetailView(FitnessPlan plan) {
        WorkoutPlanDetailView detailView = new WorkoutPlanDetailView(plan, facade, this::showMainView);
        ScrollPane sp = new ScrollPane(detailView);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("edge-to-edge");
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        
        getChildren().clear();
        getChildren().add(sp);
    }
}

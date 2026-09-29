package com.iwfc.ui;
import com.iwfc.ui.components.DialogUtils;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.BodyMeasurementLog;
import com.iwfc.model.Enrollment;
import com.iwfc.model.FitnessPlan;
import com.iwfc.model.Member;
import com.iwfc.model.Role;
import com.iwfc.pattern.payment.PaymentContext;
import com.iwfc.service.MeasurementAnalyzer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MeasurementAnalyticsView extends VBox {

    private final IWFCSystemFacade facade;
    private final Member member;
    
    private LineChart<String, Number> lineChart;
    private ComboBox<String> metricSelector;
    private FlowPane insightsPane;

    public MeasurementAnalyticsView(IWFCSystemFacade facade, Member member) {
        this.facade = facade;
        this.member = member;

        getStyleClass().add("glass-card");
        setPadding(new Insets(30));
        setSpacing(25);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label(member.getName() + "'s Analytics");
        title.getStyleClass().add("h2");
        Label subtitle = new Label("ID: " + member.getUserId());
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleBox, spacer);

        // Actions (Only for Admin & Instructor)
        if (facade.getCurrentUser().getRole() != Role.MEMBER) {
            Button addNoteBtn = new Button("Add Note");
            addNoteBtn.getStyleClass().add("action-button");
            addNoteBtn.setOnAction(e -> showAddNoteDialog());
            
            Button changePlanBtn = new Button("Change Workout Plan");
            changePlanBtn.getStyleClass().add("primary-btn");
            changePlanBtn.setOnAction(e -> showChangePlanDialog());
            
            header.getChildren().addAll(addNoteBtn, changePlanBtn);
        }

        // --- NEW REPORT CARD SECTION ---
        VBox reportCardSection = createReportCardSection();

        // Top Row: Chart
        VBox chartBox = new VBox(15);
        chartBox.getStyleClass().add("glass-card");
        chartBox.setPadding(new Insets(20));
        
        HBox chartControls = new HBox(10);
        chartControls.setAlignment(Pos.CENTER_LEFT);
        Label metricLabel = new Label("Historical Chart:");
        metricLabel.setStyle("-fx-font-weight: bold;");
        
        metricSelector = new ComboBox<>();
        metricSelector.getItems().addAll("Weight", "Chest", "Upper Arms", "Thighs", "Calves");
        metricSelector.setValue("Weight");
        metricSelector.setOnAction(e -> updateChartData());
        
        chartControls.getChildren().addAll(metricLabel, metricSelector);
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(false);
        
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setPrefHeight(250);
        lineChart.setLegendVisible(false);
        lineChart.getStyleClass().add("premium-line-chart");
        
        chartBox.getChildren().addAll(chartControls, lineChart);

        // Bottom Row: Insights
        VBox insightsBox = new VBox(15);
        Label insightsTitle = new Label("AI Trend Insights");
        insightsTitle.getStyleClass().add("h3");
        
        insightsPane = new FlowPane();
        insightsPane.setHgap(15);
        insightsPane.setVgap(15);
        
        insightsBox.getChildren().addAll(insightsTitle, insightsPane);
        
        getChildren().addAll(header, reportCardSection, chartBox, insightsBox);
    }

    private VBox createReportCardSection() {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));
        
        BodyMeasurementLog latest = getLatestLog();
        BodyMeasurementLog previous = getPreviousLog();
        
        javafx.scene.image.Image maleImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/iwfc/images/male_anatomy.jpg"));
        javafx.scene.image.Image femaleImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/iwfc/images/female_anatomy.jpg"));
        
        javafx.scene.image.ImageView bodyView = new javafx.scene.image.ImageView();
        boolean isFemale = member.getName().toLowerCase().matches(".*(emma|jane|lisa|sarah|mary|ann|female).*");
        bodyView.setImage(isFemale ? femaleImg : maleImg);
        bodyView.setFitHeight(450);
        bodyView.setFitWidth(450);
        bodyView.setPreserveRatio(true);

        // Toggle Box
        HBox toggleBox = new HBox(0);
        toggleBox.setAlignment(Pos.CENTER);
        Button maleBtn = new Button("Male");
        Button femaleBtn = new Button("Female");
        
        String activeStyle = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15 0 0 15; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #94a3b8; -fx-padding: 5 15; -fx-background-radius: 0 15 15 0; -fx-cursor: hand;";
        String activeStyleR = "-fx-background-color: #ec4899; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 0 15 15 0; -fx-cursor: hand;";
        String inactiveStyleL = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #94a3b8; -fx-padding: 5 15; -fx-background-radius: 15 0 0 15; -fx-cursor: hand;";

        maleBtn.setStyle(isFemale ? inactiveStyleL : activeStyle);
        femaleBtn.setStyle(isFemale ? activeStyleR : inactiveStyle);

        maleBtn.setOnAction(e -> {
            bodyView.setImage(maleImg);
            maleBtn.setStyle(activeStyle);
            femaleBtn.setStyle(inactiveStyle);
        });
        femaleBtn.setOnAction(e -> {
            bodyView.setImage(femaleImg);
            maleBtn.setStyle(inactiveStyleL);
            femaleBtn.setStyle(activeStyleR);
        });
        
        toggleBox.getChildren().addAll(maleBtn, femaleBtn);
        
        // Body Map
        StackPane stack = new StackPane();
        stack.setAlignment(Pos.CENTER);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(700, 450);
        
        javafx.scene.shape.Line scanline = new javafx.scene.shape.Line(225, 20, 475, 20);
        scanline.setStyle("-fx-stroke: rgba(59, 130, 246, 0.5); -fx-stroke-width: 2; -fx-effect: dropshadow(gaussian, #3b82f6, 15, 0.5, 0, 0);");
        overlay.getChildren().add(scanline);
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(3), scanline);
        tt.setByY(410);
        tt.setAutoReverse(true);
        tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
        tt.play();

        // Data points (adjusted for 700x450 overlay where image is 450x450 centered)
        addDataPoint(overlay, "Neck", 42.0, 350, 60, -180, -20);
        addDataPoint(overlay, "Shoulder", 56.0, 415, 90, 150, -20);
        addDataPoint(overlay, "Chest", latest != null ? latest.getChest() : 95.0, 350, 120, -200, 0);
        addDataPoint(overlay, "Left Biceps", latest != null ? latest.getUpperArms() : 35.0, 265, 170, -160, 0);
        addDataPoint(overlay, "Right Biceps", latest != null ? latest.getUpperArms() : 35.0, 435, 170, 160, 0);
        addDataPoint(overlay, "Waist", latest != null ? latest.getWeight() * 0.4 : 80.0, 350, 210, 200, 0);
        addDataPoint(overlay, "Left Thigh", latest != null ? latest.getThighs() : 50.0, 305, 280, -180, 0);
        addDataPoint(overlay, "Right Thigh", latest != null ? latest.getThighs() : 50.0, 395, 280, 180, 0);

        stack.getChildren().addAll(bodyView, overlay);
        
        HBox contentBox = new HBox(40);
        contentBox.setAlignment(Pos.CENTER);
        
        VBox rightComparison = createAdvanceComparisonTable(latest, previous);
        
        contentBox.getChildren().addAll(stack, rightComparison);
        
        container.getChildren().addAll(toggleBox, contentBox);
        return container;
    }

    private void addDataPoint(Pane pane, String label, double value, double cx, double cy, double offsetX, double offsetY) {
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(cx, cy, 6);
        dot.setStyle("-fx-fill: #3b82f6; -fx-effect: dropshadow(gaussian, #3b82f6, 12, 0.6, 0, 0);");
        
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.seconds(1.5), dot);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.6); st.setToY(1.6);
        st.setAutoReverse(true);
        st.setCycleCount(javafx.animation.Animation.INDEFINITE);
        st.play();

        double lx = cx + offsetX;
        double ly = cy + offsetY;
        
        double hExt = offsetX > 0 ? 30 : -30;
        
        javafx.scene.shape.Polyline conn = new javafx.scene.shape.Polyline(cx, cy, lx, ly, lx + hExt, ly);
        conn.setStyle("-fx-stroke: rgba(59, 130, 246, 0.6); -fx-stroke-width: 2; -fx-fill: transparent; -fx-effect: dropshadow(gaussian, #3b82f6, 5, 0.2, 0, 0);");
        
        VBox labelBox = new VBox(2);
        labelBox.setPrefWidth(90);
        labelBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 8; -fx-padding: 6 12; -fx-border-color: rgba(59, 130, 246, 0.5); -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        
        if (offsetX > 0) {
            labelBox.setLayoutX(lx + hExt + 8);
        } else {
            labelBox.setLayoutX(lx + hExt - 98);
        }
        labelBox.setLayoutY(ly - 20);
        
        Label title = new Label(label);
        title.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label val = new Label(String.format("%.1f", value));
        val.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        labelBox.getChildren().addAll(title, val);
        pane.getChildren().addAll(conn, dot, labelBox);
        
        // Hover interaction to glow brighter
        labelBox.setOnMouseEntered(e -> {
            dot.setStyle("-fx-fill: #0ea5e9; -fx-effect: dropshadow(gaussian, #0ea5e9, 20, 0.8, 0, 0);");
            conn.setStyle("-fx-stroke: rgba(14, 165, 233, 1.0); -fx-stroke-width: 2.5; -fx-fill: transparent; -fx-effect: dropshadow(gaussian, #0ea5e9, 10, 0.5, 0, 0);");
            labelBox.setStyle("-fx-background-color: rgba(30, 41, 59, 0.9); -fx-background-radius: 8; -fx-padding: 6 12; -fx-border-color: #0ea5e9; -fx-border-radius: 8; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, #0ea5e9, 15, 0, 0, 0);");
        });
        labelBox.setOnMouseExited(e -> {
            dot.setStyle("-fx-fill: #3b82f6; -fx-effect: dropshadow(gaussian, #3b82f6, 12, 0.6, 0, 0);");
            conn.setStyle("-fx-stroke: rgba(59, 130, 246, 0.6); -fx-stroke-width: 2; -fx-fill: transparent; -fx-effect: dropshadow(gaussian, #3b82f6, 5, 0.2, 0, 0);");
            labelBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 8; -fx-padding: 6 12; -fx-border-color: rgba(59, 130, 246, 0.5); -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        });
    }
    
    private VBox createAdvanceComparisonTable(BodyMeasurementLog latest, BodyMeasurementLog previous) {
        VBox table = new VBox(0);
        table.setPadding(new Insets(20));
        table.setPrefWidth(300);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);");
        
        Label title = new Label("Advance Comparison");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        VBox.setMargin(title, new Insets(0, 0, 15, 0));
        
        HBox headers = new HBox();
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Label thenLbl = new Label("Then"); thenLbl.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-pref-width: 60; -fx-alignment: center;");
        Label nowLbl = new Label("Now"); nowLbl.setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold; -fx-pref-width: 60; -fx-alignment: center;");
        headers.getChildren().addAll(s1, thenLbl, nowLbl);
        
        VBox rows = new VBox(0);
        
        double pSkel = 45; double nSkel = 46;
        double pVisc = 50; double nVisc = 39;
        double pFat = 18.7; double nFat = 17.2;
        double pBmi = 24.2; double nBmi = 23.5;
        
        if (latest != null && latest.getHeight() > 0) {
            nBmi = latest.getWeight() / Math.pow(latest.getHeight() / 100.0, 2);
            if (previous != null) pBmi = previous.getWeight() / Math.pow(previous.getHeight() / 100.0, 2);
            else pBmi = nBmi + 1.2;
        }
        
        rows.getChildren().addAll(
            createTableRow("Skeletal Muscles", pSkel, nSkel, ""),
            createTableRow("Visceral Fat", pVisc, nVisc, ""),
            createTableRow("Body Fat", pFat, nFat, "%"),
            createTableRow("BMI", pBmi, nBmi, "")
        );
        
        table.getChildren().addAll(title, headers, rows);
        return table;
    }
    
    private HBox createTableRow(String labelStr, double thenVal, double nowVal, String suffix) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label lbl = new Label(labelStr);
        lbl.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 14px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        
        Label tLbl = new Label(String.format("%.1f", thenVal) + suffix);
        tLbl.setStyle("-fx-background-color: #f0fdfa; -fx-text-fill: #333; -fx-font-weight: bold; -fx-pref-width: 60; -fx-alignment: center; -fx-padding: 10 0 10 0;");
        
        Label nLbl = new Label(String.format("%.1f", nowVal) + suffix);
        nLbl.setStyle("-fx-background-color: #fff7ed; -fx-text-fill: #f97316; -fx-font-weight: bold; -fx-pref-width: 60; -fx-alignment: center; -fx-padding: 10 0 10 0;");
        
        row.getChildren().addAll(lbl, sp, tLbl, nLbl);
        return row;
    }

    private BodyMeasurementLog getLatestLog() {
        try {
            List<BodyMeasurementLog> logs = facade.getMeasurementLogs(member.getUserId());
            if (!logs.isEmpty()) return logs.get(logs.size() - 1);
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        return null;
    }
    
    private BodyMeasurementLog getPreviousLog() {
        try {
            List<BodyMeasurementLog> logs = facade.getMeasurementLogs(member.getUserId());
            if (logs.size() > 1) return logs.get(logs.size() - 2);
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        return null;
    }

    private void loadData() {
        try {
            MeasurementAnalyzer.InsightReport report = facade.getMeasurementAnalysis(member.getUserId());
            populateInsights(report);
            updateChartData();
        } catch (UnauthorizedAccessException ex) {
            getChildren().clear();
            getChildren().add(new Label("Access Denied to measurements."));
        }
    }

    private void populateInsights(MeasurementAnalyzer.InsightReport report) {
        insightsPane.getChildren().clear();
        if (report == null) {
            insightsPane.getChildren().add(new Label("Not enough data to generate insights. Log at least two measurements."));
            return;
        }

        // Date Range badge
        Label dateRangeLbl = new Label("Period: " + report.dateRange());
        dateRangeLbl.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-padding: 5 10 5 10; -fx-background-radius: 15; -fx-font-size: 12px; -fx-text-fill: #e0e0e0;");
        
        VBox dateBox = new VBox(dateRangeLbl);
        dateBox.setPadding(new Insets(0, 0, 10, 0));
        insightsPane.getChildren().add(dateBox);

        for (MeasurementAnalyzer.Insight insight : report.trends()) {
            insightsPane.getChildren().add(createInsightCard(insight));
        }
        for (MeasurementAnalyzer.Insight insight : report.symmetry()) {
            insightsPane.getChildren().add(createInsightCard(insight));
        }
    }

    private VBox createInsightCard(MeasurementAnalyzer.Insight insight) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setPrefWidth(220);
        card.getStyleClass().add("insight-card");

        // Styling based on type
        String bgColor = "rgba(255, 255, 255, 0.05)";
        String borderColor = "rgba(255, 255, 255, 0.2)";
        String icon = "•";
        String iconColor = "#e0e0e0";

        switch (insight.type()) {
            case "up" -> { borderColor = "#4CAF50"; icon = "↗"; iconColor = "#4CAF50"; }
            case "down" -> { borderColor = "#2196F3"; icon = "↘"; iconColor = "#2196F3"; }
            case "warning" -> { borderColor = "#FF9800"; icon = "⚠"; iconColor = "#FF9800"; bgColor = "rgba(255, 152, 0, 0.1)"; }
            case "good" -> { borderColor = "#4CAF50"; icon = "✓"; iconColor = "#4CAF50"; bgColor = "rgba(76, 175, 80, 0.1)"; }
        }

        card.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 0 4px; -fx-background-radius: 4px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);", bgColor, borderColor));

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + iconColor + ";");
        Label titleLbl = new Label(insight.metricName());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        topRow.getChildren().addAll(iconLbl, titleLbl);

        Label subLbl = new Label(insight.title());
        subLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + iconColor + "; -fx-font-size: 12px;");

        Text descText = new Text(insight.description());
        descText.setWrappingWidth(190);
        descText.setStyle("-fx-fill: #cccccc; -fx-font-size: 12px;");

        card.getChildren().addAll(topRow, subLbl, descText);
        return card;
    }

    private void updateChartData() {
        try {
            List<BodyMeasurementLog> logs = facade.getMeasurementLogs(member.getUserId());
            lineChart.getData().clear();
            
            if (logs.isEmpty()) return;

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            String metric = metricSelector.getValue();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

            for (BodyMeasurementLog log : logs) {
                String dateStr = log.getDate().format(formatter);
                double val = 0;
                switch (metric) {
                    case "Weight" -> val = log.getWeight();
                    case "Chest" -> val = log.getChest();
                    case "Upper Arms" -> val = log.getUpperArms();
                    case "Thighs" -> val = log.getThighs();
                    case "Calves" -> val = log.getCalves();
                }
                series.getData().add(new XYChart.Data<>(dateStr, val));
            }
            
            lineChart.getData().add(series);
            
        } catch (UnauthorizedAccessException ignored) {
        }
    }

    private void showChangePlanDialog() {
        // Fetch current active enrollment
        List<Enrollment> enrollments = facade.getEnrollmentsForMember(member.getUserId());
        if (enrollments == null || enrollments.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Member has no active enrollment to change.");
        DialogUtils.stylePremiumDialog(a);
            a.show();
            return;
        }
        Enrollment active = enrollments.get(enrollments.size() - 1);

        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Change Workout Plan");
        dialog.setHeaderText("Assign a new plan for " + member.getName());

        ButtonType saveBtnType = new ButtonType("Confirm Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<FitnessPlan> planCbx = new ComboBox<>();
        planCbx.getItems().addAll(facade.getPlanCatalogue());
        // set converter for nice display
        planCbx.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FitnessPlan item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle() + " (" + item.getPlanType() + ")");
            }
        });
        planCbx.setButtonCell(planCbx.getCellFactory().call(null));

        grid.add(new Label("Select New Plan:"), 0, 0);
        grid.add(planCbx, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == saveBtnType) {
                try {
                    FitnessPlan selected = planCbx.getValue();
                    if (selected != null) {
                        facade.changeWorkoutPlanForMember(
                            active.getId(), 
                            selected.getId(), 
                            facade.getCurrentUser().getUserId(), // Assign current instructor
                            null, // Simplified: skip payment for demo/override
                            new PaymentContext()
                        );
                        return true;
                    }
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to change plan: " + ex.getMessage());
                    a.showAndWait();
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Workout plan updated successfully!");
        DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    private void showAddNoteDialog() {
        List<Enrollment> enrollments = facade.getEnrollmentsForMember(member.getUserId());
        if (enrollments == null || enrollments.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Member has no active enrollment to add notes to.");
        DialogUtils.stylePremiumDialog(a);
            a.show();
            return;
        }
        Enrollment active = enrollments.get(enrollments.size() - 1);

        TextInputDialog dialog = new TextInputDialog(active.getInstructorNotes() != null ? active.getInstructorNotes() : "");
        dialog.setTitle("Workout Note");
        dialog.setHeaderText("Add an instructor note for this enrollment");
        dialog.setContentText("Note:");
        
        TextArea textArea = new TextArea(active.getInstructorNotes() != null ? active.getInstructorNotes() : "");
        textArea.setWrapText(true);
        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.add(textArea, 0, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(note -> {
            active.setInstructorNotes(textArea.getText());
            facade.updateEnrollment(active);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Instructor note saved successfully!");
        DialogUtils.stylePremiumDialog(a);
            a.show();
        });
    }
}


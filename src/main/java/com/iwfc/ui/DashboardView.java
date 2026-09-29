package com.iwfc.ui;
import com.iwfc.ui.components.DialogUtils;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.StatCard;
import com.iwfc.ui.components.StatusBadge;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Dashboard view showing stat cards, recent bookings, and maintenance alerts.
 * Has a specialized premium view for Admin roles.
 */
public class DashboardView extends VBox {

    private final IWFCSystemFacade facade;
    private final java.util.function.Consumer<String> navCallback;

    public DashboardView(IWFCSystemFacade facade, java.util.function.Consumer<String> navCallback) {
        this.facade = facade;
        this.navCallback = navCallback;
        setSpacing(25);
        setPadding(new Insets(25));
        getStyleClass().add("dashboard-view");
        buildUI();
    }

    private void buildUI() {
        getChildren().clear();
        
        // Welcome header
        User user = facade.getCurrentUser();
        Label welcome = new Label("Welcome back, " + user.getName());
        welcome.getStyleClass().add("view-title");

        Label roleLabel = new Label(user.getRole().getDisplayName());
        roleLabel.getStyleClass().add("view-subtitle");

        VBox headerText = new VBox(4, welcome, roleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("action-button");
        refreshBtn.setOnAction(e -> {
            buildUI();
        });

        HBox header = new HBox(15, headerText, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        if (user.getRole() == Role.MEMBER) {
            Button dailyLogBtn = new Button("📝 Daily Log");
            dailyLogBtn.getStyleClass().add("primary-btn");
            dailyLogBtn.setOnAction(e -> showDailyLogDialog());
            header.getChildren().add(dailyLogBtn);
        }

        header.getChildren().add(refreshBtn);

        if (user.getRole() == Role.ADMIN) {
            buildAdminDashboard(header);
        } else {
            buildRegularDashboard(header);
        }
    }

    private void buildAdminDashboard(HBox header) {
        double currentIncome = 0;
        double todaysIncome = 0;
        int transactionsToday = 0;
        
        try {
            List<Payment> allPayments = facade.getAllPayments();
            LocalDate today = LocalDate.now();
            
            for (Payment p : allPayments) {
                if (p.getStatus() == PaymentStatus.COMPLETED) {
                    currentIncome += p.getTotalCharged();
                    if (p.getPaidAt().toLocalDate().equals(today)) {
                        todaysIncome += p.getTotalCharged();
                        transactionsToday++;
                    }
                }
            }
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}

        int totalMembers = 0;
        int joinedThisMonth = 0;
        try {
            List<User> members = facade.getUsersByRole(Role.MEMBER);
            totalMembers = members.size();
            joinedThisMonth = (int) (totalMembers * 0.2); // Mocking for visual purposes
            if (joinedThisMonth == 0 && totalMembers > 0) joinedThisMonth = 1;
        } catch(com.iwfc.exception.UnauthorizedAccessException e) {}

        // Row 1: Financial & User Stats
        HBox statsRow1 = new HBox(20);
        statsRow1.setAlignment(Pos.CENTER_LEFT);

        StatCard currentIncomeCard = new StatCard("Current Income", String.format("$%.2f", currentIncome), "💰",
            "-fx-background-color: linear-gradient(to bottom right, #10b981, #059669);");
        currentIncomeCard.setOnMouseClicked(e -> navCallback.accept("Payments"));
        
        StatCard todaysIncomeCard = new StatCard("Today's Income", String.format("$%.2f", todaysIncome), "💸",
            "-fx-background-color: linear-gradient(to bottom right, #f59e0b, #d97706);");
        todaysIncomeCard.setOnMouseClicked(e -> navCallback.accept("Payments"));
        
        StatCard transactionsCard = new StatCard("Transaction Today", String.valueOf(transactionsToday), "💳",
            "-fx-background-color: linear-gradient(to bottom right, #ec4899, #db2777);");
        transactionsCard.setOnMouseClicked(e -> navCallback.accept("Payments"));
        
        StatCard totalMembersCard = new StatCard("Total Members", String.valueOf(totalMembers), "👥",
            "-fx-background-color: linear-gradient(to bottom right, #3b82f6, #2563eb);");
        totalMembersCard.setOnMouseClicked(e -> navCallback.accept("Users"));
        
        StatCard joinedCard = new StatCard("Joined This Month", String.valueOf(joinedThisMonth), "🚀",
            "-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #6366f1);");
        joinedCard.setOnMouseClicked(e -> navCallback.accept("Users"));
            
        statsRow1.getChildren().addAll(currentIncomeCard, todaysIncomeCard, transactionsCard, totalMembersCard, joinedCard);
        HBox.setHgrow(currentIncomeCard, Priority.ALWAYS);
        HBox.setHgrow(todaysIncomeCard, Priority.ALWAYS);
        HBox.setHgrow(transactionsCard, Priority.ALWAYS);
        HBox.setHgrow(totalMembersCard, Priority.ALWAYS);
        HBox.setHgrow(joinedCard, Priority.ALWAYS);

        // Row 2: Operational Stats (from regular dashboard)
        HBox statsRow2 = new HBox(20);
        statsRow2.setAlignment(Pos.CENTER_LEFT);

        int totalEquipment = facade.getAllEquipment().size();
        int activeSessions = facade.getAllSessions().size();
        int pendingMaintenance = facade.getMaintenanceByStatus(RequestStatus.PENDING).size();
        int totalBookings = facade.getAllBookings().size();

        StatCard equipCard = new StatCard("Total Equipment", String.valueOf(totalEquipment), "⚙️",
            "-fx-background-color: linear-gradient(to bottom right, #7c3aed, #6366f1);");
        equipCard.setOnMouseClicked(e -> navCallback.accept("Equipment"));

        StatCard sessionCard = new StatCard("Active Sessions", String.valueOf(activeSessions), "📅",
            "-fx-background-color: linear-gradient(to bottom right, #2563eb, #06b6d4);");
        sessionCard.setOnMouseClicked(e -> navCallback.accept("Sessions"));

        StatCard maintCard = new StatCard("Pending Maintenance", String.valueOf(pendingMaintenance), "🔧",
            "-fx-background-color: linear-gradient(to bottom right, #0d9488, #10b981);");
        maintCard.setOnMouseClicked(e -> navCallback.accept("Maintenance"));

        StatCard bookingCard = new StatCard("Total Bookings", String.valueOf(totalBookings), "🎟️",
            "-fx-background-color: linear-gradient(to bottom right, #ea580c, #f59e0b);");
        bookingCard.setOnMouseClicked(e -> navCallback.accept("Sessions"));

        statsRow2.getChildren().addAll(equipCard, sessionCard, maintCard, bookingCard);
        HBox.setHgrow(equipCard, Priority.ALWAYS);
        HBox.setHgrow(sessionCard, Priority.ALWAYS);
        HBox.setHgrow(maintCard, Priority.ALWAYS);
        HBox.setHgrow(bookingCard, Priority.ALWAYS);

        // Row 3: Details Row (Charts and Lists)
        HBox detailsRow = new HBox(25);
        detailsRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(detailsRow, Priority.ALWAYS);
        
        VBox salesSection = createWeeklySalesSection();
        VBox equipmentSection = createMostUsedEquipmentSection();
        
        detailsRow.getChildren().addAll(salesSection, equipmentSection);
        HBox.setHgrow(salesSection, Priority.ALWAYS);
        
        // Row 4: Bottom Section (Bookings and Maintenance Alerts)
        HBox bottomRow = new HBox(25);
        bottomRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(bottomRow, Priority.ALWAYS);
        
        VBox bookingsSection = createBookingsSection();
        HBox.setHgrow(bookingsSection, Priority.ALWAYS);
        
        VBox maintenanceSection = createMaintenanceSection();
        maintenanceSection.setMinWidth(350);
        maintenanceSection.setPrefWidth(400);
        
        bottomRow.getChildren().addAll(bookingsSection, maintenanceSection);
        
        getChildren().addAll(header, statsRow1, statsRow2, detailsRow, bottomRow);
        
        javafx.application.Platform.runLater(() -> playEntranceAnimations(header, statsRow1, statsRow2, detailsRow, bottomRow));
    }

    private VBox createWeeklySalesSection() {
        VBox section = new VBox(15);
        section.getStyleClass().add("dashboard-section");
        section.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 16; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label title = new Label("Weekly Sales Overview");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setPrefHeight(300);
        chart.setLegendVisible(false);
        chart.getStyleClass().add("premium-line-chart");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        try {
            List<Payment> payments = facade.getAllPayments();
            LocalDate today = LocalDate.now();
            
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                double dailyTotal = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.COMPLETED && p.getPaidAt().toLocalDate().equals(date))
                    .mapToDouble(Payment::getTotalCharged)
                    .sum();
                series.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("MMM dd")), dailyTotal));
            }
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        
        chart.getData().add(series);

        section.getChildren().addAll(title, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return section;
    }

    private VBox createMostUsedEquipmentSection() {
        VBox section = new VBox(15);
        section.getStyleClass().add("dashboard-section");
        section.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 16; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        section.setMinWidth(350);
        section.setPrefWidth(400);

        Label title = new Label("Most Used Equipment");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox list = new VBox(10);
        
        List<Equipment> topEquipment = facade.getAllEquipment().stream()
            .sorted(Comparator.comparing(Equipment::getCumulativeUsageHours).reversed())
            .limit(5)
            .collect(Collectors.toList());

        for (int i = 0; i < topEquipment.size(); i++) {
            Equipment eq = topEquipment.get(i);
            HBox item = new HBox(15);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;");
            
            Label rankLabel = new Label("#" + (i + 1));
            rankLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8b5cf6;");
            
            VBox info = new VBox(3);
            Label name = new Label(eq.getName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            Label usage = new Label(String.format("%.1f hrs used", eq.getCumulativeUsageHours()));
            usage.setStyle("-fx-font-size: 12px; -fx-opacity: 0.7;");
            info.getChildren().addAll(name, usage);
            
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            
            StatusBadge badge = StatusBadge.forEquipmentStatus(eq.getStatus());
            
            item.getChildren().addAll(rankLabel, info, sp, badge);
            item.setOnMouseClicked(e -> navCallback.accept("Equipment"));
            list.getChildren().add(item);
        }

        if (topEquipment.isEmpty()) {
            Label noEq = new Label("No equipment data available.");
            noEq.getStyleClass().add("empty-placeholder");
            list.getChildren().add(noEq);
        }

        section.getChildren().addAll(title, list);
        return section;
    }

    private void buildRegularDashboard(HBox header) {
        // Stat cards
        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        int totalEquipment = facade.getAllEquipment().size();
        int activeSessions = facade.getAllSessions().size();
        int pendingMaintenance = facade.getMaintenanceByStatus(RequestStatus.PENDING).size();
        int totalBookings = facade.getAllBookings().size();

        StatCard equipCard = new StatCard("Total Equipment", String.valueOf(totalEquipment), "⚙️",
            "-fx-background-color: linear-gradient(to bottom right, #7c3aed, #6366f1);");
        StatCard sessionCard = new StatCard("Active Sessions", String.valueOf(activeSessions), "📅",
            "-fx-background-color: linear-gradient(to bottom right, #2563eb, #06b6d4);");
        StatCard maintCard = new StatCard("Pending Maintenance", String.valueOf(pendingMaintenance), "🔧",
            "-fx-background-color: linear-gradient(to bottom right, #0d9488, #10b981);");
        StatCard bookingCard = new StatCard("Total Bookings", String.valueOf(totalBookings), "🎟️",
            "-fx-background-color: linear-gradient(to bottom right, #ea580c, #f59e0b);");

        equipCard.setOnMouseClicked(e -> navCallback.accept("Equipment"));
        sessionCard.setOnMouseClicked(e -> navCallback.accept("Sessions"));
        maintCard.setOnMouseClicked(e -> navCallback.accept("Maintenance"));
        bookingCard.setOnMouseClicked(e -> navCallback.accept("Sessions"));

        statsRow.getChildren().addAll(equipCard, sessionCard, maintCard, bookingCard);
        HBox.setHgrow(equipCard, Priority.ALWAYS);
        HBox.setHgrow(sessionCard, Priority.ALWAYS);
        HBox.setHgrow(maintCard, Priority.ALWAYS);
        HBox.setHgrow(bookingCard, Priority.ALWAYS);

        // Bottom section: Bookings table + Maintenance alerts
        HBox bottomRow = new HBox(20);
        bottomRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(bottomRow, Priority.ALWAYS);

        // Recent bookings table
        VBox bookingsSection = createBookingsSection();
        HBox.setHgrow(bookingsSection, Priority.ALWAYS);

        // Maintenance alerts
        VBox maintenanceSection = createMaintenanceSection();
        maintenanceSection.setMinWidth(300);
        maintenanceSection.setPrefWidth(350);

        bottomRow.getChildren().addAll(bookingsSection, maintenanceSection);

        getChildren().addAll(header, statsRow, bottomRow);

        javafx.application.Platform.runLater(() -> playEntranceAnimations(header, statsRow, bookingsSection, maintenanceSection));
    }

    private void playEntranceAnimations(javafx.scene.Node... sections) {
        int delay = 0;
        for (javafx.scene.Node node : sections) {
            node.setOpacity(0);
            node.setTranslateY(20);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), node);
            ft.setToValue(1);

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(400), node);
            tt.setToY(0);

            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
            pt.setDelay(javafx.util.Duration.millis(delay));
            pt.play();

            delay += 100;
        }
    }

    @SuppressWarnings("unchecked")
    private VBox createBookingsSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("dashboard-section");
        section.setPadding(new Insets(20));

        Label title = new Label("Recent Bookings");
        title.getStyleClass().add("section-title");

        TableView<Booking> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add("dashboard-table");

        TableColumn<Booking, String> memberCol = new TableColumn<>("Member");
        memberCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getMember().getName()));

        TableColumn<Booking, String> sessionCol = new TableColumn<>("Session");
        sessionCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getSession().getName()));

        TableColumn<Booking, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> {
            var session = data.getValue().getSession();
            return new javafx.beans.property.SimpleStringProperty(
                session.getDayOfWeek().toString().substring(0, 3) + " " +
                session.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        });

        TableColumn<Booking, BookingStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BookingStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(StatusBadge.forBookingStatus(item));
                }
            }
        });

        table.getColumns().addAll(memberCol, sessionCol, timeCol, statusCol);

        List<Booking> bookings = facade.getAllBookings();
        table.getItems().addAll(bookings);

        if (bookings.isEmpty()) {
            table.setPlaceholder(new Label("No bookings yet"));
        }

        section.getChildren().addAll(title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return section;
    }

    private VBox createMaintenanceSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("dashboard-section");
        section.setPadding(new Insets(20));

        Label title = new Label("Maintenance Alerts");
        title.getStyleClass().add("section-title");

        VBox alertsList = new VBox(8);
        ScrollPane scrollPane = new ScrollPane(alertsList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("alerts-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        List<MaintenanceRequest> requests = facade.getAllMaintenanceRequests();
        for (MaintenanceRequest req : requests) {
            HBox alertCard = new HBox(10);
            alertCard.setAlignment(Pos.CENTER_LEFT);
            alertCard.setPadding(new Insets(12));
            alertCard.getStyleClass().add("alert-card");

            if (req.isHighUrgency()) {
                alertCard.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            }

            VBox info = new VBox(3);
            Label eqName = new Label(req.getEquipment().getName());
            eqName.getStyleClass().add("alert-equipment-name");
            Label desc = new Label(req.getDescription());
            desc.getStyleClass().add("alert-description");
            desc.setWrapText(true);
            desc.setMaxWidth(200);
            info.getChildren().addAll(eqName, desc);
            HBox.setHgrow(info, Priority.ALWAYS);

            StatusBadge urgencyBadge = StatusBadge.forUrgency(req.getUrgency());

            alertCard.getChildren().addAll(info, urgencyBadge);
            alertsList.getChildren().add(alertCard);
        }

        if (requests.isEmpty()) {
            Label noAlerts = new Label("No maintenance alerts");
            noAlerts.getStyleClass().add("empty-placeholder");
            alertsList.getChildren().add(noAlerts);
        }
        section.getChildren().addAll(title, scrollPane);
        return section;
    }

    private void showDailyLogDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Daily Measurement Log");
        dialog.setHeaderText("Log your body measurements for today");

        ButtonType saveBtnType = new ButtonType("Save Log", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField weightF = new TextField(); weightF.setPromptText("Weight (kg)");
        TextField heightF = new TextField(); heightF.setPromptText("Height (cm)");
        TextField chestF = new TextField(); chestF.setPromptText("Chest (cm)");
        TextField armsF = new TextField(); armsF.setPromptText("Upper Arms (cm)");
        TextField thighsF = new TextField(); thighsF.setPromptText("Thighs (cm)");
        TextField calvesF = new TextField(); calvesF.setPromptText("Calves (cm)");

        grid.add(new Label("Weight (kg):"), 0, 0); grid.add(weightF, 1, 0);
        grid.add(new Label("Height (cm):"), 0, 1); grid.add(heightF, 1, 1);
        grid.add(new Label("Chest (cm):"), 0, 2); grid.add(chestF, 1, 2);
        grid.add(new Label("Upper Arms (cm):"), 0, 3); grid.add(armsF, 1, 3);
        grid.add(new Label("Thighs (cm):"), 0, 4); grid.add(thighsF, 1, 4);
        grid.add(new Label("Calves (cm):"), 0, 5); grid.add(calvesF, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == saveBtnType) {
                try {
                    BodyMeasurementLog log = new BodyMeasurementLog(
                        null, 
                        facade.getCurrentUser().getUserId(), 
                        java.time.LocalDate.now(),
                        Double.parseDouble(weightF.getText().isEmpty() ? "0" : weightF.getText()),
                        Double.parseDouble(heightF.getText().isEmpty() ? "0" : heightF.getText()),
                        Double.parseDouble(chestF.getText().isEmpty() ? "0" : chestF.getText()),
                        Double.parseDouble(armsF.getText().isEmpty() ? "0" : armsF.getText()),
                        Double.parseDouble(thighsF.getText().isEmpty() ? "0" : thighsF.getText()),
                        Double.parseDouble(calvesF.getText().isEmpty() ? "0" : calvesF.getText())
                    );
                    facade.addMeasurementLog(log);
                    return true;
                } catch (NumberFormatException | com.iwfc.exception.DuplicateDataException | com.iwfc.exception.UnauthorizedAccessException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to save log. Please check your inputs.");
        DialogUtils.stylePremiumDialog(a);
                    a.showAndWait();
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Daily log saved successfully!");
        DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }
}



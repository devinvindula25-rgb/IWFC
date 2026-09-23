package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.ui.components.StatCard;
import com.iwfc.ui.components.StatusBadge;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dashboard view showing stat cards, recent bookings, and maintenance alerts.
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
            getChildren().clear();
            buildUI();
        });

        HBox header = new HBox(headerText, spacer, refreshBtn);
        header.setAlignment(Pos.CENTER_LEFT);

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
}

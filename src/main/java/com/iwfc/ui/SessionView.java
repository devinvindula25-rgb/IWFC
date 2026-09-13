package com.iwfc.ui;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Session scheduling view with a weekly timetable grid.
 * Clicking a session block shows details and booking option.
 */
public class SessionView extends VBox {

    private final IWFCSystemFacade facade;
    private VBox detailPanel;
    private GridPane timetable;

    public SessionView(IWFCSystemFacade facade) {
        this.facade = facade;
        setSpacing(0);
        setPadding(new Insets(25));
        getStyleClass().add("session-view");
        buildUI();
    }

    private void buildUI() {
        // Header
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 0, 15, 0));

        Label title = new Label("Session Schedule");
        title.getStyleClass().add("view-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean canCreate = facade.getCurrentUser().getPermissions().contains("CREATE_SESSION");
        Button addBtn = new Button("+ Add Session");
        addBtn.getStyleClass().add("add-button");
        addBtn.setVisible(canCreate);
        addBtn.setManaged(canCreate);
        addBtn.setOnAction(e -> showCreateSessionDialog());

        headerRow.getChildren().addAll(title, spacer, addBtn);

        // Main content: timetable + detail panel
        HBox content = new HBox(0);
        VBox.setVgrow(content, Priority.ALWAYS);

        // Timetable
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("timetable-scroll");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        timetable = buildTimetable();
        scrollPane.setContent(timetable);

        // Detail panel (hidden by default)
        detailPanel = new VBox(15);
        detailPanel.setPrefWidth(300);
        detailPanel.setMinWidth(0);
        detailPanel.setMaxWidth(0);
        detailPanel.setPadding(new Insets(20));
        detailPanel.getStyleClass().add("detail-panel");

        content.getChildren().addAll(scrollPane, detailPanel);

        getChildren().addAll(headerRow, content);
    }

    private GridPane buildTimetable() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("timetable-grid");
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPadding(new Insets(5));

        DayOfWeek[] days = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY };

        // Column headers (days)
        for (int col = 0; col < days.length; col++) {
            Label dayLabel = new Label(days[col].toString().substring(0, 3));
            dayLabel.getStyleClass().add("timetable-day-header");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setPrefWidth(120);
            dayLabel.setPadding(new Insets(8));
            grid.add(dayLabel, col + 1, 0);
        }

        // Row headers (time slots 06:00 - 21:00)
        for (int hour = 6; hour <= 21; hour++) {
            Label timeLabel = new Label(String.format("%02d:00", hour));
            timeLabel.getStyleClass().add("timetable-time-label");
            timeLabel.setPadding(new Insets(5, 10, 5, 5));
            grid.add(timeLabel, 0, hour - 5);

            // Empty cells for each day
            for (int col = 0; col < days.length; col++) {
                Pane cell = new Pane();
                cell.setPrefSize(120, 50);
                cell.getStyleClass().add("timetable-cell");
                grid.add(cell, col + 1, hour - 5);
            }
        }

        // Place sessions
        List<FitnessSession> sessions = facade.getAllSessions();
        for (FitnessSession session : sessions) {
            int col = session.getDayOfWeek().getValue(); // 1=Mon .. 7=Sun
            int startRow = session.getStartTime().getHour() - 5;
            int endRow = session.getEndTime().getHour() - 5;
            int span = endRow - startRow;
            if (span <= 0) span = 1;

            VBox block = createSessionBlock(session);
            grid.add(block, col, startRow, 1, span);
        }

        // Column constraints
        ColumnConstraints timeCol = new ColumnConstraints(60);
        grid.getColumnConstraints().add(timeCol);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(100);
            dayCol.setPrefWidth(120);
            grid.getColumnConstraints().add(dayCol);
        }

        return grid;
    }

    private VBox createSessionBlock(FitnessSession session) {
        VBox block = new VBox(2);
        block.setAlignment(Pos.TOP_LEFT);
        block.setPadding(new Insets(6));
        block.getStyleClass().add("session-block");
        block.setStyle("-fx-background-color: " + session.getColor() + ";" +
                        "-fx-background-radius: 6;");

        Label nameLabel = new Label(session.getName());
        nameLabel.getStyleClass().add("session-block-name");
        nameLabel.setWrapText(true);

        Label instrLabel = new Label(session.getInstructor().getName());
        instrLabel.getStyleClass().add("session-block-instructor");

        Label locLabel = new Label(session.getLocation().getDisplayName());
        locLabel.getStyleClass().add("session-block-location");

        block.getChildren().addAll(nameLabel, instrLabel, locLabel);

        block.setOnMouseClicked(e -> showSessionDetail(session));

        return block;
    }

    private void showSessionDetail(FitnessSession session) {
        detailPanel.getChildren().clear();
        detailPanel.setMaxWidth(300);
        detailPanel.setMinWidth(300);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("detail-close-btn");
        closeBtn.setOnAction(e -> {
            detailPanel.setMaxWidth(0);
            detailPanel.setMinWidth(0);
        });

        HBox closeRow = new HBox();
        closeRow.setAlignment(Pos.CENTER_RIGHT);
        closeRow.getChildren().add(closeBtn);

        // Session info
        Label nameLabel = new Label(session.getName());
        nameLabel.getStyleClass().add("detail-title");

        Label typeLabel = new Label(session.getSessionType());
        typeLabel.getStyleClass().add("detail-type");
        typeLabel.setStyle("-fx-background-color: " + session.getColor() + ";" +
                           "-fx-background-radius: 12; -fx-padding: 3 12;");

        Label instructorLabel = new Label("👤  " + session.getInstructor().getName());
        instructorLabel.getStyleClass().add("detail-info");

        Label timeLabel = new Label("🕐  " +
                session.getDayOfWeek().toString().substring(0, 3) + " " +
                session.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                session.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("detail-info");

        Label locationLabel = new Label("📍  " + session.getLocation().getDisplayName());
        locationLabel.getStyleClass().add("detail-info");

        // Capacity bar
        List<Booking> bookings = facade.getBookingsForSession(session.getSessionId());
        int booked = bookings.size();
        int max = session.getMaxCapacity();

        Label capacityLabel = new Label("Capacity");
        capacityLabel.getStyleClass().add("detail-label");

        ProgressBar capacityBar = new ProgressBar((double) booked / max);
        capacityBar.setPrefWidth(250);
        capacityBar.getStyleClass().add("capacity-bar");

        Label capacityText = new Label(booked + " / " + max + " booked");
        capacityText.getStyleClass().add("detail-info");

        detailPanel.getChildren().addAll(closeRow, nameLabel, typeLabel,
                new Separator(), instructorLabel, timeLabel, locationLabel,
                new Separator(), capacityLabel, capacityBar, capacityText);

        // Book button for members
        if (facade.getCurrentUser().getRole() == Role.MEMBER) {
            Button bookBtn = new Button("Book Now");
            bookBtn.getStyleClass().add("book-button");
            bookBtn.setPrefWidth(250);
            bookBtn.setOnAction(e -> {
                try {
                    facade.bookSession(session.getSessionId());
                    showSessionDetail(session); // Refresh
                    showInfo("Booking Confirmed",
                             "You have been booked for " + session.getName());
                } catch (InvalidBookingException | UnauthorizedAccessException ex) {
                    showAlert("Booking Failed", ex.getMessage());
                }
            });
            detailPanel.getChildren().add(bookBtn);
        }
    }

    private void showCreateSessionDialog() {
        Dialog<FitnessSession> dialog = new Dialog<>();
        dialog.setTitle("Create New Session");

        TextField nameField = new TextField();
        nameField.setPromptText("Session Name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("HIIT", "YOGA", "SPIN", "PILATES", "CROSSFIT", "BOXING");
        typeCombo.setPromptText("Type");

        ComboBox<DayOfWeek> dayCombo = new ComboBox<>();
        dayCombo.getItems().addAll(DayOfWeek.values());
        dayCombo.setPromptText("Day");

        Spinner<Integer> startHour = new Spinner<>(6, 21, 9);
        Spinner<Integer> endHour = new Spinner<>(7, 22, 10);

        ComboBox<Location> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll(Location.values());
        locationCombo.setPromptText("Location");

        Spinner<Integer> capacitySpinner = new Spinner<>(1, 50, 20);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1); grid.add(typeCombo, 1, 1);
        grid.add(new Label("Day:"), 0, 2); grid.add(dayCombo, 1, 2);
        grid.add(new Label("Start:"), 0, 3); grid.add(startHour, 1, 3);
        grid.add(new Label("End:"), 0, 4); grid.add(endHour, 1, 4);
        grid.add(new Label("Location:"), 0, 5); grid.add(locationCombo, 1, 5);
        grid.add(new Label("Capacity:"), 0, 6); grid.add(capacitySpinner, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().isEmpty()
                    && typeCombo.getValue() != null && dayCombo.getValue() != null
                    && locationCombo.getValue() != null) {
                Instructor instructor = (facade.getCurrentUser() instanceof Instructor i) ? i : null;
                String sessionId = "SES" + String.format("%03d",
                        facade.getAllSessions().size() + 1);
                return new FitnessSession(sessionId, nameField.getText(), typeCombo.getValue(),
                        instructor, dayCombo.getValue(),
                        LocalTime.of(startHour.getValue(), 0),
                        LocalTime.of(endHour.getValue(), 0),
                        locationCombo.getValue(), capacitySpinner.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(session -> {
            try {
                facade.createSession(session);
                // Rebuild timetable
                getChildren().clear();
                buildUI();
            } catch (InvalidBookingException | UnauthorizedAccessException ex) {
                showAlert("Error", ex.getMessage());
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

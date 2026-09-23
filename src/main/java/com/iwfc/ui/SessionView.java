package com.iwfc.ui;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/**
 * Premium Session scheduling view featuring a date-aware Monthly Calendar,
 * Weekly Timetable, and an animated frosted-glass Detail Panel.
 */
public class SessionView extends VBox {

    private final IWFCSystemFacade facade;
    
    // State
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private boolean isMonthView = true;
    
    // UI Components
    private BorderPane mainContent;
    private StackPane viewContainer;
    private VBox detailPanel;
    private Label monthLabel;
    
    public SessionView(IWFCSystemFacade facade) {
        this.facade = facade;
        setSpacing(0);
        setPadding(new Insets(25));
        getStyleClass().add("session-view");
        
        buildUI();
    }

    private void buildUI() {
        getChildren().clear();
        
        // --- Header Row ---
        HBox headerRow = new HBox(20);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("Session Schedule");
        title.getStyleClass().add("view-title");

        // Toggle Switch (Month | Week)
        HBox toggleBox = new HBox();
        toggleBox.getStyleClass().add("view-toggle");
        toggleBox.setAlignment(Pos.CENTER);
        
        Button monthBtn = new Button("📅 Month");
        monthBtn.getStyleClass().add(isMonthView ? "view-toggle-active" : "view-toggle-inactive");
        
        Button weekBtn = new Button("📋 Week");
        weekBtn.getStyleClass().add(!isMonthView ? "view-toggle-active" : "view-toggle-inactive");
        
        monthBtn.setOnAction(e -> {
            if (!isMonthView) {
                isMonthView = true;
                monthBtn.getStyleClass().setAll("button", "view-toggle-active");
                weekBtn.getStyleClass().setAll("button", "view-toggle-inactive");
                updateView();
            }
        });
        
        weekBtn.setOnAction(e -> {
            if (isMonthView) {
                isMonthView = false;
                weekBtn.getStyleClass().setAll("button", "view-toggle-active");
                monthBtn.getStyleClass().setAll("button", "view-toggle-inactive");
                updateView();
            }
        });
        
        toggleBox.getChildren().addAll(monthBtn, weekBtn);

        // Navigation Controls
        HBox navBox = new HBox(10);
        navBox.setAlignment(Pos.CENTER);
        
        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("calendar-nav-btn");
        prevBtn.setOnAction(e -> navigate(-1));
        
        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("calendar-nav-btn");
        nextBtn.setOnAction(e -> navigate(1));
        
        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("calendar-today-btn");
        todayBtn.setOnAction(e -> {
            currentMonth = YearMonth.now();
            currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            updateView();
        });
        
        monthLabel = new Label();
        monthLabel.getStyleClass().add("calendar-header");
        monthLabel.setPrefWidth(160);
        monthLabel.setAlignment(Pos.CENTER);
        
        navBox.getChildren().addAll(prevBtn, monthLabel, nextBtn, todayBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean canCreate = facade.getCurrentUser().getPermissions().contains("CREATE_SESSION");
        Button addBtn = new Button("+ Add Session");
        addBtn.getStyleClass().add("add-button");
        addBtn.setVisible(canCreate);
        addBtn.setManaged(canCreate);
        addBtn.setOnAction(e -> showCreateSessionDialog());

        headerRow.getChildren().addAll(title, toggleBox, navBox, spacer, addBtn);

        // --- Main Content Area ---
        mainContent = new BorderPane();
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        
        viewContainer = new StackPane();
        
        // --- Detail Panel (Frosted Glass) ---
        detailPanel = new VBox(15);
        detailPanel.setPrefWidth(320);
        detailPanel.setMinWidth(320);
        detailPanel.setMaxWidth(320);
        detailPanel.setPadding(new Insets(20));
        detailPanel.getStyleClass().add("detail-panel-premium");
        detailPanel.setTranslateX(320); // Hidden off-screen initially
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        mainContent.setCenter(viewContainer);
        mainContent.setRight(detailPanel);

        getChildren().addAll(headerRow, mainContent);
        
        // Initial render
        updateView();
    }
    
    private void navigate(int direction) {
        // Animate out current view
        javafx.scene.Node currentView = viewContainer.getChildren().isEmpty() ? null : viewContainer.getChildren().get(0);
        
        if (isMonthView) {
            currentMonth = currentMonth.plusMonths(direction);
        } else {
            currentWeekStart = currentWeekStart.plusWeeks(direction);
        }
        
        javafx.scene.Node newView = isMonthView ? buildMonthlyCalendar() : buildWeeklyTimetable();
        updateMonthLabel();
        
        if (currentView != null) {
            // Slide animation
            double offset = direction > 0 ? 50 : -50;
            newView.setTranslateX(offset);
            newView.setOpacity(0);
            viewContainer.getChildren().add(newView);
            
            TranslateTransition ttIn = new TranslateTransition(Duration.millis(300), newView);
            ttIn.setToX(0);
            FadeTransition ftIn = new FadeTransition(Duration.millis(300), newView);
            ftIn.setToValue(1);
            
            TranslateTransition ttOut = new TranslateTransition(Duration.millis(300), currentView);
            ttOut.setToX(-offset);
            FadeTransition ftOut = new FadeTransition(Duration.millis(300), currentView);
            ftOut.setToValue(0);
            
            ParallelTransition pt = new ParallelTransition(ttIn, ftIn, ttOut, ftOut);
            pt.setOnFinished(e -> viewContainer.getChildren().remove(currentView));
            pt.play();
        } else {
            viewContainer.getChildren().add(newView);
            updateMonthLabel();
        }
    }

    private void updateView() {
        viewContainer.getChildren().clear();
        javafx.scene.Node view = isMonthView ? buildMonthlyCalendar() : buildWeeklyTimetable();
        viewContainer.getChildren().add(view);
        updateMonthLabel();
    }
    
    private void updateMonthLabel() {
        if (isMonthView) {
            monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        } else {
            LocalDate weekEnd = currentWeekStart.plusDays(6);
            if (currentWeekStart.getMonth() == weekEnd.getMonth()) {
                monthLabel.setText(currentWeekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
                                   weekEnd.format(DateTimeFormatter.ofPattern("dd, yyyy")));
            } else {
                monthLabel.setText(currentWeekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
                                   weekEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            }
        }
    }

    // =========================================================================================
    // MONTHLY CALENDAR
    // =========================================================================================

    private GridPane buildMonthlyCalendar() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("calendar-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Column headers
        DayOfWeek[] days = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY };

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            grid.getColumnConstraints().add(cc);
            
            Label dayLabel = new Label(days[i].toString().substring(0, 3));
            dayLabel.getStyleClass().add("calendar-day-header");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            grid.add(dayLabel, i, 0);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeekOfFirst = firstOfMonth.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun
        
        LocalDate currentDay = firstOfMonth.minusDays(dayOfWeekOfFirst - 1);
        
        Map<LocalDate, List<FitnessSession>> sessionsByDate = facade.getSessionsForMonth(currentMonth);
        
        // Also get sessions for the padding days
        YearMonth prevMonth = currentMonth.minusMonths(1);
        YearMonth nextMonth = currentMonth.plusMonths(1);
        Map<LocalDate, List<FitnessSession>> prevMonthSessions = facade.getSessionsForMonth(prevMonth);
        Map<LocalDate, List<FitnessSession>> nextMonthSessions = facade.getSessionsForMonth(nextMonth);
        
        sessionsByDate.putAll(prevMonthSessions);
        sessionsByDate.putAll(nextMonthSessions);
        
        List<Booking> myBookings = facade.getMyBookings();

        for (int row = 1; row <= 6; row++) {
            for (int col = 0; col < 7; col++) {
                VBox dayCell = createDayCell(currentDay, sessionsByDate.get(currentDay), myBookings);
                grid.add(dayCell, col, row);
                GridPane.setVgrow(dayCell, Priority.ALWAYS);
                currentDay = currentDay.plusDays(1);
            }
            if (currentDay.getMonth() != currentMonth.getMonth() && currentDay.isAfter(firstOfMonth)) {
                break; // Stop adding rows if we're done with the month
            }
        }

        return grid;
    }
    
    private VBox createDayCell(LocalDate date, List<FitnessSession> sessions, List<Booking> myBookings) {
        VBox cell = new VBox(5);
        cell.setPadding(new Insets(8));
        cell.getStyleClass().add("calendar-day-cell");
        
        boolean isCurrentMonth = date.getMonth() == currentMonth.getMonth();
        boolean isToday = date.equals(LocalDate.now());
        boolean isPast = date.isBefore(LocalDate.now());
        
        if (!isCurrentMonth) {
            cell.setOpacity(0.4);
        }
        if (isPast) {
            cell.getStyleClass().add("calendar-day-past");
        } else {
            // Hover animation for future dates
            cell.setOnMouseEntered(e -> {
                TranslateTransition tt = new TranslateTransition(Duration.millis(200), cell);
                tt.setToY(-3);
                tt.play();
                cell.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.4)));
            });
            cell.setOnMouseExited(e -> {
                TranslateTransition tt = new TranslateTransition(Duration.millis(200), cell);
                tt.setToY(0);
                tt.play();
                cell.setEffect(null);
            });
            
            // Click animation and open panel
            cell.setOnMouseClicked(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(75), cell);
                st.setFromX(1.0); st.setFromY(1.0);
                st.setToX(0.96); st.setToY(0.96);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                st.play();
                
                openDetailPanelForDate(date, sessions);
            });
        }
        
        if (isToday) {
            cell.getStyleClass().add("calendar-day-today");
            // Add pulse effect
            DropShadow glow = new DropShadow(15, Color.web("#6366f1"));
            cell.setEffect(glow);
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 10)),
                new KeyFrame(Duration.seconds(1), new KeyValue(glow.radiusProperty(), 20)),
                new KeyFrame(Duration.seconds(2), new KeyValue(glow.radiusProperty(), 10))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        }

        // Date Number
        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dateLabel.getStyleClass().add("calendar-day-number");
        if (isToday) {
            dateLabel.setStyle("-fx-text-fill: #6366f1; -fx-font-weight: bold;");
        }
        cell.getChildren().add(dateLabel);

        // Session Chips
        if (sessions != null && !sessions.isEmpty()) {
            // Check if member has bookings on this date
            boolean memberBooked = myBookings.stream()
                .anyMatch(b -> b.getSessionDate().equals(date) && b.getStatus() != BookingStatus.CANCELLED);
                
            if (memberBooked) {
                cell.getStyleClass().add("calendar-day-booked");
            }
            
            HBox chipContainer = new HBox(4);
            // We use simple labels
            int maxChips = 3;
            int count = 0;
            
            for (FitnessSession s : sessions) {
                if (count < maxChips) {
                    Label chip = new Label(s.getSessionType().substring(0, Math.min(3, s.getSessionType().length())));
                    chip.getStyleClass().add("calendar-session-chip");
                    chip.setStyle("-fx-background-color: " + s.getColor() + ";");
                    chipContainer.getChildren().add(chip);
                    count++;
                }
            }
            
            cell.getChildren().add(chipContainer);
            
            if (sessions.size() > maxChips) {
                Label overflow = new Label("+" + (sessions.size() - maxChips) + " more");
                overflow.getStyleClass().add("calendar-overflow-label");
                cell.getChildren().add(overflow);
            }
        }

        return cell;
    }

    // =========================================================================================
    // WEEKLY TIMETABLE
    // =========================================================================================

    private ScrollPane buildWeeklyTimetable() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("timetable-grid");
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPadding(new Insets(5));

        // Column constraints
        ColumnConstraints timeCol = new ColumnConstraints(60);
        grid.getColumnConstraints().add(timeCol);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(120);
            grid.getColumnConstraints().add(dayCol);
        }

        // Header Dates
        LocalDate curr = currentWeekStart;
        for (int col = 0; col < 7; col++) {
            String dayStr = curr.getDayOfWeek().toString().substring(0, 3);
            String dateStr = curr.format(DateTimeFormatter.ofPattern("dd MMM"));
            Label header = new Label(dayStr + " " + dateStr);
            header.getStyleClass().add("timetable-day-header");
            if (curr.equals(LocalDate.now())) {
                header.setStyle("-fx-text-fill: #6366f1;");
            }
            header.setAlignment(Pos.CENTER);
            header.setMaxWidth(Double.MAX_VALUE);
            grid.add(header, col + 1, 0);
            curr = curr.plusDays(1);
        }

        // Row headers (time slots 06:00 - 21:00)
        for (int hour = 6; hour <= 21; hour++) {
            Label timeLabel = new Label(String.format("%02d:00", hour));
            timeLabel.getStyleClass().add("timetable-time-label");
            timeLabel.setPadding(new Insets(5, 10, 5, 5));
            grid.add(timeLabel, 0, hour - 5);

            for (int col = 0; col < 7; col++) {
                Pane cell = new Pane();
                cell.getStyleClass().add("timetable-cell");
                if (currentWeekStart.plusDays(col).equals(LocalDate.now())) {
                    cell.setStyle("-fx-background-color: rgba(99,102,241,0.05);"); // Subtle highlight for today
                }
                grid.add(cell, col + 1, hour - 5);
            }
        }

        // Place sessions for this week
        curr = currentWeekStart;
        for (int col = 0; col < 7; col++) {
            List<FitnessSession> sessions = facade.getSessionsForDate(curr);
            final LocalDate blockDate = curr;
            
            for (FitnessSession session : sessions) {
                int startRow = session.getStartTime().getHour() - 5;
                int endRow = session.getEndTime().getHour() - 5;
                int span = endRow - startRow;
                if (span <= 0) span = 1;

                VBox block = new VBox(2);
                block.setAlignment(Pos.TOP_LEFT);
                block.setPadding(new Insets(6));
                block.getStyleClass().add("session-block");
                
                // Frosted glass overlay look for blocks
                block.setStyle("-fx-background-color: linear-gradient(to bottom right, " + 
                               session.getColor() + "e6, " + session.getColor() + "b3);" +
                               "-fx-background-radius: 6; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 6;");

                Label nameLabel = new Label(session.getName());
                nameLabel.getStyleClass().add("session-block-name");
                
                Label instrLabel = new Label(session.getInstructor().getName());
                instrLabel.getStyleClass().add("session-block-instructor");

                block.getChildren().addAll(nameLabel, instrLabel);
                
                block.setOnMouseClicked(e -> openDetailPanelForDate(blockDate, List.of(session)));
                
                grid.add(block, col + 1, startRow, 1, span);
            }
            curr = curr.plusDays(1);
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("timetable-scroll");
        return scrollPane;
    }

    // =========================================================================================
    // DETAIL PANEL
    // =========================================================================================

    private void openDetailPanelForDate(LocalDate date, List<FitnessSession> sessions) {
        if (!detailPanel.isVisible()) {
            detailPanel.setVisible(true);
            detailPanel.setManaged(true);
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), detailPanel);
            tt.setFromX(320);
            tt.setToX(0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        }
        
        buildDetailPanelContent(date, sessions);
    }
    
    private void closeDetailPanel() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(250), detailPanel);
        tt.setToX(320);
        tt.setInterpolator(Interpolator.EASE_IN);
        tt.setOnFinished(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        });
        tt.play();
    }

    private void buildDetailPanelContent(LocalDate date, List<FitnessSession> sessions) {
        detailPanel.getChildren().clear();

        // Close button row
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("detail-close-btn");
        closeBtn.setOnAction(e -> closeDetailPanel());
        
        HBox topRow = new HBox(closeBtn);
        topRow.setAlignment(Pos.CENTER_RIGHT);

        // Header
        Label dateHeader = new Label(date.format(DateTimeFormatter.ofPattern("EEEE")));
        dateHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label dateSub = new Label(date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dateSub.setStyle("-fx-font-size: 14px; -fx-opacity: 0.7;");
        
        detailPanel.getChildren().addAll(topRow, dateHeader, dateSub, new Separator());

        // Sessions list
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        VBox sessionList = new VBox(15);
        sessionList.setPadding(new Insets(10, 0, 10, 0));
        
        if (sessions == null || sessions.isEmpty()) {
            Label noSessions = new Label("No sessions scheduled for this date.");
            noSessions.setWrapText(true);
            noSessions.setStyle("-fx-opacity: 0.6; -fx-padding: 20 0;");
            sessionList.getChildren().add(noSessions);
        } else {
            for (FitnessSession session : sessions) {
                sessionList.getChildren().add(createDetailSessionCard(session, date));
            }
        }
        
        scroll.setContent(sessionList);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        detailPanel.getChildren().add(scroll);
        
        // Admin controls
        if (facade.getCurrentUser().getRole() == Role.ADMIN) {
            Button scheduleBtn = new Button("+ Schedule Here");
            scheduleBtn.getStyleClass().add("action-button");
            scheduleBtn.setMaxWidth(Double.MAX_VALUE);
            scheduleBtn.setOnAction(e -> showCreateSessionDialog());
            detailPanel.getChildren().add(scheduleBtn);
        }
    }
    
    private VBox createDetailSessionCard(FitnessSession session, LocalDate date) {
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-session-card");
        card.setPadding(new Insets(15));
        
        // Left color border effect via CSS or inline style
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8;" +
                      "-fx-border-width: 0 0 0 4; -fx-border-color: " + session.getColor() + ";" +
                      "-fx-border-radius: 8;");
                      
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(session.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label type = new Label(session.getSessionType());
        type.setStyle("-fx-font-size: 10px; -fx-background-color: " + session.getColor() + "33;" +
                      "-fx-text-fill: " + session.getColor() + "; -fx-padding: 2 6; -fx-background-radius: 10;");
        header.getChildren().addAll(name, spacer, type);
        
        // Info
        Label time = new Label("🕐 " + session.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + 
                               session.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.setStyle("-fx-opacity: 0.8; -fx-font-size: 13px;");
        
        Label instr = new Label("👤 " + session.getInstructor().getName());
        instr.setStyle("-fx-opacity: 0.8; -fx-font-size: 13px;");
        
        Label loc = new Label("📍 " + session.getLocation().getDisplayName());
        loc.setStyle("-fx-opacity: 0.8; -fx-font-size: 13px;");
        
        card.getChildren().addAll(header, time, instr, loc);
        
        // Capacity & Booking
        List<Booking> bookings = facade.getBookingsForDate(session.getSessionId(), date);
        long booked = bookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).count();
        
        HBox capBox = new HBox(10);
        capBox.setAlignment(Pos.CENTER_LEFT);
        ProgressBar capBar = new ProgressBar((double) booked / session.getMaxCapacity());
        capBar.getStyleClass().add("capacity-bar");
        capBar.setPrefWidth(120);
        Label capText = new Label(booked + "/" + session.getMaxCapacity());
        capText.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");
        capBox.getChildren().addAll(capBar, capText);
        
        card.getChildren().add(capBox);
        
        // Action Button
        if (facade.getCurrentUser().getRole() == Role.MEMBER) {
            boolean isBooked = bookings.stream()
                .anyMatch(b -> b.getMember().getUserId().equals(facade.getCurrentUser().getUserId()) && 
                               b.getStatus() != BookingStatus.CANCELLED);
                               
            if (isBooked) {
                Booking myBooking = bookings.stream()
                    .filter(b -> b.getMember().getUserId().equals(facade.getCurrentUser().getUserId()) && 
                                 b.getStatus() != BookingStatus.CANCELLED)
                    .findFirst().orElse(null);
                    
                Button cancelBtn = new Button("Cancel Booking");
                cancelBtn.getStyleClass().add("cancel-button");
                cancelBtn.setMaxWidth(Double.MAX_VALUE);
                cancelBtn.setOnAction(e -> {
                    try {
                        facade.cancelBooking(myBooking.getBookingId());
                        buildDetailPanelContent(date, facade.getSessionsForDate(date));
                        updateView(); // Refresh main calendar
                    } catch (com.iwfc.exception.UnauthorizedAccessException | IllegalArgumentException ex) {
                        showAlert("Error", ex.getMessage());
                    }
                });
                card.getChildren().add(cancelBtn);
            } else if (date.isBefore(LocalDate.now())) {
                Label pastLbl = new Label("Session has ended");
                pastLbl.setStyle("-fx-opacity: 0.5; -fx-font-style: italic;");
                card.getChildren().add(pastLbl);
            } else if (booked >= session.getMaxCapacity()) {
                Label fullLbl = new Label("Fully Booked");
                fullLbl.setStyle("-fx-opacity: 0.8; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
                card.getChildren().add(fullLbl);
            } else {
                Button bookBtn = new Button("Book Now");
                bookBtn.getStyleClass().add("book-button");
                bookBtn.setMaxWidth(Double.MAX_VALUE);
                
                // Book Hover Effect
                bookBtn.setOnMouseEntered(e -> {
                    DropShadow shadow = new DropShadow(15, Color.web("#8b5cf6"));
                    bookBtn.setEffect(shadow);
                });
                bookBtn.setOnMouseExited(e -> bookBtn.setEffect(null));
                
                bookBtn.setOnAction(e -> {
                    try {
                        facade.bookSessionForDate(session.getSessionId(), date);
                        buildDetailPanelContent(date, facade.getSessionsForDate(date));
                        updateView();
                        showInfo("Success", "Booked " + session.getName() + " for " + date);
                    } catch (com.iwfc.exception.UnauthorizedAccessException | IllegalArgumentException | com.iwfc.exception.InvalidBookingException ex) {
                        showAlert("Booking Failed", ex.getMessage());
                    }
                });
                card.getChildren().add(bookBtn);
            }
        }
        
        return card;
    }

    // =========================================================================================
    // DIALOGS & UTILS
    // =========================================================================================

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
        
        DatePicker validFromPicker = new DatePicker(LocalDate.now());
        DatePicker validUntilPicker = new DatePicker(LocalDate.now().plusYears(1));

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
        grid.add(new Label("Valid From:"), 0, 3); grid.add(validFromPicker, 1, 3);
        grid.add(new Label("Valid Until:"), 0, 4); grid.add(validUntilPicker, 1, 4);
        grid.add(new Label("Start:"), 0, 5); grid.add(startHour, 1, 5);
        grid.add(new Label("End:"), 0, 6); grid.add(endHour, 1, 6);
        grid.add(new Label("Location:"), 0, 7); grid.add(locationCombo, 1, 7);
        grid.add(new Label("Capacity:"), 0, 8); grid.add(capacitySpinner, 1, 8);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().isEmpty()
                    && typeCombo.getValue() != null && dayCombo.getValue() != null
                    && locationCombo.getValue() != null) {
                Instructor instructor = (facade.getCurrentUser() instanceof Instructor i) ? i : null;
                String sessionId = "SES" + String.format("%03d", facade.getAllSessions().size() + 1);
                FitnessSession fs = new FitnessSession(sessionId, nameField.getText(), typeCombo.getValue(),
                        instructor, dayCombo.getValue(),
                        LocalTime.of(startHour.getValue(), 0),
                        LocalTime.of(endHour.getValue(), 0),
                        locationCombo.getValue(), capacitySpinner.getValue());
                fs.setValidFrom(validFromPicker.getValue());
                fs.setValidUntil(validUntilPicker.getValue());
                return fs;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(session -> {
            try {
                facade.createSession(session);
                updateView();
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

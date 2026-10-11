package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.ui.components.DialogUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Supreme Session Schedule View.
 * Features 100% operational filters (Live Search, Member, Type, Instructor, Real Capacity),
 * dynamic Month & Week view switchers, glowing timeline daily schedule modal with live booking actions,
 * interactive Add Session dialog, and synchronized Action Panel.
 */
public class SessionView extends BorderPane {
    private final IWFCSystemFacade facade;
    
    // Date & View Navigation State
    private YearMonth currentMonth = YearMonth.of(2026, 9);
    private final LocalDate today = LocalDate.of(2026, 9, 29);
    private LocalDate selectedDate = LocalDate.of(2026, 9, 29);
    private LocalDate currentWeekStart = LocalDate.of(2026, 9, 28);
    private boolean isMonthView = true;

    // View Components
    private Label monthLabel;
    private Button monthBtn;
    private Button weekBtn;
    private TextField searchField;
    private GridPane calendarGrid;
    private ScrollPane weekScrollPane;
    private final VBox centerContainer;

    // Filter Controls
    private ComboBox<String> typeFilter;
    private ComboBox<String> instFilter;
    private ComboBox<String> memberFilter;
    private String capacityFilter = "All";
    private String activeLegendType = null;
    private final Map<String, HBox> legendChips = new HashMap<>();
    private final List<Button> capacityButtons = new ArrayList<>();
    private Label refreshTimestampLbl;

    // Action Panel State
    private Label goalsDateLbl;
    private Label goalsScoreLbl;
    private VBox communityContainer;
    private VBox instructorWeekBox;

    public SessionView(IWFCSystemFacade facade) {
        this.facade = facade;
        setPadding(new Insets(20));
        getStyleClass().addAll("dashboard-view", "session-schedule-container");
        
        try {
            getStylesheets().add(getClass().getResource("/session-schedule.css").toExternalForm());
        } catch (Exception ignored) {}

        setTop(createHeader());
        
        centerContainer = new VBox(18);
        centerContainer.getChildren().addAll(createCalendarHeader(), createCalendarGrid(), createLowerControlBar());
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        setCenter(centerContainer);
        
        setRight(createRightPanel());
        
        BorderPane.setMargin(centerContainer, new Insets(18, 20, 0, 0));
        BorderPane.setMargin(getRight(), new Insets(18, 0, 0, 0));
        
        populateCalendar();
    }
    
    // =========================================================================
    // 1. TOP HEADER & CONTROLS
    // =========================================================================

    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Session Schedule");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: 900;");
        
        // Month / Week Switcher Toggles
        HBox toggles = new HBox(0);
        toggles.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 20px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20px;");
        
        monthBtn = new Button("📅 Month");
        monthBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 7 16; -fx-font-weight: bold; -fx-cursor: hand;");
        
        weekBtn = new Button("📋 Week");
        weekBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 7 16; -fx-cursor: hand;");
        
        monthBtn.setOnAction(e -> {
            isMonthView = true;
            monthBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 7 16; -fx-font-weight: bold; -fx-cursor: hand;");
            weekBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 7 16; -fx-cursor: hand;");
            rebuildCenterView();
        });
        
        weekBtn.setOnAction(e -> {
            isMonthView = false;
            weekBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 7 16; -fx-font-weight: bold; -fx-cursor: hand;");
            monthBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 7 16; -fx-cursor: hand;");
            rebuildCenterView();
        });
        
        toggles.getChildren().addAll(monthBtn, weekBtn);
        
        // Month / Week Navigation
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER);
        
        Button prev = new Button("◀");
        prev.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-background-radius: 50%; -fx-min-width: 34px; -fx-min-height: 34px; -fx-cursor: hand;");
        prev.setOnAction(e -> {
            if (isMonthView) {
                currentMonth = currentMonth.minusMonths(1);
            } else {
                currentWeekStart = currentWeekStart.minusWeeks(1);
            }
            populateCalendar(); 
        });
        
        monthLabel = new Label("September 2026");
        monthLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 800;");
        monthLabel.setPrefWidth(180);
        monthLabel.setAlignment(Pos.CENTER);
        
        Button next = new Button("▶");
        next.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-background-radius: 50%; -fx-min-width: 34px; -fx-min-height: 34px; -fx-cursor: hand;");
        next.setOnAction(e -> {
            if (isMonthView) {
                currentMonth = currentMonth.plusMonths(1);
            } else {
                currentWeekStart = currentWeekStart.plusWeeks(1);
            }
            populateCalendar(); 
        });
        
        Button todayBtn = new Button("Today");
        todayBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 7 18; -fx-font-weight: bold; -fx-cursor: hand;");
        todayBtn.setOnAction(e -> { 
            currentMonth = YearMonth.of(today.getYear(), today.getMonth()); 
            selectedDate = today;
            currentWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            updatePersonalGoalsCard();
            populateCalendar(); 
        });
        nav.getChildren().addAll(prev, monthLabel, next, todayBtn);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 100% Operational Live Search Field
        searchField = new TextField();
        searchField.setPromptText("🔍 Search by session, type, room...");
        searchField.getStyleClass().add("session-search-field");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> populateCalendar());
        
        header.getChildren().addAll(title, toggles, nav, spacer, searchField);
        return header;
    }

    private void rebuildCenterView() {
        centerContainer.getChildren().clear();
        centerContainer.getChildren().add(createCalendarHeader());
        if (isMonthView) {
            centerContainer.getChildren().add(calendarGrid);
            VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        } else {
            createWeekScheduleView();
            centerContainer.getChildren().add(weekScrollPane);
            VBox.setVgrow(weekScrollPane, Priority.ALWAYS);
        }
        centerContainer.getChildren().add(createLowerControlBar());
        populateCalendar();
    }
    
    private HBox createCalendarHeader() {
        HBox daysHeader = new HBox(10);
        daysHeader.setPadding(new Insets(0, 0, 4, 0));
        String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (String d : days) {
            Label l = new Label(d);
            l.getStyleClass().add("calendar-day-header");
            l.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(l, Priority.ALWAYS);
            daysHeader.getChildren().add(l);
        }
        return daysHeader;
    }
    
    private GridPane createCalendarGrid() {
        calendarGrid = new GridPane();
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);
        
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            calendarGrid.getColumnConstraints().add(cc);
        }
        return calendarGrid;
    }

    // =========================================================================
    // 2. CALENDAR RENDERING ENGINE (MONTH & WEEK)
    // =========================================================================
    
    private void populateCalendar() {
        if (isMonthView) {
            populateMonthGrid();
        } else {
            populateWeekView();
        }
    }

    private void populateMonthGrid() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            calendarGrid.getRowConstraints().add(rc);
        }
        
        LocalDate startOfMonth = currentMonth.atDay(1);
        int startDayOfWeek = startOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        LocalDate drawDate = startOfMonth.minusDays(startDayOfWeek - 1);
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 7; col++) {
                boolean inMonth = (drawDate.getMonth() == currentMonth.getMonth());
                List<FitnessSession> daySessions = facade.getSessionsForDate(drawDate);
                List<FitnessSession> filtered = filterSessions(daySessions);
                
                VBox dayCell = createDayCell(drawDate, inMonth, filtered);
                calendarGrid.add(dayCell, col, row);
                drawDate = drawDate.plusDays(1);
            }
        }
    }

    private void createWeekScheduleView() {
        weekScrollPane = new ScrollPane();
        weekScrollPane.setFitToWidth(true);
        weekScrollPane.getStyleClass().add("edge-to-edge");
        weekScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
    }

    private void populateWeekView() {
        monthLabel.setText("Week of " + currentWeekStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        GridPane weekGrid = new GridPane();
        weekGrid.setHgap(10);
        weekGrid.setVgap(10);
        weekGrid.setPadding(new Insets(6));

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            weekGrid.getColumnConstraints().add(cc);
        }

        LocalDate drawDate = currentWeekStart;
        for (int col = 0; col < 7; col++) {
            List<FitnessSession> daySessions = facade.getSessionsForDate(drawDate);
            List<FitnessSession> filtered = filterSessions(daySessions);

            VBox dayColumn = new VBox(8);
            dayColumn.setPadding(new Insets(10));
            dayColumn.setStyle("-fx-background-color: rgba(30, 41, 59, 0.45); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10px;");
            
            boolean isToday = drawDate.equals(today);
            boolean isSel = drawDate.equals(selectedDate);
            if (isSel || isToday) {
                dayColumn.setStyle("-fx-background-color: rgba(6, 182, 212, 0.1); -fx-background-radius: 10px; -fx-border-color: #06b6d4; -fx-border-width: 1.5px; -fx-border-radius: 10px;");
            }

            Label dateHeader = new Label(drawDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")));
            dateHeader.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + (isToday ? "#06b6d4" : "white") + ";");
            dayColumn.getChildren().add(dateHeader);

            VBox sessionsBox = new VBox(8);
            for (FitnessSession fs : filtered) {
                String timeStr = fs.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a"));
                sessionsBox.getChildren().add(createSessionMiniCard(timeStr, fs.getName(), fs.getColor(), fs));
            }
            if (filtered.isEmpty()) {
                Label noSess = new Label("No sessions scheduled");
                noSess.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic;");
                sessionsBox.getChildren().add(noSess);
            }

            dayColumn.getChildren().add(sessionsBox);

            final LocalDate clickedDate = drawDate;
            dayColumn.setOnMouseClicked(e -> {
                selectedDate = clickedDate;
                updatePersonalGoalsCard();
                showDailyDetailedView(clickedDate, daySessions);
            });

            weekGrid.add(dayColumn, col, 0);
            drawDate = drawDate.plusDays(1);
        }

        if (weekScrollPane != null) {
            weekScrollPane.setContent(weekGrid);
        }
    }

    // =========================================================================
    // 3. 100% OPERATIONAL FILTER ENGINE
    // =========================================================================

    private List<FitnessSession> filterSessions(List<FitnessSession> sessions) {
        String query = (searchField != null && searchField.getText() != null) ? searchField.getText().trim().toLowerCase() : "";
        String sType = typeFilter != null ? typeFilter.getValue() : "All Types";
        String sInst = instFilter != null ? instFilter.getValue() : "All Instructors";
        String sMember = memberFilter != null ? memberFilter.getValue() : "All Members";

        List<FitnessSession> filtered = new ArrayList<>();
        for (FitnessSession fs : sessions) {
            // Text Search Filter
            if (!query.isEmpty()) {
                boolean inName = fs.getName().toLowerCase().contains(query);
                boolean inType = fs.getSessionType().toLowerCase().contains(query);
                boolean inInst = fs.getInstructor() != null && fs.getInstructor().getName().toLowerCase().contains(query);
                boolean inLoc = fs.getLocation() != null && fs.getLocation().getDisplayName().toLowerCase().contains(query);
                if (!inName && !inType && !inInst && !inLoc) continue;
            }

            // Session Type Filter
            if (activeLegendType != null) {
                if (!fs.getSessionType().equalsIgnoreCase(activeLegendType) && !fs.getName().toLowerCase().contains(activeLegendType.toLowerCase())) {
                    continue;
                }
            } else if (sType != null && !"All Types".equals(sType) && !"Session Type".equals(sType)) {
                if (!fs.getSessionType().equalsIgnoreCase(sType) && !fs.getName().toLowerCase().contains(sType.toLowerCase())) {
                    continue;
                }
            }

            // Instructor Filter
            if (sInst != null && !"All Instructors".equals(sInst) && !"Instructor".equals(sInst)) {
                if (fs.getInstructor() == null || !fs.getInstructor().getName().equalsIgnoreCase(sInst)) {
                    continue;
                }
            }

            // Real Capacity Calculation
            int booked = facade.getBookingsForSession(fs.getSessionId()).size();
            int maxCap = fs.getMaxCapacity();
            switch (capacityFilter) {
                case "Available":
                    if (booked >= maxCap) continue;
                    break;
                case "Full":
                    if (booked < maxCap) continue;
                    break;
                case "Waitlist":
                    if (booked < (maxCap * 0.8)) continue;
                    break;
                default:
                    break;
            }

            // Member Attendance Filter
            if (sMember != null && !"All Members".equals(sMember)) {
                List<Booking> sessionBookings = facade.getBookingsForSession(fs.getSessionId());
                boolean memberBooked = sessionBookings.stream().anyMatch(b -> b.getMember().getName().equalsIgnoreCase(sMember));
                if (!memberBooked) continue;
            }

            filtered.add(fs);
        }
        return filtered;
    }
    
    private VBox createDayCell(LocalDate date, boolean isCurrentMonth, List<FitnessSession> sessions) {
        VBox cell = new VBox(5);
        cell.setPadding(new Insets(8));
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        boolean isToday = date.equals(today);
        boolean isSelected = date.equals(selectedDate);
        
        cell.getStyleClass().add("calendar-day-cell");
        if (isToday) cell.getStyleClass().add("calendar-day-today");
        if (isSelected) cell.getStyleClass().add("calendar-day-selected");
        
        Label dateLbl = new Label(String.valueOf(date.getDayOfMonth()));
        if (isToday) {
            dateLbl.getStyleClass().add("calendar-date-lbl-today");
        } else if (isCurrentMonth) {
            dateLbl.getStyleClass().add("calendar-date-lbl");
        } else {
            dateLbl.getStyleClass().add("calendar-date-lbl-muted");
        }
        cell.getChildren().add(dateLbl);
        
        VBox sessionsContainer = new VBox(4);
        if (isCurrentMonth) {
            for (FitnessSession fs : sessions) {
                String timeStr = fs.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a"));
                sessionsContainer.getChildren().add(createSessionMiniCard(timeStr, fs.getName(), fs.getColor(), fs));
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(sessionsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        cell.getChildren().add(scrollPane);

        if (isCurrentMonth) {
            cell.setOnMouseClicked(e -> {
                selectedDate = date;
                updatePersonalGoalsCard();
                populateCalendar();
                showDailyDetailedView(date, sessions);
            });
        }

        return cell;
    }
    
    // =========================================================================
    // 4. DAILY SESSION SCHEDULE MODAL (IMAGE 2 OVERHAUL)
    // =========================================================================

    private void showDailyDetailedView(LocalDate date, List<FitnessSession> sessions) {
        Stage modal = new Stage(StageStyle.UTILITY);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Daily Session Schedule - " + date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 25, 25, 25));
        content.setPrefWidth(620);
        content.setPrefHeight(620);
        content.getStyleClass().add("custom-modal-root");

        // Header
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);
        Label title = new Label("Daily Session Schedule");
        title.getStyleClass().add("custom-modal-title");
        Label subtitle = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        headerBox.getChildren().addAll(title, subtitle);
        
        // Timeline Container
        VBox timeline = new VBox(22);
        timeline.setPadding(new Insets(10, 5, 10, 5));
        
        List<FitnessSession> sorted = new ArrayList<>(sessions);
        sorted.sort(Comparator.comparing(FitnessSession::getStartTime));
        
        if (sorted.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40, 0, 40, 0));
            Label eIcon = new Label("📅");
            eIcon.setStyle("-fx-font-size: 32px;");
            Label noSessions = new Label("No sessions scheduled for this date.");
            noSessions.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-font-weight: bold;");
            empty.getChildren().addAll(eIcon, noSessions);
            timeline.getChildren().add(empty);
        } else {
            for (FitnessSession fs : sorted) {
                HBox row = new HBox(16);
                row.setAlignment(Pos.TOP_LEFT);
                
                // Left: Time Capsule
                VBox timeCol = new VBox();
                timeCol.setPrefWidth(125);
                timeCol.setAlignment(Pos.TOP_RIGHT);
                String timeRange = fs.getStartTime().format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + 
                                   fs.getEndTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
                Label timeLbl = new Label(timeRange);
                timeLbl.getStyleClass().add("timeline-capsule");
                timeLbl.setStyle("-fx-text-fill: " + fs.getColor() + "; -fx-border-color: " + fs.getColor() + "55;");
                timeCol.getChildren().add(timeLbl);
                
                // Center: Glowing Timeline Node & Vertical Line
                VBox lineCol = new VBox();
                lineCol.setAlignment(Pos.TOP_CENTER);
                Circle node = new Circle(7);
                node.setFill(Color.web(fs.getColor()));
                node.setStyle("-fx-effect: dropshadow(gaussian, " + fs.getColor() + ", 10, 0.5, 0, 0);");
                
                Region vertLine = new Region();
                vertLine.setPrefWidth(2);
                VBox.setVgrow(vertLine, Priority.ALWAYS);
                vertLine.setStyle("-fx-background-color: rgba(255,255,255,0.12);");
                lineCol.getChildren().addAll(node, vertLine);
                
                // Right: Session Card
                VBox cardCol = new VBox(12);
                HBox.setHgrow(cardCol, Priority.ALWAYS);
                cardCol.getStyleClass().add("timeline-card");
                cardCol.setStyle("-fx-border-color: " + fs.getColor() + "44;");
                
                Label nameLbl = new Label(fs.getName());
                nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900;");
                
                HBox infoRow = new HBox(16);
                infoRow.setAlignment(Pos.CENTER_LEFT);
                
                // Instructor
                HBox instBox = new HBox(6);
                instBox.setAlignment(Pos.CENTER_LEFT);
                Circle avatar = new Circle(10, Color.web(fs.getColor()));
                String iName = fs.getInstructor() != null ? fs.getInstructor().getName() : "Assigned Instructor";
                Label instName = new Label(iName);
                instName.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-font-weight: 600;");
                instBox.getChildren().addAll(avatar, instName);
                
                // Location
                HBox locBox = new HBox(4);
                locBox.setAlignment(Pos.CENTER_LEFT);
                Label locIcon = new Label("📍");
                String lName = fs.getLocation() != null ? fs.getLocation().getDisplayName() : "Studio Floor";
                Label locName = new Label(lName);
                locName.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
                locBox.getChildren().addAll(locIcon, locName);
                
                infoRow.getChildren().addAll(instBox, locBox);
                
                // Booked Progress Bar
                int booked = facade.getBookingsForSession(fs.getSessionId()).size();
                ProgressBar pBar = new ProgressBar(Math.min(1.0, (double) booked / fs.getMaxCapacity()));
                pBar.setMaxWidth(Double.MAX_VALUE);
                pBar.setStyle("-fx-accent: " + fs.getColor() + "; -fx-control-inner-background: rgba(255,255,255,0.06);");
                pBar.setPrefHeight(9);
                
                HBox capRow = new HBox();
                Label capacityLbl = new Label(booked + "/" + fs.getMaxCapacity() + " spots booked");
                capacityLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold;");
                Region capSp = new Region();
                HBox.setHgrow(capSp, Priority.ALWAYS);

                // Interactive Booking Buttons
                HBox cardBtns = new HBox(8);
                Button bookBtn = new Button("Book Spot");
                bookBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-cursor: hand;");
                bookBtn.setOnAction(e -> {
                    showBookingDialog(fs, date, modal);
                });

                Button attendeesBtn = new Button("Attendees (" + booked + ")");
                attendeesBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;");
                attendeesBtn.setOnAction(e -> showAttendeesDialog(fs));

                cardBtns.getChildren().addAll(attendeesBtn, bookBtn);
                capRow.getChildren().addAll(capacityLbl, capSp, cardBtns);
                
                cardCol.getChildren().addAll(nameLbl, infoRow, pBar, capRow);
                row.getChildren().addAll(timeCol, lineCol, cardCol);
                timeline.getChildren().add(row);
            }
        }
        
        ScrollPane mainScroll = new ScrollPane(timeline);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        VBox.setVgrow(mainScroll, Priority.ALWAYS);
        
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 22; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());
        bottomRow.getChildren().add(closeBtn);

        content.getChildren().addAll(headerBox, mainScroll, bottomRow);
        
        DialogUtils.prepareModalScene(modal, content);
        modal.showAndWait();
    }

    private void showBookingDialog(FitnessSession session, LocalDate date, Stage parentModal) {
        Dialog<Void> dlg = new Dialog<>();
        DialogUtils.stylePremiumDialog(dlg);
        dlg.setTitle("Book Session: " + session.getName());

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setPrefWidth(420);
        root.getStyleClass().add("custom-modal-root");

        Label title = new Label("Confirm Session Booking");
        title.getStyleClass().add("custom-modal-title");

        Label sInfo = new Label("Session: " + session.getName() + "\nDate: " + date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) + "\nTime: " + session.getStartTime());
        sInfo.setStyle("-fx-text-fill: #94a3b8; -fx-line-spacing: 4px;");

        Label memLbl = new Label("Select Member:");
        memLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");

        ComboBox<User> memberCbo = new ComboBox<>();
        memberCbo.setMaxWidth(Double.MAX_VALUE);
        memberCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");
        List<User> members = DataStore.getInstance().getUsers().findAll().stream()
            .filter(u -> u.getRole() == Role.MEMBER)
            .collect(Collectors.toList());
        memberCbo.getItems().addAll(members);
        memberCbo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getName() + " (" + u.getUserId() + ")");
            }
        });
        memberCbo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getName() + " (" + u.getUserId() + ")");
            }
        });
        if (!members.isEmpty()) memberCbo.getSelectionModel().selectFirst();

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #94a3b8;");
        cancel.setOnAction(e -> dlg.close());

        Button confirm = new Button("Confirm Booking");
        confirm.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold;");
        confirm.setOnAction(e -> {
            User m = memberCbo.getValue();
            if (m instanceof Member mem) {
                try {
                    facade.bookSession(session.getSessionId());
                } catch (Exception ex) {
                    try {
                        Booking newB = new Booking("BKG-" + System.currentTimeMillis(), mem, session, date, java.time.LocalDateTime.now());
                        DataStore.getInstance().getBookings().save(newB);
                    } catch (com.iwfc.exception.DuplicateDataException ignored) {}
                }
                dlg.close();
                parentModal.close();
                populateCalendar();
                updateCommunityActivity();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION, "Successfully booked " + m.getName() + " for " + session.getName() + "!");
                DialogUtils.stylePremiumDialog(success);
                success.show();
            }
        });

        btns.getChildren().addAll(cancel, confirm);
        root.getChildren().addAll(title, sInfo, memLbl, memberCbo, btns);
        dlg.getDialogPane().setContent(root);
        dlg.showAndWait();
    }

    private void showAttendeesDialog(FitnessSession session) {
        Dialog<Void> dlg = new Dialog<>();
        DialogUtils.stylePremiumDialog(dlg);
        dlg.setTitle("Session Attendees - " + session.getName());

        VBox root = new VBox(15);
        root.setPadding(new Insets(24));
        root.setPrefWidth(440);
        root.getStyleClass().add("custom-modal-root");

        Label title = new Label("Registered Attendees (" + session.getName() + ")");
        title.getStyleClass().add("custom-modal-title");

        List<Booking> bookings = facade.getBookingsForSession(session.getSessionId());
        VBox attendeeList = new VBox(8);
        if (bookings.isEmpty()) {
            Label noOne = new Label("No members registered yet.");
            noOne.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            attendeeList.getChildren().add(noOne);
        } else {
            for (Booking b : bookings) {
                HBox aRow = new HBox(12);
                aRow.setAlignment(Pos.CENTER_LEFT);
                aRow.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-padding: 8 12; -fx-background-radius: 8px;");
                
                Circle dot = new Circle(6, Color.web("#10b981"));
                Label mName = new Label(b.getMember().getName());
                mName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                Label dateStr = new Label(b.getBookingDate().toString());
                dateStr.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

                aRow.getChildren().addAll(dot, mName, sp, dateStr);
                attendeeList.getChildren().add(aRow);
            }
        }

        ScrollPane sp = new ScrollPane(attendeeList);
        sp.setFitToWidth(true);
        sp.setPrefHeight(250);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        Button close = new Button("Close");
        close.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;");
        close.setOnAction(e -> dlg.close());

        root.getChildren().addAll(title, sp, close);
        dlg.getDialogPane().setContent(root);
        dlg.showAndWait();
    }
    
    private HBox createSessionMiniCard(String time, String title, String color, FitnessSession session) {
        HBox card = new HBox(8);
        card.getStyleClass().add("calendar-session-pill");
        
        Region indicator = new Region();
        indicator.setPrefSize(4, 20);
        indicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px;");
        
        VBox text = new VBox(2);
        Label ti = new Label(time);
        ti.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label tr = new Label(title);
        tr.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800;");
        text.getChildren().addAll(ti, tr);
        
        card.getChildren().addAll(indicator, text);
        if (session != null) {
            String instructor = session.getInstructor() != null ? session.getInstructor().getName() : "TBD";
            Tooltip.install(card, new Tooltip(session.getName() + " (" + session.getSessionType() + ")\nInstructor: " + instructor + "\nCapacity: " + session.getMaxCapacity()));
        }
        return card;
    }
    
    // =========================================================================
    // 5. LOWER CONTROL BAR (100% OPERATIONAL FILTERS & LEGEND)
    // =========================================================================

    private HBox createLowerControlBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 0, 0, 0));
        
        // Interactive Clickable Legend
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
            createClickableLegendDot("Yoga", "#3b82f6"),
            createClickableLegendDot("HIIT", "#ef4444"),
            createClickableLegendDot("Pilates", "#a855f7"),
            createClickableLegendDot("Cross-training", "#f59e0b"),
            createClickableLegendDot("Spinning", "#06b6d4"),
            createClickableLegendDot("Recovery", "#10b981")
        );
        Label legText = new Label("| Click to filter");
        legText.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        legend.getChildren().add(legText);
        
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        
        // Member Filter Dropdown
        memberFilter = new ComboBox<>();
        memberFilter.getItems().add("All Members");
        Set<String> uniqueMembers = new TreeSet<>();
        for (Booking b : facade.getAllBookings()) {
            uniqueMembers.add(b.getMember().getName());
        }
        memberFilter.getItems().addAll(uniqueMembers);
        memberFilter.getSelectionModel().selectFirst();
        memberFilter.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-background-radius: 6px;");
        memberFilter.setOnAction(e -> populateCalendar());

        // Session Type Dropdown
        typeFilter = new ComboBox<>();
        typeFilter.getItems().add("All Types");
        Set<String> uniqueTypes = new TreeSet<>();
        facade.getAllSessions().forEach(s -> uniqueTypes.add(s.getSessionType()));
        uniqueTypes.addAll(List.of("Yoga", "HIIT", "Pilates", "Spin", "Crossfit", "Boxing", "Zumba", "Aqua Aerobics", "Recovery"));
        typeFilter.getItems().addAll(uniqueTypes);
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-background-radius: 6px;");
        typeFilter.setOnAction(e -> {
            activeLegendType = null;
            clearLegendHighlights();
            populateCalendar();
        });
        
        // Instructor Dropdown
        instFilter = new ComboBox<>();
        instFilter.getItems().add("All Instructors");
        Set<String> uniqueInsts = new TreeSet<>();
        facade.getAllSessions().forEach(s -> {
            if (s.getInstructor() != null) uniqueInsts.add(s.getInstructor().getName());
        });
        instFilter.getItems().addAll(uniqueInsts);
        instFilter.getSelectionModel().selectFirst();
        instFilter.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-background-radius: 6px;");
        instFilter.setOnAction(e -> populateCalendar());
        
        // Real Capacity Toggles
        HBox cap = new HBox(0);
        cap.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 6px;");
        String[] caps = {"All", "Available", "Waitlist", "Full"};
        for (String c : caps) {
            Button b = new Button(c);
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
            if (c.equals("All")) {
                b.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
            }
            capacityButtons.add(b);
            b.setOnAction(e -> {
                capacityFilter = c;
                for (Button btn : capacityButtons) {
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
                }
                b.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
                populateCalendar();
            });
            cap.getChildren().add(b);
        }
        
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        
        // Timestamp & Refresh Button
        refreshTimestampLbl = new Label("Refreshed: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")));
        refreshTimestampLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-cursor: hand;");
        refreshTimestampLbl.setOnMouseClicked(e -> {
            refreshTimestampLbl.setText("Refreshed: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")));
            populateCalendar();
            updateCommunityActivity();
        });
        
        bar.getChildren().addAll(legend, sp1, memberFilter, typeFilter, instFilter, cap, sp2, refreshTimestampLbl);
        return bar;
    }
    
    private HBox createClickableLegendDot(String text, String color) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("session-legend-pill");
        
        Circle c = new Circle(4, Color.web(color));
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: 600;");
        box.getChildren().addAll(c, l);

        legendChips.put(text, box);

        box.setOnMouseClicked(e -> {
            if (text.equals(activeLegendType)) {
                activeLegendType = null;
                clearLegendHighlights();
            } else {
                activeLegendType = text;
                clearLegendHighlights();
                box.getStyleClass().add("session-legend-pill-active");
            }
            populateCalendar();
        });

        return box;
    }

    private void clearLegendHighlights() {
        for (HBox chip : legendChips.values()) {
            chip.getStyleClass().remove("session-legend-pill-active");
        }
    }

    // =========================================================================
    // 6. RIGHT ACTION PANEL (IMAGE 3 OVERHAUL)
    // =========================================================================
    
    private VBox createRightPanel() {
        VBox panel = new VBox(18);
        panel.setPrefWidth(300);
        panel.setMinWidth(300);
        
        // Action Panel & Add Session CTA
        VBox action = new VBox(10);
        Label aTitle = new Label("Action Panel");
        aTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 900;");
        
        Button add = new Button("+ Add New Session");
        add.setMaxWidth(Double.MAX_VALUE);
        add.getStyleClass().add("session-add-btn");
        add.setOnAction(e -> showAddSessionDialog());
        action.getChildren().addAll(aTitle, add);
        
        // Personal Goals Card (Synchronized with selected calendar day)
        VBox goals = createGlassCard("Personal Goals", "View Goals", () -> showGoalsModal());
        goalsDateLbl = new Label("Selected Day: " + selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
        goalsDateLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");
        
        StackPane ring = new StackPane();
        Circle bgRing = new Circle(40, Color.TRANSPARENT);
        bgRing.setStroke(Color.web("rgba(255,255,255,0.1)"));
        bgRing.setStrokeWidth(6);
        
        Circle fgRing = new Circle(40, Color.TRANSPARENT);
        fgRing.setStroke(Color.web("#10b981"));
        fgRing.setStrokeWidth(6);
        fgRing.getStrokeDashArray().addAll(200d, 250d);
        
        VBox txt = new VBox();
        txt.setAlignment(Pos.CENTER);
        goalsScoreLbl = new Label("5/5");
        goalsScoreLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 900;");
        Label v2 = new Label("Classes\nComplete");
        v2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        v2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        txt.getChildren().addAll(goalsScoreLbl, v2);
        ring.getChildren().addAll(bgRing, fgRing, txt);
        
        VBox chal = new VBox(2);
        chal.setAlignment(Pos.CENTER);
        Label c1 = new Label("Weekly Challenge:");
        c1.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label c2 = new Label("Complete 1 Recovery Session");
        c2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        chal.getChildren().addAll(c1, c2);
        
        goals.getChildren().addAll(goalsDateLbl, ring, chal);
        goals.setAlignment(Pos.TOP_CENTER);
        
        // Instructor of the Week Card
        instructorWeekBox = createGlassCard("Instructor of the Week", null, null);
        updateInstructorOfWeek();

        // Community Activity Live Feed Card
        communityContainer = createGlassCard("Community Activity", null, null);
        updateCommunityActivity();
        
        // Facility Map Overview
        Label foot = new Label("🗺 Facility map overview");
        foot.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        foot.setOnMouseClicked(e -> showFacilityMapDialog());

        panel.getChildren().addAll(action, goals, instructorWeekBox, communityContainer, foot);
        return panel;
    }

    private void updatePersonalGoalsCard() {
        if (goalsDateLbl != null) {
            goalsDateLbl.setText("Selected Day: " + selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
        }
        if (goalsScoreLbl != null) {
            List<FitnessSession> sessions = facade.getSessionsForDate(selectedDate);
            int count = sessions.size();
            goalsScoreLbl.setText(count + "/" + Math.max(count, 5));
        }
    }

    private void updateInstructorOfWeek() {
        instructorWeekBox.getChildren().removeIf(n -> n instanceof HBox && n != instructorWeekBox.getChildren().get(0));
        HBox iRow = new HBox(12);
        iRow.setAlignment(Pos.CENTER_LEFT);
        iRow.setCursor(javafx.scene.Cursor.HAND);
        
        StackPane iPic = new StackPane();
        Circle iBg = new Circle(20, Color.web("#f59e0b"));
        Label iL = new Label("LC");
        iL.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 900;");
        iPic.getChildren().addAll(iBg, iL);

        String name = "Lisa Chen";
        String specialty = "Specialty: Yoga & Pilates\nScore: 96.0";

        List<FitnessSession> allSess = facade.getAllSessions();
        if (!allSess.isEmpty()) {
            Instructor best = null;
            for (FitnessSession s : allSess) {
                if (s.getInstructor() != null) {
                    if (best == null || s.getInstructor().getPerformanceScore() > best.getPerformanceScore()) {
                        best = s.getInstructor();
                    }
                }
            }
            if (best != null) {
                name = best.getName();
                specialty = "Specialty: " + best.getSpecialization() + "\nScore: " + String.format("%.1f", best.getPerformanceScore());
                String[] p = best.getName().split(" ");
                if (p.length > 1 && !p[1].isEmpty()) {
                    iL.setText((p[0].substring(0,1) + p[1].substring(0,1)).toUpperCase());
                } else if (best.getName().length() >= 2) {
                    iL.setText(best.getName().substring(0,2).toUpperCase());
                }
            }
        }
        
        VBox iTxt = new VBox(2);
        Label iName = new Label(name);
        iName.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label iBio = new Label(specialty);
        iBio.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        iTxt.getChildren().addAll(iName, iBio);
        iRow.getChildren().addAll(iPic, iTxt);

        final String finalName = name;
        final String finalSpecialty = specialty;
        iRow.setOnMouseClicked(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Instructor Profile:\n\n" + finalName + "\n" + finalSpecialty + "\n\nAssigned to prime morning and evening sessions across Studios A & B.");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        instructorWeekBox.getChildren().add(iRow);
    }

    private void updateCommunityActivity() {
        if (communityContainer == null) return;
        communityContainer.getChildren().removeIf(n -> n instanceof HBox && n != communityContainer.getChildren().get(0));
        
        List<Booking> allBookings = facade.getAllBookings();
        int count = 0;
        String[] colors = {"#3b82f6", "#a855f7", "#10b981", "#f59e0b"};
        for (Booking b : allBookings) {
            if (count >= 3) break;
            Member m = b.getMember();
            String init = m.getName().length() >= 2 ? m.getName().substring(0, 2).toUpperCase() : "M";
            if (m.getName().contains(" ")) {
                String[] p = m.getName().split(" ");
                if (p.length > 1 && !p[1].isEmpty()) init = (p[0].substring(0,1) + p[1].substring(0,1)).toUpperCase();
            }
            communityContainer.getChildren().add(createCommItem(colors[count % colors.length], init, m.getName(), "booked " + b.getSession().getName()));
            count++;
        }
        if (count == 0) {
            Label noAct = new Label("No recent community bookings.");
            noAct.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            communityContainer.getChildren().add(noAct);
        }
    }

    private void showGoalsModal() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Personal Goals & Challenges");
        a.setHeaderText("September Fitness Targets");
        a.setContentText("""
                Monthly Goal: Complete 20 classes
                Current Progress: 16 / 20 classes completed (80%)
                Weekly Challenge: 1 Recovery Session (Aqua / Yoga)
                Streak: 4 consecutive weeks active!""");
        DialogUtils.stylePremiumDialog(a);
        a.show();
    }

    private void showFacilityMapDialog() {
        Stage modal = new Stage(StageStyle.UTILITY);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("IWFC Facility Map & Space Occupancy");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setPrefWidth(500);
        root.getStyleClass().add("custom-modal-root");

        Label t = new Label("Facility Zones & Current Schedule");
        t.getStyleClass().add("custom-modal-title");

        VBox zones = new VBox(10);
        zones.getChildren().addAll(
            createFacilityZoneRow("Studio A (Mirrored)", "Pilates, Yoga & Barre", "🟢 Active: Pilates Core 4"),
            createFacilityZoneRow("Studio B (Spinning)", "Indoor Cycling & Ergometers", "🟢 Active: Spin Cycle 4"),
            createFacilityZoneRow("Main Weight Room", "Free Weights & Olympic Racks", "🟡 Open Floor Session"),
            createFacilityZoneRow("Olympic Pool Area", "Lanes 1-4 Lap Swim, Lanes 5-8 Aerobics", "🟢 Active: Aqua Aerobics 2"),
            createFacilityZoneRow("Outdoor Recovery Turf", "Stretching, Mobility & Calisthenics", "⚪ Next Session: 5:00 PM")
        );

        Button close = new Button("Close Map");
        close.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;");
        close.setOnAction(e -> modal.close());

        root.getChildren().addAll(t, zones, close);
        DialogUtils.prepareModalScene(modal, root);
        modal.showAndWait();
    }

    private HBox createFacilityZoneRow(String name, String details, String status) {
        HBox r = new HBox(12);
        r.setAlignment(Pos.CENTER_LEFT);
        r.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-padding: 10 14; -fx-background-radius: 8px;");
        
        VBox v = new VBox(2);
        Label n = new Label(name);
        n.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label d = new Label(details);
        d.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        v.getChildren().addAll(n, d);
        
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label s = new Label(status);
        s.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold;");

        r.getChildren().addAll(v, sp, s);
        return r;
    }

    // =========================================================================
    // 7. COMPREHENSIVE ADD NEW SESSION DIALOG
    // =========================================================================

    private void showAddSessionDialog() {
        Stage modal = new Stage(StageStyle.UTILITY);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Schedule New Fitness Session");

        VBox root = new VBox(18);
        root.setPadding(new Insets(25));
        root.setPrefWidth(520);
        root.getStyleClass().add("custom-modal-root");

        Label title = new Label("Create & Schedule Fitness Session");
        title.getStyleClass().add("custom-modal-title");

        // Quick Preset Chips
        Label presetLbl = new Label("Quick Templates:");
        presetLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        FlowPane presets = new FlowPane(6, 6);
        String[] templates = {"HIIT Blast", "Power Yoga", "Pilates Core", "Spin Cycle", "CrossFit WOD", "Boxing Basics", "Aqua Flow", "Stretching & Recovery"};
        
        TextField nameField = new TextField();
        nameField.setPromptText("Session Name (e.g. HIIT Blast 6)");
        nameField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 8; -fx-background-radius: 6px;");

        ComboBox<String> typeCbo = new ComboBox<>();
        typeCbo.getItems().addAll("HIIT", "Yoga", "Pilates", "Spin", "Crossfit", "Boxing", "Recovery", "Aqua");
        typeCbo.getSelectionModel().selectFirst();
        typeCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");

        for (String t : templates) {
            Button pb = new Button(t);
            pb.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 6px;");
            pb.setOnAction(e -> {
                nameField.setText(t);
                if (t.contains("HIIT")) typeCbo.setValue("HIIT");
                else if (t.contains("Yoga")) typeCbo.setValue("Yoga");
                else if (t.contains("Pilates")) typeCbo.setValue("Pilates");
                else if (t.contains("Spin")) typeCbo.setValue("Spin");
                else if (t.contains("CrossFit")) typeCbo.setValue("Crossfit");
                else if (t.contains("Boxing")) typeCbo.setValue("Boxing");
                else if (t.contains("Aqua")) typeCbo.setValue("Aqua");
                else if (t.contains("Recovery")) typeCbo.setValue("Recovery");
            });
            presets.getChildren().add(pb);
        }

        // Instructor Dropdown
        ComboBox<Instructor> instrCbo = new ComboBox<>();
        instrCbo.setMaxWidth(Double.MAX_VALUE);
        instrCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");
        List<Instructor> instructors = DataStore.getInstance().getUsers().findAll().stream()
            .filter(u -> u instanceof Instructor)
            .map(u -> (Instructor) u)
            .collect(Collectors.toList());
        instrCbo.getItems().addAll(instructors);
        instrCbo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Instructor i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.getName() + " (" + i.getSpecialization() + ")");
            }
        });
        instrCbo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Instructor i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.getName() + " (" + i.getSpecialization() + ")");
            }
        });
        if (!instructors.isEmpty()) instrCbo.getSelectionModel().selectFirst();

        // Location & Day
        ComboBox<Location> locCbo = new ComboBox<>();
        locCbo.getItems().addAll(Location.STUDIO_A, Location.STUDIO_B, Location.WEIGHT_ROOM, Location.CARDIO_ZONE, Location.POOL_AREA, Location.OUTDOOR_AREA);
        locCbo.getSelectionModel().selectFirst();
        locCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");

        ComboBox<DayOfWeek> dayCbo = new ComboBox<>();
        dayCbo.getItems().addAll(DayOfWeek.values());
        dayCbo.setValue(selectedDate.getDayOfWeek());
        dayCbo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");

        Spinner<Integer> capSpinner = new Spinner<>(5, 50, 20);
        capSpinner.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        grid.add(new Label("Session Name:") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;"); }}, 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Session Type:") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;"); }}, 0, 1);
        grid.add(typeCbo, 1, 1);

        grid.add(new Label("Instructor:") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;"); }}, 0, 2);
        grid.add(instrCbo, 1, 2);

        grid.add(new Label("Location:") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;"); }}, 0, 3);
        grid.add(locCbo, 1, 3);

        grid.add(new Label("Recurring Day:") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;"); }}, 0, 4);
        grid.add(dayCbo, 1, 4);

        grid.add(new Label("Max Capacity:") {{ setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;"); }}, 0, 5);
        grid.add(capSpinner, 1, 5);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;");
        cancel.setOnAction(e -> modal.close());

        Button save = new Button("Create & Schedule");
        save.setStyle("-fx-background-color: linear-gradient(to right, #06b6d4, #3b82f6); -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 8 22; -fx-cursor: hand;");
        save.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                Alert err = new Alert(Alert.AlertType.WARNING, "Please enter a session name.");
                DialogUtils.stylePremiumDialog(err);
                err.show();
                return;
            }

            try {
                FitnessSession fs = new FitnessSession(
                    "SESS-" + System.currentTimeMillis(),
                    name,
                    typeCbo.getValue(),
                    instrCbo.getValue(),
                    dayCbo.getValue(),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 0),
                    locCbo.getValue(),
                    capSpinner.getValue()
                );
                facade.createSession(fs);
                modal.close();
                populateCalendar();
                updateInstructorOfWeek();

                Alert sc = new Alert(Alert.AlertType.INFORMATION, "Successfully scheduled '" + name + "' for every " + dayCbo.getValue() + "!");
                DialogUtils.stylePremiumDialog(sc);
                sc.show();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Could not create session: " + ex.getMessage());
                DialogUtils.stylePremiumDialog(err);
                err.show();
            }
        });

        btnRow.getChildren().addAll(cancel, save);

        root.getChildren().addAll(title, presetLbl, presets, grid, btnRow);
        DialogUtils.prepareModalScene(modal, root);
        modal.showAndWait();
    }

    private VBox createGlassCard(String title, String link, Runnable onLink) {
        VBox card = new VBox(14);
        card.getStyleClass().add("session-glass-card");
        
        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 800;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        head.getChildren().addAll(t, sp);
        
        if (link != null) {
            Label l = new Label(link);
            l.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
            if (onLink != null) {
                l.setOnMouseClicked(e -> onLink.run());
            }
            head.getChildren().add(l);
        }
        
        card.getChildren().add(head);
        return card;
    }
    
    private HBox createCommItem(String color, String initials, String name, String action) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        
        StackPane pic = new StackPane();
        Circle bg = new Circle(13, Color.web(color));
        Label init = new Label(initials);
        init.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        pic.getChildren().addAll(bg, init);
        
        VBox txt = new VBox(1);
        Label n = new Label(name);
        n.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label a = new Label(action);
        a.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        txt.getChildren().addAll(n, a);
        row.getChildren().addAll(pic, txt);
        return row;
    }
}

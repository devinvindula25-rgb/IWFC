package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.ui.components.DialogUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class SessionView extends BorderPane {
    private final IWFCSystemFacade facade;
    
    private YearMonth currentMonth = YearMonth.of(2026, 9);
    private LocalDate today = LocalDate.of(2026, 9, 29);
    private LocalDate currentWeekStart = LocalDate.of(2026, 9, 28);
    private boolean isMonthView = true;
    private Label monthLabel;
    private GridPane calendarGrid;
    
    private ComboBox<String> typeFilter;
    private ComboBox<String> instFilter;
    private ComboBox<String> memberFilter;
    private String capacityFilter = "All";

    public SessionView(IWFCSystemFacade facade) {
        this.facade = facade;
        setPadding(new Insets(20));
        getStyleClass().add("dashboard-view");
        setStyle("-fx-background-color: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
        
        try {
            getStylesheets().add(getClass().getResource("/session-schedule.css").toExternalForm());
        } catch (Exception e) {}

        setTop(createHeader());
        
        VBox centerArea = new VBox(20);
        centerArea.getChildren().addAll(createCalendarHeader(), createCalendarGrid(), createLowerControlBar());
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        setCenter(centerArea);
        
        setRight(createRightPanel());
        
        BorderPane.setMargin(centerArea, new Insets(20, 20, 0, 0));
        BorderPane.setMargin(getRight(), new Insets(20, 0, 0, 0));
        
        populateCalendar();
    }
    
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Session Schedule");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        // Toggles
        HBox toggles = new HBox(0);
        toggles.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 20px;");
        Button monthBtn = new Button("📅 Month");
        monthBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8 15; -fx-cursor: hand;");
        Button weekBtn = new Button("📋 Week");
        weekBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 8 15; -fx-cursor: hand;");
        
        monthBtn.setOnAction(e -> {
            isMonthView = true;
            monthBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8 15; -fx-cursor: hand;");
            weekBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 8 15; -fx-cursor: hand;");
            populateCalendar();
        });
        
        weekBtn.setOnAction(e -> {
            isMonthView = false;
            weekBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8 15; -fx-cursor: hand;");
            monthBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 8 15; -fx-cursor: hand;");
            populateCalendar();
        });
        
        toggles.getChildren().addAll(monthBtn, weekBtn);
        
        // Nav
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER);
        Button prev = new Button("◀");
        prev.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-background-radius: 50%; -fx-min-width: 35px; -fx-min-height: 35px; -fx-cursor: hand;");
        prev.setOnAction(e -> {
            if (isMonthView) currentMonth = currentMonth.minusMonths(1);
            else currentWeekStart = currentWeekStart.minusWeeks(1);
            populateCalendar(); 
        });
        
        monthLabel = new Label("September 2026");
        monthLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        monthLabel.setPrefWidth(140);
        monthLabel.setAlignment(Pos.CENTER);
        
        Button next = new Button("▶");
        next.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-background-radius: 50%; -fx-min-width: 35px; -fx-min-height: 35px; -fx-cursor: hand;");
        next.setOnAction(e -> {
            if (isMonthView) currentMonth = currentMonth.plusMonths(1);
            else currentWeekStart = currentWeekStart.plusWeeks(1);
            populateCalendar(); 
        });
        
        Button todayBtn = new Button("Today");
        todayBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8 20; -fx-cursor: hand;");
        todayBtn.setOnAction(e -> { 
            currentMonth = YearMonth.of(today.getYear(), today.getMonth()); 
            currentWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            populateCalendar(); 
        });
        nav.getChildren().addAll(prev, monthLabel, next, todayBtn);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Search
        TextField search = new TextField();
        search.setPromptText("🔍 Search");
        search.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8 15;");
        
        header.getChildren().addAll(title, toggles, nav, spacer, search);
        return header;
    }
    
    private HBox createCalendarHeader() {
        HBox daysHeader = new HBox(10);
        String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (String d : days) {
            Label l = new Label(d);
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            HBox.setHgrow(l, Priority.ALWAYS);
            daysHeader.getChildren().add(l);
        }
        return daysHeader;
    }
    
    private GridPane createCalendarGrid() {
        calendarGrid = new GridPane();
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);
        
        for (int i=0; i<7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            calendarGrid.getColumnConstraints().add(cc);
        }
        // Row constraints applied dynamically in populateCalendar
        return calendarGrid;
    }
    
    private void populateCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        
        if (isMonthView) {
            monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        } else {
            monthLabel.setText("Week of " + currentWeekStart.format(DateTimeFormatter.ofPattern("MMM d")));
        }
        
        int rows = isMonthView ? 5 : 1;
        for (int i=0; i<rows; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            calendarGrid.getRowConstraints().add(rc);
        }
        
        LocalDate drawDate;
        if (isMonthView) {
            LocalDate startOfMonth = currentMonth.atDay(1);
            int startDayOfWeek = startOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
            drawDate = startOfMonth.minusDays(startDayOfWeek - 1);
        } else {
            drawDate = currentWeekStart;
        }
        
        for (int row=0; row<rows; row++) {
            for (int col=0; col<7; col++) {
                boolean inMonth = isMonthView ? (drawDate.getMonth() == currentMonth.getMonth()) : true;
                
                // Fetch real sessions for the date
                java.util.List<com.iwfc.model.FitnessSession> daySessions = facade.getSessionsForDate(drawDate);
                
                // Apply filters
                String sType = typeFilter != null ? typeFilter.getValue() : "Session Type";
                String sInst = instFilter != null ? instFilter.getValue() : "Instructor";
                String sMember = memberFilter != null ? memberFilter.getValue() : "All Members";
                
                java.util.List<com.iwfc.model.FitnessSession> filtered = new java.util.ArrayList<>();
                for (com.iwfc.model.FitnessSession fs : daySessions) {
                    boolean matchType = sType.equals("Session Type") || fs.getSessionType().equalsIgnoreCase(sType);
                    boolean matchInst = sInst.equals("Instructor") || (fs.getInstructor() != null && fs.getInstructor().getName().contains(sInst));
                    boolean matchCap = true;
                    if (capacityFilter.equals("Available")) matchCap = true; // simplified check
                    if (capacityFilter.equals("Full")) matchCap = false; // simplified check
                    
                    boolean matchMember = sMember.equals("All Members");
                    if (!matchMember) {
                        java.util.List<com.iwfc.model.Booking> sessionBookings = facade.getBookingsForSession(fs.getSessionId());
                        for (com.iwfc.model.Booking b : sessionBookings) {
                            if (b.getMember().getName().equals(sMember)) {
                                matchMember = true;
                                break;
                            }
                        }
                    }
                    
                    if (matchType && matchInst && matchCap && matchMember) {
                        filtered.add(fs);
                    }
                }
                
                VBox dayCell = createDayCell(drawDate, inMonth, filtered);
                calendarGrid.add(dayCell, col, row);
                drawDate = drawDate.plusDays(1);
            }
        }
    }
    
    private VBox createDayCell(LocalDate date, boolean isCurrentMonth, java.util.List<com.iwfc.model.FitnessSession> sessions) {
        VBox cell = new VBox(5);
        cell.setPadding(new Insets(10));
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        boolean isToday = date.equals(today);
        
        String bg = isToday ? "rgba(15,23,42,0.8)" : "rgba(30,41,59,0.3)";
        String border = isToday ? "#06b6d4" : "rgba(255,255,255,0.05)";
        
        cell.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8px; -fx-border-color: " + border + "; -fx-border-radius: 8px; -fx-border-width: " + (isToday ? "2" : "1") + ";");
        
        Label dateLbl = new Label(String.valueOf(date.getDayOfMonth()));
        dateLbl.setStyle("-fx-text-fill: " + (isCurrentMonth ? (isToday ? "#06b6d4" : "white") : "#475569") + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        cell.getChildren().add(dateLbl);
        
        javafx.scene.layout.VBox sessionsContainer = new javafx.scene.layout.VBox(5);
        if (isCurrentMonth) {
            for (com.iwfc.model.FitnessSession fs : sessions) {
                String timeStr = fs.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a"));
                sessionsContainer.getChildren().add(createSessionMiniCard(timeStr, fs.getName(), fs.getColor(), fs));
            }
        }
        
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(sessionsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        
        javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        cell.getChildren().add(scrollPane);
        if (isCurrentMonth) {
            cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8px; -fx-border-color: " + border + "; -fx-border-radius: 8px; -fx-border-width: " + (isToday ? "2" : "1") + "; -fx-cursor: hand;"));
            cell.setOnMouseExited(e -> cell.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8px; -fx-border-color: " + border + "; -fx-border-radius: 8px; -fx-border-width: " + (isToday ? "2" : "1") + ";"));
            cell.setOnMouseClicked(e -> showDailyDetailedView(date, sessions));
        }

        return cell;
    }
    
    private void showDailyDetailedView(LocalDate date, java.util.List<com.iwfc.model.FitnessSession> sessions) {
        Dialog<Void> dialog = new Dialog<>();
        com.iwfc.ui.components.DialogUtils.stylePremiumDialog(dialog);
        
        String dateStr = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));
        dialog.setTitle("Daily Session Schedule");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(600);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 20, 20, 20));
        
        // Header
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        Label title = new Label("Daily Session Schedule");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label(dateStr);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        headerBox.getChildren().addAll(title, subtitle);
        
        // Timeline container
        VBox timeline = new VBox(20);
        timeline.setPadding(new Insets(10, 0, 10, 0));
        
        // Sort sessions by time
        java.util.List<com.iwfc.model.FitnessSession> sortedSessions = new java.util.ArrayList<>(sessions);
        sortedSessions.sort(java.util.Comparator.comparing(com.iwfc.model.FitnessSession::getStartTime));
        
        if (sortedSessions.isEmpty()) {
            Label noSessions = new Label("No sessions scheduled for today.");
            noSessions.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px;");
            timeline.getChildren().add(noSessions);
            timeline.setAlignment(Pos.CENTER);
        } else {
            for (com.iwfc.model.FitnessSession fs : sortedSessions) {
                HBox row = new HBox(20);
                row.setAlignment(Pos.TOP_LEFT);
                
                // Left: Time Capsule
                VBox timeCol = new VBox();
                timeCol.setPrefWidth(120);
                timeCol.setAlignment(Pos.TOP_RIGHT);
                String timeRange = fs.getStartTime().format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + 
                                   fs.getEndTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
                Label timeLbl = new Label(timeRange);
                timeLbl.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: " + fs.getColor() + "; -fx-padding: 6 12; -fx-background-radius: 20px; -fx-font-size: 12px; -fx-border-color: " + fs.getColor() + "33; -fx-border-radius: 20px;");
                timeCol.getChildren().add(timeLbl);
                
                // Middle: Timeline Node & Line
                VBox lineCol = new VBox();
                lineCol.setAlignment(Pos.TOP_CENTER);
                javafx.scene.shape.Circle node = new javafx.scene.shape.Circle(6);
                node.setFill(javafx.scene.paint.Color.web(fs.getColor()));
                node.setStyle("-fx-effect: dropshadow(gaussian, " + fs.getColor() + ", 8, 0.4, 0, 0);");
                
                Region vertLine = new Region();
                vertLine.setPrefWidth(2);
                VBox.setVgrow(vertLine, Priority.ALWAYS);
                vertLine.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
                
                lineCol.getChildren().addAll(node, vertLine);
                
                // Right: Session Card
                VBox cardCol = new VBox(10);
                HBox.setHgrow(cardCol, Priority.ALWAYS);
                cardCol.setPadding(new Insets(15));
                cardCol.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12px; -fx-border-color: " + fs.getColor() + "44; -fx-border-radius: 12px;");
                
                cardCol.setOnMouseEntered(e -> {
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), cardCol);
                    st.setToX(1.02);
                    st.setToY(1.02);
                    st.play();
                    cardCol.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 12px; -fx-border-color: " + fs.getColor() + "99; -fx-border-radius: 12px; -fx-effect: dropshadow(gaussian, " + fs.getColor() + "33, 15, 0, 0, 0);");
                });
                cardCol.setOnMouseExited(e -> {
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), cardCol);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                    cardCol.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12px; -fx-border-color: " + fs.getColor() + "44; -fx-border-radius: 12px;");
                });
                
                Label nameLbl = new Label(fs.getName());
                nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
                
                HBox infoRow = new HBox(15);
                infoRow.setAlignment(Pos.CENTER_LEFT);
                
                // Instructor
                HBox instBox = new HBox(6);
                instBox.setAlignment(Pos.CENTER_LEFT);
                javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(10);
                avatar.setFill(javafx.scene.paint.Color.web(fs.getColor()));
                Label instName = new Label(fs.getInstructor().getName());
                instName.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
                instBox.getChildren().addAll(avatar, instName);
                
                // Location
                HBox locBox = new HBox(4);
                locBox.setAlignment(Pos.CENTER_LEFT);
                Label locIcon = new Label("📍");
                locIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                Label locName = new Label(fs.getLocation().getDisplayName());
                locName.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
                locBox.getChildren().addAll(locIcon, locName);
                
                infoRow.getChildren().addAll(instBox, locBox);
                
                // Progress Bar
                VBox progBox = new VBox(5);
                // Fake booked spots based on hash for stable demo visuals
                int booked = Math.abs(fs.getId().hashCode()) % fs.getMaxCapacity();
                if (booked == 0) booked = fs.getMaxCapacity() / 2; // just in case
                
                ProgressBar pBar = new ProgressBar((double) booked / fs.getMaxCapacity());
                pBar.setMaxWidth(Double.MAX_VALUE);
                pBar.setStyle("-fx-accent: " + fs.getColor() + "; -fx-control-inner-background: rgba(255,255,255,0.05);");
                pBar.setPrefHeight(10);
                
                Label capacityLbl = new Label(booked + "/" + fs.getMaxCapacity() + " spots booked");
                capacityLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                
                progBox.getChildren().addAll(pBar, capacityLbl);
                
                cardCol.getChildren().addAll(nameLbl, infoRow, progBox);
                row.getChildren().addAll(timeCol, lineCol, cardCol);
                timeline.getChildren().add(row);
            }
        }
        
        javafx.scene.control.ScrollPane mainScroll = new javafx.scene.control.ScrollPane(timeline);
        mainScroll.setFitToWidth(true);
        mainScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        mainScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        
        content.getChildren().addAll(headerBox, mainScroll);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    private HBox createSessionMiniCard(String time, String title, String color, com.iwfc.model.FitnessSession session) {
        HBox card = new HBox(8);
        card.setPadding(new Insets(5, 8, 5, 8));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6px; -fx-cursor: hand;");
        
        Region indicator = new Region();
        indicator.setPrefSize(4, 20);
        indicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2px;");
        
        VBox text = new VBox(2);
        Label ti = new Label(time);
        ti.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        Label tr = new Label(title);
        tr.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        text.getChildren().addAll(ti, tr);
        
        card.getChildren().addAll(indicator, text);
        
        card.setOnMouseClicked(e -> {
            java.util.List<com.iwfc.model.Booking> bookings = facade.getBookingsForSession(session.getSessionId());
            StringBuilder bookedNames = new StringBuilder();
            if (bookings.isEmpty()) {
                bookedNames.append("None yet.");
            } else {
                for (com.iwfc.model.Booking b : bookings) {
                    bookedNames.append(b.getMember().getName()).append(", ");
                }
                bookedNames.setLength(bookedNames.length() - 2); // remove last comma
            }
            
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Session Details:\n\nTitle: " + title + "\nTime: " + time + "\nType: " + session.getSessionType() + "\nInstructor: " + (session.getInstructor() != null ? session.getInstructor().getName() : "TBD") + "\n\nBooked Members: " + bookedNames.toString());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        
        return card;
    }
    
    private HBox createLowerControlBar() {
        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 0, 0, 0));
        
        // Legend
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
            createLegendDot("Yoga", "#3b82f6"),
            createLegendDot("HIIT", "#ef4444"),
            createLegendDot("Pilates", "#a855f7"),
            createLegendDot("Cross-training", "#f59e0b"),
            createLegendDot("Spinning", "#06b6d4"),
            createLegendDot("Recovery", "#10b981")
        );
        Label legText = new Label(" |  Color coding indicates session type.");
        legText.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        legend.getChildren().add(legText);
        
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        
        // Dropdowns
        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("Session Type", "Yoga", "HIIT", "Pilates", "Spin", "Crossfit");
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 6px;");
        typeFilter.setOnAction(e -> populateCalendar());
        
        instFilter = new ComboBox<>();
        instFilter.getItems().add("Instructor");
        java.util.Set<String> uniqueInsts = new java.util.HashSet<>();
        facade.getAllSessions().forEach(s -> {
            if (s.getInstructor() != null) uniqueInsts.add(s.getInstructor().getName());
        });
        instFilter.getItems().addAll(uniqueInsts);
        instFilter.getSelectionModel().selectFirst();
        instFilter.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 6px;");
        instFilter.setOnAction(e -> populateCalendar());
        
        memberFilter = new ComboBox<>();
        memberFilter.getItems().add("All Members");
        java.util.Set<String> uniqueMembers = new java.util.HashSet<>();
        for (com.iwfc.model.Booking b : facade.getAllBookings()) {
            uniqueMembers.add(b.getMember().getName());
        }
        memberFilter.getItems().addAll(uniqueMembers);
        memberFilter.getSelectionModel().selectFirst();
        memberFilter.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 6px;");
        memberFilter.setOnAction(e -> populateCalendar());
        
        // Capacity toggles
        HBox cap = new HBox(0);
        cap.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6px;");
        String[] caps = {"All", "Available", "Waitlist", "Full"};
        for(String c : caps) {
            Button b = new Button(c);
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
            if(c.equals("All")) b.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
            b.setOnAction(e -> {
                for (javafx.scene.Node n : cap.getChildren()) {
                    if (n instanceof Button) {
                        n.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
                    }
                }
                b.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
                populateCalendar();
            });
            cap.getChildren().add(b);
        }
        
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        
        Label timestamp = new Label("Refreshed: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")));
        timestamp.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        
        bar.getChildren().addAll(legend, sp1, memberFilter, typeFilter, instFilter, cap, sp2, timestamp);
        return bar;
    }
    
    private HBox createLegendDot(String text, String color) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Circle c = new Circle(4, Color.web(color));
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        box.getChildren().addAll(c, l);
        return box;
    }
    
    private VBox createRightPanel() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(300);
        
        // Action Panel
        VBox action = new VBox(10);
        Label aTitle = new Label("Action Panel");
        aTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Button add = new Button("+ Add New Session");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setStyle("-fx-background-color: rgba(6, 182, 212, 0.2); -fx-text-fill: #06b6d4; -fx-border-color: #06b6d4; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        add.setOnAction(e -> {
            try {
                com.iwfc.model.FitnessSession newSession = new com.iwfc.model.FitnessSession(
                    "SESS-" + System.currentTimeMillis(),
                    "Custom Group Training",
                    "HIIT",
                    null, // Instructor
                    today.getDayOfWeek(),
                    java.time.LocalTime.of(17, 30),
                    java.time.LocalTime.of(18, 30),
                    null, // Location
                    20
                );
                facade.createSession(newSession);
                populateCalendar();
                
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Successfully scheduled 'Custom Group Training' for every " + today.getDayOfWeek() + " at 5:30 PM.");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            } catch (com.iwfc.exception.InvalidBookingException | com.iwfc.exception.UnauthorizedAccessException ex) {
                System.err.println("Error scheduling session: " + ex.getMessage());
            }
        });
        action.getChildren().addAll(aTitle, add);
        
        // Personal Goals
        VBox goals = createGlassCard("Personal Goals", "View Goals");
        Label date = new Label("Selected Day: Tuesday, Sep 29");
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        
        StackPane ring = new StackPane();
        Circle bgRing = new Circle(40, Color.TRANSPARENT);
        bgRing.setStroke(Color.web("rgba(255,255,255,0.1)"));
        bgRing.setStrokeWidth(6);
        Circle fgRing = new Circle(40, Color.TRANSPARENT);
        fgRing.setStroke(Color.web("#10b981"));
        fgRing.setStrokeWidth(6);
        fgRing.getStrokeDashArray().addAll(200d, 250d); // simple visual hack for 5/5
        VBox txt = new VBox();
        txt.setAlignment(Pos.CENTER);
        Label v1 = new Label("5/5");
        v1.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label v2 = new Label("Classes\nComplete");
        v2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        v2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        txt.getChildren().addAll(v1, v2);
        ring.getChildren().addAll(bgRing, fgRing, txt);
        
        VBox chal = new VBox(2);
        chal.setAlignment(Pos.CENTER);
        Label c1 = new Label("Weekly Challenge:");
        c1.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label c2 = new Label("Complete 1 Recovery Session");
        c2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        chal.getChildren().addAll(c1, c2);
        
        goals.getChildren().addAll(date, ring, chal);
        goals.setAlignment(Pos.TOP_CENTER);
        
        // Instructor
        VBox instPanel = createGlassCard("Instructor of the Week", null);
        HBox iRow = new HBox(12);
        iRow.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iPic = new StackPane();
        Circle iBg = new Circle(20, Color.web("#f59e0b"));
        Label iL = new Label("JS");
        String iNameStr = "Jam Siaran";
        String iBioStr = "Instructor Avatar\nInroiwoutlan";
        
        java.util.List<com.iwfc.model.FitnessSession> allSess = facade.getAllSessions();
        if (!allSess.isEmpty()) {
            com.iwfc.model.Instructor best = null;
            for (com.iwfc.model.FitnessSession s : allSess) {
                if (s.getInstructor() != null) {
                    if (best == null || s.getInstructor().getPerformanceScore() > best.getPerformanceScore()) {
                        best = s.getInstructor();
                    }
                }
            }
            if (best != null) {
                iNameStr = best.getName();
                iBioStr = "Specialty: " + best.getSpecialization() + "\nScore: " + best.getPerformanceScore();
                String[] p = best.getName().split(" ");
                if (p.length > 1 && p[1].length() > 0) {
                    iL.setText((p[0].substring(0,1) + p[1].substring(0,1)).toUpperCase());
                } else if (best.getName().length() >= 2) {
                    iL.setText(best.getName().substring(0,2).toUpperCase());
                }
            }
        }
        
        iL.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        iPic.getChildren().addAll(iBg, iL);
        
        VBox iTxt = new VBox(2);
        Label iName = new Label(iNameStr);
        iName.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label iBio = new Label(iBioStr);
        iBio.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        iTxt.getChildren().addAll(iName, iBio);
        iRow.getChildren().addAll(iPic, iTxt);
        instPanel.getChildren().add(iRow);
        
        // Community
        VBox comm = createGlassCard("Community Activity", null);
        java.util.List<com.iwfc.model.Booking> allBookings = facade.getAllBookings();
        int count = 0;
        String[] colors = {"#3b82f6", "#a855f7", "#10b981"};
        for (com.iwfc.model.Booking b : allBookings) {
            if (count >= 3) break;
            com.iwfc.model.Member m = b.getMember();
            String init = m.getName().length() >= 2 ? m.getName().substring(0, 2).toUpperCase() : "M";
            if (m.getName().contains(" ")) {
                String[] p = m.getName().split(" ");
                if(p.length > 1 && p[1].length() > 0) init = (p[0].substring(0,1) + p[1].substring(0,1)).toUpperCase();
            }
            comm.getChildren().add(createCommItem(colors[count % colors.length], init, m.getName(), "booked " + b.getSession().getName()));
            count++;
        }
        if (count == 0) {
            Label noAct = new Label("No recent activity.");
            noAct.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            comm.getChildren().add(noAct);
        }
        
        Label foot = new Label("Facility map overview");
        foot.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        panel.getChildren().addAll(action, goals, instPanel, comm, foot);
        return panel;
    }
    
    private VBox createGlassCard(String title, String link) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(30,41,59,0.5); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px;");
        
        HBox head = new HBox();
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        head.getChildren().addAll(t, sp);
        
        if (link != null) {
            Label l = new Label(link);
            l.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-cursor: hand;");
            l.setOnMouseClicked(e -> {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Navigating to " + link);
                DialogUtils.stylePremiumDialog(a);
                a.show();
            });
            head.getChildren().add(l);
        }
        
        card.getChildren().add(head);
        return card;
    }
    
    private HBox createCommItem(String color, String initials, String name, String action) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        
        StackPane pic = new StackPane();
        Circle bg = new Circle(14, Color.web(color));
        Label init = new Label(initials);
        init.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        pic.getChildren().addAll(bg, init);
        
        VBox txt = new VBox(2);
        Label n = new Label(name);
        n.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label a = new Label(action);
        a.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        txt.getChildren().addAll(n, a);
        row.getChildren().addAll(pic, txt);
        return row;
    }
}

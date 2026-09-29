package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.FollowUp;
import com.iwfc.model.FollowUpType;
import com.iwfc.model.Role;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.iwfc.ui.components.DialogUtils;
import javafx.scene.control.Alert;

public class FollowUpView extends VBox {

    private final IWFCSystemFacade facade;
    private final VBox listContainer;
    private final GridPane overviewGrid;
    private final Label titleCount;

    public FollowUpView(IWFCSystemFacade facade) {
        this.facade = facade;
        
        getStyleClass().add("followup-view");
        setPadding(new Insets(25));
        setSpacing(20);
        // Premium textured obsidian and sapphire background
        setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 120%, #171626, #090812);");

        List<FollowUp> myFollowUps = facade.getFollowUps();

        // --- TOP HEADER: Title, Search, Bell ---
        HBox topHeader = new HBox(15);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label titleMain = new Label("Follow-up Pipeline ");
        titleMain.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        titleCount = new Label("(" + myFollowUps.size() + " Active)");
        titleCount.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
        Label titleSub = new Label(" PIPELINE OVERVIEW");
        titleSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-letter-spacing: 1px;");
        
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        
        javafx.scene.control.TextField searchF = new javafx.scene.control.TextField();
        searchF.setPromptText("Search");
        searchF.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 6 15 6 35;");
        searchF.setPrefWidth(220);
        
        Button bellBtn = new Button("🔔");
        bellBtn.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 10; -fx-cursor: hand;");
        
        Button newLeadBtn = new Button("+ New Follow-up");
        newLeadBtn.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #6366f1); -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 15; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.5), 10, 0, 0, 2);");
        newLeadBtn.setOnAction(e -> simulateIncomingFollowUp());
        
        topHeader.getChildren().addAll(titleMain, titleCount, titleSub, spacerHeader, searchF, newLeadBtn, bellBtn);

        // --- TABLE CARD ---
        VBox tableCard = new VBox(15);
        tableCard.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-background-radius: 12px;");
        tableCard.setPadding(new Insets(20));
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        // Filter Bar (Chips & Date Range)
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        
        Label typeLbl = new Label("Follow-up Type");
        typeLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        
        filterBar.getChildren().add(typeLbl);
        
        String[] filters = {"All", "Trial", "Anniversary", "Enquiry", "Feedback", "Retention"};
        for (String f : filters) {
            Button chip = new Button(f);
            if (f.equals("All")) {
                chip.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12;");
            } else {
                chip.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12; -fx-cursor: hand;");
            }
            filterBar.getChildren().add(chip);
        }
        
        Button customChip = new Button("Custom Multi-select...");
        customChip.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-border-color: rgba(255,255,255,0.05); -fx-border-style: dashed; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12; -fx-cursor: hand;");
        filterBar.getChildren().add(customChip);
        
        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        
        Label dateLbl = new Label("Date Range");
        dateLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        
        Button dToday = new Button("Today");
        dToday.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12;");
        Button dWeek = new Button("This Week");
        dWeek.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12; -fx-cursor: hand;");
        Button dMonth = new Button("This Month");
        dMonth.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 12; -fx-cursor: hand;");
        
        filterBar.getChildren().addAll(filterSpacer, dateLbl, dToday, dWeek, dMonth);

        // Column Headers
        HBox columnHeaders = new HBox();
        columnHeaders.setPadding(new Insets(10, 0, 10, 0));
        columnHeaders.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent;");
        
        Label hPrio = createColHeader("PRIORITY", 100);
        Label hName = createColHeader("Name", 200);
        Label hType = createColHeader("Type", 120);
        Label hSched = createColHeader("Scheduled", 120);
        Label hInterest = createColHeader("INTEREST LEVEL & ACTIONS", 280);
        Label hComm = createColHeader("Comments", 250);
        Label hQuick = createColHeader("QUICK ACTIONS", 150);
        
        columnHeaders.getChildren().addAll(hPrio, hName, hType, hSched, hInterest, hComm, hQuick);

        // List Container
        listContainer = new VBox(5);
        
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Pagination & Rows Per Page
        HBox paginationBar = new HBox(10);
        paginationBar.setAlignment(Pos.CENTER_LEFT);
        paginationBar.setPadding(new Insets(15, 0, 0, 0));
        
        for (int i = 1; i <= 4; i++) {
            Button pageBtn = new Button(String.valueOf(i));
            if (i == 1) {
                pageBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #ef4444; -fx-border-color: rgba(239, 68, 68, 0.4); -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-min-width: 32px;");
            } else {
                pageBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-min-width: 32px; -fx-cursor: hand;");
            }
            paginationBar.getChildren().add(pageBtn);
        }

        Region pageSpacer = new Region();
        HBox.setHgrow(pageSpacer, Priority.ALWAYS);

        Label rowsLabel = new Label("Rows per page");
        rowsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        
        Button rowsBtn = new Button("6  ▼");
        rowsBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 4px; -fx-background-radius: 4px;");

        paginationBar.getChildren().addAll(pageSpacer, rowsLabel, rowsBtn);

        tableCard.getChildren().addAll(filterBar, columnHeaders, scrollPane, paginationBar);

        // --- OVERVIEW SECTION ---
        VBox overviewSection = new VBox(15);
        Label overviewTitle = new Label("Overview");
        overviewTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold;");

        overviewGrid = new GridPane();
        overviewGrid.setHgap(15);
        overviewGrid.setVgap(15);
        
        refreshOverviewCards();
        
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(33.33);
        overviewGrid.getColumnConstraints().addAll(cc, cc, cc);

        overviewSection.getChildren().addAll(overviewTitle, overviewGrid);

        getChildren().addAll(topHeader, tableCard, overviewSection);
        
        renderList();
    }
    
    private void refreshOverviewCards() {
        overviewGrid.getChildren().clear();
        List<FollowUp> myFollowUps = facade.getFollowUps();
        long activeMembers = myFollowUps.stream().filter(f -> f.getType() == FollowUpType.MEMBERSHIP_RENEWAL).count();
        long activeTrials = myFollowUps.stream().filter(f -> f.getType() == FollowUpType.TRIAL).count();
        long newEnquiries = myFollowUps.stream().filter(f -> f.getType() == FollowUpType.ENQUIRY).count();
        long hotPipeline = myFollowUps.stream().filter(f -> f.getConvertibility() == com.iwfc.model.Convertibility.HOT).count();

        // Cards (Blue, Orange, Green, Yellow, Teal, Purple)
        overviewGrid.add(createComplexOverviewCard("MEMBERS", String.valueOf(activeMembers > 0 ? activeMembers : 250), "Total", "Active", "Upcoming", "#3b82f6", true), 0, 0);
        overviewGrid.add(createComplexOverviewCard("ATTENDANCE", "92%", "Avg", "Present", "Absent", "#f59e0b", false), 1, 0);
        overviewGrid.add(createComplexOverviewCard("ENQUIRIES", String.valueOf(newEnquiries), "New", "Sale Enquiries", "Lead Qualified Rate", "#10b981", true), 2, 0);
        
        overviewGrid.add(createComplexOverviewCard("PIPELINE", String.valueOf(hotPipeline), "Active", "Trial", "Enquiry", "#eab308", true), 0, 1);
        overviewGrid.add(createComplexOverviewCard("TRIALS", String.valueOf(activeTrials), "Active", "Conversion Rate", "Expiration", "#06b6d4", false), 1, 1);
        overviewGrid.add(createComplexOverviewCard("MEMBERSHIPS", "$10.5K", "Rev", "New Sales", "Renewals", "#a855f7", false), 2, 1);

    }

    private VBox createComplexOverviewCard(String title, String bigNum, String subNum, String l1, String l2, String accentColor, boolean isLine) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.05), rgba(" + hexToRgb(accentColor) + ", 0.05)); -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 15;");
        
        // Top Row: Title
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        
        // Middle Row: Values + Visuals
        HBox midRow = new HBox(15);
        midRow.setAlignment(Pos.CENTER_LEFT);
        
        HBox numBox = new HBox(5);
        numBox.setAlignment(Pos.BASELINE_LEFT);
        Label bNum = new Label(bigNum);
        bNum.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label sNum = new Label(subNum);
        sNum.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
        numBox.getChildren().addAll(bNum, sNum);
        
        Region midSpc = new Region();
        HBox.setHgrow(midSpc, Priority.ALWAYS);
        
        // Chart Visualizer
        StackPane chartPane = new StackPane();
        if (isLine) {
            javafx.scene.shape.Polyline line = new javafx.scene.shape.Polyline(0,20, 20,25, 40,10, 60,30, 80,15, 100,20);
            line.setStroke(javafx.scene.paint.Color.web(accentColor));
            line.setStrokeWidth(2);
            line.setEffect(new javafx.scene.effect.DropShadow(10, javafx.scene.paint.Color.web(accentColor)));
            chartPane.getChildren().add(line);
        } else {
            // Smooth curve
            javafx.scene.shape.Path path = new javafx.scene.shape.Path();
            path.getElements().add(new javafx.scene.shape.MoveTo(0, 30));
            path.getElements().add(new javafx.scene.shape.QuadCurveTo(25, 5, 50, 25));
            path.getElements().add(new javafx.scene.shape.QuadCurveTo(75, 40, 100, 15));
            path.setStroke(javafx.scene.paint.Color.web(accentColor));
            path.setStrokeWidth(2);
            path.setEffect(new javafx.scene.effect.DropShadow(10, javafx.scene.paint.Color.web(accentColor)));
            chartPane.getChildren().add(path);
        }
        
        // Mini Pie Chart
        StackPane pieChart = new StackPane();
        pieChart.setPrefSize(30, 30);
        javafx.scene.shape.Arc a1 = new javafx.scene.shape.Arc(15, 15, 12, 12, 90, 220);
        a1.setType(javafx.scene.shape.ArcType.ROUND);
        a1.setFill(javafx.scene.paint.Color.web(accentColor));
        javafx.scene.shape.Arc a2 = new javafx.scene.shape.Arc(15, 15, 12, 12, 310, 140);
        a2.setType(javafx.scene.shape.ArcType.ROUND);
        a2.setFill(javafx.scene.paint.Color.web(accentColor).darker());
        javafx.scene.shape.Circle cInner = new javafx.scene.shape.Circle(15, 15, 6, javafx.scene.paint.Color.web("#171626"));
        pieChart.getChildren().addAll(a1, a2, cInner);
        
        midRow.getChildren().addAll(numBox, midSpc, chartPane, pieChart);
        
        // Bottom Row: Legend
        HBox legendRow = new HBox(15);
        legendRow.setAlignment(Pos.CENTER_LEFT);
        
        HBox l1Box = new HBox(5);
        l1Box.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle c1 = new javafx.scene.shape.Circle(3, javafx.scene.paint.Color.web(accentColor));
        Label l1Lbl = new Label(l1);
        l1Lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        l1Box.getChildren().addAll(c1, l1Lbl);
        
        HBox l2Box = new HBox(5);
        l2Box.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle c2 = new javafx.scene.shape.Circle(3, javafx.scene.paint.Color.web("#fbbf24"));
        Label l2Lbl = new Label(l2);
        l2Lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        l2Box.getChildren().addAll(c2, l2Lbl);
        
        legendRow.getChildren().addAll(l1Box, l2Box);
        
        card.getChildren().addAll(titleLbl, midRow, legendRow);
        
        // Interaction
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.08), rgba(" + hexToRgb(accentColor) + ", 0.15)); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 15;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.05), rgba(" + hexToRgb(accentColor) + ", 0.05)); -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 15;");
        });
        
        return card;
    }
    
    private String hexToRgb(String hex) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        return (int)(c.getRed()*255) + "," + (int)(c.getGreen()*255) + "," + (int)(c.getBlue()*255);
    }

    private Label createColHeader(String text, double width) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        label.setPrefWidth(width);
        return label;
    }

    private void renderList() {
        listContainer.getChildren().clear();
        
        List<FollowUp> followUps = facade.getFollowUps();
        Role currentUserRole = facade.getCurrentUser().getRole();
        
        if (currentUserRole == Role.MEMBER) {
            followUps = followUps.stream()
                .filter(f -> f.getType() == FollowUpType.BIRTHDAY || 
                             f.getType() == FollowUpType.ANNIVERSARY || 
                             f.getType() == FollowUpType.PARTY)
                .collect(Collectors.toList());
        }
        
        int idx = 1;
        for (FollowUp f : followUps) {
            listContainer.getChildren().add(createRow(f, idx++));
        }
    }

    private HBox createRow(FollowUp f, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 0, 15, 0));
        row.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent;");
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent;"));

        // 1. PRIORITY (width 100)
        HBox prioCol = new HBox(8);
        prioCol.setAlignment(Pos.CENTER_LEFT);
        prioCol.setPrefWidth(100);
        
        String pColor = index % 3 == 1 ? "#ef4444" : (index % 3 == 2 ? "#eab308" : "#22c55e");
        String pText = index % 3 == 1 ? "High" : (index % 3 == 2 ? "Med " : "Low ");
        javafx.scene.shape.Circle pDot = new javafx.scene.shape.Circle(4, javafx.scene.paint.Color.web(pColor));
        Label pLbl = new Label(pText);
        pLbl.setStyle("-fx-text-fill: " + pColor + "; -fx-font-size: 13px;");
        Label pNum = new Label(String.valueOf(index));
        pNum.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        
        prioCol.getChildren().addAll(pDot, pLbl, pNum);

        // 2. Name (width 200)
        HBox nameCol = new HBox(12);
        nameCol.setAlignment(Pos.CENTER_LEFT);
        nameCol.setPrefWidth(200);
        
        StackPane avatarPane = new StackPane();
        javafx.scene.shape.Circle avBg = new javafx.scene.shape.Circle(16, javafx.scene.paint.Color.web("rgba(255,255,255,0.1)"));
        Label avTxt = new Label(f.getName().substring(0, 1).toUpperCase());
        avTxt.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().addAll(avBg, avTxt);
        
        VBox nameInfo = new VBox(2);
        Label nameLbl = new Label(f.getName());
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label phoneLbl = new Label(f.getPhoneNumber());
        phoneLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        nameInfo.getChildren().addAll(nameLbl, phoneLbl);
        
        nameCol.getChildren().addAll(avatarPane, nameInfo);

        // 3. Type (width 120)
        HBox typeCol = new HBox(6);
        typeCol.setAlignment(Pos.CENTER_LEFT);
        typeCol.setPrefWidth(120);
        
        Label tIcon = new Label(f.getType() == FollowUpType.TRIAL ? "⏱" : (f.getType() == FollowUpType.ANNIVERSARY ? "🎁" : (f.getType() == FollowUpType.ENQUIRY ? "❓" : "💬")));
        tIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        Label tLbl = new Label(f.getType().getDisplayName());
        tLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        
        typeCol.getChildren().addAll(tIcon, tLbl);

        // 4. Scheduled (width 120)
        HBox schedCol = new HBox();
        schedCol.setAlignment(Pos.CENTER_LEFT);
        schedCol.setPrefWidth(120);
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label dateLbl = new Label(f.getScheduledDate().format(dateFormatter));
        dateLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        schedCol.getChildren().add(dateLbl);

        // 5. INTEREST LEVEL & ACTIONS (width 280)
        HBox intCol = new HBox(15);
        intCol.setAlignment(Pos.CENTER_LEFT);
        intCol.setPrefWidth(280);
        
        VBox heatBox = new VBox(2);
        heatBox.setAlignment(Pos.CENTER_LEFT);
        HBox bars = new HBox(2);
        
        int hotness = 4 - (index % 4); // Just for demo variety
        String[] colors = {"#ef4444", "#f59e0b", "#10b981", "#3b82f6"};
        for (int i=0; i<4; i++) {
            Region b = new Region();
            b.setPrefSize(12, 6);
            if (i < hotness) {
                b.setStyle("-fx-background-color: " + colors[i] + "; -fx-background-radius: 2px;");
            } else {
                b.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 2px;");
            }
            bars.getChildren().add(b);
        }
        
        String heatText = hotness >= 3 ? "Hot" : (hotness == 2 ? "Warm" : "Cold");
        String heatColor = hotness >= 3 ? "#ef4444" : (hotness == 2 ? "#f59e0b" : "#3b82f6");
        Label hLbl = new Label(heatText);
        hLbl.setStyle("-fx-text-fill: " + heatColor + "; -fx-font-size: 11px;");
        heatBox.getChildren().addAll(bars, hLbl);
        
        String actType = hotness >= 3 ? "Move to Nurture" : (hotness == 2 ? "Book Appointment" : "Create Task");
        Button actBtn = new Button(actType);
        actBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #cbd5e1; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand;");
        
        actBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, actType + " action triggered for " + f.getName());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        
        intCol.getChildren().addAll(heatBox, actBtn);

        // 6. Comments (width 250)
        HBox commCol = new HBox();
        commCol.setAlignment(Pos.CENTER_LEFT);
        commCol.setPrefWidth(250);
        
        String txt = f.getComments() == null || f.getComments().isEmpty() ? "Call Me, about membership options and next steps..." : f.getComments();
        Label commentsLbl = new Label(txt);
        commentsLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        commentsLbl.setMaxWidth(230);
        // We could use setWrapText, but single line truncated looks cleaner for table
        
        commCol.getChildren().add(commentsLbl);
        
        // 7. Quick Actions (width 150)
        HBox quickCol = new HBox(8);
        quickCol.setAlignment(Pos.CENTER_LEFT);
        quickCol.setPrefWidth(150);
        
        Button btnCall = new Button("📞");
        btnCall.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnCall.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Initiating call to " + f.getName() + " at " + f.getPhoneNumber());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        
        Button btnMsg = new Button("💬");
        btnMsg.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #94a3b8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnMsg.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Opening chat with " + f.getName());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        
        Button btnOpt = new Button("⋮");
        btnOpt.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-padding: 4 8; -fx-cursor: hand;");
        btnOpt.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "More options for " + f.getName());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });
        
        quickCol.getChildren().addAll(btnCall, btnMsg, btnOpt);

        row.getChildren().addAll(prioCol, nameCol, typeCol, schedCol, intCol, commCol, quickCol);
        return row;
    }
    private void simulateIncomingFollowUp() {
        String[] names = {"Elena Gilbert", "Tony Stark", "Bruce Wayne", "Clark Kent", "Peter Parker", "Wanda Maximoff", "Natasha Romanoff"};
        String name = names[new java.util.Random().nextInt(names.length)];
        FollowUpType[] types = FollowUpType.values();
        FollowUpType type = types[new java.util.Random().nextInt(types.length)];
        
        int hotness = new java.util.Random().nextInt(3);
        com.iwfc.model.Convertibility conv = switch (hotness) {
            case 0 -> com.iwfc.model.Convertibility.HOT;
            case 1 -> com.iwfc.model.Convertibility.WARM;
            default -> com.iwfc.model.Convertibility.COLD;
        };

        FollowUp f = new FollowUp(name, "555-010" + new java.util.Random().nextInt(9), type, java.time.LocalDateTime.now().plusDays(new java.util.Random().nextInt(10)), conv, "Simulated incoming lead via premium UI");
        
        try {
            com.iwfc.pattern.DataStore.getInstance().getFollowUps().save(f);
        } catch (com.iwfc.exception.DuplicateDataException ignored) {}
        
        titleCount.setText("(" + facade.getFollowUps().size() + " Active)");
        refreshOverviewCards();
        renderList();
        
        Alert a = new Alert(Alert.AlertType.INFORMATION, "⚡ Incoming Lead Detected:\n" + name + " has entered the " + type.getDisplayName() + " pipeline.");
        a.setHeaderText("New Follow-up Automatically Assigned");
        DialogUtils.stylePremiumDialog(a);
        a.show();
    }
}

package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminReportsView extends HBox {

    private final IWFCSystemFacade facade;
    private final StackPane contentArea;
    private final VBox sidebar;
    private Button activeTabBtn;

    public AdminReportsView(IWFCSystemFacade facade) {
        this.facade = facade;
        getStyleClass().add("admin-reports-view");
        
        // Setup layout
        contentArea = new StackPane();
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        contentArea.setPadding(new Insets(20));
        
        sidebar = createSidebar();

        getChildren().addAll(sidebar, contentArea);
    }

    private VBox createSidebar() {
        VBox nav = new VBox(5);
        nav.setPrefWidth(220);
        nav.setPadding(new Insets(20, 10, 20, 10));
        nav.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 1 0 0;");

        Label title = new Label("📊 Reports");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        nav.getChildren().addAll(title, new Separator());
        VBox.setMargin(title, new Insets(0, 0, 15, 5));

        String[] tabs = {
            "Balance Due Report",
            "Sales Report",
            "Expired Member Report",
            "Member Report Card",
            "Due Membership Report",
            "SMS Report",
            "Attendance Report",
            "PT Report"
        };

        for (String tabName : tabs) {
            Button btn = new Button("○  " + tabName);
            btn.getStyleClass().add("nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 10 15; -fx-cursor: hand;");
            
            btn.setOnAction(e -> {
                if (activeTabBtn != null) {
                    activeTabBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 10 15; -fx-cursor: hand;");
                }
                btn.setStyle("-fx-background-color: rgba(59, 130, 246, 0.1); -fx-text-fill: #3b82f6; -fx-padding: 10 15; -fx-font-weight: bold; -fx-cursor: hand;");
                activeTabBtn = btn;
                loadTab(tabName);
            });
            
            nav.getChildren().add(btn);
            
            if (tabName.equals("Sales Report")) {
                btn.fire(); // Select by default
            }
        }
        
        return nav;
    }

    private void loadTab(String tabName) {
        contentArea.getChildren().clear();
        Node content;
        
        try {
            content = switch (tabName) {
                case "Sales Report" -> createSalesReport();
                case "Balance Due Report" -> createBalanceDueReport();
                case "Expired Member Report" -> createExpiredMemberReport();
                case "Member Report Card" -> createMemberReportCard();
                case "Due Membership Report" -> createDueMembershipReport();
                case "SMS Report" -> createSMSReport();
                case "Attendance Report" -> createAttendanceReport();
                case "PT Report" -> createPTReport();
                default -> new Label("Tab under construction.");
            };
        } catch (Exception ex) {
            content = new Label("Error loading report: " + ex.getMessage());
        }

        // Add fade animation
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), content);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        
        contentArea.getChildren().add(content);
    }
    
    // =========================================================================
    // UI COMPONENTS
    // =========================================================================

    private VBox createReportStatCard(String title, String value, String bgStyle) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setPrefSize(180, 80);
        card.setStyle(bgStyle + " -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);");
        
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");
        
        card.getChildren().addAll(valLbl, titleLbl);
        return card;
    }
    
    private HBox createFilterBar(String searchPrompt, javafx.event.EventHandler<javafx.event.ActionEvent> onExportClick) {
        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 0, 10, 0));
        
        ComboBox<String> timeFilter = new ComboBox<>();
        timeFilter.getItems().addAll("Last 6 months", "Last Year", "All Time");
        timeFilter.setValue("Last 6 months");
        timeFilter.setStyle("-fx-background-color: transparent; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: white;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 " + searchPrompt);
        searchField.setStyle("-fx-background-color: transparent; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: white; -fx-pref-width: 250;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button exportBtn = new Button("📥 Generate XLS/CSV Report");
        exportBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 6; -fx-cursor: hand;");
        exportBtn.setOnAction(onExportClick);
        
        bar.getChildren().addAll(timeFilter, searchField, spacer, exportBtn);
        return bar;
    }
    
    private <T> TableView<T> createDarkTable() {
        TableView<T> table = new TableView<>();
        table.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-radius: 8;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private <T> void exportToCsv(TableView<T> table, String filenamePrefix) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName(filenamePrefix + "_" + LocalDate.now() + ".csv");
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Headers
                String headers = table.getColumns().stream()
                        .map(TableColumnBase::getText)
                        .collect(Collectors.joining(","));
                writer.println(headers);
                
                // Data
                for (T item : table.getItems()) {
                    String row = table.getColumns().stream()
                            .map(col -> {
                                TableColumn<T, ?> typedCol = (TableColumn<T, ?>) col;
                                Object cellData = typedCol.getCellData(item);
                                String str = cellData == null ? "" : cellData.toString();
                                // escape commas
                                if (str.contains(",")) str = "\"" + str + "\"";
                                return str;
                            })
                            .collect(Collectors.joining(","));
                    writer.println(row);
                }
            } catch (Exception e) {
                System.err.println("Export failed: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 1. SALES REPORT
    // =========================================================================
    private VBox createSalesReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("Sales Report");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        List<Payment> payments = null;
        try {
            payments = facade.getAllPayments();
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        
        double totalAmt = 0;
        double paidAmt = 0;
        int cashCnt = 0;
        int cardCnt = 0;
        
        if (payments != null) {
            for(Payment p : payments) {
                totalAmt += p.getTotalCharged();
                if(p.getStatus() == PaymentStatus.COMPLETED) {
                    paidAmt += p.getTotalCharged();
                    if(p.getMethod() == PaymentMethod.CASH) cashCnt++;
                    else cardCnt++;
                }
            }
        }
        
        String bgCard = "-fx-background-color: #334155;";
        
        HBox row1 = new HBox(15);
        row1.getChildren().addAll(
            createReportStatCard("Invoice Generated", String.valueOf(payments == null ? 0 : payments.size()), bgCard),
            createReportStatCard("Total Amount", String.format("%.2f", totalAmt), bgCard),
            createReportStatCard("Paid Amount", String.format("%.2f", paidAmt), bgCard),
            createReportStatCard("Balance Amount", String.format("%.2f", totalAmt - paidAmt), bgCard),
            createReportStatCard("Tax Amount", "0.00", bgCard)
        );
        
        HBox row2 = new HBox(15);
        row2.getChildren().addAll(
            createReportStatCard("Online", String.valueOf(cardCnt), bgCard),
            createReportStatCard("Wallet", "0", bgCard),
            createReportStatCard("Cash", String.valueOf(cashCnt), bgCard),
            createReportStatCard("Other", "0", bgCard)
        );
        
        TableView<Payment> table = createDarkTable();
        TableColumn<Payment, String> colId = new TableColumn<>("Payment ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentId()));
        
        TableColumn<Payment, String> colMem = new TableColumn<>("Member ID");
        colMem.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMemberId()));
        
        TableColumn<Payment, String> colAmt = new TableColumn<>("Total Amount");
        colAmt.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getTotalCharged())));
        
        TableColumn<Payment, String> colMethod = new TableColumn<>("Method");
        colMethod.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMethod().getDisplayName()));
        
        TableColumn<Payment, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        
        TableColumn<Payment, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        table.getColumns().addAll(List.of(colId, colMem, colAmt, colMethod, colStatus, colDate));
        
        if (payments != null) {
            table.setItems(FXCollections.observableArrayList(payments));
        }

        HBox filterBar = createFilterBar("Search sales...", e -> exportToCsv(table, "Sales_Report"));
        
        container.getChildren().addAll(title, row1, row2, filterBar, table);
        return container;
    }

    // =========================================================================
    // 2. BALANCE DUE REPORT
    // =========================================================================
    private VBox createBalanceDueReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("Balance Due Report");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        List<Payment> duePayments = null;
        try {
            duePayments = facade.getAllPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.FAILED)
                .collect(Collectors.toList());
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        
        double totalDue = duePayments == null ? 0 : duePayments.stream().mapToDouble(Payment::getTotalCharged).sum();
        
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
            createReportStatCard("Total Due", String.format("%.2f", totalDue), "-fx-background-color: #991b1b;"), // Red
            createReportStatCard("Overdue Invoices", String.valueOf(duePayments == null ? 0 : duePayments.size()), "-fx-background-color: #ea580c;") // Orange
        );
        
        TableView<Payment> table = createDarkTable();
        TableColumn<Payment, String> colId = new TableColumn<>("Payment ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentId()));
        
        TableColumn<Payment, String> colMem = new TableColumn<>("Member ID");
        colMem.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMemberId()));
        
        TableColumn<Payment, String> colAmt = new TableColumn<>("Amount Due");
        colAmt.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getTotalCharged())));
        
        TableColumn<Payment, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));

        table.getColumns().addAll(List.of(colId, colMem, colAmt, colStatus));
        
        if (duePayments != null) table.setItems(FXCollections.observableArrayList(duePayments));

        HBox filterBar = createFilterBar("Search due balances...", e -> exportToCsv(table, "Balance_Due_Report"));
        container.getChildren().addAll(title, stats, filterBar, table);
        return container;
    }

    // =========================================================================
    // 3. EXPIRED MEMBER REPORT
    // =========================================================================
    private VBox createExpiredMemberReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("Expired Member Report");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
            createReportStatCard("Expired Members", "0", "-fx-background-color: #be123c;")
        );
        
        TableView<String> table = createDarkTable();
        table.getColumns().add(new TableColumn<>("Member ID"));
        table.getColumns().add(new TableColumn<>("Name"));
        table.getColumns().add(new TableColumn<>("Expired Date"));
        
        HBox filterBar = createFilterBar("Search members...", e -> exportToCsv(table, "Expired_Members"));
        container.getChildren().addAll(title, stats, filterBar, table);
        return container;
    }

    // =========================================================================
    // 4. MEMBER REPORT CARD
    // =========================================================================
    private VBox createMemberReportCard() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("Member Report Card");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        javafx.scene.layout.FlowPane cardsPane = new javafx.scene.layout.FlowPane();
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        
        try {
            List<User> members = facade.getAllUsers().stream().filter(u -> u instanceof Member).collect(Collectors.toList());
            for (User u : members) {
                Member m = (Member) u;
                VBox card = new VBox(10);
                card.setAlignment(Pos.CENTER);
                card.setPadding(new Insets(20));
                card.setPrefSize(180, 160);
                card.setStyle("-fx-background-color: #334155; -fx-background-radius: 12; -fx-cursor: hand;");
                card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #475569; -fx-background-radius: 12; -fx-cursor: hand;"));
                card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #334155; -fx-background-radius: 12; -fx-cursor: hand;"));
                
                Label icon = new Label("👤"); icon.setStyle("-fx-font-size: 40px;");
                Label name = new Label(m.getName()); name.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
                card.getChildren().addAll(icon, name);
                
                card.setOnMouseClicked(e -> {
                    contentArea.getChildren().clear();
                    MeasurementAnalyticsView analyticsView = new MeasurementAnalyticsView(facade, m);
                    Button backBtn = new Button("← Back to Report Cards");
                    backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;");
                    backBtn.setOnAction(ev -> loadTab("Member Report Card"));
                    
                    VBox subContainer = new VBox(10);
                    VBox.setVgrow(analyticsView, Priority.ALWAYS);
                    subContainer.getChildren().addAll(backBtn, analyticsView);
                    contentArea.getChildren().add(subContainer);
                });
                
                cardsPane.getChildren().add(card);
            }
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        
        ScrollPane scroll = new ScrollPane(cardsPane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        
        container.getChildren().addAll(title, scroll);
        return container;
    }

    // =========================================================================
    // 5. DUE MEMBERSHIP REPORT
    // =========================================================================
    private VBox createDueMembershipReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("Due Membership Report (Expiring Soon)");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
            createReportStatCard("Expiring < 7 Days", "0", "-fx-background-color: #eab308;"),
            createReportStatCard("Expiring < 30 Days", "0", "-fx-background-color: #3b82f6;")
        );
        
        TableView<String> table = createDarkTable();
        table.getColumns().add(new TableColumn<>("Member ID"));
        table.getColumns().add(new TableColumn<>("Expiration Date"));
        
        HBox filterBar = createFilterBar("Search...", e -> exportToCsv(table, "Due_Membership"));
        container.getChildren().addAll(title, stats, filterBar, table);
        return container;
    }

    // =========================================================================
    // 6. SMS REPORT
    // =========================================================================
    private VBox createSMSReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("SMS Report");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        List<Notification> notifs = facade.getNotifications();
        
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
            createReportStatCard("Total SMS", String.valueOf(notifs.size()), "-fx-background-color: #10b981;"),
            createReportStatCard("Remaining SMS", "540", "-fx-background-color: #3b82f6;")
        );
        
        TableView<Notification> table = createDarkTable();
        TableColumn<Notification, String> colId = new TableColumn<>("Client ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotificationId()));
        
        TableColumn<Notification, String> colMobile = new TableColumn<>("Category");
        colMobile.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().toString()));
        
        TableColumn<Notification, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));
        
        TableColumn<Notification, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isRead() ? "Read" : "Unread"));
        
        TableColumn<Notification, String> colDate = new TableColumn<>("Sent Date");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        table.getColumns().addAll(List.of(colId, colMobile, colMsg, colStatus, colDate));
        table.setItems(FXCollections.observableArrayList(notifs));

        HBox filterBar = createFilterBar("Search SMS...", e -> exportToCsv(table, "SMS_Report"));
        container.getChildren().addAll(title, stats, filterBar, table);
        return container;
    }

    // =========================================================================
    // 7. ATTENDANCE REPORT
    // =========================================================================
    private VBox createAttendanceReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("Attendance Report");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        List<Booking> bookings = facade.getAllBookings();
        
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
            createReportStatCard("Total Bookings", String.valueOf(bookings.size()), "-fx-background-color: #8b5cf6;")
        );
        
        TableView<Booking> table = createDarkTable();
        TableColumn<Booking, String> colMem = new TableColumn<>("Member");
        colMem.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMember().getName()));
        
        TableColumn<Booking, String> colSession = new TableColumn<>("Session");
        colSession.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSession().getName()));
        
        TableColumn<Booking, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSessionDate().toString()));

        table.getColumns().addAll(List.of(colMem, colSession, colDate));
        table.setItems(FXCollections.observableArrayList(bookings));

        HBox filterBar = createFilterBar("Search attendance...", e -> exportToCsv(table, "Attendance_Report"));
        container.getChildren().addAll(title, stats, filterBar, table);
        return container;
    }

    // =========================================================================
    // 8. PT REPORT
    // =========================================================================
    private VBox createPTReport() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10));
        
        Label title = new Label("PT Report");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
            createReportStatCard("Active PT Clients", "0", "-fx-background-color: #d946ef;")
        );
        
        TableView<String> table = createDarkTable();
        table.getColumns().add(new TableColumn<>("Member Name"));
        table.getColumns().add(new TableColumn<>("Trainer Name"));
        
        HBox filterBar = createFilterBar("Search PT...", e -> exportToCsv(table, "PT_Report"));
        container.getChildren().addAll(title, stats, filterBar, table);
        return container;
    }
}

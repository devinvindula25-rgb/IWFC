package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminReportsView extends HBox {

    private final IWFCSystemFacade facade;
    private final StackPane contentArea;
    private final ScrollPane contentScrollPane;
    private final VBox sidebar;
    private Button activeTabBtn;

    // Premium color palette
    private static final String GRADIENT_INDIGO = "-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6);";
    private static final String GRADIENT_EMERALD = "-fx-background-color: linear-gradient(to bottom right, #10b981, #059669);";
    private static final String GRADIENT_BLUE = "-fx-background-color: linear-gradient(to bottom right, #3b82f6, #2563eb);";
    private static final String GRADIENT_AMBER = "-fx-background-color: linear-gradient(to bottom right, #f59e0b, #d97706);";
    private static final String GRADIENT_ROSE = "-fx-background-color: linear-gradient(to bottom right, #f43f5e, #e11d48);";
    private static final String GRADIENT_CYAN = "-fx-background-color: linear-gradient(to bottom right, #06b6d4, #0891b2);";
    private static final String GRADIENT_PURPLE = "-fx-background-color: linear-gradient(to bottom right, #a855f7, #9333ea);";
    private static final String GRADIENT_ORANGE = "-fx-background-color: linear-gradient(to bottom right, #f97316, #ea580c);";
    private static final String GRADIENT_TEAL = "-fx-background-color: linear-gradient(to bottom right, #14b8a6, #0d9488);";
    private static final String GRADIENT_PINK = "-fx-background-color: linear-gradient(to bottom right, #ec4899, #db2777);";

    public AdminReportsView(IWFCSystemFacade facade) {
        this.facade = facade;
        getStyleClass().add("admin-reports-view");
        setAlignment(Pos.TOP_LEFT);

        try {
            getStylesheets().add(getClass().getResource("/com/iwfc/css/member-reports.css").toExternalForm());
        } catch (Exception ignored) {}
        
        // Setup layout
        contentArea = new StackPane();
        contentArea.setAlignment(Pos.TOP_LEFT);
        contentArea.setPadding(new Insets(0));
        
        sidebar = createSidebar();
        sidebar.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(sidebar, Priority.ALWAYS);

        contentScrollPane = new ScrollPane(contentArea);
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setFitToHeight(true);
        contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScrollPane.getStyleClass().add("report-main-scroll");
        HBox.setHgrow(contentScrollPane, Priority.ALWAYS);

        getChildren().addAll(sidebar, contentScrollPane);
    }

    // =========================================================================
    // PREMIUM SIDEBAR
    // =========================================================================
    private VBox createSidebar() {
        VBox nav = new VBox(4);
        nav.setPrefWidth(240);
        nav.setMinWidth(240);
        nav.setMaxWidth(240);
        nav.setPadding(new Insets(24, 14, 24, 14));
        nav.getStyleClass().add("report-sidebar");

        // Premium header with icon
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 8, 6));
        
        Label chartIcon = new Label("📊");
        chartIcon.setStyle("-fx-font-size: 22px;");
        
        Label title = new Label("Reports");
        title.getStyleClass().add("report-sidebar-title");

        headerBox.getChildren().addAll(chartIcon, title);
        
        // Subtle divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.getStyleClass().add("report-sidebar-divider");
        VBox.setMargin(divider, new Insets(4, 0, 12, 0));

        nav.getChildren().addAll(headerBox, divider);

        // Tab definitions: [name, icon]
        String[][] tabs = {
            {"Balance Due Report", "💰"},
            {"Sales Report", "📈"},
            {"Expired Member Report", "⏰"},
            {"Member Report Card", "👥"},
            {"Due Membership Report", "📋"},
            {"SMS Report", "💬"},
            {"Attendance Report", "📅"},
            {"PT Report", "🏋"}
        };

        for (String[] tab : tabs) {
            String tabName = tab[0];
            String icon = tab[1];
            
            Button btn = new Button();
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.getStyleClass().add("report-nav-btn");
            
            // Create the button content with icon
            HBox btnContent = new HBox(10);
            btnContent.setAlignment(Pos.CENTER_LEFT);
            
            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-font-size: 15px; -fx-min-width: 20px;");
            
            // Truncate long names for sidebar
            String displayName = tabName.length() > 22 ? tabName.substring(0, 20) + ".." : tabName;
            Label textLbl = new Label(displayName);
            textLbl.getStyleClass().add("report-nav-btn-text");
            
            btnContent.getChildren().addAll(iconLbl, textLbl);
            btn.setGraphic(btnContent);
            
            // Active indicator dot
            Circle indicator = new Circle(3);
            indicator.getStyleClass().add("report-nav-indicator");
            indicator.setVisible(false);
            
            btn.setOnAction(e -> {
                // Deactivate previous
                if (activeTabBtn != null) {
                    activeTabBtn.getStyleClass().remove("report-nav-btn-active");
                    // Find and hide previous indicator
                    Node prevParent = activeTabBtn.getParent();
                    if (prevParent instanceof HBox hbox) {
                        hbox.getChildren().stream()
                            .filter(n -> n instanceof Circle)
                            .forEach(n -> n.setVisible(false));
                    }
                }
                
                // Activate current
                btn.getStyleClass().add("report-nav-btn-active");
                indicator.setVisible(true);
                
                // Pulse animation on indicator
                ScaleTransition pulse = new ScaleTransition(Duration.millis(400), indicator);
                pulse.setFromX(0);
                pulse.setFromY(0);
                pulse.setToX(1);
                pulse.setToY(1);
                pulse.setInterpolator(Interpolator.EASE_OUT);
                pulse.play();
                
                activeTabBtn = btn;
                loadTab(tabName);
            });
            
            HBox btnRow = new HBox(8);
            btnRow.setAlignment(Pos.CENTER_LEFT);
            btnRow.getChildren().addAll(indicator, btn);
            HBox.setHgrow(btn, Priority.ALWAYS);
            
            nav.getChildren().add(btnRow);
            
            // Default select Sales Report
            if (tabName.equals("Sales Report")) {
                btn.fire();
            }
        }
        
        return nav;
    }

    // =========================================================================
    // TAB LOADING WITH PREMIUM TRANSITIONS
    // =========================================================================
    private void loadTab(String tabName) {
        contentArea.getChildren().clear();
        if (contentScrollPane != null) {
            contentScrollPane.setVvalue(0.0);
        }
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

        // Premium slide + fade entrance animation
        content.setTranslateY(20);
        content.setOpacity(0);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(400), content);
        slide.setFromY(20);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition fade = new FadeTransition(Duration.millis(400), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ParallelTransition entrance = new ParallelTransition(slide, fade);
        entrance.play();
        
        contentArea.getChildren().add(content);
    }
    
    // =========================================================================
    // PREMIUM UI COMPONENTS
    // =========================================================================

    /**
     * Creates a premium gradient stat card with animated counter, glassmorphism border,
     * hover float effect, and subtle glow.
     */
    private VBox createPremiumStatCard(String title, String value, String icon, String gradientStyle) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setPrefHeight(110);
        card.setMinWidth(160);
        card.setMaxWidth(220);
        card.getStyleClass().add("report-stat-card");
        card.setStyle(gradientStyle);
        HBox.setHgrow(card, Priority.ALWAYS);
        
        // Top row: icon badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("report-stat-icon");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        topRow.getChildren().addAll(iconLbl, spacer);
        
        // Value with animated counter
        Label valLbl = new Label("0");
        valLbl.getStyleClass().add("report-stat-value");
        
        // Try to animate numeric values
        try {
            double numVal = Double.parseDouble(value);
            DoubleProperty animVal = new SimpleDoubleProperty(0);
            
            // Format based on whether it's an integer or decimal
            boolean isInteger = numVal == Math.floor(numVal) && !Double.isInfinite(numVal);
            animVal.addListener((obs, old, nw) -> {
                if (isInteger) {
                    valLbl.setText(String.format("%.0f", nw.doubleValue()));
                } else {
                    valLbl.setText(String.format("%.2f", nw.doubleValue()));
                }
            });
            
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(animVal, 0)),
                new KeyFrame(Duration.millis(1200), new KeyValue(animVal, numVal, Interpolator.EASE_OUT))
            );
            timeline.setDelay(Duration.millis(300));
            timeline.play();
        } catch (NumberFormatException e) {
            valLbl.setText(value);
        }
        
        // Subtitle
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("report-stat-title");
        
        card.getChildren().addAll(topRow, valLbl, titleLbl);
        
        // Premium hover animation: float up + glow
        DropShadow glowShadow = new DropShadow();
        glowShadow.setColor(Color.rgb(255, 255, 255, 0.15));
        glowShadow.setRadius(20);
        glowShadow.setOffsetY(8);
        
        card.setOnMouseEntered(e -> {
            card.setEffect(glowShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(250), card);
            st.setToX(1.04);
            st.setToY(1.04);
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), card);
            tt.setToY(-6);
            new ParallelTransition(st, tt).play();
        });
        
        card.setOnMouseExited(e -> {
            card.setEffect(null);
            ScaleTransition st = new ScaleTransition(Duration.millis(250), card);
            st.setToX(1.0);
            st.setToY(1.0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), card);
            tt.setToY(0);
            new ParallelTransition(st, tt).play();
        });
        
        return card;
    }
    
    /**
     * Creates a premium frosted-glass filter bar with search, date filter, and export button.
     */
    private HBox createPremiumFilterBar(String searchPrompt, javafx.event.EventHandler<javafx.event.ActionEvent> onExportClick) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 18, 14, 18));
        bar.getStyleClass().add("report-filter-bar");
        
        // Date range selector
        ComboBox<String> timeFilter = new ComboBox<>();
        timeFilter.getItems().addAll("Last 7 days", "Last 30 days", "Last 6 months", "Last Year", "All Time");
        timeFilter.setValue("Last 6 months");
        timeFilter.getStyleClass().add("report-date-combo");
        
        // Search field with icon
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("report-search-box");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 13px; -fx-opacity: 0.6;");
        
        TextField searchField = new TextField();
        searchField.setPromptText(searchPrompt);
        searchField.getStyleClass().add("report-search-field");
        searchField.setPrefWidth(220);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        searchBox.getChildren().addAll(searchIcon, searchField);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Premium export button with gradient
        Button exportBtn = new Button("📥  Export Report");
        exportBtn.getStyleClass().add("report-export-btn");
        exportBtn.setOnAction(onExportClick);
        
        // Hover pulse on export button
        exportBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), exportBtn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        exportBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), exportBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        
        bar.getChildren().addAll(timeFilter, searchBox, spacer, exportBtn);
        return bar;
    }
    
    /**
     * Creates a premium dark table with glassmorphism container, styled headers,
     * and row hover effects.
     */
    private <T> TableView<T> createPremiumTable() {
        TableView<T> table = new TableView<>();
        table.getStyleClass().add("report-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }
    
    /**
     * Wraps a table in a premium glassmorphism container card.
     */
    private VBox wrapInTableCard(TableView<?> table) {
        VBox card = new VBox(0);
        card.getStyleClass().add("report-table-card");
        card.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    /**
     * Creates a premium report header with title and optional subtitle.
     */
    private VBox createReportHeader(String titleText, String subtitleText) {
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 4, 0));
        
        Label title = new Label(titleText);
        title.getStyleClass().add("report-title");
        
        header.getChildren().add(title);
        
        if (subtitleText != null && !subtitleText.isEmpty()) {
            Label subtitle = new Label(subtitleText);
            subtitle.getStyleClass().add("report-subtitle");
            header.getChildren().add(subtitle);
        }
        
        return header;
    }

    /**
     * Creates a status badge pill for table cells.
     */
    private Label createStatusPill(String text, String status) {
        Label pill = new Label(text);
        pill.getStyleClass().add("report-status-pill");
        
        String statusClass = switch (status.toUpperCase()) {
            case "COMPLETED" -> "pill-success";
            case "PENDING" -> "pill-warning";
            case "FAILED" -> "pill-danger";
            case "REFUNDED" -> "pill-info";
            default -> "pill-default";
        };
        pill.getStyleClass().add(statusClass);
        
        return pill;
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
    // 1. SALES REPORT — Premium
    // =========================================================================
    private VBox createSalesReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("Sales Report", "Complete financial overview of all transactions");
        
        List<Payment> payments = null;
        try {
            payments = facade.getAllPayments();
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        
        double totalAmt = 0;
        double paidAmt = 0;
        int cashCnt = 0;
        int cardCnt = 0;
        int walletCnt = 0;
        
        if (payments != null) {
            for(Payment p : payments) {
                totalAmt += p.getTotalCharged();
                if(p.getStatus() == PaymentStatus.COMPLETED) {
                    paidAmt += p.getTotalCharged();
                    if (p.getMethod() != null) {
                        switch (p.getMethod()) {
                            case CASH -> cashCnt++;
                            case DIGITAL_WALLET -> walletCnt++;
                            default -> cardCnt++;
                        }
                    }
                }
            }
        }
        
        // Premium stat cards row 1 - Major metrics
        HBox row1 = new HBox(14);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.getChildren().addAll(
            createPremiumStatCard("Invoices Generated", String.valueOf(payments == null ? 0 : payments.size()), "🧾", GRADIENT_INDIGO),
            createPremiumStatCard("Total Amount", String.format("%.2f", totalAmt), "💎", GRADIENT_EMERALD),
            createPremiumStatCard("Paid Amount", String.format("%.2f", paidAmt), "✅", GRADIENT_BLUE),
            createPremiumStatCard("Balance Amount", String.format("%.2f", totalAmt - paidAmt), "⚖", GRADIENT_AMBER),
            createPremiumStatCard("Tax Amount", "0.00", "📊", GRADIENT_PURPLE)
        );
        
        // Premium stat cards row 2 - Payment methods
        HBox row2 = new HBox(14);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().addAll(
            createPremiumStatCard("Online / Card", String.valueOf(cardCnt), "💳", GRADIENT_CYAN),
            createPremiumStatCard("Digital Wallet", String.valueOf(walletCnt), "📱", GRADIENT_TEAL),
            createPremiumStatCard("Cash", String.valueOf(cashCnt), "💵", GRADIENT_ORANGE),
            createPremiumStatCard("Other", "0", "📦", GRADIENT_PINK)
        );
        
        // Premium table
        TableView<Payment> table = createPremiumTable();
        
        TableColumn<Payment, String> colId = new TableColumn<>("Payment ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentId()));
        colId.getStyleClass().add("report-col-id");
        
        TableColumn<Payment, String> colMem = new TableColumn<>("Member ID");
        colMem.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMemberId()));
        
        TableColumn<Payment, String> colAmt = new TableColumn<>("Total Amount");
        colAmt.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getTotalCharged())));
        colAmt.getStyleClass().add("report-col-amount");
        
        TableColumn<Payment, String> colMethod = new TableColumn<>("Method");
        colMethod.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMethod().getDisplayName()));
        // Custom cell factory for method badge
        colMethod.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("report-method-badge");
                    if (item.toLowerCase().contains("cash")) {
                        badge.getStyleClass().add("method-cash");
                    } else if (item.toLowerCase().contains("wallet") || item.toLowerCase().contains("digital")) {
                        badge.getStyleClass().add("method-wallet");
                    } else {
                        badge.getStyleClass().add("method-card");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        
        TableColumn<Payment, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        // Custom cell factory for status pill
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = createStatusPill(item, item);
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        TableColumn<Payment, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        table.getColumns().addAll(List.of(colId, colMem, colAmt, colMethod, colStatus, colDate));
        
        if (payments != null) {
            table.setItems(FXCollections.observableArrayList(payments));
        }

        HBox filterBar = createPremiumFilterBar("Search sales...", e -> exportToCsv(table, "Sales_Report"));
        VBox tableCard = wrapInTableCard(table);
        
        container.getChildren().addAll(header, row1, row2, filterBar, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // 2. BALANCE DUE REPORT — Premium
    // =========================================================================
    private VBox createBalanceDueReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("Balance Due Report", "Track outstanding payments and overdue invoices");
        
        List<Payment> duePayments = null;
        try {
            duePayments = facade.getAllPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.FAILED)
                .collect(Collectors.toList());
        } catch (com.iwfc.exception.UnauthorizedAccessException e) {}
        
        double totalDue = duePayments == null ? 0 : duePayments.stream().mapToDouble(Payment::getTotalCharged).sum();
        int overdueCount = duePayments == null ? 0 : duePayments.size();
        
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createPremiumStatCard("Total Due", String.format("%.2f", totalDue), "💰", GRADIENT_ROSE),
            createPremiumStatCard("Overdue Invoices", String.valueOf(overdueCount), "⚠", GRADIENT_ORANGE)
        );
        
        TableView<Payment> table = createPremiumTable();
        TableColumn<Payment, String> colId = new TableColumn<>("Payment ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentId()));
        colId.getStyleClass().add("report-col-id");
        
        TableColumn<Payment, String> colMem = new TableColumn<>("Member ID");
        colMem.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMemberId()));
        
        TableColumn<Payment, String> colAmt = new TableColumn<>("Amount Due");
        colAmt.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getTotalCharged())));
        colAmt.getStyleClass().add("report-col-amount");
        
        TableColumn<Payment, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = createStatusPill(item, item);
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(List.of(colId, colMem, colAmt, colStatus));
        
        if (duePayments != null) table.setItems(FXCollections.observableArrayList(duePayments));

        HBox filterBar = createPremiumFilterBar("Search due balances...", e -> exportToCsv(table, "Balance_Due_Report"));
        VBox tableCard = wrapInTableCard(table);
        
        container.getChildren().addAll(header, stats, filterBar, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // 3. EXPIRED MEMBER REPORT — Premium
    // =========================================================================
    private VBox createExpiredMemberReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("Expired Member Report", "Members with lapsed memberships");
        
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createPremiumStatCard("Expired Members", "0", "⏰", GRADIENT_ROSE)
        );
        
        TableView<String> table = createPremiumTable();
        TableColumn<String, String> colId = new TableColumn<>("Member ID");
        colId.getStyleClass().add("report-col-id");
        TableColumn<String, String> colName = new TableColumn<>("Name");
        TableColumn<String, String> colDate = new TableColumn<>("Expired Date");
        table.getColumns().addAll(List.of(colId, colName, colDate));
        
        HBox filterBar = createPremiumFilterBar("Search members...", e -> exportToCsv(table, "Expired_Members"));
        VBox tableCard = wrapInTableCard(table);
        
        container.getChildren().addAll(header, stats, filterBar, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // 4. MEMBER REPORT CARD — Premium
    // =========================================================================
    private VBox createMemberReportCard() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(28, 28, 24, 28));
        
        VBox header = createReportHeader("Member Report Card", "Real-time biometric tracking, body composition analytics & progress matrix");
        
        // Filter section (Search + Filter Chips + Count)
        VBox filterSection = new VBox(12);
        
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("report-filter-bar");
        searchBar.setPadding(new Insets(10, 16, 10, 16));
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 14px; -fx-opacity: 0.6;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search members by name or ID...");
        searchField.getStyleClass().add("report-search-field");
        searchField.setPrefWidth(280);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand; -fx-font-size: 12px;");
        clearBtn.setVisible(false);
        clearBtn.setOnAction(e -> searchField.clear());
        searchField.textProperty().addListener((obs, o, n) -> clearBtn.setVisible(n != null && !n.isEmpty()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label countLabel = new Label();
        countLabel.getStyleClass().add("report-subtitle");
        countLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #818cf8;");
        
        searchBar.getChildren().addAll(searchIcon, searchField, clearBtn, spacer, countLabel);

        // Filter chips row
        HBox chipsRow = new HBox(8);
        chipsRow.setAlignment(Pos.CENTER_LEFT);
        String[] chipLabels = {"All Members", "Active", "With Biometrics", "Male", "Female"};
        List<Button> chipButtons = new ArrayList<>();
        final String[] selectedFilter = {"All Members"};

        FlowPane cardsPane = new FlowPane();
        cardsPane.setHgap(18);
        cardsPane.setVgap(18);
        cardsPane.setPadding(new Insets(8, 0, 0, 0));

        Runnable applyFilters = () -> {
            String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String chip = selectedFilter[0];
            int visibleCount = 0;

            for (Node node : cardsPane.getChildren()) {
                if (node instanceof VBox cardNode) {
                    Member m = (Member) cardNode.getUserData();
                    if (m == null) continue;

                    boolean matchesQuery = query.isEmpty() ||
                        m.getName().toLowerCase().contains(query) ||
                        m.getUserId().toLowerCase().contains(query);

                    boolean isFemale = m.getName().toLowerCase().matches(".*(emma|jane|lisa|sarah|mary|ann|female|clara|sophia).*");

                    boolean matchesChip = switch (chip) {
                        case "Active" -> true;
                        case "With Biometrics" -> {
                            try {
                                List<BodyMeasurementLog> logs = facade.getMeasurementLogs(m.getUserId());
                                yield logs != null && !logs.isEmpty();
                            } catch (Exception ex) {
                                yield false;
                            }
                        }
                        case "Male" -> !isFemale;
                        case "Female" -> isFemale;
                        default -> true;
                    };

                    boolean visible = matchesQuery && matchesChip;
                    cardNode.setVisible(visible);
                    cardNode.setManaged(visible);
                    if (visible) visibleCount++;
                }
            }
            countLabel.setText(visibleCount + " members shown");
        };

        for (String label : chipLabels) {
            Button chipBtn = new Button(label);
            chipBtn.getStyleClass().add("filter-chip-btn");
            if (label.equals("All Members")) {
                chipBtn.getStyleClass().add("filter-chip-btn-active");
            }
            chipBtn.setOnAction(e -> {
                for (Button b : chipButtons) {
                    b.getStyleClass().remove("filter-chip-btn-active");
                }
                chipBtn.getStyleClass().add("filter-chip-btn-active");
                selectedFilter[0] = label;
                applyFilters.run();
            });
            chipButtons.add(chipBtn);
            chipsRow.getChildren().add(chipBtn);
        }

        filterSection.getChildren().addAll(searchBar, chipsRow);

        try {
            List<User> members = facade.getAllUsers().stream().filter(u -> u instanceof Member).collect(Collectors.toList());
            countLabel.setText(members.size() + " members");
            
            int delay = 0;
            for (User u : members) {
                Member m = (Member) u;
                VBox card = new VBox(10);
                card.setUserData(m);
                card.setAlignment(Pos.CENTER);
                card.setPadding(new Insets(18, 16, 16, 16));
                card.setPrefSize(200, 190);
                card.getStyleClass().add("member-report-grid-card");
                
                // Avatar circle with initials
                StackPane avatarPane = new StackPane();
                Circle avatarBg = new Circle(26);
                avatarBg.getStyleClass().add("report-avatar-circle");
                
                String initials = getInitials(m.getName());
                Label initialsLbl = new Label(initials);
                initialsLbl.getStyleClass().add("report-avatar-text");
                
                avatarPane.getChildren().addAll(avatarBg, initialsLbl);
                
                Label name = new Label(m.getName());
                name.getStyleClass().add("report-member-name");
                name.setWrapText(true);
                name.setAlignment(Pos.CENTER);
                name.setMaxWidth(170);
                
                Label idLbl = new Label("ID: " + m.getUserId());
                idLbl.getStyleClass().add("report-member-id");

                // Telemetry summary glance
                List<BodyMeasurementLog> logs = null;
                try {
                    logs = facade.getMeasurementLogs(m.getUserId());
                } catch (Exception ignored) {}

                HBox glanceStrip = new HBox(6);
                glanceStrip.setAlignment(Pos.CENTER);
                glanceStrip.getStyleClass().add("member-card-glance-strip");

                Label telemetryIcon = new Label(logs != null && !logs.isEmpty() ? "⚡" : "⚪");
                telemetryIcon.setStyle("-fx-font-size: 11px;");

                String telemetryStr = (logs != null && !logs.isEmpty())
                    ? String.format("%.1f kg • %d logs", logs.get(logs.size() - 1).getWeight(), logs.size())
                    : "No logs recorded";
                Label telemetryLbl = new Label(telemetryStr);
                telemetryLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
                glanceStrip.getChildren().addAll(telemetryIcon, telemetryLbl);

                Button inspectBtn = new Button("View Analytics →");
                inspectBtn.getStyleClass().add("member-card-inspect-btn");
                inspectBtn.setMaxWidth(Double.MAX_VALUE);
                
                card.getChildren().addAll(avatarPane, name, idLbl, glanceStrip, inspectBtn);
                
                // Staggered entrance animation
                card.setOpacity(0);
                card.setTranslateY(15);
                
                int finalDelay = delay;
                javafx.application.Platform.runLater(() -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(350), card);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.setDelay(Duration.millis(finalDelay));
                    
                    TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
                    tt.setFromY(15);
                    tt.setToY(0);
                    tt.setDelay(Duration.millis(finalDelay));
                    tt.setInterpolator(Interpolator.EASE_OUT);
                    
                    new ParallelTransition(ft, tt).play();
                });
                delay += 25;
                
                Runnable openAnalytics = () -> {
                    contentArea.getChildren().clear();
                    if (contentScrollPane != null) {
                        contentScrollPane.setVvalue(0.0);
                    }
                    MeasurementAnalyticsView analyticsView = new MeasurementAnalyticsView(facade, m);
                    
                    Button backBtn = new Button("← Back to Member Directory");
                    backBtn.getStyleClass().add("report-back-btn");
                    backBtn.setOnAction(ev -> loadTab("Member Report Card"));
                    
                    VBox subContainer = new VBox(16);
                    subContainer.setPadding(new Insets(24));
                    VBox.setVgrow(analyticsView, Priority.ALWAYS);
                    subContainer.getChildren().addAll(backBtn, analyticsView);
                    contentArea.getChildren().add(subContainer);
                };

                card.setOnMouseClicked(e -> openAnalytics.run());
                inspectBtn.setOnAction(e -> openAnalytics.run());
                
                cardsPane.getChildren().add(card);
            }
            
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
            
        } catch (com.iwfc.exception.UnauthorizedAccessException ignored) {}
        
        container.getChildren().addAll(header, filterSection, cardsPane);
        return container;
    }

    // =========================================================================
    // 5. DUE MEMBERSHIP REPORT — Premium
    // =========================================================================
    private VBox createDueMembershipReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("Due Membership Report", "Members with expiring memberships");
        
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createPremiumStatCard("Expiring < 7 Days", "0", "🔥", GRADIENT_AMBER),
            createPremiumStatCard("Expiring < 30 Days", "0", "📅", GRADIENT_BLUE)
        );
        
        TableView<String> table = createPremiumTable();
        TableColumn<String, String> colId = new TableColumn<>("Member ID");
        colId.getStyleClass().add("report-col-id");
        TableColumn<String, String> colDate = new TableColumn<>("Expiration Date");
        table.getColumns().addAll(List.of(colId, colDate));
        
        HBox filterBar = createPremiumFilterBar("Search...", e -> exportToCsv(table, "Due_Membership"));
        VBox tableCard = wrapInTableCard(table);
        
        container.getChildren().addAll(header, stats, filterBar, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // 6. SMS REPORT — Premium
    // =========================================================================
    private VBox createSMSReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("SMS Report", "Track all SMS notifications and delivery status");
        
        List<Notification> notifs = facade.getNotifications();
        
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createPremiumStatCard("Total SMS Sent", String.valueOf(notifs.size()), "💬", GRADIENT_EMERALD),
            createPremiumStatCard("Remaining Credits", "540", "📨", GRADIENT_BLUE)
        );
        
        TableView<Notification> table = createPremiumTable();
        TableColumn<Notification, String> colId = new TableColumn<>("Notification ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotificationId()));
        colId.getStyleClass().add("report-col-id");
        
        TableColumn<Notification, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().toString()));
        colCat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("report-method-badge", "method-card");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        
        TableColumn<Notification, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));
        
        TableColumn<Notification, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isRead() ? "Read" : "Unread"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("report-status-pill");
                    pill.getStyleClass().add(item.equals("Read") ? "pill-success" : "pill-warning");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        TableColumn<Notification, String> colDate = new TableColumn<>("Sent Date");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        table.getColumns().addAll(List.of(colId, colCat, colMsg, colStatus, colDate));
        table.setItems(FXCollections.observableArrayList(notifs));

        HBox filterBar = createPremiumFilterBar("Search SMS...", e -> exportToCsv(table, "SMS_Report"));
        VBox tableCard = wrapInTableCard(table);
        
        container.getChildren().addAll(header, stats, filterBar, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // 7. ATTENDANCE REPORT — Supreme UI/UX
    // =========================================================================
    private VBox createAttendanceReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("Attendance Report", "Real-time session bookings, check-in history, and capacity analytics");
        
        List<Booking> allBookings = facade.getAllBookings();
        int totalBookings = allBookings.size();
        long confirmedCount = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelledCount = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long distinctSessions = allBookings.stream().map(b -> b.getSession().getName()).distinct().count();
        
        // Find most popular session
        Map<String, Long> sessionCounts = allBookings.stream()
            .collect(Collectors.groupingBy(b -> b.getSession().getName(), Collectors.counting()));
        String topSessionName = sessionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(e -> e.getKey() + " (" + e.getValue() + ")")
            .orElse("CrossFit 2");
        
        // 5 Premium Gradient Stat Cards
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createPremiumStatCard("Total Bookings", String.valueOf(totalBookings), "📅", GRADIENT_INDIGO),
            createPremiumStatCard("Confirmed", String.valueOf(confirmedCount), "✅", GRADIENT_EMERALD),
            createPremiumStatCard("Cancelled", String.valueOf(cancelledCount), "⚠️", GRADIENT_ROSE),
            createPremiumStatCard("Active Classes", String.valueOf(distinctSessions), "🏋️", GRADIENT_BLUE),
            createPremiumStatCard("Top Activity", topSessionName, "🏆", GRADIENT_PURPLE)
        );
        
        // Master & Filtered Collections
        ObservableList<Booking> masterData = FXCollections.observableArrayList(allBookings);
        FilteredList<Booking> filteredData = new FilteredList<>(masterData, b -> true);
        
        // Premium Table
        TableView<Booking> table = createPremiumTable();
        table.setItems(filteredData);
        
        // Member Column (Avatar + Name + ID)
        TableColumn<Booking, Booking> colMem = new TableColumn<>("Member");
        colMem.setPrefWidth(210);
        colMem.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colMem.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Member m = item.getMember();
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("report-avatar-mini");
                    avatar.setStyle(getAvatarGradient(m.getName()));
                    Label ini = new Label(getInitials(m.getName()));
                    ini.getStyleClass().add("report-avatar-mini-text");
                    avatar.getChildren().add(ini);
                    
                    VBox text = new VBox(2);
                    text.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(m.getName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    Label idLbl = new Label(m.getUserId() + " • " + (m.getMembershipType() != null ? m.getMembershipType() : "Member"));
                    idLbl.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");
                    text.getChildren().addAll(nameLbl, idLbl);
                    
                    box.getChildren().addAll(avatar, text);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Session Column (Title + Category Pill)
        TableColumn<Booking, Booking> colSession = new TableColumn<>("Activity / Session");
        colSession.setPrefWidth(190);
        colSession.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSession.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    FitnessSession fs = item.getSession();
                    VBox box = new VBox(4);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    Label nameLbl = new Label(fs.getName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    
                    String sType = fs.getSessionType() != null ? fs.getSessionType() : "General";
                    Label pill = new Label(sType.toUpperCase());
                    pill.getStyleClass().addAll("session-type-pill", getSessionPillClass(sType));
                    
                    box.getChildren().addAll(nameLbl, pill);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Coach & Location Column
        TableColumn<Booking, Booking> colCoach = new TableColumn<>("Coach & Studio");
        colCoach.setPrefWidth(180);
        colCoach.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colCoach.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    FitnessSession fs = item.getSession();
                    VBox box = new VBox(4);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    String coach = fs.getInstructor() != null ? fs.getInstructor().getName() : "Lead Trainer";
                    Label coachLbl = new Label("👤 " + coach);
                    coachLbl.getStyleClass().add("report-instructor-tag");
                    
                    String loc = fs.getLocation() != null ? fs.getLocation().getDisplayName() : "Studio";
                    Label locLbl = new Label("📍 " + loc);
                    locLbl.getStyleClass().add("report-location-tag");
                    
                    box.getChildren().addAll(coachLbl, locLbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Schedule Column (Date + Time)
        TableColumn<Booking, Booking> colSchedule = new TableColumn<>("Schedule");
        colSchedule.setPrefWidth(170);
        colSchedule.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSchedule.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    String dateStr = item.getSessionDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
                    Label dateLbl = new Label("🗓 " + dateStr);
                    dateLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 12px;");
                    
                    FitnessSession fs = item.getSession();
                    String timeStr = (fs.getStartTime() != null ? fs.getStartTime().toString() : "07:00")
                            + " - " + (fs.getEndTime() != null ? fs.getEndTime().toString() : "08:00");
                    Label timeLbl = new Label("⏰ " + timeStr);
                    timeLbl.setStyle("-fx-opacity: 0.65; -fx-font-size: 11px;");
                    
                    box.getChildren().addAll(dateLbl, timeLbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Status Column
        TableColumn<Booking, String> colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(120);
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().add("report-status-pill");
                    if ("CONFIRMED".equalsIgnoreCase(item)) {
                        pill.getStyleClass().add("pill-success");
                    } else if ("CANCELLED".equalsIgnoreCase(item)) {
                        pill.getStyleClass().add("pill-danger");
                    } else {
                        pill.getStyleClass().add("pill-info");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        // Action Column
        TableColumn<Booking, Booking> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(110);
        colAction.setSortable(false);
        colAction.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Button btn = new Button("Quick Info");
                    btn.getStyleClass().add("report-action-btn-sm");
                    btn.setOnAction(e -> showBookingDetailDialog(item));
                    setGraphic(btn);
                    setText(null);
                }
            }
        });
        
        table.getColumns().addAll(List.of(colMem, colSession, colCoach, colSchedule, colStatus, colAction));
        
        // Dynamic Filter Bar with Listeners
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(14, 18, 14, 18));
        filterBar.getStyleClass().add("report-filter-bar");
        
        ComboBox<String> timeFilter = new ComboBox<>();
        timeFilter.getItems().addAll("All Time", "Today", "Upcoming", "Past 7 Days", "Past 30 Days", "Past 6 Months");
        timeFilter.setValue("All Time");
        timeFilter.getStyleClass().add("report-date-combo");
        
        // Populate unique session types
        Set<String> sessionTypes = new TreeSet<>();
        sessionTypes.add("All Types");
        for (Booking b : allBookings) {
            if (b.getSession().getSessionType() != null) {
                sessionTypes.add(b.getSession().getSessionType());
            }
        }
        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll(sessionTypes);
        typeFilter.setValue("All Types");
        typeFilter.getStyleClass().add("report-date-combo");
        
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("report-search-box");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 13px; -fx-opacity: 0.6;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search attendance by member, coach, activity...");
        searchField.getStyleClass().add("report-search-field");
        searchField.setPrefWidth(240);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);
        
        Label countBadge = new Label(totalBookings + " Bookings");
        countBadge.getStyleClass().add("report-count-badge");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button exportBtn = new Button("📥  Export CSV");
        exportBtn.getStyleClass().add("report-export-btn");
        exportBtn.setOnAction(e -> exportToCsv(table, "Attendance_Report"));
        
        filterBar.getChildren().addAll(timeFilter, typeFilter, searchBox, countBadge, spacer, exportBtn);
        
        // Multi-attribute live predicate update
        Runnable updatePredicate = () -> {
            String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String tRange = timeFilter.getValue();
            String sType = typeFilter.getValue();
            LocalDate now = LocalDate.now();
            
            filteredData.setPredicate(booking -> {
                // Time Range Filter
                if (tRange != null) {
                    switch (tRange) {
                        case "Today":
                            if (!now.equals(booking.getSessionDate())) return false;
                            break;
                        case "Upcoming":
                            if (!booking.getSessionDate().isAfter(now)) return false;
                            break;
                        case "Past 7 Days":
                            if (booking.getSessionDate().isBefore(now.minusDays(7)) || booking.getSessionDate().isAfter(now)) return false;
                            break;
                        case "Past 30 Days":
                            if (booking.getSessionDate().isBefore(now.minusDays(30)) || booking.getSessionDate().isAfter(now)) return false;
                            break;
                        case "Past 6 Months":
                            if (booking.getSessionDate().isBefore(now.minusMonths(6)) || booking.getSessionDate().isAfter(now)) return false;
                            break;
                        default:
                            break;
                    }
                }
                
                // Session Type Filter
                if (sType != null && !"All Types".equals(sType)) {
                    String actualType = booking.getSession().getSessionType();
                    if (actualType == null || !actualType.equalsIgnoreCase(sType)) return false;
                }
                
                // Search Text Query
                if (!q.isEmpty()) {
                    String memName = booking.getMember().getName().toLowerCase();
                    String memId = booking.getMember().getUserId().toLowerCase();
                    String sessName = booking.getSession().getName().toLowerCase();
                    String coach = booking.getSession().getInstructor() != null ? booking.getSession().getInstructor().getName().toLowerCase() : "";
                    String dateStr = booking.getSessionDate().toString().toLowerCase();
                    
                    return memName.contains(q) || memId.contains(q) || sessName.contains(q) ||
                           coach.contains(q) || dateStr.contains(q);
                }
                return true;
            });
            countBadge.setText(filteredData.size() + " Bookings");
        };
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
        timeFilter.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
        
        VBox tableCard = wrapInTableCard(table);
        
        container.getChildren().addAll(header, stats, filterBar, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // 8. PT (PERSONAL TRAINING) REPORT — Supreme UI/UX
    // =========================================================================
    public static class PTClientModel {
        private final Member member;
        private final Instructor instructor;
        private final FitnessPlan plan;
        private final int sessionsPerWeek;
        private final boolean includesDiet;
        private final int totalBookings;
        private final String status;

        public PTClientModel(Member member, Instructor instructor, FitnessPlan plan,
                             int sessionsPerWeek, boolean includesDiet, int totalBookings, String status) {
            this.member = member;
            this.instructor = instructor;
            this.plan = plan;
            this.sessionsPerWeek = sessionsPerWeek;
            this.includesDiet = includesDiet;
            this.totalBookings = totalBookings;
            this.status = status;
        }

        public Member getMember() { return member; }
        public Instructor getInstructor() { return instructor; }
        public FitnessPlan getPlan() { return plan; }
        public int getSessionsPerWeek() { return sessionsPerWeek; }
        public boolean isIncludesDiet() { return includesDiet; }
        public int getTotalBookings() { return totalBookings; }
        public String getStatus() { return status; }
    }

    private VBox createPTReport() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(28, 28, 20, 28));
        
        VBox header = createReportHeader("PT Report", "Personal training clients, dedicated 1-on-1 coaching, and trainer assignments");
        
        // 1. Gather all instructors
        List<Instructor> instructors = new ArrayList<>();
        try {
            for (User u : com.iwfc.pattern.DataStore.getInstance().getUsers().findAll()) {
                if (u instanceof Instructor inst) {
                    instructors.add(inst);
                }
            }
        } catch (Exception ignored) {}
        
        // 2. Gather all 1-on-1 PT bookings
        List<Booking> allBookings = facade.getAllBookings();
        List<Booking> ptBookings = allBookings.stream()
            .filter(b -> {
                String type = b.getSession().getSessionType();
                String name = b.getSession().getName();
                return (type != null && (type.toLowerCase().contains("personal") || type.equalsIgnoreCase("pt"))) ||
                       (name != null && name.toLowerCase().contains("1-on-1 pt"));
            })
            .collect(Collectors.toList());
        
        Map<String, Long> ptBookingsPerMember = ptBookings.stream()
            .collect(Collectors.groupingBy(b -> b.getMember().getUserId(), Collectors.counting()));
        
        // 3. Consolidate PT Clients Map
        Map<String, PTClientModel> clientMap = new LinkedHashMap<>();
        
        // A. From Instructor traineeIds
        for (Instructor inst : instructors) {
            for (String traineeId : inst.getTraineeIds()) {
                User u = com.iwfc.pattern.DataStore.getInstance().getUsers().findById(traineeId).orElse(null);
                if (u instanceof Member m) {
                    FitnessPlan chosenPlan = null;
                    List<Enrollment> enrollments = facade.getEnrollmentsForMember(m.getUserId());
                    if (enrollments != null) {
                        for (Enrollment enr : enrollments) {
                            FitnessPlan p = facade.getPlanById(enr.getPlanId());
                            if (p instanceof PersonalTrainingPlan) {
                                chosenPlan = p;
                                break;
                            }
                        }
                    }
                    if (chosenPlan == null) {
                        chosenPlan = facade.getPlanById("FP007");
                        if (chosenPlan == null && !facade.getPlanCatalogue().isEmpty()) {
                            chosenPlan = facade.getPlanCatalogue().stream()
                                .filter(p -> p instanceof PersonalTrainingPlan)
                                .findFirst()
                                .orElse(facade.getPlanCatalogue().get(0));
                        }
                    }
                    int sessionsPerWeek = (chosenPlan instanceof PersonalTrainingPlan ptp) ? ptp.getOneOnOneSessionsPerWeek() : 3;
                    boolean includesDiet = (chosenPlan instanceof PersonalTrainingPlan ptp) && ptp.isIncludesDietaryPlan();
                    int bCount = ptBookingsPerMember.getOrDefault(m.getUserId(), 0L).intValue();
                    
                    clientMap.put(m.getUserId(), new PTClientModel(m, inst, chosenPlan, sessionsPerWeek, includesDiet, bCount, "ACTIVE"));
                }
            }
        }
        
        // B. From Members enrolled in PersonalTrainingPlan
        for (User u : com.iwfc.pattern.DataStore.getInstance().getUsers().findAll()) {
            if (u instanceof Member m && !clientMap.containsKey(m.getUserId())) {
                List<Enrollment> enrollments = facade.getEnrollmentsForMember(m.getUserId());
                if (enrollments != null) {
                    for (Enrollment enr : enrollments) {
                        FitnessPlan p = facade.getPlanById(enr.getPlanId());
                        if (p instanceof PersonalTrainingPlan ptp) {
                            Instructor assigned = null;
                            if (enr.getInstructorId() != null) {
                                User instrUser = com.iwfc.pattern.DataStore.getInstance().getUsers().findById(enr.getInstructorId()).orElse(null);
                                if (instrUser instanceof Instructor i) assigned = i;
                            }
                            if (assigned == null && !instructors.isEmpty()) {
                                assigned = instructors.get(clientMap.size() % instructors.size());
                            }
                            if (assigned != null) {
                                int bCount = ptBookingsPerMember.getOrDefault(m.getUserId(), 0L).intValue();
                                clientMap.put(m.getUserId(), new PTClientModel(m, assigned, ptp, ptp.getOneOnOneSessionsPerWeek(), ptp.isIncludesDietaryPlan(), bCount, "ACTIVE"));
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // C. From PT bookings
        for (Booking b : ptBookings) {
            Member m = b.getMember();
            if (!clientMap.containsKey(m.getUserId())) {
                Instructor assigned = b.getSession().getInstructor();
                if (assigned == null && !instructors.isEmpty()) assigned = instructors.get(0);
                FitnessPlan fallbackPlan = facade.getPlanById("FP007");
                if (fallbackPlan == null) fallbackPlan = facade.getPlanCatalogue().get(0);
                int sessionsPerWeek = (fallbackPlan instanceof PersonalTrainingPlan ptp) ? ptp.getOneOnOneSessionsPerWeek() : 2;
                boolean includesDiet = (fallbackPlan instanceof PersonalTrainingPlan ptp) && ptp.isIncludesDietaryPlan();
                int bCount = ptBookingsPerMember.getOrDefault(m.getUserId(), 0L).intValue();
                if (assigned != null) {
                    clientMap.put(m.getUserId(), new PTClientModel(m, assigned, fallbackPlan, sessionsPerWeek, includesDiet, bCount, "ACTIVE"));
                }
            }
        }
        
        List<PTClientModel> ptClients = new ArrayList<>(clientMap.values());
        
        // Metrics Calculation
        int totalClients = ptClients.size();
        long distinctTrainers = ptClients.stream().map(c -> c.getInstructor().getUserId()).distinct().count();
        int totalPtSessions = ptBookings.size();
        double avgSessions = ptClients.isEmpty() ? 0 : ptClients.stream().mapToInt(PTClientModel::getSessionsPerWeek).average().orElse(0);
        long dietPlansCount = ptClients.stream().filter(PTClientModel::isIncludesDiet).count();
        
        // 5 Premium Gradient Stat Cards
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            createPremiumStatCard("Active PT Clients", String.valueOf(totalClients), "🏋️", GRADIENT_EMERALD),
            createPremiumStatCard("Assigned Trainers", String.valueOf(distinctTrainers), "🧑‍🏫", GRADIENT_INDIGO),
            createPremiumStatCard("1-on-1 Sessions", String.valueOf(totalPtSessions), "⏱️", GRADIENT_BLUE),
            createPremiumStatCard("Avg Weekly Load", String.format("%.1f/wk", avgSessions), "📊", GRADIENT_AMBER),
            createPremiumStatCard("Custom Diet Plans", String.valueOf(dietPlansCount), "🥗", GRADIENT_PINK)
        );
        
        // Mode Switcher: Active Clients vs 1-on-1 Sessions Log
        HBox switchBar = new HBox(12);
        switchBar.setAlignment(Pos.CENTER_LEFT);
        
        HBox viewToggle = new HBox(6);
        viewToggle.getStyleClass().add("report-view-toggle-bar");
        viewToggle.setAlignment(Pos.CENTER_LEFT);
        
        Button btnClients = new Button("🏋️  Active PT Clients (" + ptClients.size() + ")");
        btnClients.getStyleClass().addAll("report-view-toggle-btn", "report-view-toggle-btn-active");
        
        Button btnSessions = new Button("📅  1-on-1 Sessions Log (" + ptBookings.size() + ")");
        btnSessions.getStyleClass().add("report-view-toggle-btn");
        
        viewToggle.getChildren().addAll(btnClients, btnSessions);
        switchBar.getChildren().add(viewToggle);
        
        // Master Collections
        ObservableList<PTClientModel> clientsMaster = FXCollections.observableArrayList(ptClients);
        FilteredList<PTClientModel> clientsFiltered = new FilteredList<>(clientsMaster, c -> true);
        
        ObservableList<Booking> sessionsMaster = FXCollections.observableArrayList(ptBookings);
        FilteredList<Booking> sessionsFiltered = new FilteredList<>(sessionsMaster, s -> true);
        
        // TABLE 1: Active PT Clients
        TableView<PTClientModel> tableClients = createPremiumTable();
        tableClients.setItems(clientsFiltered);
        
        // Client Column
        TableColumn<PTClientModel, PTClientModel> colClient = new TableColumn<>("Client");
        colClient.setPrefWidth(210);
        colClient.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colClient.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PTClientModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Member m = item.getMember();
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("report-avatar-mini");
                    avatar.setStyle(getAvatarGradient(m.getName()));
                    Label ini = new Label(getInitials(m.getName()));
                    ini.getStyleClass().add("report-avatar-mini-text");
                    avatar.getChildren().add(ini);
                    
                    VBox text = new VBox(2);
                    text.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(m.getName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    Label idLbl = new Label(m.getUserId() + " • " + (m.getPhone() != null ? m.getPhone() : "Member"));
                    idLbl.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");
                    text.getChildren().addAll(nameLbl, idLbl);
                    
                    box.getChildren().addAll(avatar, text);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Assigned Trainer Column
        TableColumn<PTClientModel, PTClientModel> colTrainer = new TableColumn<>("Dedicated Trainer");
        colTrainer.setPrefWidth(190);
        colTrainer.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colTrainer.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PTClientModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Instructor inst = item.getInstructor();
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("report-avatar-mini");
                    avatar.setStyle(GRADIENT_INDIGO);
                    Label ini = new Label(getInitials(inst.getName()));
                    ini.getStyleClass().add("report-avatar-mini-text");
                    avatar.getChildren().add(ini);
                    
                    VBox text = new VBox(2);
                    text.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(inst.getName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    Label specLbl = new Label(inst.getSpecialization() != null ? inst.getSpecialization() : "Master Coach");
                    specLbl.setStyle("-fx-opacity: 0.65; -fx-font-size: 11px;");
                    text.getChildren().addAll(nameLbl, specLbl);
                    
                    box.getChildren().addAll(avatar, text);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Plan & Duration Column
        TableColumn<PTClientModel, PTClientModel> colPlan = new TableColumn<>("Personal Training Plan");
        colPlan.setPrefWidth(190);
        colPlan.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colPlan.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PTClientModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    FitnessPlan p = item.getPlan();
                    VBox box = new VBox(4);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    Label titleLbl = new Label(p.getTitle());
                    titleLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    
                    Label durLbl = new Label("⏱ " + p.getDurationWeeks() + " Weeks Program");
                    durLbl.getStyleClass().add("report-location-tag");
                    
                    box.getChildren().addAll(titleLbl, durLbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        // Frequency Column
        TableColumn<PTClientModel, String> colFreq = new TableColumn<>("Frequency");
        colFreq.setPrefWidth(130);
        colFreq.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSessionsPerWeek() + "x / Week"));
        colFreq.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label("⚡ " + item);
                    badge.getStyleClass().addAll("session-type-pill", "pill-spin");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        
        // Diet Support Column
        TableColumn<PTClientModel, Boolean> colDiet = new TableColumn<>("Dietary Support");
        colDiet.setPrefWidth(140);
        colDiet.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isIncludesDiet()));
        colDiet.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean hasDiet, boolean empty) {
                super.updateItem(hasDiet, empty);
                if (empty || hasDiet == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(hasDiet ? "🥗 Nutrition Plan" : "Standard Advice");
                    pill.getStyleClass().add("report-status-pill");
                    pill.getStyleClass().add(hasDiet ? "pill-success" : "pill-warning");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        // Booked Sessions Column
        TableColumn<PTClientModel, String> colBooked = new TableColumn<>("Booked");
        colBooked.setPrefWidth(100);
        colBooked.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalBookings() + " Sessions"));
        colBooked.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().addAll("report-status-pill", "pill-info");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        // Status Column
        TableColumn<PTClientModel, String> colClientStatus = new TableColumn<>("Status");
        colClientStatus.setPrefWidth(100);
        colClientStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colClientStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().addAll("report-status-pill", "pill-success");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        // Action Column
        TableColumn<PTClientModel, PTClientModel> colClientAction = new TableColumn<>("Action");
        colClientAction.setPrefWidth(110);
        colClientAction.setSortable(false);
        colClientAction.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colClientAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PTClientModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Button btn = new Button("Client Profile");
                    btn.getStyleClass().add("report-action-btn-sm");
                    btn.setOnAction(e -> showClientDetailDialog(item));
                    setGraphic(btn);
                    setText(null);
                }
            }
        });
        
        tableClients.getColumns().addAll(List.of(colClient, colTrainer, colPlan, colFreq, colDiet, colBooked, colClientStatus, colClientAction));
        
        // TABLE 2: 1-on-1 Sessions Log
        TableView<Booking> tableSessions = createPremiumTable();
        tableSessions.setItems(sessionsFiltered);
        
        TableColumn<Booking, Booking> colSessClient = new TableColumn<>("Client");
        colSessClient.setPrefWidth(200);
        colSessClient.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSessClient.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Member m = item.getMember();
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("report-avatar-mini");
                    avatar.setStyle(getAvatarGradient(m.getName()));
                    Label ini = new Label(getInitials(m.getName()));
                    ini.getStyleClass().add("report-avatar-mini-text");
                    avatar.getChildren().add(ini);
                    
                    VBox text = new VBox(2);
                    text.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(m.getName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    Label idLbl = new Label(m.getUserId());
                    idLbl.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");
                    text.getChildren().addAll(nameLbl, idLbl);
                    
                    box.getChildren().addAll(avatar, text);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        TableColumn<Booking, Booking> colSessTrainer = new TableColumn<>("Dedicated Trainer");
        colSessTrainer.setPrefWidth(180);
        colSessTrainer.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSessTrainer.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    FitnessSession fs = item.getSession();
                    String coach = fs.getInstructor() != null ? fs.getInstructor().getName() : "Coach";
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label("👤 " + coach);
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    String spec = fs.getInstructor() != null && fs.getInstructor().getSpecialization() != null
                            ? fs.getInstructor().getSpecialization() : "PT Specialist";
                    Label specLbl = new Label(spec);
                    specLbl.setStyle("-fx-opacity: 0.6; -fx-font-size: 11px;");
                    box.getChildren().addAll(nameLbl, specLbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        TableColumn<Booking, String> colSessName = new TableColumn<>("Session");
        colSessName.setPrefWidth(180);
        colSessName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSession().getName()));
        colSessName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        
        TableColumn<Booking, Booking> colSessSchedule = new TableColumn<>("Scheduled Date & Time");
        colSessSchedule.setPrefWidth(180);
        colSessSchedule.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSessSchedule.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    
                    String dateStr = item.getSessionDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
                    Label dateLbl = new Label("🗓 " + dateStr);
                    dateLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 12px;");
                    
                    FitnessSession fs = item.getSession();
                    String timeStr = (fs.getStartTime() != null ? fs.getStartTime().toString() : "10:00")
                            + " - " + (fs.getEndTime() != null ? fs.getEndTime().toString() : "11:00");
                    Label timeLbl = new Label("⏰ " + timeStr);
                    timeLbl.setStyle("-fx-opacity: 0.65; -fx-font-size: 11px;");
                    
                    box.getChildren().addAll(dateLbl, timeLbl);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        
        TableColumn<Booking, String> colSessLoc = new TableColumn<>("Location");
        colSessLoc.setPrefWidth(140);
        colSessLoc.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getSession().getLocation() != null ? data.getValue().getSession().getLocation().getDisplayName() : "Cardio Zone"
        ));
        colSessLoc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label("📍 " + item);
                    lbl.getStyleClass().add("report-location-tag");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        
        TableColumn<Booking, String> colSessStatus = new TableColumn<>("Status");
        colSessStatus.setPrefWidth(110);
        colSessStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colSessStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.getStyleClass().addAll("report-status-pill", "pill-success");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });
        
        TableColumn<Booking, Booking> colSessAction = new TableColumn<>("Action");
        colSessAction.setPrefWidth(110);
        colSessAction.setSortable(false);
        colSessAction.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSessAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Button btn = new Button("Quick Info");
                    btn.getStyleClass().add("report-action-btn-sm");
                    btn.setOnAction(e -> showBookingDetailDialog(item));
                    setGraphic(btn);
                    setText(null);
                }
            }
        });
        
        tableSessions.getColumns().addAll(List.of(colSessClient, colSessTrainer, colSessName, colSessSchedule, colSessLoc, colSessStatus, colSessAction));
        
        VBox tableCardClients = wrapInTableCard(tableClients);
        VBox tableCardSessions = wrapInTableCard(tableSessions);
        tableCardSessions.setVisible(false);
        tableCardSessions.setManaged(false);
        
        StackPane tablesStack = new StackPane(tableCardClients, tableCardSessions);
        VBox.setVgrow(tablesStack, Priority.ALWAYS);
        
        // Interactive Filter Bar for PT
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(14, 18, 14, 18));
        filterBar.getStyleClass().add("report-filter-bar");
        
        // Trainer Selector
        Set<String> trainerNames = new TreeSet<>();
        trainerNames.add("All Trainers");
        for (Instructor i : instructors) {
            trainerNames.add(i.getName());
        }
        ComboBox<String> trainerFilter = new ComboBox<>();
        trainerFilter.getItems().addAll(trainerNames);
        trainerFilter.setValue("All Trainers");
        trainerFilter.getStyleClass().add("report-date-combo");
        
        // Search Box
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("report-search-box");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 13px; -fx-opacity: 0.6;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search PT clients, trainers, plans...");
        searchField.getStyleClass().add("report-search-field");
        searchField.setPrefWidth(240);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);
        
        Label countBadge = new Label(ptClients.size() + " PT Clients");
        countBadge.getStyleClass().add("report-count-badge");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button exportBtn = new Button("📥  Export CSV");
        exportBtn.getStyleClass().add("report-export-btn");
        exportBtn.setOnAction(e -> {
            if (tableCardClients.isVisible()) {
                exportToCsv(tableClients, "PT_Clients_Report");
            } else {
                exportToCsv(tableSessions, "PT_Sessions_Log");
            }
        });
        
        filterBar.getChildren().addAll(trainerFilter, searchBox, countBadge, spacer, exportBtn);
        
        // Dynamic Filter Updater for both views
        Runnable updatePtPredicate = () -> {
            String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String selTrainer = trainerFilter.getValue();
            
            // Filter clients table
            clientsFiltered.setPredicate(client -> {
                if (selTrainer != null && !"All Trainers".equals(selTrainer)) {
                    if (!selTrainer.equalsIgnoreCase(client.getInstructor().getName())) return false;
                }
                if (!q.isEmpty()) {
                    String cName = client.getMember().getName().toLowerCase();
                    String cId = client.getMember().getUserId().toLowerCase();
                    String tName = client.getInstructor().getName().toLowerCase();
                    String pTitle = client.getPlan().getTitle().toLowerCase();
                    return cName.contains(q) || cId.contains(q) || tName.contains(q) || pTitle.contains(q);
                }
                return true;
            });
            
            // Filter sessions table
            sessionsFiltered.setPredicate(booking -> {
                if (selTrainer != null && !"All Trainers".equals(selTrainer)) {
                    String coach = booking.getSession().getInstructor() != null ? booking.getSession().getInstructor().getName() : "";
                    if (!selTrainer.equalsIgnoreCase(coach)) return false;
                }
                if (!q.isEmpty()) {
                    String cName = booking.getMember().getName().toLowerCase();
                    String cId = booking.getMember().getUserId().toLowerCase();
                    String sName = booking.getSession().getName().toLowerCase();
                    String coach = booking.getSession().getInstructor() != null ? booking.getSession().getInstructor().getName().toLowerCase() : "";
                    return cName.contains(q) || cId.contains(q) || sName.contains(q) || coach.contains(q);
                }
                return true;
            });
            
            if (tableCardClients.isVisible()) {
                countBadge.setText(clientsFiltered.size() + " PT Clients");
            } else {
                countBadge.setText(sessionsFiltered.size() + " PT Sessions");
            }
        };
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePtPredicate.run());
        trainerFilter.valueProperty().addListener((obs, oldVal, newVal) -> updatePtPredicate.run());
        
        // Mode Switcher handlers
        btnClients.setOnAction(e -> {
            if (!tableCardClients.isVisible()) {
                btnClients.getStyleClass().add("report-view-toggle-btn-active");
                btnSessions.getStyleClass().remove("report-view-toggle-btn-active");
                
                tableCardSessions.setVisible(false);
                tableCardSessions.setManaged(false);
                
                tableCardClients.setVisible(true);
                tableCardClients.setManaged(true);
                
                FadeTransition ft = new FadeTransition(Duration.millis(250), tableCardClients);
                ft.setFromValue(0.4);
                ft.setToValue(1.0);
                ft.play();
                
                countBadge.setText(clientsFiltered.size() + " PT Clients");
            }
        });
        
        btnSessions.setOnAction(e -> {
            if (!tableCardSessions.isVisible()) {
                btnSessions.getStyleClass().add("report-view-toggle-btn-active");
                btnClients.getStyleClass().remove("report-view-toggle-btn-active");
                
                tableCardClients.setVisible(false);
                tableCardClients.setManaged(false);
                
                tableCardSessions.setVisible(true);
                tableCardSessions.setManaged(true);
                
                FadeTransition ft = new FadeTransition(Duration.millis(250), tableCardSessions);
                ft.setFromValue(0.4);
                ft.setToValue(1.0);
                ft.play();
                
                countBadge.setText(sessionsFiltered.size() + " PT Sessions");
            }
        });
        
        container.getChildren().addAll(header, stats, switchBar, filterBar, tablesStack);
        VBox.setVgrow(tablesStack, Priority.ALWAYS);
        return container;
    }

    // =========================================================================
    // UTILITY METHODS & DIALOGS
    // =========================================================================
    
    private void showBookingDetailDialog(Booking booking) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Booking Record — " + booking.getBookingId());
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(460);
        
        VBox content = new VBox(14);
        content.setPadding(new Insets(18));
        
        HBox userHeader = new HBox(14);
        userHeader.setAlignment(Pos.CENTER_LEFT);
        
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("report-avatar-mini");
        avatar.setPrefSize(42, 42);
        avatar.setMinSize(42, 42);
        avatar.setMaxSize(42, 42);
        avatar.setStyle(getAvatarGradient(booking.getMember().getName()));
        Label ini = new Label(getInitials(booking.getMember().getName()));
        ini.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 14px;");
        avatar.getChildren().add(ini);
        
        VBox userText = new VBox(2);
        Label nameLbl = new Label(booking.getMember().getName());
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");
        Label subLbl = new Label(booking.getMember().getUserId() + " • " + booking.getMember().getEmail());
        subLbl.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");
        userText.getChildren().addAll(nameLbl, subLbl);
        userHeader.getChildren().addAll(avatar, userText);
        
        Separator sep = new Separator();
        
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        
        addDetailRow(grid, 0, "Booking ID:", booking.getBookingId());
        addDetailRow(grid, 1, "Status:", booking.getStatus().getDisplayName());
        addDetailRow(grid, 2, "Session:", booking.getSession().getName());
        addDetailRow(grid, 3, "Activity Type:", booking.getSession().getSessionType());
        String coach = booking.getSession().getInstructor() != null ? booking.getSession().getInstructor().getName() : "Lead Trainer";
        addDetailRow(grid, 4, "Instructor:", coach);
        String loc = booking.getSession().getLocation() != null ? booking.getSession().getLocation().getDisplayName() : "Studio";
        addDetailRow(grid, 5, "Studio / Zone:", loc);
        addDetailRow(grid, 6, "Scheduled Date:", booking.getSessionDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        String time = (booking.getSession().getStartTime() != null ? booking.getSession().getStartTime().toString() : "07:00")
                + " - " + (booking.getSession().getEndTime() != null ? booking.getSession().getEndTime().toString() : "08:00");
        addDetailRow(grid, 7, "Time Slot:", time);
        addDetailRow(grid, 8, "Contact Phone:", booking.getMember().getPhone());
        
        content.getChildren().addAll(userHeader, sep, grid);
        pane.setContent(content);
        dialog.showAndWait();
    }
    
    private void showClientDetailDialog(PTClientModel client) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Personal Training Profile — " + client.getMember().getName());
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStyleClass().add("report-table-card");
        pane.setPrefWidth(480);
        
        VBox content = new VBox(14);
        content.setPadding(new Insets(18));
        
        HBox userHeader = new HBox(14);
        userHeader.setAlignment(Pos.CENTER_LEFT);
        
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("report-avatar-mini");
        avatar.setPrefSize(44, 44);
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.setStyle(getAvatarGradient(client.getMember().getName()));
        Label ini = new Label(getInitials(client.getMember().getName()));
        ini.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 15px;");
        avatar.getChildren().add(ini);
        
        VBox userText = new VBox(2);
        Label nameLbl = new Label(client.getMember().getName());
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");
        Label subLbl = new Label(client.getMember().getUserId() + " • " + client.getMember().getEmail());
        subLbl.setStyle("-fx-opacity: 0.6; -fx-font-size: 12px;");
        userText.getChildren().addAll(nameLbl, subLbl);
        userHeader.getChildren().addAll(avatar, userText);
        
        Separator sep = new Separator();
        
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        
        addDetailRow(grid, 0, "Assigned Trainer:", client.getInstructor().getName() + " (" + client.getInstructor().getSpecialization() + ")");
        addDetailRow(grid, 1, "Enrolled PT Plan:", client.getPlan().getTitle());
        addDetailRow(grid, 2, "Plan Objectives:", String.join(", ", client.getPlan().getObjectives()));
        addDetailRow(grid, 3, "Duration:", client.getPlan().getDurationWeeks() + " Weeks");
        addDetailRow(grid, 4, "1-on-1 Frequency:", client.getSessionsPerWeek() + " Sessions / Week");
        addDetailRow(grid, 5, "Dietary Plan:", client.isIncludesDiet() ? "Custom Nutrition Included" : "Standard Advice");
        addDetailRow(grid, 6, "Total PT Sessions:", client.getTotalBookings() + " Bookings");
        addDetailRow(grid, 7, "Client Status:", client.getStatus());
        addDetailRow(grid, 8, "Contact Phone:", client.getMember().getPhone());
        
        content.getChildren().addAll(userHeader, sep, grid);
        pane.setContent(content);
        dialog.showAndWait();
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: 700; -fx-opacity: 0.7; -fx-font-size: 12px;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
    
    private String getSessionPillClass(String type) {
        if (type == null) return "pill-recovery";
        String t = type.toLowerCase();
        if (t.contains("hiit")) return "pill-hiit";
        if (t.contains("yoga")) return "pill-yoga";
        if (t.contains("spin")) return "pill-spin";
        if (t.contains("crossfit")) return "pill-crossfit";
        if (t.contains("pilates")) return "pill-pilates";
        if (t.contains("personal") || t.contains("pt")) return "pill-pt";
        if (t.contains("zumba")) return "pill-zumba";
        return "pill-recovery";
    }
    
    private String getAvatarGradient(String name) {
        if (name == null || name.isEmpty()) name = "User";
        int hash = Math.abs(name.hashCode());
        String[] gradients = {
            "-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6);",
            "-fx-background-color: linear-gradient(to bottom right, #ec4899, #f43f5e);",
            "-fx-background-color: linear-gradient(to bottom right, #10b981, #059669);",
            "-fx-background-color: linear-gradient(to bottom right, #3b82f6, #06b6d4);",
            "-fx-background-color: linear-gradient(to bottom right, #f59e0b, #ea580c);",
            "-fx-background-color: linear-gradient(to bottom right, #8b5cf6, #d946ef);"
        };
        return gradients[hash % gradients.length];
    }
    
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }
}

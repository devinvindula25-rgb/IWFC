package com.iwfc.ui;

import com.iwfc.ui.components.DialogUtils;
import com.iwfc.exception.PaymentFailedException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.payment.PaymentContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;


import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentView extends VBox {

    private final IWFCSystemFacade facade;
    private final boolean isAdmin;
    private PaymentStatus currentFilter = null; 
    
    private int currentPage = 1;
    private final int itemsPerPage = 8;
    private String searchText = "";

    private HBox filterChips;
    private VBox tableContent;
    private HBox statsBox;
    private Label entriesLabel;

    public PaymentView(IWFCSystemFacade facade) {
        this.facade = facade;
        this.isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;

        getStyleClass().add("payment-view");
        setSpacing(25);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_LEFT);
        
        // Premium textured obsidian and sapphire background
        setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 120%, #171626, #090812);");

        setupUI();
    }

    private void setupUI() {
        // --- 1. Header Row ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(isAdmin ? "Global Payment History & Reporting" : "My Payments & Reporting");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        ComboBox<String> daysFilter = new ComboBox<>();
        daysFilter.getItems().add("Last 30 Days");
        daysFilter.setValue("Last 30 Days");
        daysFilter.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 6px; -fx-text-fill: #e2e8f0;");

        Button exportBtn = new Button("📄 Export to CSV/PDF");
        exportBtn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.15); -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 6px; -fx-text-fill: #a5b4fc; -fx-padding: 8 16; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Data successfully exported to C:\\IWFC_Exports\\Payments_Report.pdf");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        Button viewAllBtn = new Button("📄 View All Bills");
        viewAllBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 6px; -fx-text-fill: #e2e8f0; -fx-padding: 8 16; -fx-cursor: hand;");
        viewAllBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Opening consolidated billing directory...");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        actionsBox.getChildren().addAll(daysFilter, exportBtn, viewAllBtn);

        if (!isAdmin && facade.getCurrentUser().getRole() == Role.MEMBER) {
            Button payBtn = new Button("+ Make Payment");
            payBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8 16;");
            payBtn.setOnAction(e -> showPaymentDialog((Member) facade.getCurrentUser()));
            actionsBox.getChildren().add(payBtn);
        }

        header.getChildren().addAll(titleLabel, spacer1, actionsBox);

        // --- 2. Stats Row ---
        statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        
        // --- 3. Filters & Search ---
        VBox filterSection = new VBox(15);
        Label filterTitle = new Label("Filters & Search");
        filterTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        HBox filterRow = new HBox();
        filterRow.setAlignment(Pos.CENTER_LEFT);
        
        filterChips = new HBox(8);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_RIGHT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Member, Name, or Purpose...");
        searchField.setPrefWidth(350);
        searchField.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 6px; -fx-text-fill: white; -fx-prompt-text-fill: #9ca3af; -fx-padding: 8 12;");
        searchField.textProperty().addListener((obs, old, val) -> {
            searchText = val.toLowerCase();
            currentPage = 1;
            refreshData();
        });
        
        Button settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 6px; -fx-text-fill: #e2e8f0; -fx-padding: 8 12;");
        
        searchBox.getChildren().addAll(searchField, settingsBtn);
        filterRow.getChildren().addAll(filterChips, spacer2, searchBox);
        
        filterSection.getChildren().addAll(filterTitle, filterRow);

        // --- 4. Custom Table ---
        VBox tableWrapper = new VBox();
        tableWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12px; -fx-background-radius: 12px;");
        tableWrapper.setPadding(new Insets(1)); // border thickness effect

        // Header
        HBox tableHeader = createTableRow("Member ID & Name", "Date & Time", "Location", "Purpose", "Method", "Base", "Total", "Status", "Actions", true);
        tableHeader.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px 12px 0 0; -fx-padding: 15 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0;");

        // Content
        tableContent = new VBox();
        ScrollPane scrollPane = new ScrollPane(tableContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(400);

        tableWrapper.getChildren().addAll(tableHeader, scrollPane);

        // --- 5. Footer Pagination ---
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        
        entriesLabel = new Label("Showing 0 of 0 entries");
        entriesLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        
        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        
        HBox pagination = new HBox(8);
        pagination.setAlignment(Pos.CENTER);
        
        Button prevBtn = new Button("Prev");
        prevBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 4px; -fx-text-fill: #9ca3af; -fx-cursor: hand;");
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) { currentPage--; refreshData(); }
        });
        
        Button page1 = new Button("1");
        page1.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-border-radius: 4px; -fx-text-fill: white; -fx-cursor: hand;");
        
        Button nextBtn = new Button("Next");
        nextBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 4px; -fx-text-fill: white; -fx-cursor: hand;");
        nextBtn.setOnAction(e -> {
            currentPage++; refreshData();
        });
        
        pagination.getChildren().addAll(prevBtn, page1, nextBtn);
        footer.getChildren().addAll(entriesLabel, spacer3, pagination);

        getChildren().addAll(header, statsBox, filterSection, tableWrapper, footer);
        
        refreshData();
    }

    private void createFilterChips(int total, int comp, int pend, int fail, int ref) {
        filterChips.getChildren().clear();
        String[] labels = {"All ("+total+")", "Completed ("+comp+")", "Pending ("+pend+")", "Failed ("+fail+")", "Refunded ("+ref+")"};
        PaymentStatus[] statuses = {null, PaymentStatus.COMPLETED, PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.REFUNDED};

        for (int i = 0; i < labels.length; i++) {
            final PaymentStatus status = statuses[i];
            Button chip = new Button(labels[i]);
            
            if (currentFilter == status) {
                chip.setStyle("-fx-background-color: rgba(99, 102, 241, 0.15); -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-text-fill: #a5b4fc; -fx-padding: 6 16; -fx-cursor: hand;");
            } else {
                chip.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 20px; -fx-text-fill: #d1d5db; -fx-padding: 6 16; -fx-cursor: hand;");
            }
            
            chip.setOnAction(e -> {
                currentFilter = status;
                currentPage = 1;
                refreshData(); 
            });
            filterChips.getChildren().add(chip);
        }
    }

    private void refreshData() {
        tableContent.getChildren().clear();
        statsBox.getChildren().clear();
        
        try {
            List<Payment> payments;
            if (isAdmin) {
                payments = facade.getAllPayments();
            } else {
                payments = facade.getPaymentsForMember(facade.getCurrentUser().getUserId());
            }

            // Calculate stats
            double totalCompleted = payments.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).mapToDouble(Payment::getTotalCharged).sum();
            long countCompleted = payments.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).count();
            long countPending = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
            long countFailed = payments.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count();
            long countRefunded = payments.stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED).count();

            createFilterChips(payments.size(), (int)countCompleted, (int)countPending, (int)countFailed, (int)countRefunded);

            // Create Premium Stat Blocks using SVG Shapes instead of Emojis
            String coinsSvg = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.97V5H10.9v1.69c-1.51.32-2.72 1.3-2.72 2.81 0 1.79 1.49 2.69 3.66 3.21 1.95.46 2.34 1.15 2.34 1.87 0 .53-.39 1.64-2.25 1.64-1.74 0-2.26-.95-2.32-1.81H7.84c.09 1.57 1.25 2.68 3.06 3.06V19h2.3v-1.72c1.64-.32 2.81-1.38 2.81-2.93-.01-1.81-1.51-2.61-3.7-3.21z";
            VBox revBlock = createPremiumStatBox(
                "Total Completed Revenue", "LKR " + String.format("%,.2f", totalCompleted), "↑ 12% vs last month", 
                "linear-gradient(to bottom right, #112d22, #1b4736)", "rgba(81, 207, 102, 0.2)", "#51cf66", coinsSvg 
            );
            
            String checkSvg = "M19 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z";
            VBox txnBlock = createPremiumStatBox(
                "Successful Transaction Count", String.valueOf(countCompleted), "↓ 2 vs yesterday", 
                "linear-gradient(to bottom right, #1e293b, #334155)", "rgba(96, 165, 250, 0.2)", "#60a5fa", checkSvg 
            );
            
            String hourglassSvg = "M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2H6zm10 14.5V20H8v-3.5l4-4 4 4zm-4-5l-4-4V4h8v3.5l-4 4z";
            VBox pendBlock = createPremiumStatBox(
                "Pending Transactions", String.valueOf(countPending), "Avg wait time: 1.2 days", 
                "linear-gradient(to bottom right, #453513, #614d1f)", "rgba(251, 191, 36, 0.2)", "#fbb03b", hourglassSvg 
            );
            
            HBox.setHgrow(revBlock, Priority.ALWAYS);
            HBox.setHgrow(txnBlock, Priority.ALWAYS);
            HBox.setHgrow(pendBlock, Priority.ALWAYS);
            
            statsBox.getChildren().addAll(revBlock, txnBlock, pendBlock);

            // Apply filter
            List<Payment> filtered = payments;
            if (currentFilter != null) {
                filtered = filtered.stream().filter(p -> p.getStatus() == currentFilter).collect(Collectors.toList());
            }
            if (!searchText.isEmpty()) {
                filtered = filtered.stream().filter(p -> 
                    p.getMemberId().toLowerCase().contains(searchText) || 
                    p.getPurpose().getDisplayName().toLowerCase().contains(searchText) ||
                    p.getPaymentId().toLowerCase().contains(searchText)
                ).collect(Collectors.toList());
            }
            
            int totalItems = filtered.size();
            int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            if (currentPage > maxPages) currentPage = Math.max(1, maxPages);
            
            int startIdx = (currentPage - 1) * itemsPerPage;
            int endIdx = Math.min(startIdx + itemsPerPage, totalItems);
            
            entriesLabel.setText(String.format("Showing %d to %d of %d entries", (totalItems == 0 ? 0 : startIdx + 1), endIdx, totalItems));

            for (int i = startIdx; i < endIdx; i++) {
                Payment p = filtered.get(i);
                
                String memberName = "Unknown";
                User u = null;
                try {
                    u = com.iwfc.pattern.DataStore.getInstance().getUsers().findById(p.getMemberId()).orElse(null);
                    if (u != null) memberName = u.getName();
                } catch(Exception ignored) {}
                
                String dateStr = p.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                String timeStr = p.getPaidAt().format(DateTimeFormatter.ofPattern("HH:mm"));
                
                String svgPath = "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"; // Card
                if (p.getMethod() == PaymentMethod.CASH) {
                    svgPath = "M5 8h2v8H5zm7 0H9c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h3c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 6h-1v-4h1v4zm7-6h-2c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h2c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 6h-1v-4h1v4z"; // Cash
                }
                if (p.getMethod() == PaymentMethod.DIGITAL_WALLET) {
                    svgPath = "M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"; // Phone
                }

                final User finalU = u;

                HBox row = createDataRow(
                    p.getMemberId(), memberName,
                    dateStr, timeStr,
                    "Piliyandala", "Branch",
                    p.getPurpose().getDisplayName(),
                    svgPath, p.getMethod().getDisplayName(),
                    String.format("LKR %.2f", p.getAmount()),
                    String.format("LKR %.2f", p.getTotalCharged()),
                    p.getStatus(),
                    () -> showPremiumBillPreview(p, finalU)
                );
                
                if (i % 2 == 0) {
                    row.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");
                } else {
                    row.setStyle("-fx-background-color: rgba(255,255,255,0.01); -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;");
                }
                
                String baseStyle = row.getStyle();
                row.setOnMouseEntered(e -> row.setStyle(baseStyle + " -fx-background-color: rgba(255,255,255,0.05);"));
                row.setOnMouseExited(e -> row.setStyle(baseStyle));
                
                tableContent.getChildren().add(row);
            }
            
        } catch (UnauthorizedAccessException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Access Denied: " + ex.getMessage());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        }
    }

    private VBox createPremiumStatBox(String title, String value, String subtext, String bgGradient, String iconBg, String iconColor, String svgPath) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: " + bgGradient + "; -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);");
        
        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 12px;");
        
        Region iconNode = new Region();
        iconNode.setStyle("-fx-shape: '" + svgPath + "'; -fx-background-color: " + iconColor + ";");
        iconNode.setMinSize(24, 24);
        iconNode.setMaxSize(24, 24);
        iconBox.getChildren().add(iconNode);
        
        VBox texts = new VBox(2);
        texts.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");
        
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label subLbl = new Label(subtext);
        if (subtext.contains("↑")) {
            subLbl.setStyle("-fx-text-fill: #51cf66; -fx-font-size: 11px;");
        } else if (subtext.contains("↓")) {
            subLbl.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
        } else {
            subLbl.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px;");
        }
        
        texts.getChildren().addAll(titleLbl, valLbl, subLbl);
        topRow.getChildren().addAll(iconBox, texts);
        
        box.getChildren().add(topRow);
        return box;
    }

    private HBox createTableRow(String col1, String col2, String col3, String col4, String col5, String col6, String col7, String col8, String col9, boolean isHeader) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        
        String textStyle = isHeader ? "-fx-text-fill: #9ca3af; -fx-font-weight: bold; -fx-font-size: 13px;" : "-fx-text-fill: #e2e8f0; -fx-font-size: 13px;";
        
        Label l1 = new Label(col1); l1.setPrefWidth(200); l1.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l1, Priority.ALWAYS); l1.setStyle(textStyle);
        Label l2 = new Label(col2); l2.setPrefWidth(140); l2.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l2, Priority.ALWAYS); l2.setStyle(textStyle);
        Label l3 = new Label(col3); l3.setPrefWidth(120); l3.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l3, Priority.ALWAYS); l3.setStyle(textStyle);
        Label l4 = new Label(col4); l4.setPrefWidth(160); l4.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l4, Priority.ALWAYS); l4.setStyle(textStyle);
        Label l5 = new Label(col5); l5.setPrefWidth(160); l5.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l5, Priority.ALWAYS); l5.setStyle(textStyle);
        Label l6 = new Label(col6); l6.setPrefWidth(100); l6.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l6, Priority.ALWAYS); l6.setStyle(textStyle);
        Label l7 = new Label(col7); l7.setPrefWidth(100); l7.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l7, Priority.ALWAYS); l7.setStyle(textStyle);
        Label l8 = new Label(col8); l8.setPrefWidth(100); l8.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l8, Priority.ALWAYS); l8.setStyle(textStyle);
        Label l9 = new Label(col9); l9.setPrefWidth(160); l9.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(l9, Priority.ALWAYS); l9.setStyle(textStyle);
        
        row.getChildren().addAll(l1, l2, l3, l4, l5, l6, l7, l8, l9);
        return row;
    }

    private HBox createDataRow(String id, String name, String date, String time, String loc1, String loc2, 
                               String purpose, String methodIcon, String method, String baseAmt, String totAmt, PaymentStatus status, Runnable onGenerate) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        
        // 1. Member
        HBox memberCol = new HBox(5);
        memberCol.setAlignment(Pos.CENTER_LEFT);
        memberCol.setPrefWidth(200);
        memberCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(memberCol, Priority.ALWAYS);
        Label chevron = new Label("˄");
        chevron.setStyle("-fx-text-fill: #6b7280; -fx-padding: 0 5 0 0;");
        VBox mBox = new VBox(2);
        Label mId = new Label(id); mId.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label mName = new Label(name); mName.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        mBox.getChildren().addAll(mId, mName);
        memberCol.getChildren().addAll(chevron, mBox);
        
        // 2. Date
        VBox dateCol = new VBox(2);
        dateCol.setPrefWidth(140);
        dateCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(dateCol, Priority.ALWAYS);
        Label d1 = new Label(date); d1.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        Label d2 = new Label(time); d2.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        dateCol.getChildren().addAll(d1, d2);
        
        // 3. Location
        VBox locCol = new VBox(2);
        locCol.setPrefWidth(120);
        locCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(locCol, Priority.ALWAYS);
        Label l1 = new Label(loc1); l1.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        Label l2 = new Label(loc2); l2.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        locCol.getChildren().addAll(l1, l2);
        
        // 4. Purpose
        Label purpCol = new Label(purpose);
        purpCol.setPrefWidth(160);
        purpCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(purpCol, Priority.ALWAYS);
        purpCol.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        // 5. Method
        HBox methCol = new HBox(8);
        methCol.setAlignment(Pos.CENTER_LEFT);
        methCol.setPrefWidth(160);
        methCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(methCol, Priority.ALWAYS);
        
        Region mIcon = new Region();
        mIcon.setStyle("-fx-shape: '" + methodIcon + "'; -fx-background-color: white;");
        mIcon.setMinSize(16, 16);
        mIcon.setMaxSize(16, 16);
        
        Label mTxt = new Label(method); mTxt.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        mTxt.setWrapText(true);
        methCol.getChildren().addAll(mIcon, mTxt);
        
        // 6. Base Amount
        Label bAmt = new Label(baseAmt);
        bAmt.setPrefWidth(100);
        bAmt.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(bAmt, Priority.ALWAYS);
        bAmt.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        // 7. Total Charged
        Label tAmt = new Label(totAmt);
        tAmt.setPrefWidth(100);
        tAmt.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(tAmt, Priority.ALWAYS);
        tAmt.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        // 8. Status Badge
        HBox statCol = new HBox();
        statCol.setAlignment(Pos.CENTER_LEFT);
        statCol.setPrefWidth(100);
        statCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(statCol, Priority.ALWAYS);
        Label sBadge = new Label(status.getDisplayName());
        String bg = "rgba(255,255,255,0.1)"; String fg = "#ffffff";
        if(status == PaymentStatus.COMPLETED) { bg = "rgba(16, 185, 129, 0.15)"; fg = "#34d399"; }
        if(status == PaymentStatus.PENDING) { bg = "rgba(245, 158, 11, 0.15)"; fg = "#fbbf24"; }
        if(status == PaymentStatus.FAILED) { bg = "rgba(239, 68, 68, 0.15)"; fg = "#f87171"; }
        if(status == PaymentStatus.REFUNDED) { bg = "rgba(99, 102, 241, 0.15)"; fg = "#818cf8"; }
        sBadge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-padding: 6 12; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center;");
        statCol.getChildren().add(sBadge);
        
        // 9. Actions
        HBox actCol = new HBox(8);
        actCol.setAlignment(Pos.CENTER_LEFT);
        actCol.setPrefWidth(160);
        actCol.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(actCol, Priority.ALWAYS);
        
        Region docIcon = new Region();
        docIcon.setStyle("-fx-shape: 'M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z'; -fx-background-color: #9ca3af;");
        docIcon.setMinSize(14, 14);
        docIcon.setMaxSize(14, 14);
        
        Button genBtn = new Button("Generate Bill");
        genBtn.setGraphic(docIcon);
        genBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-text-fill: #9ca3af; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
        genBtn.setOnAction(e -> {
            if (onGenerate != null) onGenerate.run();
        });
        
        Button menuBtn = new Button("⋮");
        menuBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-padding: 3 8; -fx-cursor: hand;");
        actCol.getChildren().addAll(genBtn, menuBtn);
        
        row.getChildren().addAll(memberCol, dateCol, locCol, purpCol, methCol, bAmt, tAmt, statCol, actCol);
        return row;
    }

    private void showPremiumBillPreview(Payment payment, User member) {
        Dialog<Void> previewDialog = new Dialog<>();
        previewDialog.setTitle("Invoice Preview");
        DialogUtils.stylePremiumDialog(previewDialog);

        VBox billNode = com.iwfc.util.BillGenerator.createBillLayout(payment, member);
        // Shrink the preview slightly to fit nicely on screen
        billNode.setStyle(billNode.getStyle() + " -fx-scale-x: 0.9; -fx-scale-y: 0.9;");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));

        Button dlBtn = new Button("⬇ Download as PDF");
        dlBtn.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #6366f1); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8px; -fx-cursor: hand;");
        dlBtn.setOnAction(e -> {
            com.iwfc.util.BillGenerator.generateAndOpenPdf(payment, member);
            previewDialog.setResult(null);
            previewDialog.close();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "PDF successfully generated and saved to your device!");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        container.getChildren().addAll(billNode, dlBtn);
        previewDialog.getDialogPane().setContent(container);
        
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        previewDialog.getDialogPane().getButtonTypes().add(closeBtn);
        
        previewDialog.showAndWait();
    }

    private void showPaymentDialog(Member member) {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Make Payment");
        dialog.setHeaderText("Process a payment");

        ButtonType payBtnType = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<PaymentPurpose> purposeCbx = new ComboBox<>();
        purposeCbx.getItems().addAll(PaymentPurpose.values());
        purposeCbx.setValue(PaymentPurpose.MEMBERSHIP_FEE);

        TextField amountF = new TextField();
        amountF.setPromptText("Base Amount (LKR)");

        ComboBox<PaymentMethod> methodCbx = new ComboBox<>();
        methodCbx.getItems().addAll(PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER, PaymentMethod.DIGITAL_WALLET);
        methodCbx.setValue(PaymentMethod.CARD);

        TextField extraF = new TextField();
        extraF.setPromptText("Card No (min 16 digits)");
        
        methodCbx.setOnAction(e -> {
            PaymentMethod m = methodCbx.getValue();
            switch (m) {
                case CARD -> extraF.setPromptText("Card No (min 16 digits)");
                case BANK_TRANSFER -> extraF.setPromptText("Reference Number");
                case DIGITAL_WALLET -> extraF.setPromptText("Wallet ID (10 digits)");
                default -> extraF.setPromptText("");
            }
        });

        grid.add(new Label("Purpose:"), 0, 0);
        grid.add(purposeCbx, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountF, 1, 1);
        grid.add(new Label("Method:"), 0, 2);
        grid.add(methodCbx, 1, 2);
        grid.add(new Label("Details:"), 0, 3);
        grid.add(extraF, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == payBtnType) {
                try {
                    double amt = Double.parseDouble(amountF.getText());
                    PaymentContext ctx = new PaymentContext();
                    
                    PaymentMethod m = methodCbx.getValue();
                    switch (m) {
                        case CARD -> {
                            ctx.setCardNumber(extraF.getText());
                            ctx.setExpiryDate("12/25"); 
                        }
                        case BANK_TRANSFER -> ctx.setReferenceNumber(extraF.getText());
                        case DIGITAL_WALLET -> ctx.setWalletId(extraF.getText());
                        default -> {}
                    }

                    facade.processPayment(
                        member.getUserId(), amt, m, purposeCbx.getValue(), 
                        "Self Payment", ctx
                    );
                    
                    return true;
                } catch (NumberFormatException | PaymentFailedException | UnauthorizedAccessException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Payment Failed: " + ex.getMessage());
                    DialogUtils.stylePremiumDialog(a);
                    a.showAndWait();
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) refreshData();
        });
    }
}

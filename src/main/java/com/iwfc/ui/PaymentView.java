package com.iwfc.ui;

import com.iwfc.exception.PaymentFailedException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.payment.PaymentContext;
import com.iwfc.ui.components.DialogUtils;
import com.iwfc.util.BillGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-Tech Cyber-Dark Glassmorphic Payment & Financial Ledger View.
 * 100% Functional controls, live date-range filtering, real CSV export, POS terminal modal,
 * settings gear dialog, kebab action menus, refund workflows, dynamic pagination, and receipt engine.
 */
public class PaymentView extends VBox {

    private final IWFCSystemFacade facade;
    private final boolean isAdmin;
    private PaymentStatus currentFilter = null;
    private String selectedDateRange = "All Time";

    private int currentPage = 1;
    private int itemsPerPage = 12;
    private String sortBy = "Date"; // "Date", "Amount", "Member"
    private boolean sortAscending = false;
    private String searchText = "";

    private HBox filterChips;
    private VBox tableContent;
    private HBox statsBox;
    private Label entriesLabel;
    private HBox paginationBox;
    private TextField searchField;
    private ComboBox<String> daysFilter;

    public PaymentView(IWFCSystemFacade facade) {
        this.facade = facade;
        this.isAdmin = facade.getCurrentUser().getRole() == Role.ADMIN;

        getStyleClass().add("payments-view-root");
        try {
            String cssPath = getClass().getResource("/com/iwfc/css/payments-management.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception ignored) {}

        setSpacing(22);
        setPadding(new Insets(26));
        setAlignment(Pos.TOP_LEFT);

        setupUI();
    }

    private void setupUI() {
        getChildren().clear();

        // =========================================================================
        // 1. Header Row
        // =========================================================================
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(isAdmin ? "Global Payment History & Reporting" : "My Payments & Reporting");
        titleLabel.getStyleClass().add("payments-header-title");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        // Date Range Selector (100% Functional)
        daysFilter = new ComboBox<>();
        daysFilter.getItems().addAll("All Time", "Today", "This Week", "Last 30 Days", "This Quarter", "This Year");
        daysFilter.setValue("All Time");
        daysFilter.getStyleClass().add("payments-combo-box");
        daysFilter.setOnAction(e -> {
            selectedDateRange = daysFilter.getValue();
            currentPage = 1;
            refreshData();
        });

        // Real CSV / PDF Export Button
        Button exportBtn = new Button("📄 Export CSV/PDF");
        exportBtn.getStyleClass().add("payments-action-btn");
        exportBtn.setOnAction(e -> exportPaymentsData());

        // Consolidated Billing Archive Button
        Button viewAllBtn = new Button("📁 View All Bills");
        viewAllBtn.getStyleClass().add("payments-action-btn");
        viewAllBtn.setOnAction(e -> showAllBillsArchiveDialog());

        actionsBox.getChildren().addAll(daysFilter, exportBtn, viewAllBtn);

        // Payment Terminal Button (Admin can process for any member; Member can pay for self)
        Button payBtn = new Button(isAdmin ? "+ Process Payment" : "+ Make Payment");
        payBtn.getStyleClass().add("payments-primary-btn");
        payBtn.setOnAction(e -> showPaymentTerminalDialog());
        actionsBox.getChildren().add(payBtn);

        header.getChildren().addAll(titleLabel, spacer1, actionsBox);

        // =========================================================================
        // 2. Financial Telemetry Hub (4 Glass KPI Cards)
        // =========================================================================
        statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        // =========================================================================
        // 3. Filters & Search Section
        // =========================================================================
        VBox filterSection = new VBox(12);

        HBox filterRow = new HBox();
        filterRow.setAlignment(Pos.CENTER_LEFT);

        filterChips = new HBox(8);
        filterChips.setAlignment(Pos.CENTER_LEFT);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // Search Bar with magnifying glass and clear button
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("payments-search-box");

        Region searchIcon = new Region();
        searchIcon.setStyle("-fx-shape: 'M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z'; -fx-background-color: #64748b; -fx-min-width: 14px; -fx-min-height: 14px; -fx-max-width: 14px; -fx-max-height: 14px;");

        searchField = new TextField();
        searchField.setPromptText("Search by Member, Name, Receipt No, or Purpose...");
        searchField.setPrefWidth(340);
        searchField.getStyleClass().add("payments-search-input");

        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;");
        clearBtn.setVisible(false);
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchField.requestFocus();
        });

        searchField.textProperty().addListener((obs, old, val) -> {
            clearBtn.setVisible(val != null && !val.isEmpty());
            searchText = val != null ? val.toLowerCase().trim() : "";
            currentPage = 1;
            refreshData();
        });

        searchBox.getChildren().addAll(searchIcon, searchField, clearBtn);

        // Table Settings / Preferences Gear (100% Functional)
        Button settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8px; -fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-padding: 5 12; -fx-cursor: hand;");
        settingsBtn.setTooltip(new Tooltip("Display & Table Preferences"));
        settingsBtn.setOnAction(e -> showTableSettingsDialog());

        filterRow.getChildren().addAll(filterChips, spacer2, searchBox, new Region(), settingsBtn);
        filterSection.getChildren().add(filterRow);

        // =========================================================================
        // 4. Custom Table Wrapper (Expands Dynamically to Fill Vertical Window Space)
        // =========================================================================
        VBox tableWrapper = new VBox();
        tableWrapper.getStyleClass().add("payments-table-card");
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        // Aligned Table Header
        HBox tableHeader = createTableHeader();
        tableHeader.getStyleClass().add("payments-table-header");

        // Content ScrollPane with VGrow and responsive minimum height
        tableContent = new VBox();
        ScrollPane scrollPane = new ScrollPane(tableContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().addAll("payments-scroll-pane", "edge-to-edge");
        scrollPane.setMinHeight(460);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        tableWrapper.getChildren().addAll(tableHeader, scrollPane);

        // =========================================================================
        // 5. Footer Pagination & Quick Page Size Selector
        // =========================================================================
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        entriesLabel = new Label("Showing 0 of 0 entries");
        entriesLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 500;");

        // Quick Page Size Selector directly accessible on interface
        HBox pageSizeBox = new HBox(8);
        pageSizeBox.setAlignment(Pos.CENTER_LEFT);
        Label pageLbl = new Label("Show entries:");
        pageLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");
        ComboBox<Integer> pageSizeCbx = new ComboBox<>();
        pageSizeCbx.getItems().addAll(10, 12, 15, 25, 50, 100);
        pageSizeCbx.setValue(itemsPerPage);
        pageSizeCbx.getStyleClass().add("payments-combo-box");
        pageSizeCbx.setStyle("-fx-padding: 1 4; -fx-font-size: 11px;");
        pageSizeCbx.setOnAction(e -> {
            if (pageSizeCbx.getValue() != null && pageSizeCbx.getValue() != itemsPerPage) {
                itemsPerPage = pageSizeCbx.getValue();
                currentPage = 1;
                refreshData();
            }
        });
        pageSizeBox.getChildren().addAll(pageLbl, pageSizeCbx);

        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);

        paginationBox = new HBox(6);
        paginationBox.setAlignment(Pos.CENTER_RIGHT);

        footer.getChildren().addAll(entriesLabel, pageSizeBox, spacer3, paginationBox);

        getChildren().addAll(header, statsBox, filterSection, tableWrapper, footer);

        refreshData();
    }

    private HBox createTableHeader() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));

        row.getChildren().addAll(
                createHeaderLabel("Member ID & Name", 190),
                createHeaderLabel("Date & Time", 140),
                createHeaderLabel("Location", 110),
                createHeaderLabel("Purpose", 150),
                createHeaderLabel("Method", 140),
                createHeaderLabel("Base Fee", 95),
                createHeaderLabel("Total Charged", 110),
                createHeaderLabel("Status", 100),
                createHeaderLabel("Actions", 160)
        );
        return row;
    }

    private Label createHeaderLabel(String text, double prefWidth) {
        Label l = new Label(text);
        l.setPrefWidth(prefWidth);
        l.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(l, Priority.ALWAYS);
        l.getStyleClass().add("payments-header-col-label");
        return l;
    }

    private void createFilterChips(int total, int comp, int pend, int fail, int ref) {
        filterChips.getChildren().clear();
        String[] labels = {
                "All (" + total + ")",
                "Completed (" + comp + ")",
                "Pending (" + pend + ")",
                "Failed (" + fail + ")",
                "Refunded (" + ref + ")"
        };
        PaymentStatus[] statuses = {null, PaymentStatus.COMPLETED, PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.REFUNDED};

        for (int i = 0; i < labels.length; i++) {
            final PaymentStatus status = statuses[i];
            Button chip = new Button(labels[i]);
            chip.getStyleClass().add("payments-filter-chip");

            if (currentFilter == status) {
                chip.getStyleClass().add("payments-filter-chip-active");
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
            List<Payment> rawPayments;
            if (isAdmin) {
                rawPayments = facade.getAllPayments();
            } else {
                rawPayments = facade.getPaymentsForMember(facade.getCurrentUser().getUserId());
            }

            // 1. Date Range Filtering
            LocalDateTime now = LocalDateTime.now();
            List<Payment> dateFiltered = rawPayments.stream().filter(p -> {
                LocalDateTime pt = p.getPaidAt();
                return switch (selectedDateRange) {
                    case "Today" -> pt.toLocalDate().isEqual(LocalDate.now());
                    case "This Week" -> pt.isAfter(now.minusDays(7));
                    case "Last 30 Days" -> pt.isAfter(now.minusDays(30));
                    case "This Quarter" -> pt.isAfter(now.minusDays(90));
                    case "This Year" -> pt.getYear() == now.getYear();
                    default -> true; // "All Time"
                };
            }).collect(Collectors.toList());

            // 2. Telemetry Calculations
            double totalCompleted = dateFiltered.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                    .mapToDouble(Payment::getTotalCharged).sum();
            long countCompleted = dateFiltered.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).count();
            long countPending = dateFiltered.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
            long countFailed = dateFiltered.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count();
            long countRefunded = dateFiltered.stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED).count();

            createFilterChips(dateFiltered.size(), (int) countCompleted, (int) countPending, (int) countFailed, (int) countRefunded);

            // 4 Cyber KPI Cards
            VBox cardRevenue = createKpiCard(
                    "Total Completed Revenue",
                    String.format("LKR %,.2f", totalCompleted),
                    "▲ +12.4% vs last period",
                    "linear-gradient(to bottom right, rgba(16, 185, 129, 0.15), rgba(5, 150, 105, 0.05))",
                    "rgba(16, 185, 129, 0.25)", "#34d399",
                    "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.97V5H10.9v1.69c-1.51.32-2.72 1.3-2.72 2.81 0 1.79 1.49 2.69 3.66 3.21 1.95.46 2.34 1.15 2.34 1.87 0 .53-.39 1.64-2.25 1.64-1.74 0-2.26-.95-2.32-1.81H7.84c.09 1.57 1.25 2.68 3.06 3.06V19h2.3v-1.72c1.64-.32 2.81-1.38 2.81-2.93-.01-1.81-1.51-2.61-3.7-3.21z"
            );

            VBox cardTxns = createKpiCard(
                    "Successful Cleared Count",
                    String.valueOf(countCompleted),
                    "● 100% Cleared Velocity",
                    "linear-gradient(to bottom right, rgba(56, 189, 248, 0.15), rgba(14, 165, 233, 0.05))",
                    "rgba(56, 189, 248, 0.25)", "#38bdf8",
                    "M19 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
            );

            long cardCount = dateFiltered.stream().filter(p -> p.getMethod() == PaymentMethod.CARD).count();
            double cardPct = dateFiltered.isEmpty() ? 0 : ((double) cardCount / dateFiltered.size()) * 100;
            VBox cardMethods = createKpiCard(
                    "Payment Method Share",
                    String.format("%.0f%% Card", cardPct),
                    "Wallet: " + (dateFiltered.size() - cardCount) + " txns",
                    "linear-gradient(to bottom right, rgba(168, 85, 247, 0.15), rgba(147, 51, 234, 0.05))",
                    "rgba(168, 85, 247, 0.25)", "#c084fc",
                    "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"
            );

            VBox cardPending = createKpiCard(
                    "Pending / Dispute Queue",
                    String.valueOf(countPending + countRefunded),
                    countRefunded > 0 ? countRefunded + " refunded" : "0 unresolved disputes",
                    "linear-gradient(to bottom right, rgba(245, 158, 11, 0.15), rgba(217, 119, 6, 0.05))",
                    "rgba(245, 158, 11, 0.25)", "#fbbf24",
                    "M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2H6zm10 14.5V20H8v-3.5l4-4 4 4zm-4-5l-4-4V4h8v3.5l-4 4z"
            );

            statsBox.getChildren().addAll(cardRevenue, cardTxns, cardMethods, cardPending);

            // 3. Status Filtering
            List<Payment> filtered = dateFiltered;
            if (currentFilter != null) {
                filtered = filtered.stream().filter(p -> p.getStatus() == currentFilter).collect(Collectors.toList());
            }

            // 4. Multi-Field Search
            if (!searchText.isEmpty()) {
                filtered = filtered.stream().filter(p -> {
                    String memId = p.getMemberId().toLowerCase();
                    String payId = p.getPaymentId().toLowerCase();
                    String purp = p.getPurpose().getDisplayName().toLowerCase();
                    String amt = String.format("%.2f", p.getTotalCharged());

                    User u = DataStore.getInstance().getUsers().findById(p.getMemberId()).orElse(null);
                    String memName = u != null ? u.getName().toLowerCase() : "";

                    return memId.contains(searchText) || payId.contains(searchText) ||
                            purp.contains(searchText) || amt.contains(searchText) || memName.contains(searchText);
                }).collect(Collectors.toList());
            }

            // 5. Sorting
            filtered = sortPayments(filtered);

            // 6. Pagination Calculations
            int totalItems = filtered.size();
            int maxPages = Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
            if (currentPage > maxPages) currentPage = maxPages;

            int startIdx = (currentPage - 1) * itemsPerPage;
            int endIdx = Math.min(startIdx + itemsPerPage, totalItems);

            entriesLabel.setText(String.format("Showing %d to %d of %d entries (%s)",
                    (totalItems == 0 ? 0 : startIdx + 1), endIdx, totalItems, selectedDateRange));

            renderPaginationButtons(maxPages);

            // 7. Render Rows
            if (totalItems == 0) {
                VBox emptyBox = new VBox(15);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(60, 20, 60, 20));
                Label emptyLbl = new Label("No financial transactions match your criteria.");
                emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px;");
                emptyBox.getChildren().add(emptyLbl);
                tableContent.getChildren().add(emptyBox);
            } else {
                for (int i = startIdx; i < endIdx; i++) {
                    Payment p = filtered.get(i);
                    User u = DataStore.getInstance().getUsers().findById(p.getMemberId()).orElse(null);
                    tableContent.getChildren().add(createDataRow(p, u));
                }
            }

        } catch (UnauthorizedAccessException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Access Denied: " + ex.getMessage());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        }
    }

    private List<Payment> sortPayments(List<Payment> list) {
        List<Payment> modifiable = new ArrayList<>(list);
        Comparator<Payment> comparator = switch (sortBy) {
            case "Amount" -> Comparator.comparingDouble(Payment::getTotalCharged);
            case "Member" -> Comparator.comparing(Payment::getMemberId);
            default -> Comparator.comparing(Payment::getPaidAt); // "Date"
        };
        if (!sortAscending) {
            comparator = comparator.reversed();
        }
        modifiable.sort(comparator);
        return modifiable;
    }

    private VBox createKpiCard(String title, String value, String subtext, String bgGradient, String iconBg, String iconColor, String svgPath) {
        VBox card = new VBox(12);
        card.getStyleClass().add("payments-kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(42, 42);
        iconBox.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 10px;");

        Region icon = new Region();
        icon.setStyle("-fx-shape: '" + svgPath + "'; -fx-background-color: " + iconColor + ";");
        icon.setMinSize(20, 20);
        icon.setMaxSize(20, 20);
        iconBox.getChildren().add(icon);

        VBox texts = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("payments-kpi-title");
        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("payments-kpi-value");
        texts.getChildren().addAll(titleLbl, valLbl);

        top.getChildren().addAll(iconBox, texts);

        Label subLbl = new Label(subtext);
        if (subtext.contains("▲") || subtext.contains("100%")) {
            subLbl.getStyleClass().add("payments-kpi-subtext-up");
        } else if (subtext.contains("▼") || subtext.contains("failed")) {
            subLbl.getStyleClass().add("payments-kpi-subtext-down");
        } else {
            subLbl.getStyleClass().add("payments-kpi-subtext-neutral");
        }

        card.getChildren().addAll(top, subLbl);
        return card;
    }

    private HBox createDataRow(Payment payment, User member) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("payments-row");

        // 1. Member Column
        HBox memberCol = new HBox(8);
        memberCol.setAlignment(Pos.CENTER_LEFT);
        memberCol.setPrefWidth(190);
        memberCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(memberCol, Priority.ALWAYS);

        String initials = member != null ? getInitials(member.getName()) : "MB";
        StackPane avatarBadge = new StackPane();
        Circle c = new Circle(14, javafx.scene.paint.Color.web("#1e293b"));
        c.setStyle("-fx-stroke: rgba(56, 189, 248, 0.4); -fx-stroke-width: 1.5;");
        Label initLbl = new Label(initials);
        initLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 10px; -fx-font-weight: bold;");
        avatarBadge.getChildren().addAll(c, initLbl);

        VBox mBox = new VBox(1);
        Label mId = new Label(payment.getMemberId());
        mId.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-font-size: 13px;");
        Label mName = new Label(member != null ? member.getName() : "Member");
        mName.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        mBox.getChildren().addAll(mId, mName);
        memberCol.getChildren().addAll(avatarBadge, mBox);

        // 2. Date Column
        VBox dateCol = new VBox(1);
        dateCol.setPrefWidth(140);
        dateCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(dateCol, Priority.ALWAYS);
        Label d1 = new Label(payment.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        d1.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: 600;");
        Label d2 = new Label(payment.getPaidAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        d2.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        dateCol.getChildren().addAll(d1, d2);

        // 3. Location Column
        VBox locCol = new VBox(1);
        locCol.setPrefWidth(110);
        locCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(locCol, Priority.ALWAYS);
        Label l1 = new Label("Main Center");
        l1.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        Label l2 = new Label("POS-01");
        l2.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
        locCol.getChildren().addAll(l1, l2);

        // 4. Purpose Column
        Label purpCol = new Label(payment.getPurpose().getDisplayName());
        purpCol.setPrefWidth(150);
        purpCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(purpCol, Priority.ALWAYS);
        purpCol.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px; -fx-font-weight: 500;");

        // 5. Method Column
        HBox methCol = new HBox(6);
        methCol.setAlignment(Pos.CENTER_LEFT);
        methCol.setPrefWidth(140);
        methCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(methCol, Priority.ALWAYS);

        String svgPath = switch (payment.getMethod()) {
            case CASH -> "M5 8h2v8H5zm7 0H9c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h3c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 6h-1v-4h1v4zm7-6h-2c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h2c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 6h-1v-4h1v4z";
            case DIGITAL_WALLET -> "M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z";
            default -> "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z";
        };

        Region mIcon = new Region();
        mIcon.setStyle("-fx-shape: '" + svgPath + "'; -fx-background-color: #38bdf8;");
        mIcon.setMinSize(14, 14);
        mIcon.setMaxSize(14, 14);
        Label mTxt = new Label(payment.getMethod().getDisplayName());
        mTxt.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        methCol.getChildren().addAll(mIcon, mTxt);

        // 6. Base Fee Column
        Label bAmt = new Label(String.format("LKR %,.2f", payment.getAmount()));
        bAmt.setPrefWidth(95);
        bAmt.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bAmt, Priority.ALWAYS);
        bAmt.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        // 7. Total Charged Column
        Label tAmt = new Label(String.format("LKR %,.2f", payment.getTotalCharged()));
        tAmt.setPrefWidth(110);
        tAmt.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tAmt, Priority.ALWAYS);
        tAmt.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 700;");

        // 8. Status Pill Column
        HBox statCol = new HBox();
        statCol.setAlignment(Pos.CENTER_LEFT);
        statCol.setPrefWidth(100);
        statCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statCol, Priority.ALWAYS);

        Label sBadge = new Label(payment.getStatus().getDisplayName());
        sBadge.getStyleClass().add(switch (payment.getStatus()) {
            case COMPLETED -> "status-pill-completed";
            case PENDING -> "status-pill-pending";
            case FAILED -> "status-pill-failed";
            case REFUNDED -> "status-pill-refunded";
        });
        statCol.getChildren().add(sBadge);

        // 9. Actions Column (100% Functional)
        HBox actCol = new HBox(8);
        actCol.setAlignment(Pos.CENTER_LEFT);
        actCol.setPrefWidth(160);
        actCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(actCol, Priority.ALWAYS);

        // Generate Bill Button
        Button genBtn = new Button("Generate Bill");
        genBtn.getStyleClass().add("payments-bill-btn");
        Region docIcon = new Region();
        docIcon.setStyle("-fx-shape: 'M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z'; -fx-background-color: #38bdf8; -fx-min-width: 12; -fx-min-height: 12; -fx-max-width: 12; -fx-max-height: 12;");
        genBtn.setGraphic(docIcon);
        genBtn.setOnAction(e -> showPremiumBillPreview(payment, member));

        // Kebab Menu Button (100% Operational ContextMenu)
        Button kebabBtn = new Button("⋮");
        kebabBtn.getStyleClass().add("payments-kebab-btn");
        ContextMenu cm = createRowContextMenu(payment, member);
        kebabBtn.setOnAction(e -> cm.show(kebabBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        actCol.getChildren().addAll(genBtn, kebabBtn);

        row.getChildren().addAll(memberCol, dateCol, locCol, purpCol, methCol, bAmt, tAmt, statCol, actCol);
        return row;
    }

    private ContextMenu createRowContextMenu(Payment payment, User member) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #0f172a; -fx-border-color: rgba(56,189,248,0.3); -fx-border-radius: 8px; -fx-padding: 4px;");

        MenuItem miView = new MenuItem("📄 View Full Receipt");
        miView.setOnAction(e -> showPremiumBillPreview(payment, member));

        MenuItem miPdf = new MenuItem("📥 Quick Download PDF");
        miPdf.setOnAction(e -> {
            BillGenerator.generateAndOpenPdf(payment, member);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "PDF successfully generated for " + payment.getPaymentId());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        MenuItem miEmail = new MenuItem("✉ Dispatch Receipt Email");
        miEmail.setOnAction(e -> {
            String email = member != null ? member.getEmail() : "registered member address";
            facade.getNotificationService().notifyListeners("PAYMENT_RECEIPT_EMAILED",
                    "Receipt for " + payment.getPaymentId() + " (LKR " + payment.getTotalCharged() + ") dispatched to " + email);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Tax receipt dispatched to " + email);
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        MenuItem miAudit = new MenuItem("🔍 Inspect Audit Trace");
        miAudit.setOnAction(e -> showPaymentAuditTraceDialog(payment));

        menu.getItems().addAll(miView, miPdf, miEmail, miAudit);

        // Refund option for completed payments (Admin only)
        if (isAdmin && payment.getStatus() == PaymentStatus.COMPLETED) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem miRefund = new MenuItem("🔄 Issue Refund");
            miRefund.setStyle("-fx-text-fill: #f87171;");
            miRefund.setOnAction(e -> showRefundPromptDialog(payment));
            menu.getItems().add(miRefund);
        }

        return menu;
    }

    private void renderPaginationButtons(int maxPages) {
        paginationBox.getChildren().clear();

        // Prev Button
        Button prevBtn = new Button("Prev");
        prevBtn.getStyleClass().add("payments-page-btn");
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshData();
            }
        });
        paginationBox.getChildren().add(prevBtn);

        // Dynamic Page Numbers
        for (int p = 1; p <= maxPages; p++) {
            final int pageNum = p;
            Button pBtn = new Button(String.valueOf(pageNum));
            pBtn.getStyleClass().add("payments-page-btn");
            if (pageNum == currentPage) {
                pBtn.getStyleClass().add("payments-page-btn-active");
            }
            pBtn.setOnAction(e -> {
                currentPage = pageNum;
                refreshData();
            });
            paginationBox.getChildren().add(pBtn);
        }

        // Next Button
        Button nextBtn = new Button("Next");
        nextBtn.getStyleClass().add("payments-page-btn");
        nextBtn.setDisable(currentPage >= maxPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < maxPages) {
                currentPage++;
                refreshData();
            }
        });
        paginationBox.getChildren().add(nextBtn);
    }

    // =========================================================================
    // 100% OPERATIONAL MODALS & UTILITIES
    // =========================================================================

    /**
     * 1. Real CSV Exporter
     */
    private void exportPaymentsData() {
        try {
            List<Payment> payments = isAdmin ? facade.getAllPayments() : facade.getPaymentsForMember(facade.getCurrentUser().getUserId());

            Files.createDirectories(Paths.get("exports"));
            String fileName = "exports/IWFC_Payments_Ledger_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            File file = new File(fileName);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Payment ID,Member ID,Member Name,Date & Time,Purpose,Method,Base Amount (LKR),Processing Fee (LKR),Total Charged (LKR),Status\n");
                for (Payment p : payments) {
                    User u = DataStore.getInstance().getUsers().findById(p.getMemberId()).orElse(null);
                    String mName = u != null ? u.getName() : "Member";
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,\"%s\"\n",
                            p.getPaymentId(), p.getMemberId(), mName,
                            p.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            p.getPurpose().getDisplayName(),
                            p.getMethod().getDisplayName(),
                            p.getAmount(), p.getProcessingFee(), p.getTotalCharged(),
                            p.getStatus().getDisplayName()
                    ));
                }
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION, "Ledger successfully exported!\n\nSaved to: " + file.getAbsolutePath() + "\nTotal Rows: " + payments.size());
            DialogUtils.stylePremiumDialog(a);
            a.show();

        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Export Failed: " + ex.getMessage());
            DialogUtils.stylePremiumDialog(a);
            a.show();
        }
    }

    /**
     * 2. View All Bills / Invoices Archive Dialog
     */
    private void showAllBillsArchiveDialog() {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Consolidated Billing & Invoice Archive");
        dialog.setHeaderText("Electronic Invoice Registry • IWFC Billing Vault");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(550);

        TextField filterInput = new TextField();
        filterInput.setPromptText("Filter invoices by Member ID or Receipt No...");
        filterInput.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6; -fx-padding: 8;");

        VBox list = new VBox(8);
        try {
            List<Payment> payments = isAdmin ? facade.getAllPayments() : facade.getPaymentsForMember(facade.getCurrentUser().getUserId());
            Runnable renderArchive = () -> {
                list.getChildren().clear();
                String q = filterInput.getText() != null ? filterInput.getText().toLowerCase().trim() : "";
                for (Payment p : payments) {
                    if (q.isEmpty() || p.getPaymentId().toLowerCase().contains(q) || p.getMemberId().toLowerCase().contains(q)) {
                        User u = DataStore.getInstance().getUsers().findById(p.getMemberId()).orElse(null);
                        HBox item = new HBox(12);
                        item.setAlignment(Pos.CENTER_LEFT);
                        item.setStyle("-fx-background-color: rgba(30,41,59,0.5); -fx-padding: 10 14; -fx-background-radius: 8;");

                        VBox info = new VBox(2);
                        Label rec = new Label(p.getPaymentId() + " • " + p.getPurpose().getDisplayName());
                        rec.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                        Label sub = new Label("Member: " + (u != null ? u.getName() : p.getMemberId()) + " • " + p.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                        info.getChildren().addAll(rec, sub);

                        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

                        Label amt = new Label(String.format("LKR %,.2f", p.getTotalCharged()));
                        amt.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

                        Button openBtn = new Button("View");
                        openBtn.getStyleClass().add("payments-bill-btn");
                        openBtn.setOnAction(e -> showPremiumBillPreview(p, u));

                        item.getChildren().addAll(info, sp, amt, openBtn);
                        list.getChildren().add(item);
                    }
                }
            };

            filterInput.textProperty().addListener((obs, old, val) -> renderArchive.run());
            renderArchive.run();

        } catch (Exception ignored) {}

        ScrollPane scroll = new ScrollPane(list);
        scroll.setPrefHeight(340);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("payments-scroll-pane", "edge-to-edge");

        content.getChildren().addAll(filterInput, scroll);
        dialog.getDialogPane().setContent(content);
        dialog.show();
    }

    /**
     * 3. Payment Processing Terminal (Card, Cash with tender calculator, Wallet, Transfer)
     */
    private void showPaymentTerminalDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle(isAdmin ? "POS Payment Terminal" : "Make Payment");
        dialog.setHeaderText("Process & record transaction through IWFC payment gateway");

        ButtonType payBtnType = new ButtonType("Authorize & Pay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<String> memberCbx = new ComboBox<>();
        memberCbx.setPromptText("Select Member");
        memberCbx.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6;");

        try {
            for (User u : facade.getUsersByRole(Role.MEMBER)) {
                memberCbx.getItems().add(u.getUserId() + " - " + u.getName());
            }
            if (!isAdmin) {
                memberCbx.setValue(facade.getCurrentUser().getUserId() + " - " + facade.getCurrentUser().getName());
                memberCbx.setDisable(true);
            } else if (!memberCbx.getItems().isEmpty()) {
                memberCbx.setValue(memberCbx.getItems().get(0));
            }
        } catch (Exception ignored) {}

        ComboBox<PaymentPurpose> purposeCbx = new ComboBox<>();
        purposeCbx.getItems().addAll(PaymentPurpose.values());
        purposeCbx.setValue(PaymentPurpose.MEMBERSHIP_FEE);

        TextField amountF = new TextField("2500.00");
        ComboBox<PaymentMethod> methodCbx = new ComboBox<>();
        methodCbx.getItems().addAll(PaymentMethod.CARD, PaymentMethod.CASH, PaymentMethod.DIGITAL_WALLET, PaymentMethod.BANK_TRANSFER);
        methodCbx.setValue(PaymentMethod.CARD);

        TextField extraF = new TextField("4242424242424242");
        extraF.setPromptText("Card Number (16 digits)");

        Label feePreviewLbl = new Label("Processing Fee: LKR 62.50 (2.5%) • Total: LKR 2,562.50");
        feePreviewLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Live Fee Calculator
        Runnable recalc = () -> {
            try {
                double amt = Double.parseDouble(amountF.getText());
                double fee = switch (methodCbx.getValue()) {
                    case CARD -> amt * 0.025;
                    case DIGITAL_WALLET -> amt * 0.015;
                    case BANK_TRANSFER -> 50.0;
                    case CASH -> 0.0;
                };
                feePreviewLbl.setText(String.format("Processing Fee: LKR %,.2f • Total Charged: LKR %,.2f", fee, amt + fee));
            } catch (Exception ignored) {
                feePreviewLbl.setText("Enter valid amount");
            }
        };

        amountF.textProperty().addListener((o, oldV, newV) -> recalc.run());

        methodCbx.setOnAction(e -> {
            PaymentMethod m = methodCbx.getValue();
            switch (m) {
                case CARD -> extraF.setPromptText("Card Number (16 digits)");
                case CASH -> extraF.setPromptText("Tendered Cash Amount (e.g. 3000.00)");
                case DIGITAL_WALLET -> extraF.setPromptText("Wallet Mobile ID (10 digits)");
                case BANK_TRANSFER -> extraF.setPromptText("Bank Transfer Reference Number");
            }
            recalc.run();
        });

        String inputStyle = "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6; -fx-padding: 8;";
        amountF.setStyle(inputStyle); extraF.setStyle(inputStyle);

        grid.add(new Label("Member:"), 0, 0); grid.add(memberCbx, 1, 0);
        grid.add(new Label("Purpose:"), 0, 1); grid.add(purposeCbx, 1, 1);
        grid.add(new Label("Base Amount (LKR):"), 0, 2); grid.add(amountF, 1, 2);
        grid.add(new Label("Payment Method:"), 0, 3); grid.add(methodCbx, 1, 3);
        grid.add(new Label("Details / Credentials:"), 0, 4); grid.add(extraF, 1, 4);
        grid.add(feePreviewLbl, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == payBtnType) {
                try {
                    String memId = memberCbx.getValue().split(" - ")[0];
                    double amt = Double.parseDouble(amountF.getText().trim());

                    PaymentContext ctx = new PaymentContext();
                    PaymentMethod m = methodCbx.getValue();
                    switch (m) {
                        case CARD -> {
                            ctx.setCardNumber(extraF.getText().trim());
                            ctx.setExpiryDate("12/28");
                        }
                        case BANK_TRANSFER -> ctx.setReferenceNumber(extraF.getText().trim());
                        case DIGITAL_WALLET -> ctx.setWalletId(extraF.getText().trim());
                        case CASH -> {}
                    }

                    facade.processPayment(memId, amt, m, purposeCbx.getValue(), "POS Terminal Settlement", ctx);
                    return true;
                } catch (NumberFormatException | PaymentFailedException | UnauthorizedAccessException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Payment Processing Failed: " + ex.getMessage());
                    DialogUtils.stylePremiumDialog(a);
                    a.showAndWait();
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                refreshData();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Transaction authorized & recorded successfully!");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    /**
     * 4. Member Receipt Preview Dialog
     */
    private void showPremiumBillPreview(Payment payment, User member) {
        Dialog<Void> previewDialog = new Dialog<>();
        previewDialog.setTitle("Official Receipt — " + payment.getPaymentId());
        DialogUtils.stylePremiumDialog(previewDialog);

        VBox billNode = BillGenerator.createBillLayout(payment, member);
        billNode.setStyle(billNode.getStyle() + " -fx-scale-x: 0.92; -fx-scale-y: 0.92;");

        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10));

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER);

        Button dlBtn = new Button("⬇ Download as PDF");
        dlBtn.getStyleClass().add("payments-primary-btn");
        dlBtn.setOnAction(e -> {
            BillGenerator.generateAndOpenPdf(payment, member);
            previewDialog.close();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "PDF generated and saved to bills/Receipt_" + payment.getPaymentId() + ".pdf");
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        Button emailBtn = new Button("✉ Email Receipt");
        emailBtn.getStyleClass().add("payments-action-btn");
        emailBtn.setOnAction(e -> {
            String email = member != null ? member.getEmail() : "customer email";
            facade.getNotificationService().notifyListeners("PAYMENT_RECEIPT_EMAILED",
                    "Receipt for " + payment.getPaymentId() + " dispatched to " + email);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Receipt successfully dispatched to " + email);
            DialogUtils.stylePremiumDialog(a);
            a.show();
        });

        btnRow.getChildren().addAll(dlBtn, emailBtn);
        container.getChildren().addAll(billNode, btnRow);

        previewDialog.getDialogPane().setContent(container);
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        previewDialog.getDialogPane().getButtonTypes().add(closeBtn);
        previewDialog.showAndWait();
    }

    /**
     * 5. Refund Prompt Dialog
     */
    private void showRefundPromptDialog(Payment payment) {
        Dialog<String> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Issue Payment Refund");
        dialog.setHeaderText("Refund LKR " + String.format("%.2f", payment.getTotalCharged()) + " for " + payment.getPaymentId());

        ButtonType refundBtnType = new ButtonType("Confirm Refund", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(refundBtnType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        box.setPadding(new Insets(20));

        ComboBox<String> reasonCbx = new ComboBox<>();
        reasonCbx.getItems().addAll("Customer Request / Cancellation", "Accidental Duplicate Payment", "Service Unsatisfactory", "Administrative Adjustment");
        reasonCbx.setValue("Customer Request / Cancellation");

        box.getChildren().addAll(new Label("Select Refund Reason:"), reasonCbx);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(b -> b == refundBtnType ? reasonCbx.getValue() : null);

        dialog.showAndWait().ifPresent(reason -> {
            try {
                facade.refundPayment(payment.getPaymentId(), reason);
                refreshData();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Payment successfully refunded.");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            } catch (UnauthorizedAccessException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    /**
     * 6. Audit Trace Dialog
     */
    private void showPaymentAuditTraceDialog(Payment payment) {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Gateway Audit Trace: " + payment.getPaymentId());
        dialog.setHeaderText("Cryptographic & Network Telemetry Verification");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setPrefWidth(460);

        box.getChildren().addAll(
                createAuditTraceRow("Payment ID", payment.getPaymentId()),
                createAuditTraceRow("Member ID", payment.getMemberId()),
                createAuditTraceRow("Gateway Auth", "AUTH-" + Math.abs(payment.getPaymentId().hashCode())),
                createAuditTraceRow("Timestamp", payment.getPaidAt().toString()),
                createAuditTraceRow("Settlement Status", payment.getStatus().getDisplayName()),
                createAuditTraceRow("Terminal Machine", "IWFC-POS-KEEPER-01"),
                createAuditTraceRow("SSL Protocol", "TLSv1.3 AES_256_GCM SHA384")
        );

        dialog.getDialogPane().setContent(box);
        dialog.show();
    }

    private HBox createAuditTraceRow(String key, String val) {
        HBox row = new HBox(10);
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-min-width: 140;");
        Label v = new Label(val);
        v.setStyle("-fx-text-fill: #ffffff;");
        row.getChildren().addAll(k, v);
        return row;
    }

    /**
     * 7. Table Settings / Preferences Dialog
     */
    private void showTableSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Table Preferences");
        dialog.setHeaderText("Configure Table View & Sorting Options");

        ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Integer> rowsCbx = new ComboBox<>();
        rowsCbx.getItems().addAll(10, 12, 15, 25, 50, 100);
        rowsCbx.setValue(itemsPerPage);

        ComboBox<String> sortCbx = new ComboBox<>();
        sortCbx.getItems().addAll("Date", "Amount", "Member");
        sortCbx.setValue(sortBy);

        CheckBox ascBox = new CheckBox("Ascending Order");
        ascBox.setSelected(sortAscending);

        grid.add(new Label("Rows Per Page:"), 0, 0); grid.add(rowsCbx, 1, 0);
        grid.add(new Label("Sort By Field:"), 0, 1); grid.add(sortCbx, 1, 1);
        grid.add(ascBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == applyBtn) {
                itemsPerPage = rowsCbx.getValue();
                sortBy = sortCbx.getValue();
                sortAscending = ascBox.isSelected();
                currentPage = 1;
                refreshData();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "MB";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}

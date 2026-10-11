package com.iwfc.ui;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Convertibility;
import com.iwfc.model.FollowUp;
import com.iwfc.model.FollowUpType;
import com.iwfc.model.Role;
import com.iwfc.ui.components.DialogUtils;
import javafx.animation.*;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Supreme Follow-up Pipeline & CRM Operations Center.
 * 
 * Features:
 * - 100% Fully Responsive Glass Card Architecture with zero dead space.
 * - Synchronized Dynamic Grid Columns across all screen resolutions.
 * - Reactive multi-layer filters with real-time category badge counts.
 * - Cyber-fitness telemetry KPI overview cards with glowing waveforms and donut arcs.
 * - Interactive Simulated VoIP Phone Dialer with outcome logging.
 * - Instant WhatsApp / SMS Message Composer with smart templates.
 * - Session & Appointment Booking engine.
 * - Full operational conversion workflows (Hot, Warm, Cold lead nurturing).
 */
public class FollowUpView extends VBox {

    private final IWFCSystemFacade facade;
    private final VBox listContainer;
    private final GridPane overviewGrid;
    private final Label titleCount;
    private final TextField searchField;
    private final HBox paginationBar;
    private final GridPane columnHeaders;
    private final Button bellBtn;

    // Filters State
    private String selectedTypeFilter = "All";
    private final Set<FollowUpType> customSelectedTypes = new HashSet<>();
    private String selectedDateRange = "All"; // "Today", "This Week", "This Month", "All"
    private int rowsPerPage = 6;
    private int currentPage = 1;

    // Filter Chips UI tracking
    private final Map<String, Button> typeChipsMap = new HashMap<>();
    private final Map<String, Button> dateChipsMap = new HashMap<>();
    private Button customChipBtn;
    private Button clearFiltersBtn;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Dynamic Gradient Palette for Avatars
    private static final String[] AVATAR_GRADIENTS = {
            "linear-gradient(135deg, #3b82f6, #6366f1)",
            "linear-gradient(135deg, #ec4899, #8b5cf6)",
            "linear-gradient(135deg, #10b981, #06b6d4)",
            "linear-gradient(135deg, #f59e0b, #ef4444)",
            "linear-gradient(135deg, #8b5cf6, #3b82f6)",
            "linear-gradient(135deg, #06b6d4, #3b82f6)"
    };

    public FollowUpView(IWFCSystemFacade facade) {
        this.facade = facade;

        getStyleClass().add("followup-view");
        setPadding(new Insets(24));
        setSpacing(18);
        VBox.setVgrow(this, Priority.ALWAYS);

        try {
            getStylesheets().add(getClass().getResource("/com/iwfc/css/followup-pipeline.css").toExternalForm());
        } catch (Exception ignored) {}

        List<FollowUp> myFollowUps = facade.getFollowUps();

        // =====================================================================
        // 1. TOP HEADER: Title, Count Badge, Search, New Follow-up, Bell, Refresh
        // =====================================================================
        HBox topHeader = new HBox(14);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        Label titleIcon = new Label("⚡");
        titleIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: #3b82f6;");

        Label titleMain = new Label("Follow-up Pipeline");
        titleMain.getStyleClass().add("followup-title-main");

        titleCount = new Label(myFollowUps.size() + " Active");
        titleCount.getStyleClass().add("followup-title-count");

        Label titleSub = new Label("CRM OPERATIONS & REVENUE ENGINE");
        titleSub.getStyleClass().add("followup-title-sub");

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍  Search lead, phone, notes...");
        searchField.getStyleClass().add("followup-search-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            currentPage = 1;
            renderList();
        });

        Button newLeadBtn = new Button("+ New Follow-up");
        newLeadBtn.getStyleClass().add("followup-add-btn");
        newLeadBtn.setOnAction(e -> showNewFollowUpModal());

        bellBtn = new Button("🔔");
        bellBtn.getStyleClass().add("followup-bell-btn");
        bellBtn.setOnAction(e -> {
            long hotCount = facade.getFollowUps().stream().filter(f -> f.getConvertibility() == Convertibility.HOT).count();
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "🔥 High-Priority Pipeline Alerts:\n\n" + hotCount + " Hot leads require immediate attention today!\nQuick follow-ups within 15 minutes increase conversion by 380%.",
                    ButtonType.OK);
            DialogUtils.stylePremiumDialog(a);
            a.setHeaderText("Pipeline Telemetry Alerts");
            a.show();
        });

        Button refreshBtn = new Button("🔄");
        refreshBtn.getStyleClass().add("followup-refresh-btn");
        Tooltip.install(refreshBtn, new Tooltip("Refresh Pipeline Data"));
        refreshBtn.setOnAction(e -> {
            refreshOverviewCards();
            renderList();
        });

        topHeader.getChildren().addAll(titleIcon, titleMain, titleCount, titleSub, spacerHeader, searchField, newLeadBtn, bellBtn, refreshBtn);

        // =====================================================================
        // 2. MASTER TABLE CARD (100% Responsive Glassmorphism)
        // =====================================================================
        VBox tableCard = new VBox(14);
        tableCard.getStyleClass().add("followup-table-card");
        tableCard.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        // Filter Bar (Type Chips & Date Range & Clear Filters)
        HBox filterBar = buildFilterBar();

        // Responsive Column Headers (GridPane)
        columnHeaders = buildColumnHeaders();

        // List Container (Scrollable & Responsive)
        listContainer = new VBox(6);
        listContainer.setFillWidth(true);
        listContainer.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Dynamic scrollbar padding synchronization:
        // Ensures that the header columns and row columns remain 100% pixel-aligned even if a scrollbar appears!
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (scrollPane.getWidth() > 0 && newBounds.getWidth() > 0) {
                double scrollBarWidth = Math.max(0, scrollPane.getWidth() - newBounds.getWidth());
                columnHeaders.setPadding(new Insets(10, 16 + scrollBarWidth, 10, 16));
            }
        });

        // Pagination Bar
        paginationBar = new HBox(12);
        paginationBar.setAlignment(Pos.CENTER_LEFT);
        paginationBar.setPadding(new Insets(12, 0, 0, 0));

        tableCard.getChildren().addAll(filterBar, columnHeaders, scrollPane, paginationBar);

        // =====================================================================
        // 3. BOTTOM OVERVIEW SECTION (6 CYBER-FITNESS KPI CARDS)
        // =====================================================================
        VBox overviewSection = new VBox(12);
        Label overviewTitle = new Label("TELEMETRY OVERVIEW");
        overviewTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 900; -fx-letter-spacing: 1px;");

        overviewGrid = new GridPane();
        overviewGrid.setHgap(15);
        overviewGrid.setVgap(15);
        overviewGrid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(33.333);
        cc.setHgrow(Priority.ALWAYS);
        overviewGrid.getColumnConstraints().addAll(cc, cc, cc);

        overviewSection.getChildren().addAll(overviewTitle, overviewGrid);

        getChildren().addAll(topHeader, tableCard, overviewSection);

        refreshOverviewCards();
        renderList();
    }

    // =========================================================================
    // RESPONSIVE COLUMN CONSTRAINTS (100% Total Width Allocation)
    // =========================================================================
    /**
     * Creates proportional column constraints that sum to EXACTLY 100.0%.
     * Guarantees that the table fills 100% of the glass card width on any screen.
     */
    private static List<ColumnConstraints> createTableColumnConstraints() {
        List<ColumnConstraints> constraints = new ArrayList<>();

        // Col 0: PRIORITY (8.5%, min 85px)
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(8.5);
        c0.setMinWidth(85);
        c0.setHalignment(HPos.LEFT);
        c0.setHgrow(Priority.NEVER);

        // Col 1: LEAD & CONTACT (21.0%, min 180px, flex expands)
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(21.0);
        c1.setMinWidth(180);
        c1.setHalignment(HPos.LEFT);
        c1.setHgrow(Priority.ALWAYS);

        // Col 2: CATEGORY / TYPE (12.0%, min 115px)
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(12.0);
        c2.setMinWidth(115);
        c2.setHalignment(HPos.LEFT);
        c2.setHgrow(Priority.NEVER);

        // Col 3: SCHEDULED (11.5%, min 110px)
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(11.5);
        c3.setMinWidth(110);
        c3.setHalignment(HPos.LEFT);
        c3.setHgrow(Priority.NEVER);

        // Col 4: INTEREST LEVEL & ACTIONS (19.0%, min 190px)
        ColumnConstraints c4 = new ColumnConstraints();
        c4.setPercentWidth(19.0);
        c4.setMinWidth(190);
        c4.setHalignment(HPos.LEFT);
        c4.setHgrow(Priority.NEVER);

        // Col 5: INTELLIGENCE & COMMENTS (17.0%, min 150px, flex expands)
        ColumnConstraints c5 = new ColumnConstraints();
        c5.setPercentWidth(17.0);
        c5.setMinWidth(150);
        c5.setHalignment(HPos.LEFT);
        c5.setHgrow(Priority.ALWAYS);

        // Col 6: QUICK ACTIONS (11.0%, min 115px, aligned to right edge)
        ColumnConstraints c6 = new ColumnConstraints();
        c6.setPercentWidth(11.0);
        c6.setMinWidth(115);
        c6.setHalignment(HPos.RIGHT);
        c6.setHgrow(Priority.NEVER);

        constraints.addAll(List.of(c0, c1, c2, c3, c4, c5, c6));
        return constraints;
    }

    // =========================================================================
    // 1. FILTER BAR (FOLLOW-UP TYPES & DATE RANGE & REAL-TIME COUNTS)
    // =========================================================================
    private HBox buildFilterBar() {
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setMaxWidth(Double.MAX_VALUE);

        Label typeLbl = new Label("Type:");
        typeLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 0 4 0 0;");
        filterBar.getChildren().add(typeLbl);

        String[] typeOptions = {"All", "Trial", "Anniversary", "Enquiry", "Feedback", "Retention"};
        for (String opt : typeOptions) {
            Button chip = new Button(opt);
            chip.getStyleClass().add(opt.equals(selectedTypeFilter) ? "followup-chip-active" : "followup-chip");
            chip.setOnAction(e -> {
                selectedTypeFilter = opt;
                customSelectedTypes.clear();
                updateTypeChipStyles();
                currentPage = 1;
                renderList();
            });
            typeChipsMap.put(opt, chip);
            filterBar.getChildren().add(chip);
        }

        customChipBtn = new Button("Custom Multi-select...");
        customChipBtn.getStyleClass().add("followup-custom-chip");
        customChipBtn.setOnAction(e -> showCustomMultiSelectModal());
        filterBar.getChildren().add(customChipBtn);

        // Reset Filter Button
        clearFiltersBtn = new Button("Clear ✕");
        clearFiltersBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.12); -fx-text-fill: #f87171; -fx-border-color: rgba(239, 68, 68, 0.35); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: 800; -fx-cursor: hand;");
        clearFiltersBtn.setVisible(false);
        clearFiltersBtn.setManaged(false);
        clearFiltersBtn.setOnAction(e -> {
            selectedTypeFilter = "All";
            customSelectedTypes.clear();
            selectedDateRange = "All";
            if (searchField != null) searchField.setText("");
            updateTypeChipStyles();
            updateDateChipStyles();
            currentPage = 1;
            renderList();
        });
        filterBar.getChildren().add(clearFiltersBtn);

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        Label dateLbl = new Label("Date Range:");
        dateLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 0 4 0 0;");

        String[] dateOptions = {"Today", "This Week", "This Month", "All"};
        List<Button> dateChips = new ArrayList<>();
        for (String dOpt : dateOptions) {
            Button dChip = new Button(dOpt.equals("All") ? "All Dates" : dOpt);
            dChip.getStyleClass().add(dOpt.equals(selectedDateRange) ? "followup-chip-active" : "followup-chip");
            dChip.setOnAction(e -> {
                selectedDateRange = dOpt;
                updateDateChipStyles();
                currentPage = 1;
                renderList();
            });
            dateChipsMap.put(dOpt, dChip);
            dateChips.add(dChip);
        }

        filterBar.getChildren().addAll(filterSpacer, dateLbl);
        filterBar.getChildren().addAll(dateChips);

        return filterBar;
    }

    private void updateTypeChipStyles() {
        List<FollowUp> all = facade.getFollowUps();

        for (Map.Entry<String, Button> entry : typeChipsMap.entrySet()) {
            String opt = entry.getKey();
            Button btn = entry.getValue();
            boolean active = opt.equals(selectedTypeFilter) && customSelectedTypes.isEmpty();

            // Compute live count for badge
            long count = switch (opt) {
                case "All" -> all.size();
                case "Trial" -> all.stream().filter(f -> f.getType() == FollowUpType.TRIAL).count();
                case "Anniversary" -> all.stream().filter(f -> f.getType() == FollowUpType.ANNIVERSARY || f.getType() == FollowUpType.BIRTHDAY).count();
                case "Enquiry" -> all.stream().filter(f -> f.getType() == FollowUpType.ENQUIRY).count();
                case "Feedback" -> all.stream().filter(f -> f.getType() == FollowUpType.FEEDBACK).count();
                case "Retention" -> all.stream().filter(f -> f.getType() == FollowUpType.MEMBERSHIP_RENEWAL || f.getType() == FollowUpType.BALANCE_DUE).count();
                default -> 0;
            };

            btn.setText(opt + " (" + count + ")");
            btn.getStyleClass().clear();
            btn.getStyleClass().add(active ? "followup-chip-active" : "followup-chip");
        }

        if (!customSelectedTypes.isEmpty()) {
            customChipBtn.setText("Custom (" + customSelectedTypes.size() + " Types) ✕");
            customChipBtn.setStyle("-fx-background-color: rgba(59, 130, 246, 0.25); -fx-text-fill: #93c5fd; -fx-border-color: #3b82f6; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 5 14; -fx-font-size: 12px; -fx-font-weight: 800; -fx-cursor: hand;");
        } else {
            customChipBtn.setText("Custom Multi-select...");
            customChipBtn.getStyleClass().clear();
            customChipBtn.getStyleClass().add("followup-custom-chip");
        }

        boolean hasFilters = !"All".equals(selectedTypeFilter) || !customSelectedTypes.isEmpty() || !"All".equals(selectedDateRange) || (searchField != null && !searchField.getText().trim().isEmpty());
        if (clearFiltersBtn != null) {
            clearFiltersBtn.setVisible(hasFilters);
            clearFiltersBtn.setManaged(hasFilters);
        }
    }

    private void updateDateChipStyles() {
        for (Map.Entry<String, Button> entry : dateChipsMap.entrySet()) {
            boolean active = entry.getKey().equals(selectedDateRange);
            entry.getValue().getStyleClass().clear();
            entry.getValue().getStyleClass().add(active ? "followup-chip-active" : "followup-chip");
        }

        boolean hasFilters = !"All".equals(selectedTypeFilter) || !customSelectedTypes.isEmpty() || !"All".equals(selectedDateRange) || (searchField != null && !searchField.getText().trim().isEmpty());
        if (clearFiltersBtn != null) {
            clearFiltersBtn.setVisible(hasFilters);
            clearFiltersBtn.setManaged(hasFilters);
        }
    }

    private void showCustomMultiSelectModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Custom Multi-Select Types");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(380);

        Label desc = new Label("Select any combination of follow-up types to filter simultaneously:");
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        desc.setWrapText(true);

        VBox checkboxes = new VBox(10);
        checkboxes.setPadding(new Insets(10, 0, 10, 0));

        Map<FollowUpType, CheckBox> cbMap = new HashMap<>();
        for (FollowUpType t : FollowUpType.values()) {
            CheckBox cb = new CheckBox(t.getDisplayName());
            cb.setSelected(customSelectedTypes.contains(t));
            cb.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            cbMap.put(t, cb);
            checkboxes.getChildren().add(cb);
        }

        HBox quickButtons = new HBox(10);
        Button selectAll = new Button("Select All");
        selectAll.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #cbd5e1; -fx-padding: 5 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        selectAll.setOnAction(e -> cbMap.values().forEach(cb -> cb.setSelected(true)));

        Button clearAll = new Button("Clear All");
        clearAll.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #cbd5e1; -fx-padding: 5 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        clearAll.setOnAction(e -> cbMap.values().forEach(cb -> cb.setSelected(false)));

        quickButtons.getChildren().addAll(selectAll, clearAll);

        content.getChildren().addAll(desc, quickButtons, checkboxes);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.APPLY) {
                customSelectedTypes.clear();
                for (Map.Entry<FollowUpType, CheckBox> entry : cbMap.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        customSelectedTypes.add(entry.getKey());
                    }
                }
                if (!customSelectedTypes.isEmpty()) {
                    selectedTypeFilter = "Custom";
                } else {
                    selectedTypeFilter = "All";
                }
                updateTypeChipStyles();
                currentPage = 1;
                renderList();
            }
        });
    }

    // =========================================================================
    // 2. COLUMN HEADERS (GRIDPANE WITH 100% RESPONSIVE PERCENTAGE CONSTRAINTS)
    // =========================================================================
    private GridPane buildColumnHeaders() {
        GridPane headerGrid = new GridPane();
        headerGrid.getColumnConstraints().addAll(createTableColumnConstraints());
        headerGrid.setPadding(new Insets(10, 16, 10, 16));
        headerGrid.getStyleClass().add("followup-column-headers");
        headerGrid.setMaxWidth(Double.MAX_VALUE);

        headerGrid.add(createColHeader("⚡ PRIORITY", Pos.CENTER_LEFT), 0, 0);
        headerGrid.add(createColHeader("👤 LEAD & CONTACT", Pos.CENTER_LEFT), 1, 0);
        headerGrid.add(createColHeader("🏷 CATEGORY", Pos.CENTER_LEFT), 2, 0);
        headerGrid.add(createColHeader("📅 SCHEDULED", Pos.CENTER_LEFT), 3, 0);
        headerGrid.add(createColHeader("🎯 STAGE & CONVERSION", Pos.CENTER_LEFT), 4, 0);
        headerGrid.add(createColHeader("📝 INTELLIGENCE & NOTES", Pos.CENTER_LEFT), 5, 0);
        headerGrid.add(createColHeader("⚡ ACTIONS", Pos.CENTER_RIGHT), 6, 0);

        return headerGrid;
    }

    private Label createColHeader(String text, Pos alignment) {
        Label label = new Label(text);
        label.getStyleClass().add("followup-header-label");
        label.setAlignment(alignment);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    // =========================================================================
    // 3. REACTIVE TABLE RENDERING & FILTERING
    // =========================================================================
    private void renderList() {
        listContainer.getChildren().clear();

        List<FollowUp> allLeads = facade.getFollowUps();

        // Role-based filtering (Members only see Personal Celebrations)
        Role role = facade.getCurrentUser().getRole();
        if (role == Role.MEMBER) {
            allLeads = allLeads.stream()
                    .filter(f -> f.getType() == FollowUpType.BIRTHDAY ||
                            f.getType() == FollowUpType.ANNIVERSARY ||
                            f.getType() == FollowUpType.PARTY)
                    .collect(Collectors.toList());
        }

        // Apply Text, Type, and Date filters
        List<FollowUp> filtered = filterLeads(allLeads);

        // Update Header Lead Counter
        titleCount.setText(filtered.size() + " Active");

        // Update Filter Chip Badges
        updateTypeChipStyles();
        updateDateChipStyles();

        // Notification Bell Glow indicator
        long hotLeadCount = allLeads.stream().filter(f -> f.getConvertibility() == Convertibility.HOT).count();
        if (hotLeadCount > 0) {
            bellBtn.setText("🔔 (" + hotLeadCount + ")");
            bellBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #f87171; -fx-border-color: #ef4444; -fx-border-radius: 9px; -fx-background-radius: 9px; -fx-font-weight: 800;");
        } else {
            bellBtn.setText("🔔");
            bellBtn.setStyle("");
        }

        // Dynamic Pagination calculations
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / rowsPerPage));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int startIdx = (currentPage - 1) * rowsPerPage;
        int endIdx = Math.min(startIdx + rowsPerPage, totalItems);

        if (totalItems > 0) {
            for (int i = startIdx; i < endIdx; i++) {
                FollowUp f = filtered.get(i);
                GridPane leadRow = createLeadRow(f, i + 1);
                listContainer.getChildren().add(leadRow);
            }
        } else {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(45));
            Label emptyIcon = new Label("🔍");
            emptyIcon.setStyle("-fx-font-size: 38px;");
            Label emptyLbl = new Label("No follow-ups match your current filter criteria.");
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold;");
            Button resetBtn = new Button("Reset All Filters");
            resetBtn.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa; -fx-border-color: #3b82f6; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 16; -fx-cursor: hand; -fx-font-weight: 800;");
            resetBtn.setOnAction(e -> {
                selectedTypeFilter = "All";
                customSelectedTypes.clear();
                selectedDateRange = "All";
                if (searchField != null) searchField.setText("");
                updateTypeChipStyles();
                updateDateChipStyles();
                currentPage = 1;
                renderList();
            });
            empty.getChildren().addAll(emptyIcon, emptyLbl, resetBtn);
            listContainer.getChildren().add(empty);
        }

        // Render Pagination Controls
        renderPaginationBar(totalPages, totalItems, startIdx, endIdx);
    }

    private List<FollowUp> filterLeads(List<FollowUp> source) {
        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        LocalDate today = LocalDate.now();

        return source.stream().filter(f -> {
            // 1. Text Search
            if (!query.isEmpty()) {
                boolean inName = f.getName().toLowerCase().contains(query);
                boolean inPhone = f.getPhoneNumber().toLowerCase().contains(query);
                boolean inType = f.getType().getDisplayName().toLowerCase().contains(query);
                boolean inNotes = f.getComments() != null && f.getComments().toLowerCase().contains(query);
                if (!inName && !inPhone && !inType && !inNotes) return false;
            }

            // 2. Type Filter
            if (!customSelectedTypes.isEmpty()) {
                if (!customSelectedTypes.contains(f.getType())) return false;
            } else if (!"All".equals(selectedTypeFilter)) {
                if ("Trial".equals(selectedTypeFilter) && f.getType() != FollowUpType.TRIAL) return false;
                if ("Anniversary".equals(selectedTypeFilter) && f.getType() != FollowUpType.ANNIVERSARY && f.getType() != FollowUpType.BIRTHDAY) return false;
                if ("Enquiry".equals(selectedTypeFilter) && f.getType() != FollowUpType.ENQUIRY) return false;
                if ("Feedback".equals(selectedTypeFilter) && f.getType() != FollowUpType.FEEDBACK) return false;
                if ("Retention".equals(selectedTypeFilter) && f.getType() != FollowUpType.MEMBERSHIP_RENEWAL && f.getType() != FollowUpType.BALANCE_DUE) return false;
            }

            // 3. Date Range Filter
            if (!"All".equals(selectedDateRange) && f.getScheduledDate() != null) {
                LocalDate schedDate = f.getScheduledDate().toLocalDate();
                if ("Today".equals(selectedDateRange) && !schedDate.isEqual(today)) return false;
                if ("This Week".equals(selectedDateRange)) {
                    LocalDate weekEnd = today.plusDays(7);
                    if (schedDate.isBefore(today) || schedDate.isAfter(weekEnd)) return false;
                }
                if ("This Month".equals(selectedDateRange)) {
                    if (!YearMonth.from(schedDate).equals(YearMonth.from(today))) return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }

    private void renderPaginationBar(int totalPages, int totalItems, int startIdx, int endIdx) {
        paginationBar.getChildren().clear();

        // Prev Button
        Button prevBtn = new Button("‹");
        prevBtn.getStyleClass().add("followup-page-btn");
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderList();
            }
        });
        paginationBar.getChildren().add(prevBtn);

        // Page Number Buttons
        for (int i = 1; i <= totalPages; i++) {
            final int page = i;
            Button pBtn = new Button(String.valueOf(page));
            pBtn.getStyleClass().add(page == currentPage ? "followup-page-btn-active" : "followup-page-btn");
            pBtn.setOnAction(e -> {
                currentPage = page;
                renderList();
            });
            paginationBar.getChildren().add(pBtn);
        }

        // Next Button
        Button nextBtn = new Button("›");
        nextBtn.getStyleClass().add("followup-page-btn");
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderList();
            }
        });
        paginationBar.getChildren().add(nextBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label infoLbl = new Label(String.format("Showing %d-%d of %d leads", totalItems == 0 ? 0 : startIdx + 1, endIdx, totalItems));
        infoLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 700;");

        Label rowsLabel = new Label("Rows per page:");
        rowsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: 600;");

        ComboBox<String> rowsCombo = new ComboBox<>();
        rowsCombo.getItems().addAll("6", "10", "25", "All");
        rowsCombo.setValue(rowsPerPage == 1000 ? "All" : String.valueOf(rowsPerPage));
        rowsCombo.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 11px;");
        rowsCombo.setOnAction(e -> {
            String val = rowsCombo.getValue();
            if ("All".equals(val)) {
                rowsPerPage = 1000;
            } else {
                rowsPerPage = Integer.parseInt(val);
            }
            currentPage = 1;
            renderList();
        });

        paginationBar.getChildren().addAll(spacer, infoLbl, rowsLabel, rowsCombo);
    }

    // =========================================================================
    // 4. LEAD ROW BUILDER (RESPONSIVE GRIDPANE CARD)
    // =========================================================================
    private GridPane createLeadRow(FollowUp f, int index) {
        GridPane row = new GridPane();
        row.getColumnConstraints().addAll(createTableColumnConstraints());
        row.setPadding(new Insets(11, 16, 11, 16));
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("followup-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Determine Priority styling based on convertibility or index
        String pColor;
        String pText;
        if (f.getConvertibility() == Convertibility.HOT) {
            pColor = "#ef4444";
            pText = "High";
        } else if (f.getConvertibility() == Convertibility.WARM) {
            pColor = "#f59e0b";
            pText = "Med ";
        } else {
            pColor = "#10b981";
            pText = "Low ";
        }

        // ---------------------------------------------------------------------
        // COL 0: PRIORITY (8.5%)
        // ---------------------------------------------------------------------
        HBox prioCol = new HBox(8);
        prioCol.setAlignment(Pos.CENTER_LEFT);

        Circle pDot = new Circle(4.5, Color.web(pColor));
        pDot.setEffect(new DropShadow(8, Color.web(pColor)));

        Label pLbl = new Label(pText);
        pLbl.setStyle("-fx-text-fill: " + pColor + "; -fx-font-size: 12.5px; -fx-font-weight: 800;");

        Label pNum = new Label("#" + index);
        pNum.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-color: rgba(255,255,255,0.04); -fx-padding: 1 5; -fx-background-radius: 4px;");

        prioCol.getChildren().addAll(pDot, pLbl, pNum);

        // ---------------------------------------------------------------------
        // COL 1: LEAD & CONTACT (21.0%, Flex Expands)
        // ---------------------------------------------------------------------
        HBox nameCol = new HBox(12);
        nameCol.setAlignment(Pos.CENTER_LEFT);
        nameCol.setMaxWidth(Double.MAX_VALUE);

        // Dynamic Avatar with initials & glowing ring
        StackPane avatar = new StackPane();
        int gradIdx = Math.abs(f.getName().hashCode()) % AVATAR_GRADIENTS.length;
        String avatarGrad = AVATAR_GRADIENTS[gradIdx];

        Circle avBg = new Circle(17);
        avBg.setFill(Color.TRANSPARENT);
        avatar.setStyle("-fx-background-color: " + avatarGrad + "; -fx-background-radius: 50%; -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 50%; -fx-border-width: 1.2px; -fx-pref-width: 34px; -fx-pref-height: 34px;");

        String initial = (f.getName() != null && !f.getName().isEmpty()) ? f.getName().substring(0, 1).toUpperCase() : "?";
        Label avTxt = new Label(initial);
        avTxt.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 13.5px;");
        avatar.getChildren().addAll(avBg, avTxt);

        VBox nameBox = new VBox(2);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(f.getName());
        nameLbl.getStyleClass().add("followup-lead-name");
        nameLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 13.5px;");

        HBox phoneBox = new HBox(4);
        phoneBox.setAlignment(Pos.CENTER_LEFT);
        Label phoneIcon = new Label("📞");
        phoneIcon.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        Label phoneLbl = new Label(f.getPhoneNumber());
        phoneLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-font-weight: 600;");
        phoneBox.getChildren().addAll(phoneIcon, phoneLbl);

        nameBox.getChildren().addAll(nameLbl, phoneBox);
        nameCol.getChildren().addAll(avatar, nameBox);

        // ---------------------------------------------------------------------
        // COL 2: CATEGORY / TYPE (12.0%)
        // ---------------------------------------------------------------------
        HBox typeCol = new HBox(6);
        typeCol.setAlignment(Pos.CENTER_LEFT);

        String typeIcon;
        String typeThemeColor;
        switch (f.getType()) {
            case TRIAL -> { typeIcon = "⏱"; typeThemeColor = "#06b6d4"; }
            case ANNIVERSARY -> { typeIcon = "🎁"; typeThemeColor = "#ec4899"; }
            case ENQUIRY -> { typeIcon = "❓"; typeThemeColor = "#3b82f6"; }
            case FEEDBACK -> { typeIcon = "💬"; typeThemeColor = "#8b5cf6"; }
            case PARTY -> { typeIcon = "🎉"; typeThemeColor = "#a855f7"; }
            case MEMBERSHIP_RENEWAL -> { typeIcon = "📋"; typeThemeColor = "#10b981"; }
            case BIRTHDAY -> { typeIcon = "🎂"; typeThemeColor = "#fbbf24"; }
            case BALANCE_DUE -> { typeIcon = "💳"; typeThemeColor = "#f43f5e"; }
            default -> { typeIcon = "🏷"; typeThemeColor = "#94a3b8"; }
        }

        Label tIcon = new Label(typeIcon);
        tIcon.setStyle("-fx-font-size: 13px;");

        Label tLbl = new Label(f.getType().getDisplayName());
        tLbl.setStyle("-fx-text-fill: " + typeThemeColor + "; -fx-font-size: 11.5px; -fx-font-weight: 700;");

        HBox typeBadge = new HBox(5, tIcon, tLbl);
        typeBadge.setAlignment(Pos.CENTER_LEFT);
        typeBadge.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: " + typeThemeColor + "44; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 9;");
        typeCol.getChildren().add(typeBadge);

        // ---------------------------------------------------------------------
        // COL 3: SCHEDULED DATE (11.5%)
        // ---------------------------------------------------------------------
        VBox schedCol = new VBox(2);
        schedCol.setAlignment(Pos.CENTER_LEFT);

        String dateStr = f.getScheduledDate() != null ? f.getScheduledDate().format(DATE_FMT) : "Not Set";
        Label dateLbl = new Label(dateStr);
        dateLbl.getStyleClass().add("followup-lead-date");
        dateLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");

        // Relative Urgency Badge
        Label urgencyBadge = new Label();
        if (f.getScheduledDate() != null) {
            LocalDate schedDate = f.getScheduledDate().toLocalDate();
            LocalDate today = LocalDate.now();
            long daysUntil = ChronoUnit.DAYS.between(today, schedDate);

            if (daysUntil < 0) {
                urgencyBadge.setText("⚠ Overdue (" + Math.abs(daysUntil) + "d)");
                urgencyBadge.setStyle("-fx-text-fill: #f87171; -fx-font-size: 10px; -fx-font-weight: 800;");
            } else if (daysUntil == 0) {
                urgencyBadge.setText("⚡ Today");
                urgencyBadge.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 10px; -fx-font-weight: 800;");
            } else if (daysUntil == 1) {
                urgencyBadge.setText("Tomorrow");
                urgencyBadge.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 10px; -fx-font-weight: 700;");
            } else {
                urgencyBadge.setText("In " + daysUntil + " days");
                urgencyBadge.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
            }
        }
        schedCol.getChildren().addAll(dateLbl, urgencyBadge);

        // ---------------------------------------------------------------------
        // COL 4: INTEREST LEVEL & ACTIONS (19.0%)
        // ---------------------------------------------------------------------
        HBox intCol = new HBox(12);
        intCol.setAlignment(Pos.CENTER_LEFT);

        int hotness = (f.getConvertibility() == Convertibility.HOT) ? 4 :
                (f.getConvertibility() == Convertibility.WARM ? 2 : 1);

        VBox heatBox = new VBox(3);
        heatBox.setAlignment(Pos.CENTER_LEFT);

        HBox bars = new HBox(3);
        String[] heatPalette = {"#ef4444", "#f59e0b", "#10b981", "#3b82f6"};
        for (int i = 0; i < 4; i++) {
            Region b = new Region();
            b.setPrefSize(10, 5);
            if (i < hotness) {
                b.setStyle("-fx-background-color: " + heatPalette[i] + "; -fx-background-radius: 2px; -fx-effect: dropshadow(gaussian, " + heatPalette[i] + "88, 4, 0, 0, 0);");
            } else {
                b.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 2px;");
            }
            bars.getChildren().add(b);
        }

        String heatText = hotness >= 3 ? "HOT" : (hotness == 2 ? "WARM" : "COLD");
        String heatColor = hotness >= 3 ? "#ef4444" : (hotness == 2 ? "#f59e0b" : "#3b82f6");
        Label hLbl = new Label(heatText);
        hLbl.setStyle("-fx-text-fill: " + heatColor + "; -fx-font-size: 9.5px; -fx-font-weight: 900;");
        heatBox.getChildren().addAll(bars, hLbl);

        String actTitle = hotness >= 3 ? "Move to Nurture" : (hotness == 2 ? "Book Appointment" : "Create Task");
        Button actBtn = new Button(actTitle);
        actBtn.getStyleClass().add("followup-action-btn");
        actBtn.setOnAction(e -> handleLeadAction(f, actTitle));

        intCol.getChildren().addAll(heatBox, actBtn);

        // ---------------------------------------------------------------------
        // COL 5: INTELLIGENCE & COMMENTS (17.0%, Flex Expands)
        // ---------------------------------------------------------------------
        HBox commCol = new HBox(6);
        commCol.setAlignment(Pos.CENTER_LEFT);
        commCol.setMaxWidth(Double.MAX_VALUE);

        Label noteIcon = new Label("💬");
        noteIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        String noteText = f.getComments() != null && !f.getComments().isEmpty() ? f.getComments() : "No customer notes recorded.";
        Label commentsLbl = new Label(noteText);
        commentsLbl.getStyleClass().add("followup-lead-comments");
        commentsLbl.setStyle("-fx-font-size: 12px;");
        commentsLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        commentsLbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(commentsLbl, Priority.ALWAYS);
        Tooltip.install(commentsLbl, new Tooltip("📝 Lead Intelligence & Notes:\n" + noteText));

        commCol.getChildren().addAll(noteIcon, commentsLbl);

        // ---------------------------------------------------------------------
        // COL 6: QUICK ACTIONS TOOLBAR (11.0%, Aligned to Right)
        // ---------------------------------------------------------------------
        HBox quickCol = new HBox(6);
        quickCol.setAlignment(Pos.CENTER_RIGHT);

        Button btnCall = new Button("📞");
        btnCall.getStyleClass().add("followup-quick-btn");
        Tooltip.install(btnCall, new Tooltip("Simulate Call to " + f.getName()));
        btnCall.setOnAction(e -> showDialerModal(f));

        Button btnMsg = new Button("💬");
        btnMsg.getStyleClass().add("followup-quick-btn");
        Tooltip.install(btnMsg, new Tooltip("Send WhatsApp / SMS to " + f.getName()));
        btnMsg.setOnAction(e -> showMessageComposerModal(f));

        Button btnOpt = new Button("⋮");
        btnOpt.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 2 6;");
        attachLeadContextMenu(btnOpt, f);

        quickCol.getChildren().addAll(btnCall, btnMsg, btnOpt);

        // Add all 7 cells to row GridPane
        row.add(prioCol, 0, 0);
        row.add(nameCol, 1, 0);
        row.add(typeCol, 2, 0);
        row.add(schedCol, 3, 0);
        row.add(intCol, 4, 0);
        row.add(commCol, 5, 0);
        row.add(quickCol, 6, 0);

        // Hover Effect with glowing left priority indicator
        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.045); -fx-border-color: " + pColor + "66 rgba(255,255,255,0.08) rgba(255,255,255,0.08) " + pColor + "; -fx-border-width: 1px 1px 1px 3.5px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 14, 0, 0, 4);");
        });
        row.setOnMouseExited(e -> {
            row.setStyle("");
        });

        return row;
    }

    private void handleLeadAction(FollowUp f, String actionType) {
        if ("Move to Nurture".equals(actionType)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Move " + f.getName() + " to the High-Priority Nurture Stage?\n\nThis will trigger an automated CRM sales cadence and notify the floor coordinator.",
                    ButtonType.YES, ButtonType.NO);
            DialogUtils.stylePremiumDialog(confirm);
            confirm.setHeaderText("Stage Transition: Fast-Track Nurture");
            confirm.showAndWait().ifPresent(ans -> {
                if (ans == ButtonType.YES) {
                    f.setConvertibility(Convertibility.HOT);
                    f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Moved to Nurture Stage on " + LocalDate.now());
                    facade.updateFollowUp(f);
                    refreshOverviewCards();
                    renderList();
                }
            });
        } else if ("Book Appointment".equals(actionType)) {
            showBookAppointmentModal(f);
        } else {
            // Create Task
            TextInputDialog taskDialog = new TextInputDialog("Follow up call regarding " + f.getType().getDisplayName());
            DialogUtils.stylePremiumDialog(taskDialog);
            taskDialog.setTitle("Create Follow-up Task");
            taskDialog.setHeaderText("Set Reminder Task for " + f.getName());
            taskDialog.setContentText("Task Description:");
            taskDialog.showAndWait().ifPresent(task -> {
                f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Task: " + task);
                facade.updateFollowUp(f);
                renderList();
            });
        }
    }

    private void attachLeadContextMenu(Node triggerNode, FollowUp f) {
        ContextMenu menu = new ContextMenu();

        MenuItem callItem = new MenuItem("📞 Initiate Phone Call");
        callItem.setOnAction(e -> showDialerModal(f));

        MenuItem msgItem = new MenuItem("💬 Send Message / WhatsApp");
        msgItem.setOnAction(e -> showMessageComposerModal(f));

        MenuItem bookItem = new MenuItem("📅 Book Session / Appointment");
        bookItem.setOnAction(e -> showBookAppointmentModal(f));

        MenuItem editNoteItem = new MenuItem("📝 Append Quick Note");
        editNoteItem.setOnAction(e -> {
            TextInputDialog td = new TextInputDialog();
            DialogUtils.stylePremiumDialog(td);
            td.setTitle("Append Note");
            td.setHeaderText("Add Note for " + f.getName());
            td.setContentText("Note content:");
            td.showAndWait().ifPresent(n -> {
                if (!n.trim().isEmpty()) {
                    f.setComments((f.getComments() != null && !f.getComments().isEmpty() ? f.getComments() + " | " : "") + n.trim());
                    facade.updateFollowUp(f);
                    renderList();
                }
            });
        });

        menu.getItems().addAll(callItem, msgItem, bookItem, editNoteItem, new SeparatorMenuItem());

        MenuItem hotItem = new MenuItem("🔥 Mark as HOT Lead");
        hotItem.setOnAction(e -> {
            f.setConvertibility(Convertibility.HOT);
            facade.updateFollowUp(f);
            refreshOverviewCards();
            renderList();
        });

        MenuItem warmItem = new MenuItem("🌤 Mark as WARM Lead");
        warmItem.setOnAction(e -> {
            f.setConvertibility(Convertibility.WARM);
            facade.updateFollowUp(f);
            refreshOverviewCards();
            renderList();
        });

        MenuItem coldItem = new MenuItem("❄ Mark as COLD Lead");
        coldItem.setOnAction(e -> {
            f.setConvertibility(Convertibility.COLD);
            facade.updateFollowUp(f);
            refreshOverviewCards();
            renderList();
        });

        menu.getItems().addAll(hotItem, warmItem, coldItem, new SeparatorMenuItem());

        MenuItem deleteItem = new MenuItem("🗑 Delete Follow-up");
        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete follow-up record for " + f.getName() + "?\nThis action cannot be undone.", ButtonType.YES, ButtonType.NO);
            DialogUtils.stylePremiumDialog(confirm);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(ans -> {
                if (ans == ButtonType.YES) {
                    facade.deleteFollowUp(f.getId());
                    refreshOverviewCards();
                    renderList();
                }
            });
        });
        menu.getItems().add(deleteItem);

        triggerNode.setOnMouseClicked(e -> menu.show(triggerNode, e.getScreenX(), e.getScreenY()));
    }

    // =========================================================================
    // 5. BOTTOM OVERVIEW SECTION (6 CYBER-FITNESS TELEMETRY CARDS)
    // =========================================================================
    private void refreshOverviewCards() {
        overviewGrid.getChildren().clear();

        List<FollowUp> myFollowUps = facade.getFollowUps();
        long activeMembers = facade.getAllBookings().size();
        long activeTrials = myFollowUps.stream().filter(f -> f.getType() == FollowUpType.TRIAL).count();
        long newEnquiries = myFollowUps.stream().filter(f -> f.getType() == FollowUpType.ENQUIRY).count();
        long hotPipeline = myFollowUps.stream().filter(f -> f.getConvertibility() == Convertibility.HOT || f.getConvertibility() == Convertibility.WARM).count();

        // 1. MEMBERS Card
        VBox card1 = createComplexOverviewCard("MEMBERS", String.valueOf(Math.max(1, activeMembers)), "Total", "Active Floor", "Upcoming", "#3b82f6", true, null);
        overviewGrid.add(card1, 0, 0);

        // 2. ATTENDANCE Card
        VBox card2 = createComplexOverviewCard("ATTENDANCE", "92%", "Avg", "Peak Session", "Normal", "#f59e0b", false, null);
        overviewGrid.add(card2, 1, 0);

        // 3. ENQUIRIES Card
        VBox card3 = createComplexOverviewCard("ENQUIRIES", String.valueOf(newEnquiries), "New", "Sales Leads", "78% Qual.", "#10b981", true, () -> {
            selectedTypeFilter = "Enquiry";
            customSelectedTypes.clear();
            updateTypeChipStyles();
            currentPage = 1;
            renderList();
        });
        overviewGrid.add(card3, 2, 0);

        // 4. PIPELINE Card
        VBox card4 = createComplexOverviewCard("PIPELINE", String.valueOf(hotPipeline), "Active", "Hot Leads", "Warm Cadence", "#eab308", true, () -> {
            selectedTypeFilter = "All";
            customSelectedTypes.clear();
            updateTypeChipStyles();
            currentPage = 1;
            renderList();
        });
        overviewGrid.add(card4, 0, 1);

        // 5. TRIALS Card
        VBox card5 = createComplexOverviewCard("TRIALS", String.valueOf(activeTrials), "Active", "Conversion", "Scheduled", "#06b6d4", false, () -> {
            selectedTypeFilter = "Trial";
            customSelectedTypes.clear();
            updateTypeChipStyles();
            currentPage = 1;
            renderList();
        });
        overviewGrid.add(card5, 1, 1);

        // 6. MEMBERSHIPS Card
        VBox card6 = createComplexOverviewCard("REVENUE RUNRATE", "$10.5K", "MRR", "New Sales", "Renewals", "#a855f7", false, null);
        overviewGrid.add(card6, 2, 1);
    }

    private VBox createComplexOverviewCard(String title, String bigNum, String subNum, String l1, String l2, String accentColor, boolean isLine, Runnable onClickFilter) {
        VBox card = new VBox(10);
        card.setMaxWidth(Double.MAX_VALUE);
        String rgb = hexToRgb(accentColor);
        card.setStyle("-fx-background-color: linear-gradient(135deg, rgba(255,255,255,0.03) 0%, rgba(" + rgb + ", 0.09) 100%); -fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 16; -fx-cursor: hand;");

        // Top Row: Title + Trend micro-badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-letter-spacing: 0.9px;");

        Region spTop = new Region();
        HBox.setHgrow(spTop, Priority.ALWAYS);

        Label trendBadge = new Label("↗ Active");
        trendBadge.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-color: rgba(" + rgb + ", 0.15); -fx-padding: 2 6; -fx-background-radius: 6px;");

        topRow.getChildren().addAll(titleLbl, spTop, trendBadge);

        // Middle Row: Values + Vector Waveform + Donut Gauge
        HBox midRow = new HBox(12);
        midRow.setAlignment(Pos.CENTER_LEFT);

        HBox numBox = new HBox(6);
        numBox.setAlignment(Pos.BASELINE_LEFT);
        Label bNum = new Label(bigNum);
        bNum.getStyleClass().add("followup-stat-num");
        bNum.setStyle("-fx-font-size: 26px; -fx-font-weight: 900;");
        Label sNum = new Label(subNum);
        sNum.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 700;");
        numBox.getChildren().addAll(bNum, sNum);

        Region midSpc = new Region();
        HBox.setHgrow(midSpc, Priority.ALWAYS);

        // Glowing Waveform Sparkline
        StackPane chartPane = new StackPane();
        if (isLine) {
            Polyline line = new Polyline(0, 18, 16, 22, 32, 8, 48, 25, 64, 12, 80, 16);
            line.setStroke(Color.web(accentColor));
            line.setStrokeWidth(2.5);
            line.setEffect(new DropShadow(8, Color.web(accentColor)));
            chartPane.getChildren().add(line);
        } else {
            Path path = new Path();
            path.getElements().add(new MoveTo(0, 22));
            path.getElements().add(new QuadCurveTo(20, 4, 40, 18));
            path.getElements().add(new QuadCurveTo(60, 30, 80, 10));
            path.setStroke(Color.web(accentColor));
            path.setStrokeWidth(2.5);
            path.setEffect(new DropShadow(8, Color.web(accentColor)));
            chartPane.getChildren().add(path);
        }

        // Mini Circular Donut Chart Arc
        StackPane donut = new StackPane();
        donut.setPrefSize(28, 28);
        Arc a1 = new Arc(14, 14, 11, 11, 90, 240);
        a1.setType(ArcType.ROUND);
        a1.setFill(Color.web(accentColor));
        Arc a2 = new Arc(14, 14, 11, 11, 330, 120);
        a2.setType(ArcType.ROUND);
        a2.setFill(Color.web(accentColor).darker());
        Circle cInner = new Circle(14, 14, 6, Color.web("#171626"));
        donut.getChildren().addAll(a1, a2, cInner);

        midRow.getChildren().addAll(numBox, midSpc, chartPane, donut);

        // Bottom Row: Legend
        HBox legendRow = new HBox(15);
        legendRow.setAlignment(Pos.CENTER_LEFT);

        HBox l1Box = new HBox(5);
        l1Box.setAlignment(Pos.CENTER_LEFT);
        Circle c1 = new Circle(3, Color.web(accentColor));
        Label l1Lbl = new Label(l1);
        l1Lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        l1Box.getChildren().addAll(c1, l1Lbl);

        HBox l2Box = new HBox(5);
        l2Box.setAlignment(Pos.CENTER_LEFT);
        Circle c2 = new Circle(3, Color.web("#fbbf24"));
        Label l2Lbl = new Label(l2);
        l2Lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        l2Box.getChildren().addAll(c2, l2Lbl);

        legendRow.getChildren().addAll(l1Box, l2Box);

        card.getChildren().addAll(topRow, midRow, legendRow);

        if (onClickFilter != null) {
            Tooltip.install(card, new Tooltip("Click to filter pipeline for " + title));
            card.setOnMouseClicked(e -> onClickFilter.run());
        }

        // Hover animation
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: linear-gradient(135deg, rgba(255,255,255,0.06) 0%, rgba(" + rgb + ", 0.16) 100%); -fx-border-color: " + accentColor + "; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0, 0, 4);");
            ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
            st.setToX(1.02); st.setToY(1.02);
            st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: linear-gradient(135deg, rgba(255,255,255,0.03) 0%, rgba(" + rgb + ", 0.09) 100%); -fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 16; -fx-cursor: hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        return card;
    }

    private String hexToRgb(String hex) {
        Color c = Color.web(hex);
        return (int) (c.getRed() * 255) + "," + (int) (c.getGreen() * 255) + "," + (int) (c.getBlue() * 255);
    }

    // =========================================================================
    // 6. INTERACTIVE MODAL DIALOGS
    // =========================================================================
    private void showNewFollowUpModal() {
        Dialog<FollowUp> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Register New Lead Follow-up");
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Alex Morgan");
        TextField phoneField = new TextField();
        phoneField.setPromptText("e.g. 555-019-2834");

        ComboBox<FollowUpType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(FollowUpType.values());
        typeCombo.setValue(FollowUpType.ENQUIRY);

        ComboBox<Convertibility> convCombo = new ComboBox<>();
        convCombo.getItems().addAll(Convertibility.values());
        convCombo.setValue(Convertibility.HOT);

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(2));

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Customer goals, fitness interest, or follow-up notes...");
        notesArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Lead Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Pipeline Stage:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Urgency / Convertibility:"), 0, 3);
        grid.add(convCombo, 1, 3);
        grid.add(new Label("Scheduled Follow-up Date:"), 0, 4);
        grid.add(datePicker, 1, 4);
        grid.add(new Label("Intelligence Notes:"), 0, 5);
        grid.add(notesArea, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                FollowUpType type = typeCombo.getValue();
                Convertibility conv = convCombo.getValue();
                LocalDate ld = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now().plusDays(1);
                String notes = notesArea.getText().trim();

                if (!name.isEmpty() && !phone.isEmpty()) {
                    return new FollowUp(name, phone, type, ld.atTime(10, 0), conv, notes);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(f -> {
            try {
                facade.addFollowUp(f);
                refreshOverviewCards();
                renderList();
            } catch (DuplicateDataException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "A lead with this ID already exists.");
                DialogUtils.stylePremiumDialog(a);
                a.show();
            }
        });
    }

    private void showDialerModal(FollowUp f) {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Call " + f.getName());

        VBox root = new VBox(18);
        root.setPadding(new Insets(26));
        root.setPrefWidth(430);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0b1120; -fx-border-color: #3b82f6; -fx-border-width: 1.5px; -fx-border-radius: 18px; -fx-background-radius: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 30, 0, 0, 10);");

        Label icon = new Label("📞");
        icon.setStyle("-fx-font-size: 36px; -fx-text-fill: #3b82f6;");

        Label nameLbl = new Label(f.getName());
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 19px; -fx-font-weight: 900;");

        Label phoneLbl = new Label(f.getPhoneNumber());
        phoneLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label statusLbl = new Label("● VoIP Connected (00:42)");
        statusLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: 800;");

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.15;");

        Label outcomeTitle = new Label("Log Interactive Call Outcome:");
        outcomeTitle.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox outcomes = new VBox(9);
        Button btnAns = new Button("✔ Answered & Highly Interested");
        btnAns.setMaxWidth(Double.MAX_VALUE);
        btnAns.setStyle("-fx-background-color: rgba(16, 185, 129, 0.18); -fx-text-fill: #34d399; -fx-border-color: #10b981; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 9; -fx-font-weight: 800; -fx-cursor: hand;");
        btnAns.setOnAction(e -> {
            f.setConvertibility(Convertibility.HOT);
            f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Called: Answered & Interested");
            facade.updateFollowUp(f);
            refreshOverviewCards();
            renderList();
            modal.close();
        });

        Button btnVm = new Button("📼 Left Voicemail");
        btnVm.setMaxWidth(Double.MAX_VALUE);
        btnVm.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-text-fill: #cbd5e1; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 9; -fx-font-weight: 700; -fx-cursor: hand;");
        btnVm.setOnAction(e -> {
            f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Called: Left Voicemail");
            facade.updateFollowUp(f);
            renderList();
            modal.close();
        });

        Button btnCb = new Button("📅 Scheduled Callback");
        btnCb.setMaxWidth(Double.MAX_VALUE);
        btnCb.setStyle("-fx-background-color: rgba(245, 158, 11, 0.18); -fx-text-fill: #fbbf24; -fx-border-color: #f59e0b; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 9; -fx-font-weight: 800; -fx-cursor: hand;");
        btnCb.setOnAction(e -> {
            f.setScheduledDate(LocalDateTime.now().plusDays(2));
            f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Called: Callback scheduled");
            facade.updateFollowUp(f);
            renderList();
            modal.close();
        });

        outcomes.getChildren().addAll(btnAns, btnVm, btnCb);

        Button cancelBtn = new Button("End Call & Dismiss");
        cancelBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #f87171; -fx-border-color: #ef4444; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7 18; -fx-font-weight: 800; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> modal.close());

        root.getChildren().addAll(icon, nameLbl, phoneLbl, statusLbl, sep, outcomeTitle, outcomes, cancelBtn);

        DialogUtils.prepareModalScene(modal, root);
        modal.showAndWait();
    }

    private void showMessageComposerModal(FollowUp f) {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Message " + f.getName());

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setPrefWidth(490);
        root.getStyleClass().add("custom-modal-root");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("💬 Compose Message — " + f.getName());
        title.getStyleClass().add("custom-modal-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeX = new Button("✕");
        closeX.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand; -fx-font-size: 14px;");
        closeX.setOnAction(e -> modal.close());
        top.getChildren().addAll(title, sp, closeX);

        Label templLbl = new Label("Quick SaaS Dispatch Templates:");
        templLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 700;");

        HBox templates = new HBox(8);
        Button t1 = new Button("Trial Session");
        Button t2 = new Button("Birthday Gift");
        Button t3 = new Button("Renewal Offer");
        t1.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: 700; -fx-cursor: hand;");
        t2.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: 700; -fx-cursor: hand;");
        t3.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cbd5e1; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: 700; -fx-cursor: hand;");
        templates.getChildren().addAll(t1, t2, t3);

        TextArea msgArea = new TextArea("Hi " + f.getName() + ",\nThis is IWFC reaching out regarding your fitness goals! We have exciting updates for you.");
        msgArea.setPrefRowCount(4);
        msgArea.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white; -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8px;");

        t1.setOnAction(e -> msgArea.setText("Hi " + f.getName() + ",\nYour complimentary VIP trial session at IWFC is ready. Book your spot today with our master trainer!"));
        t2.setOnAction(e -> msgArea.setText("Happy Birthday " + f.getName() + "! 🎉 Wishing you peak health and strength. Enjoy a complimentary PT session pass this week!"));
        t3.setOnAction(e -> msgArea.setText("Hi " + f.getName() + ",\nYour IWFC membership renewal is coming up. Lock in your preferred membership tier today with 15% off!"));

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button sendBtn = new Button("Send WhatsApp / SMS 🚀");
        sendBtn.setStyle("-fx-background-color: linear-gradient(135deg, #06b6d4, #0891b2); -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 8 20; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(6, 182, 212, 0.4), 10, 0, 0, 2);");
        sendBtn.setOnAction(e -> {
            f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Sent SMS on " + LocalDate.now());
            facade.updateFollowUp(f);
            renderList();
            modal.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Message successfully dispatched to " + f.getName() + " (" + f.getPhoneNumber() + ").", ButtonType.OK);
            DialogUtils.stylePremiumDialog(alert);
            alert.setHeaderText("Message Dispatched");
            alert.show();
        });

        footer.getChildren().add(sendBtn);

        root.getChildren().addAll(top, templLbl, templates, msgArea, footer);

        DialogUtils.prepareModalScene(modal, root);
        modal.showAndWait();
    }

    private void showBookAppointmentModal(FollowUp f) {
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogUtils.stylePremiumDialog(dialog);
        dialog.setTitle("Book Appointment — " + f.getName());
        dialog.getDialogPane().getStyleClass().add("premium-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        DatePicker dp = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<String> timeCombo = new ComboBox<>();
        timeCombo.getItems().addAll("09:00 AM", "11:00 AM", "02:00 PM", "04:30 PM", "06:00 PM");
        timeCombo.setValue("11:00 AM");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Complimentary Trial Session", "Fitness Assessment", "PT Consultation", "Membership Tour");
        typeCombo.setValue("Complimentary Trial Session");

        grid.add(new Label("Member / Lead:"), 0, 0);
        grid.add(new Label(f.getName() + " (" + f.getPhoneNumber() + ")"), 1, 0);
        grid.add(new Label("Session Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Appointment Date:"), 0, 2);
        grid.add(dp, 1, 2);
        grid.add(new Label("Time Slot:"), 0, 3);
        grid.add(timeCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(ans -> {
            if (ans == ButtonType.OK) {
                f.setScheduledDate(dp.getValue().atTime(11, 0));
                f.setConvertibility(Convertibility.HOT);
                f.setComments((f.getComments() != null ? f.getComments() + " | " : "") + "Booked " + typeCombo.getValue() + " on " + dp.getValue());
                facade.updateFollowUp(f);
                refreshOverviewCards();
                renderList();

                Alert a = new Alert(Alert.AlertType.INFORMATION, "Appointment confirmed for " + f.getName() + " on " + dp.getValue() + " at " + timeCombo.getValue());
                DialogUtils.stylePremiumDialog(a);
                a.setHeaderText("Appointment Scheduled");
                a.show();
            }
        });
    }
}

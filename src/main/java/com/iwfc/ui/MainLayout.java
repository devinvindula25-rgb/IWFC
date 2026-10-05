package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Role;
import com.iwfc.model.User;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Main layout with sidebar navigation and swappable content area.
 * Shows role-appropriate navigation items.
 */
public class MainLayout extends BorderPane {

    private final IWFCSystemFacade facade;
    private final StackPane contentArea;
    private final Runnable onLogout;
    private VBox sidebar;
    private Button activeNavButton;
    private final java.util.Map<String, Button> navButtons = new java.util.HashMap<>();

    public MainLayout(IWFCSystemFacade facade, StackPane toastContainer, Runnable onLogout) {
        this.facade = facade;
        this.onLogout = onLogout;
        this.contentArea = toastContainer;
        this.contentArea.setAlignment(Pos.TOP_LEFT);

        getStyleClass().add("main-layout");

        buildSidebar();
        setLeft(sidebar);
        setCenter(contentArea);

        // Show dashboard by default
        navigateTo("Dashboard");
    }

    private void buildSidebar() {
        sidebar = new VBox(5);
        sidebar.setPrefWidth(270);
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.getStyleClass().add("sidebar");

        // Logo
        Label logo = new Label("IWFC");
        logo.setFont(Font.font("System", FontWeight.BOLD, 28));
        logo.getStyleClass().add("sidebar-logo");
        logo.setPadding(new Insets(0, 0, 5, 10));

        // Theme toggle
        Button themeToggle = new Button(ThemeManager.getInstance().isDark() ? "🌙" : "☀️");
        themeToggle.getStyleClass().add("theme-toggle-btn");
        themeToggle.setOnAction(e -> {
            ThemeManager tm = ThemeManager.getInstance();
            tm.toggle();
            tm.applyTheme(getScene());
            themeToggle.setText(tm.isDark() ? "🌙" : "☀️");
        });

        HBox logoBar = new HBox(10);
        logoBar.setAlignment(Pos.CENTER_LEFT);
        Region logoSpacer = new Region();
        HBox.setHgrow(logoSpacer, Priority.ALWAYS);
        logoBar.getChildren().addAll(logo, logoSpacer, themeToggle);
        logoBar.setPadding(new Insets(0, 0, 15, 0));

        Separator sep1 = new Separator();

        // Navigation buttons grouped into premium sections
        VBox navItems = new VBox(2);
        navItems.setPadding(new Insets(10, 15, 0, 15)); // Inner padding for floated buttons

        // --- OVERVIEW ---
        navItems.getChildren().add(createSectionHeader("Overview"));
        Button dashboardBtn = createNavButton("📊", "Dashboard");
        Button plansBtn = createNavButton("💪", "Workout Plans", facade.getPlanCatalogue().size());
        Button sessionsBtn = createNavButton("📅", "Sessions", facade.getAllSessions().size());
        navItems.getChildren().addAll(dashboardBtn, plansBtn, sessionsBtn);
        
        // --- MANAGEMENT ---
        navItems.getChildren().add(createSectionHeader("Management"));
        if (facade.getCurrentUser().getRole() == com.iwfc.model.Role.ADMIN || facade.getCurrentUser().getRole() == com.iwfc.model.Role.INSTRUCTOR) {
            long memberCount = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll().stream()
                .filter(u -> u.getRole() == com.iwfc.model.Role.MEMBER).count();
            navItems.getChildren().add(createNavButton("🙋‍♂️", "Members", (int) memberCount));
        }
        Button equipmentBtn = createNavButton("🏋️", "Equipment", facade.getAllEquipment().size());
        Button maintenanceBtn = createNavButton("🔧", "Maintenance");
        navItems.getChildren().addAll(equipmentBtn, maintenanceBtn);

        // --- OPERATIONS ---
        navItems.getChildren().add(createSectionHeader("Operations"));
        Button followUpsBtn = createNavButton("📋", "Follow Ups", facade.getFollowUps().size());
        long unreadCount = facade.getUnreadNotificationCount();
        Button notificationsBtn = createNavButton("🔔", "Notifications", unreadCount > 0 ? (int) unreadCount : null);
        Button reportsBtn = createNavButton("📈", "Reports");
        navItems.getChildren().addAll(followUpsBtn, notificationsBtn, reportsBtn);

        // --- BUSINESS SETTINGS ---
        navItems.getChildren().add(createSectionHeader("Business Settings"));
        if (facade.getCurrentUser().getRole() == com.iwfc.model.Role.ADMIN || facade.getCurrentUser().getRole() == com.iwfc.model.Role.INSTRUCTOR) {
            long teamCount = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll().stream()
                .filter(u -> u.getRole() != com.iwfc.model.Role.MEMBER).count();
            Button teamBtn = createNavButton("👥", "Team", (int) teamCount); 
            navItems.getChildren().add(teamBtn);
        }
        Button paymentsBtn = createNavButton("💳", "Payments");
        navItems.getChildren().add(paymentsBtn);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User info at bottom
        User user = facade.getCurrentUser();
        VBox userInfo = new VBox(3);
        userInfo.setPadding(new Insets(10));
        userInfo.getStyleClass().add("sidebar-user-info");

        Label userName = new Label(user.getName());
        userName.getStyleClass().add("sidebar-user-name");

        Label userRole = new Label(user.getRole().getDisplayName());
        userRole.getStyleClass().add("sidebar-user-role");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("logout-button");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            facade.logout();
            onLogout.run();
        });

        userInfo.getChildren().addAll(userName, userRole, logoutBtn);

        sidebar.getChildren().addAll(logoBar, sep1, navItems, spacer, userInfo);
    }

    private Label createSectionHeader(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.getStyleClass().add("sidebar-section-header");
        lbl.setPadding(new Insets(20, 10, 8, 10)); // High padding for breathing room
        return lbl;
    }

    private Button createNavButton(String icon, String label) {
        return createNavButton(icon, label, null);
    }

    private Button createNavButton(String icon, String label, Integer count) {
        Button btn = new Button();
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPrefHeight(40);
        btn.setUserData(label);

        HBox graphic = new HBox(12);
        graphic.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px; -fx-min-width: 24px;");
        iconLbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        
        Label textLbl = new Label(label);
        textLbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        graphic.getChildren().addAll(iconLbl, textLbl, spacer);
        
        if (count != null && count > 0) {
            Label badge = new Label(String.valueOf(count));
            badge.getStyleClass().add("nav-badge");
            badge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            graphic.getChildren().add(badge);
        }
        
        btn.setGraphic(graphic);

        btn.setOnAction(e -> {
            navigateTo(label);
            setActiveButton(btn);
            
            if ("Notifications".equals(label)) {
                long unread = facade.getUnreadNotificationCount();
                if (unread == 0) {
                    if (graphic.getChildren().size() > 3) {
                        graphic.getChildren().remove(3);
                    }
                } else {
                    if (graphic.getChildren().size() > 3) {
                        ((Label)graphic.getChildren().get(3)).setText(String.valueOf(unread));
                    } else {
                        Label badge = new Label(String.valueOf(unread));
                        badge.getStyleClass().add("nav-badge");
                        badge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                        graphic.getChildren().add(badge);
                    }
                }
            }
        });

        if ("Dashboard".equals(label)) {
            setActiveButton(btn);
        }
        
        navButtons.put(label, btn);
        return btn;
    }

    private void navigateToWithHighlight(String view) {
        Button btn = navButtons.get(view);
        if (btn != null) {
            setActiveButton(btn);
        }
        navigateTo(view);
    }

    private void setActiveButton(Button btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        btn.getStyleClass().add("nav-button-active");
        activeNavButton = btn;
    }

    private void navigateTo(String view) {
        // Remove only non-toast children (keep toast notifications)
        contentArea.getChildren().removeIf(node ->
            !node.getStyleClass().contains("toast-notification"));

        Node content = switch (view.split(" \\(")[0]) { // Remove the unread count like " (3)" if it exists
            case "Dashboard" -> new DashboardView(facade, this::navigateToWithHighlight);
            case "Equipment" -> new EquipmentView(facade);
            case "Sessions" -> new SessionView(facade);
            case "Maintenance" -> new MaintenanceView(facade);
            case "Notifications" -> new NotificationView(facade);
            case "Follow Ups" -> new FollowUpView(facade);
            case "Payments" -> new PaymentView(facade);
            case "Reports" -> facade.getCurrentUser().getRole() == Role.ADMIN ? new AdminReportsView(facade) : new AnalysisView(facade);
            case "Team" -> new UserManagementView(facade); // Updated mapping from Users to Team
            case "Members" -> new MembersManagementView(facade);
            case "Workout Plans" -> new WorkoutPlansView(facade);
            default -> new DashboardView(facade, this::navigateToWithHighlight);
        };

        // Insert content behind any existing toasts
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        contentArea.getChildren().add(0, content);

        // Add premium fade-in animation
        FadeTransition ft = new FadeTransition(Duration.millis(300), content);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}

package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Role;
import com.iwfc.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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

    public MainLayout(IWFCSystemFacade facade, StackPane toastContainer, Runnable onLogout) {
        this.facade = facade;
        this.onLogout = onLogout;
        this.contentArea = toastContainer;

        getStyleClass().add("main-layout");

        buildSidebar();
        setLeft(sidebar);
        setCenter(contentArea);

        // Show dashboard by default
        navigateTo("Dashboard");
    }

    private void buildSidebar() {
        sidebar = new VBox(5);
        sidebar.setPrefWidth(220);
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

        // Navigation buttons
        VBox navItems = new VBox(3);
        navItems.setPadding(new Insets(15, 0, 0, 0));

        Button dashboardBtn = createNavButton("📊", "Dashboard");
        Button equipmentBtn = createNavButton("🏋️", "Equipment");
        Button sessionsBtn = createNavButton("📅", "Sessions");
        Button maintenanceBtn = createNavButton("🔧", "Maintenance");

        navItems.getChildren().addAll(dashboardBtn, equipmentBtn, sessionsBtn, maintenanceBtn);

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

    private Button createNavButton(String icon, String label) {
        Button btn = new Button(icon + "   " + label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPrefHeight(40);

        btn.setOnAction(e -> {
            navigateTo(label);
            setActiveButton(btn);
        });

        if ("Dashboard".equals(label)) {
            setActiveButton(btn);
        }

        return btn;
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

        Node content = switch (view) {
            case "Dashboard" -> new DashboardView(facade);
            case "Equipment" -> new EquipmentView(facade);
            case "Sessions" -> new SessionView(facade);
            case "Maintenance" -> new MaintenanceView(facade);
            default -> new DashboardView(facade);
        };

        // Insert content behind any existing toasts
        contentArea.getChildren().addFirst(content);
    }
}

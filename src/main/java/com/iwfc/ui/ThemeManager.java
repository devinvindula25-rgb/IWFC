package com.iwfc.ui;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

/**
 * Manages theme switching between dark and light modes.
 * Uses AtlantaFX base themes with custom CSS overlays.
 * Persists user preference.
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT, DARK_BLUE }

    private static ThemeManager instance;

    private final ObjectProperty<Theme> currentTheme;

    private ThemeManager() {
        this.currentTheme = new SimpleObjectProperty<>(Theme.DARK);
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Sets a specific theme directly.
     */
    public void setTheme(Theme theme) {
        if (theme != null) {
            this.currentTheme.set(theme);
        }
    }

    /**
     * Cycles between dark, light, and dark blue (navy) theme.
     */
    public void toggle() {
        switch (currentTheme.get()) {
            case DARK -> setTheme(Theme.LIGHT);
            case LIGHT -> setTheme(Theme.DARK_BLUE);
            case DARK_BLUE -> setTheme(Theme.DARK);
        }
    }

    /**
     * Applies the current theme to the given scene.
     * Sets the AtlantaFX base theme and adds custom CSS overlays.
     */
    public void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();

        addStylesheet(scene, "/com/iwfc/css/common.css");
        addStylesheet(scene, "/com/iwfc/css/sidebar-navigation.css");
        addStylesheet(scene, "/com/iwfc/css/login.css");
        addStylesheet(scene, "/com/iwfc/css/dashboard-management.css");
        addStylesheet(scene, "/com/iwfc/css/payments-management.css");
        addStylesheet(scene, "/workout-plans.css");
        addStylesheet(scene, "/session-schedule.css");
        addStylesheet(scene, "/com/iwfc/css/members-management.css");
        addStylesheet(scene, "/com/iwfc/css/team-management.css");
        addStylesheet(scene, "/com/iwfc/css/equipment-management.css");
        addStylesheet(scene, "/com/iwfc/css/notification-center.css");
        addStylesheet(scene, "/com/iwfc/css/followup-pipeline.css");
        addStylesheet(scene, "/com/iwfc/css/member-reports.css");

        if (currentTheme.get() == Theme.DARK) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            addStylesheet(scene, "/com/iwfc/css/dark-overrides.css");
        } else if (currentTheme.get() == Theme.DARK_BLUE) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            addStylesheet(scene, "/com/iwfc/css/navy-overrides.css");
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            addStylesheet(scene, "/com/iwfc/css/light-overrides.css");
        }

        if (scene.getRoot() != null) {
            scene.getRoot().getStyleClass().removeAll("theme-dark", "theme-light", "theme-navy");
            if (currentTheme.get() == Theme.LIGHT) {
                scene.getRoot().getStyleClass().add("theme-light");
            } else if (currentTheme.get() == Theme.DARK_BLUE) {
                scene.getRoot().getStyleClass().add("theme-navy");
            } else {
                scene.getRoot().getStyleClass().add("theme-dark");
            }
        }
    }

    private void addStylesheet(Scene scene, String resourcePath) {
        try {
            var res = getClass().getResource(resourcePath);
            if (res != null) {
                scene.getStylesheets().add(res.toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    public ObjectProperty<Theme> currentThemeProperty() {
        return currentTheme;
    }

    public Theme getCurrentTheme() {
        return currentTheme.get();
    }

    public boolean isDark() {
        return currentTheme.get() != Theme.LIGHT;
    }
}

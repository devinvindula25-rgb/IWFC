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

    public enum Theme { DARK, LIGHT }

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
     * Toggles between dark and light theme.
     */
    public void toggle() {
        if (currentTheme.get() == Theme.DARK) {
            currentTheme.set(Theme.LIGHT);
        } else {
            currentTheme.set(Theme.DARK);
        }
    }

    /**
     * Applies the current theme to the given scene.
     * Sets the AtlantaFX base theme and adds custom CSS overlays.
     */
    public void applyTheme(Scene scene) {
        scene.getStylesheets().clear();

        if (currentTheme.get() == Theme.DARK) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            String darkCss = getClass().getResource("/com/iwfc/css/dark-overrides.css").toExternalForm();
            scene.getStylesheets().add(darkCss);
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            String lightCss = getClass().getResource("/com/iwfc/css/light-overrides.css").toExternalForm();
            scene.getStylesheets().add(lightCss);
        }

        String commonCss = getClass().getResource("/com/iwfc/css/common.css").toExternalForm();
        scene.getStylesheets().add(commonCss);
    }

    public ObjectProperty<Theme> currentThemeProperty() {
        return currentTheme;
    }

    public Theme getCurrentTheme() {
        return currentTheme.get();
    }

    public boolean isDark() {
        return currentTheme.get() == Theme.DARK;
    }
}

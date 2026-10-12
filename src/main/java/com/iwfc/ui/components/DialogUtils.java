package com.iwfc.ui.components;

import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;

import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.stage.Window;
import com.iwfc.ui.ThemeManager;

public class DialogUtils {

    /**
     * Styles a standard Dialog or Alert to have an iOS-like premium glassmorphism appearance
     * without the native OS window borders.
     */
    public static void stylePremiumDialog(Dialog<?> dialog) {
        // Remove OS window borders
        try {
            dialog.initStyle(StageStyle.TRANSPARENT);
        } catch (IllegalStateException ignored) {
            // Already initialized style
        }
        
        // Add the premium CSS class
        if (!dialog.getDialogPane().getStyleClass().contains("premium-dialog")) {
            dialog.getDialogPane().getStyleClass().add("premium-dialog");
        }
        
        // Make sure the dialog pane's scene is transparent, inherits active theme, and sizes properly
        Runnable applyStylesAndSize = () -> {
            if (dialog.getDialogPane().getScene() != null) {
                javafx.scene.Scene sc = dialog.getDialogPane().getScene();
                sc.setFill(Color.TRANSPARENT);
                ThemeManager tm = ThemeManager.getInstance();
                tm.applyTheme(sc);
                String themeClass = switch (tm.getCurrentTheme()) {
                    case LIGHT -> "theme-light";
                    case DARK_BLUE -> "theme-navy";
                    case DARK -> "theme-dark";
                };
                dialog.getDialogPane().getStyleClass().removeAll("theme-dark", "theme-light", "theme-navy");
                dialog.getDialogPane().getStyleClass().add(themeClass);

                dialog.getDialogPane().applyCss();
                dialog.getDialogPane().layout();

                Window window = sc.getWindow();
                if (window instanceof javafx.stage.Stage stage) {
                    stage.sizeToScene();
                }

                if (window != null) {
                    if (dialog.getOwner() != null) {
                        Window owner = dialog.getOwner();
                        if (owner.getWidth() > 0 && owner.getHeight() > 0) {
                            double x = owner.getX() + (owner.getWidth() - window.getWidth()) / 2.0;
                            double y = owner.getY() + (owner.getHeight() - window.getHeight()) / 2.0;
                            window.setX(Math.max(20, x));
                            window.setY(Math.max(20, y));
                        } else {
                            window.centerOnScreen();
                        }
                    } else {
                        window.centerOnScreen();
                    }
                }
            }
        };

        // Attach scene change listener to capture when dialog scene is created
        dialog.getDialogPane().sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(applyStylesAndSize);
            }
        });

        // Attach showing listener to ensure proper sizing after window is displayed
        dialog.showingProperty().addListener((obs, oldV, isShowing) -> {
            if (isShowing) {
                Platform.runLater(applyStylesAndSize);
            }
        });

        // Also schedule via runLater in case scene is already attached
        Platform.runLater(applyStylesAndSize);
    }

    /**
     * Prepares and themes a custom modal Stage with transparency and active theme stylesheets.
     */
    public static javafx.scene.Scene prepareModalScene(javafx.stage.Stage stage, javafx.scene.Parent root) {
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(Color.TRANSPARENT);
        ThemeManager tm = ThemeManager.getInstance();
        tm.applyTheme(scene);
        String themeClass = switch (tm.getCurrentTheme()) {
            case LIGHT -> "theme-light";
            case DARK_BLUE -> "theme-navy";
            case DARK -> "theme-dark";
        };
        root.getStyleClass().removeAll("theme-dark", "theme-light", "theme-navy");
        root.getStyleClass().add(themeClass);
        root.getStyleClass().add("custom-modal-root");
        stage.setScene(scene);
        return scene;
    }
}

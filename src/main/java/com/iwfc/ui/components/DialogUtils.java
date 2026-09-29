package com.iwfc.ui.components;

import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;

import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.stage.Window;

public class DialogUtils {

    /**
     * Styles a standard Dialog or Alert to have an iOS-like premium glassmorphism appearance
     * without the native OS window borders.
     */
    public static void stylePremiumDialog(Dialog<?> dialog) {
        // Remove OS window borders
        dialog.initStyle(StageStyle.TRANSPARENT);
        
        // Add the premium CSS class
        dialog.getDialogPane().getStyleClass().add("premium-dialog");
        
        // Make sure the dialog pane's scene is transparent so the shadow shows properly
        Platform.runLater(() -> {
            if (dialog.getDialogPane().getScene() != null) {
                dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
                
                // Center on parent window if possible
                Window window = dialog.getDialogPane().getScene().getWindow();
                if (window != null && dialog.getOwner() != null) {
                    window.centerOnScreen();
                }
            }
        });
        
        // Optional: you can add a custom header node if you want to implement window dragging.
    }
}

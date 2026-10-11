package com.iwfc.ui.components;

import com.iwfc.ui.ThemeManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

/**
 * CyberGridCanvas renders the high-tech tactical CAD grid background,
 * HUD corner brackets, and theme-specific gradients (such as the deep navy
 * gradient and top-right blue glow).
 */
public class CyberGridCanvas extends Canvas {

    public CyberGridCanvas() {
        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());
    }

    public void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        // -------------------------------------------------------------
        // 1. Background Fill & Gradients
        // -------------------------------------------------------------
        if (theme == ThemeManager.Theme.DARK_BLUE) {
            // Deep navy gradient with blue glow in top-right corner
            LinearGradient navyBg = new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#020817")),
                    new Stop(0.45, Color.web("#07162c")),
                    new Stop(1.0, Color.web("#0b1d3a"))
            );
            gc.setFill(navyBg);
            gc.fillRect(0, 0, w, h);

            // Ambient blue glow in the top-right corner
            double glowRadius = Math.max(w * 0.65, 450);
            RadialGradient blueGlow = new RadialGradient(
                    0, 0, w, 0, glowRadius, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#1e40af", 0.35)),
                    new Stop(0.3, Color.web("#2563eb", 0.18)),
                    new Stop(0.65, Color.web("#0ea5e9", 0.05)),
                    new Stop(1.0, Color.TRANSPARENT)
            );
            gc.setFill(blueGlow);
            gc.fillRect(0, 0, w, h);

        } else if (theme == ThemeManager.Theme.LIGHT) {
            // Warm off-white / light cream
            gc.setFill(Color.web("#f5f6f2"));
            gc.fillRect(0, 0, w, h);
        } else {
            // Dark (black and lime): deep carbon / tactical black
            gc.setFill(Color.web("#090c0b"));
            gc.fillRect(0, 0, w, h);
        }

        // -------------------------------------------------------------
        // 2. Technical CAD Grid Lines
        // -------------------------------------------------------------
        double gridSize = 42.0;
        Color gridColor;

        if (theme == ThemeManager.Theme.DARK_BLUE) {
            gridColor = Color.rgb(28, 76, 150, 0.20);
        } else if (theme == ThemeManager.Theme.LIGHT) {
            gridColor = Color.rgb(0, 0, 0, 0.045);
        } else {
            gridColor = Color.rgb(38, 62, 48, 0.24);
        }

        gc.setStroke(gridColor);
        gc.setLineWidth(1.0);

        // Vertical lines
        for (double x = 0; x <= w; x += gridSize) {
            gc.strokeLine(x, 0, x, h);
        }
        // Horizontal lines
        for (double y = 0; y <= h; y += gridSize) {
            gc.strokeLine(0, y, w, y);
        }

        // -------------------------------------------------------------
        // 3. HUD Corner Brackets
        // -------------------------------------------------------------
        Color bracketColor = (theme == ThemeManager.Theme.LIGHT) ? Color.web("#0a0d0a") : Color.web("#ccff00");
        gc.setStroke(bracketColor);
        gc.setLineWidth(2.5);

        double margin = 32.0;
        double bracketArm = 24.0;

        // Top-Left corner bracket: ┌
        gc.strokeLine(margin, margin + bracketArm, margin, margin);
        gc.strokeLine(margin, margin, margin + bracketArm, margin);

        // Bottom-Left corner bracket: └
        gc.strokeLine(margin, h - margin - bracketArm, margin, h - margin);
        gc.strokeLine(margin, h - margin, margin + bracketArm, h - margin);
    }
}

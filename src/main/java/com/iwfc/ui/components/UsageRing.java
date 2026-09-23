package com.iwfc.ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Circular progress ring component showing usage hours vs threshold.
 * Animates the arc fill on data load. Changes color near threshold.
 */
public class UsageRing extends StackPane {

    private static final double SIZE = 80;
    private static final double STROKE_WIDTH = 8;

    private final Canvas canvas;
    private final Label centerLabel;
    private final DoubleProperty animatedProgress;
    private final DoubleProperty animatedValue; // For counter animation

    private double currentValue;
    private double maxValue;

    public UsageRing(double currentValue, double maxValue) {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        this.animatedProgress = new SimpleDoubleProperty(0);
        this.animatedValue = new SimpleDoubleProperty(0);

        canvas = new Canvas(SIZE, SIZE);
        
        centerLabel = new Label("0");
        centerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        centerLabel.getStyleClass().add("usage-ring-label");
        
        // Value counter listener
        animatedValue.addListener((obs, oldVal, newVal) -> 
            centerLabel.setText(String.format("%.0f", newVal.doubleValue()))
        );

        setAlignment(Pos.CENTER);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        getChildren().addAll(canvas, centerLabel);

        animatedProgress.addListener((obs, oldVal, newVal) -> draw(newVal.doubleValue()));

        playAnimations();
    }
    
    private void playAnimations() {
        double targetProgress = this.maxValue > 0 ? Math.min(this.currentValue / this.maxValue, 1.0) : 0;
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(animatedProgress, 0),
                new KeyValue(animatedValue, 0)
            ),
            new KeyFrame(Duration.millis(1200), 
                new KeyValue(animatedProgress, targetProgress, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(animatedValue, this.currentValue, javafx.animation.Interpolator.EASE_OUT)
            )
        );
        timeline.play();
    }

    private void draw(double progress) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double center = SIZE / 2.0;
        double radius = center - STROKE_WIDTH;

        gc.clearRect(0, 0, SIZE, SIZE);

        // Background track ring
        gc.setStroke(Color.web("#ffffff", 0.05)); // Subtle transparent background
        gc.setLineWidth(STROKE_WIDTH);
        gc.strokeOval(STROKE_WIDTH / 2, STROKE_WIDTH / 2, radius * 2, radius * 2);

        // Determine glow/arc color based on threshold
        Color ringColor;
        if (progress >= 0.9) {
            ringColor = Color.web("#FF6B6B"); // Red
        } else if (progress >= 0.7) {
            ringColor = Color.web("#FFD43B"); // Yellow
        } else {
            ringColor = Color.web("#51CF66"); // Green
        }

        // Apply drop shadow glow to the canvas itself matching the color
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(ringColor);
        glow.setRadius(10);
        glow.setSpread(0.2);
        canvas.setEffect(glow);

        gc.setStroke(ringColor);
        gc.setLineWidth(STROKE_WIDTH);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        double arcAngle = progress * 360;
        if (arcAngle > 0) {
            gc.strokeArc(STROKE_WIDTH / 2, STROKE_WIDTH / 2,
                    radius * 2, radius * 2, 90, -arcAngle,
                    javafx.scene.shape.ArcType.OPEN);
        }
    }

    public void update(double currentValue, double maxValue) {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        playAnimations();
    }
}

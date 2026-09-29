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
    private final com.iwfc.model.EquipmentStatus status;

    public UsageRing(double currentValue, double maxValue, com.iwfc.model.EquipmentStatus status) {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        this.status = status;
        this.animatedProgress = new SimpleDoubleProperty(0);
        this.animatedValue = new SimpleDoubleProperty(0);

        canvas = new Canvas(SIZE, SIZE / 2 + STROKE_WIDTH); // Height is half size plus stroke
        
        centerLabel = new Label("0");
        centerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        centerLabel.getStyleClass().add("usage-ring-label");
        
        // Value counter listener
        animatedValue.addListener((obs, oldVal, newVal) -> 
            centerLabel.setText(String.format("%.0f", newVal.doubleValue()))
        );

        setAlignment(Pos.BOTTOM_CENTER);
        setPrefSize(SIZE, SIZE / 2 + STROKE_WIDTH);
        setMaxSize(SIZE, SIZE / 2 + STROKE_WIDTH);
        
        // Push the label slightly up from the absolute bottom
        centerLabel.setTranslateY(-2); 
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

        // Background track arc (180 degree semi-circle)
        gc.setStroke(Color.web("#ffffff", 0.05)); 
        gc.setLineWidth(STROKE_WIDTH);
        gc.strokeArc(STROKE_WIDTH / 2, STROKE_WIDTH / 2, radius * 2, radius * 2, 180, -180, javafx.scene.shape.ArcType.OPEN);

        // Determine color based on exact status
        Color ringColor;
        switch (this.status) {
            case FAULTY -> ringColor = Color.web("#ef4444"); // Red
            case UNDER_MAINTENANCE -> ringColor = Color.web("#f59e0b"); // Yellow
            default -> ringColor = Color.web("#4ade80"); // Green
        }

        // Apply drop shadow glow to the canvas itself matching the color
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(new Color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), 0.3));
        glow.setRadius(10);
        canvas.setEffect(glow);

        // Draw progress arc
        gc.setStroke(ringColor);
        gc.strokeArc(STROKE_WIDTH / 2, STROKE_WIDTH / 2, radius * 2, radius * 2, 180, -180 * progress, javafx.scene.shape.ArcType.OPEN);
    }
    public void update(double currentValue, double maxValue) {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        playAnimations();
    }
}

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

    private static final double SIZE = 70;
    private static final double STROKE_WIDTH = 6;

    private final Canvas canvas;
    private final Label centerLabel;
    private final DoubleProperty animatedProgress;

    private double currentValue;
    private double maxValue;

    public UsageRing(double currentValue, double maxValue) {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        this.animatedProgress = new SimpleDoubleProperty(0);

        canvas = new Canvas(SIZE, SIZE);
        centerLabel = new Label(String.format("%.0f", this.currentValue));
        centerLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        centerLabel.getStyleClass().add("usage-ring-label");

        setAlignment(Pos.CENTER);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        getChildren().addAll(canvas, centerLabel);

        animatedProgress.addListener((obs, oldVal, newVal) -> draw(newVal.doubleValue()));

        // Animate fill
        double targetProgress = this.maxValue > 0 ? Math.min(this.currentValue / this.maxValue, 1.0) : 0;
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(animatedProgress, 0)),
            new KeyFrame(Duration.millis(800), new KeyValue(animatedProgress, targetProgress))
        );
        timeline.play();
    }

    private void draw(double progress) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double center = SIZE / 2.0;
        double radius = center - STROKE_WIDTH;

        gc.clearRect(0, 0, SIZE, SIZE);

        // Background ring
        gc.setStroke(Color.gray(0.3, 0.3));
        gc.setLineWidth(STROKE_WIDTH);
        gc.strokeOval(STROKE_WIDTH / 2, STROKE_WIDTH / 2, radius * 2, radius * 2);

        // Progress arc
        Color ringColor;
        if (progress >= 0.9) {
            ringColor = Color.web("#FF6B6B"); // Red — near/at threshold
        } else if (progress >= 0.7) {
            ringColor = Color.web("#FFD43B"); // Yellow — approaching
        } else {
            ringColor = Color.web("#51CF66"); // Green — healthy
        }

        gc.setStroke(ringColor);
        gc.setLineWidth(STROKE_WIDTH);
        double arcAngle = progress * 360;
        gc.strokeArc(STROKE_WIDTH / 2, STROKE_WIDTH / 2,
                radius * 2, radius * 2, 90, -arcAngle,
                javafx.scene.shape.ArcType.OPEN);
    }

    public void update(double currentValue, double maxValue) {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        centerLabel.setText(String.format("%.0f", currentValue));
        double targetProgress = maxValue > 0 ? Math.min(currentValue / maxValue, 1.0) : 0;
        animatedProgress.set(targetProgress);
    }
}

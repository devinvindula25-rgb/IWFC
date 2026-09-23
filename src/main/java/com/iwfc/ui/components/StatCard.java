package com.iwfc.ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Reusable gradient stat card component for the dashboard.
 * Displays a title and a large numeric value with a gradient background.
 * Includes hover animation (scale-up effect).
 */
public class StatCard extends VBox {

    private final Label titleLabel;
    private final Label valueLabel;
    private final DoubleProperty animatedValue;
    private double targetValue;

    public StatCard(String title, String valueStr, String icon, String gradientStyle) {
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-card-title");

        valueLabel = new Label("0");
        valueLabel.getStyleClass().add("stat-card-value");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-card-icon");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(iconLabel, spacer, titleLabel);

        animatedValue = new SimpleDoubleProperty(0);
        animatedValue.addListener((obs, oldVal, newVal) -> 
            valueLabel.setText(String.format("%.0f", newVal.doubleValue()))
        );

        try {
            this.targetValue = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            this.targetValue = 0;
            valueLabel.setText(valueStr); // Fallback for non-numeric
        }

        setAlignment(Pos.BOTTOM_LEFT);
        setPadding(new Insets(20));
        setSpacing(12);
        setPrefWidth(220);
        setPrefHeight(130);
        getStyleClass().add("stat-card");
        setStyle(gradientStyle);

        getChildren().addAll(topRow, valueLabel);

        // Hover animation (Float up and scale) with Drop Shadow for premium feel
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(15);
        shadow.setOffsetY(8);

        setOnMouseEntered(e -> {
            setCursor(javafx.scene.Cursor.HAND);
            setEffect(shadow);
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(250), this);
            st.setToX(1.03);
            st.setToY(1.03);
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(250), this);
            tt.setToY(-5);
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(st, tt);
            pt.play();
        });
        
        setOnMouseExited(e -> {
            setEffect(null);
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(250), this);
            st.setToX(1.0);
            st.setToY(1.0);
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(250), this);
            tt.setToY(0);
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(st, tt);
            pt.play();
        });

        playEntranceAnimation();
    }

    private void playEntranceAnimation() {
        if (targetValue > 0) {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(animatedValue, 0)),
                new KeyFrame(Duration.millis(1200), 
                    new KeyValue(animatedValue, targetValue, javafx.animation.Interpolator.EASE_OUT)
                )
            );
            timeline.setDelay(Duration.millis(200));
            timeline.play();
        }
    }

    public void setValue(String value) {
        try {
            this.targetValue = Double.parseDouble(value);
            playEntranceAnimation();
        } catch (NumberFormatException e) {
            valueLabel.setText(value);
        }
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}

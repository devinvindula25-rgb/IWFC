package com.iwfc.ui.components;

import javafx.animation.ScaleTransition;
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

    public StatCard(String title, String value, String gradientStyle) {
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-card-title");

        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-card-value");

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(20));
        setSpacing(8);
        setPrefWidth(200);
        setPrefHeight(120);
        getStyleClass().add("stat-card");
        setStyle(gradientStyle);

        getChildren().addAll(titleLabel, valueLabel);

        // Hover animation
        setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}

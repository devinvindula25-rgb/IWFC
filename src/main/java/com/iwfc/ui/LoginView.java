package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Login screen view.
 * Centered card with IWFC branding, user ID input, and role display.
 */
public class LoginView extends StackPane {

    private final IWFCSystemFacade facade;
    private final Runnable onLoginSuccess;

    public LoginView(IWFCSystemFacade facade, Runnable onLoginSuccess) {
        this.facade = facade;
        this.onLoginSuccess = onLoginSuccess;

        getStyleClass().add("login-background");
        buildUI();
    }

    private void buildUI() {
        HBox splitContainer = new HBox();
        splitContainer.getStyleClass().add("login-split-container");

        // Left Pane (Branding/Image)
        StackPane leftPane = new StackPane();
        leftPane.getStyleClass().add("login-split-left");
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        // Right Pane (Login Form)
        StackPane rightPane = new StackPane();
        rightPane.getStyleClass().add("login-split-right");

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 40, 50, 40));
        card.setMaxWidth(400);
        card.getStyleClass().add("login-card");

        // Logo / Title
        Label logo = new Label("IWFC");
        logo.getStyleClass().add("login-logo");

        Label subtitle = new Label("Intelligent Wellness & Fitness Center");
        subtitle.getStyleClass().add("login-subtitle");

        // User ID input
        VBox inputSection = new VBox(8);
        inputSection.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("User ID");
        idLabel.getStyleClass().add("form-label");

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter your User ID (e.g., ADM001)");
        userIdField.getStyleClass().add("login-input");
        userIdField.setPrefHeight(42);
        inputSection.getChildren().addAll(idLabel, userIdField);

        // Quick select buttons
        VBox quickSection = new VBox(10);
        quickSection.setAlignment(Pos.CENTER);
        Label quickLabel = new Label("Quick Login");
        quickLabel.getStyleClass().add("form-label");

        HBox quickButtons = new HBox(10);
        quickButtons.setAlignment(Pos.CENTER);

        Button adminBtn = new Button("Admin");
        adminBtn.getStyleClass().addAll("quick-login-btn", "quick-admin");

        Button instrBtn = new Button("Instructor");
        instrBtn.getStyleClass().addAll("quick-login-btn", "quick-instructor");

        Button memberBtn = new Button("Member");
        memberBtn.getStyleClass().addAll("quick-login-btn", "quick-member");

        quickButtons.getChildren().addAll(adminBtn, instrBtn, memberBtn);
        quickSection.getChildren().addAll(quickLabel, quickButtons);

        // Error label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

        // Sign in button
        Button signInBtn = new Button("Sign In");
        
        // Setup quick login actions
        adminBtn.setOnAction(e -> {
            userIdField.setText("ADM001");
            signInBtn.fire();
        });
        instrBtn.setOnAction(e -> {
            userIdField.setText("INS001");
            signInBtn.fire();
        });
        memberBtn.setOnAction(e -> {
            userIdField.setText("MEM001");
            signInBtn.fire();
        });

        signInBtn.getStyleClass().add("login-button");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setPrefHeight(45);
        signInBtn.setOnAction(e -> {
            String userId = userIdField.getText().trim();
            if (userId.isEmpty()) {
                showError(errorLabel, "Please enter a User ID.");
                return;
            }
            try {
                facade.login(userId);
                onLoginSuccess.run();
            } catch (Exception ex) {
                showError(errorLabel, "Invalid User ID. Try ADM001, INS001, or MEM001.");
            }
        });

        // Enter key to submit
        userIdField.setOnAction(e -> signInBtn.fire());

        card.getChildren().addAll(logo, subtitle, inputSection, quickSection, errorLabel, signInBtn);
        rightPane.getChildren().add(card);
        splitContainer.getChildren().addAll(leftPane, rightPane);

        // Theme toggle floating top right
        Button themeToggle = new Button("🌙");
        themeToggle.getStyleClass().add("theme-toggle-btn");
        themeToggle.setOnAction(e -> {
            ThemeManager tm = ThemeManager.getInstance();
            tm.toggle();
            tm.applyTheme(getScene());
            themeToggle.setText(tm.isDark() ? "🌙" : "☀️");
        });

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.getChildren().add(themeToggle);
        StackPane.setAlignment(topBar, Pos.TOP_RIGHT);
        StackPane.setMargin(topBar, new Insets(15));
        topBar.setPickOnBounds(false);

        getChildren().addAll(splitContainer, topBar);

        // Trigger Animations
        javafx.application.Platform.runLater(() -> playEntranceAnimations(card.getChildren()));
    }

    private void playEntranceAnimations(javafx.collections.ObservableList<javafx.scene.Node> nodes) {
        int delay = 0;
        for (javafx.scene.Node node : nodes) {
            node.setOpacity(0);
            node.setTranslateY(20);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(400), node);
            ft.setToValue(1);

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(400), node);
            tt.setToY(0);

            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(delay));
            pt.play();

            delay += 60;
        }
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}

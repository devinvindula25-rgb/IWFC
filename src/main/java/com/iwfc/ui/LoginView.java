package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.User;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.setMaxHeight(450);
        card.getStyleClass().add("login-card");

        // Logo / Title
        Label logo = new Label("IWFC");
        logo.setFont(Font.font("System", FontWeight.BOLD, 36));
        logo.getStyleClass().add("login-logo");

        Label subtitle = new Label("Intelligent Wellness & Fitness Center");
        subtitle.getStyleClass().add("login-subtitle");

        // User ID input
        Label idLabel = new Label("User ID");
        idLabel.getStyleClass().add("form-label");

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter your User ID (e.g., ADM001)");
        userIdField.getStyleClass().add("login-input");
        userIdField.setPrefHeight(40);

        // Quick select buttons
        Label quickLabel = new Label("Quick Login");
        quickLabel.getStyleClass().add("form-label");
        quickLabel.setPadding(new Insets(5, 0, 0, 0));

        HBox quickButtons = new HBox(8);
        quickButtons.setAlignment(Pos.CENTER);

        Button adminBtn = new Button("Admin");
        adminBtn.getStyleClass().addAll("quick-login-btn", "quick-admin");
        adminBtn.setOnAction(e -> userIdField.setText("ADM001"));

        Button instrBtn = new Button("Instructor");
        instrBtn.getStyleClass().addAll("quick-login-btn", "quick-instructor");
        instrBtn.setOnAction(e -> userIdField.setText("INS001"));

        Button memberBtn = new Button("Member");
        memberBtn.getStyleClass().addAll("quick-login-btn", "quick-member");
        memberBtn.setOnAction(e -> userIdField.setText("MEM001"));

        quickButtons.getChildren().addAll(adminBtn, instrBtn, memberBtn);

        // Error label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

        // Sign in button
        Button signInBtn = new Button("Sign In");
        signInBtn.getStyleClass().add("login-button");
        signInBtn.setPrefWidth(200);
        signInBtn.setPrefHeight(42);
        signInBtn.setOnAction(e -> {
            String userId = userIdField.getText().trim();
            if (userId.isEmpty()) {
                showError(errorLabel, "Please enter a User ID.");
                return;
            }
            try {
                User user = facade.login(userId);
                onLoginSuccess.run();
            } catch (Exception ex) {
                showError(errorLabel, "Invalid User ID. Try ADM001, INS001, or MEM001.");
            }
        });

        // Enter key to submit
        userIdField.setOnAction(e -> signInBtn.fire());

        // Theme toggle
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

        card.getChildren().addAll(logo, subtitle, idLabel, userIdField,
                quickLabel, quickButtons, errorLabel, signInBtn);

        StackPane.setAlignment(topBar, Pos.TOP_RIGHT);
        StackPane.setMargin(topBar, new Insets(15));

        getChildren().addAll(card, topBar);
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

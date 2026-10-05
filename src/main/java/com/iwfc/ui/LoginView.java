package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Supreme Login View.
 * Balanced, centered dual-pane glassmorphic card with IWFC branding,
 * quick login roles, and resilient responsive layout.
 */
public class LoginView extends StackPane {

    private final IWFCSystemFacade facade;
    private final Runnable onLoginSuccess;

    public LoginView(IWFCSystemFacade facade, Runnable onLoginSuccess) {
        this.facade = facade;
        this.onLoginSuccess = onLoginSuccess;

        getStyleClass().add("login-background");
        setAlignment(Pos.CENTER);
        buildUI();
    }

    private void buildUI() {
        // Main centered dual-pane card container
        HBox mainCard = new HBox();
        mainCard.setAlignment(Pos.CENTER);
        mainCard.setMaxWidth(860);
        mainCard.setPrefWidth(840);
        mainCard.setMaxHeight(540);
        mainCard.setPrefHeight(520);
        mainCard.getStyleClass().add("login-hero-card");

        // -------------------------------------------------------------
        // LEFT: Branding & Enterprise Feature Showcase
        // -------------------------------------------------------------
        VBox leftBrand = new VBox(22);
        leftBrand.setAlignment(Pos.CENTER_LEFT);
        leftBrand.setPadding(new Insets(45, 40, 45, 45));
        leftBrand.setPrefWidth(380);
        leftBrand.setMinWidth(350);
        leftBrand.getStyleClass().add("login-brand-pane");

        // Top Pill Badge
        Label platformBadge = new Label("✦  ENTERPRISE PORTAL");
        platformBadge.getStyleClass().add("login-platform-badge");

        // Logo & Title
        VBox titleBox = new VBox(4);
        Label logo = new Label("IWFC");
        logo.getStyleClass().add("login-logo");

        Label subtitle = new Label("Intelligent Wellness & Fitness Center");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);
        titleBox.getChildren().addAll(logo, subtitle);

        // Feature Highlights
        VBox featureList = new VBox(12);
        featureList.getChildren().addAll(
                createFeatureRow("⚡", "Real-Time Telemetry & Floor Map"),
                createFeatureRow("📅", "Smart Scheduling & Check-In Stream"),
                createFeatureRow("💳", "Integrated POS & Automatic Billing"),
                createFeatureRow("📈", "Personalized Body Metrics & Analytics")
        );

        Region brandSpacer = new Region();
        VBox.setVgrow(brandSpacer, Priority.ALWAYS);

        // Footer version chip
        Label footerNote = new Label("● System Operational  •  v2.4 Enterprise Edition");
        footerNote.getStyleClass().add("login-system-status");

        leftBrand.getChildren().addAll(platformBadge, titleBox, featureList, brandSpacer, footerNote);

        // -------------------------------------------------------------
        // RIGHT: Interactive Authentication Form
        // -------------------------------------------------------------
        VBox rightForm = new VBox(20);
        rightForm.setAlignment(Pos.CENTER_LEFT);
        rightForm.setPadding(new Insets(45, 45, 45, 45));
        rightForm.setPrefWidth(460);
        rightForm.setMinWidth(400);
        rightForm.getStyleClass().add("login-form-pane");

        VBox formHeader = new VBox(4);
        Label signInTitle = new Label("Welcome Back");
        signInTitle.getStyleClass().add("login-card-title");

        Label signInSub = new Label("Select a quick profile or enter your User ID to proceed.");
        signInSub.getStyleClass().add("login-card-subtitle");
        formHeader.getChildren().addAll(signInTitle, signInSub);

        // Quick profile chip selectors
        VBox quickSection = new VBox(10);
        Label quickLabel = new Label("QUICK PROFILE SELECT");
        quickLabel.getStyleClass().add("form-label");

        HBox quickChips = new HBox(10);
        quickChips.setAlignment(Pos.CENTER_LEFT);

        Button adminBtn = new Button("👑  Admin");
        adminBtn.getStyleClass().addAll("quick-login-chip", "chip-admin");

        Button instrBtn = new Button("🏋️  Trainer");
        instrBtn.getStyleClass().addAll("quick-login-chip", "chip-instructor");

        Button memberBtn = new Button("🏃  Member");
        memberBtn.getStyleClass().addAll("quick-login-chip", "chip-member");

        quickChips.getChildren().addAll(adminBtn, instrBtn, memberBtn);
        quickSection.getChildren().addAll(quickLabel, quickChips);

        // User ID input field
        VBox inputSection = new VBox(8);
        Label idLabel = new Label("USER CREDENTIAL ID");
        idLabel.getStyleClass().add("form-label");

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter ID (ADM001, INS001, MEM001)...");
        userIdField.getStyleClass().add("login-input");
        userIdField.setPrefHeight(46);
        inputSection.getChildren().addAll(idLabel, userIdField);

        // Error message label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Sign In Action Button
        Button signInBtn = new Button("Sign In to Portal  →");
        signInBtn.getStyleClass().add("login-primary-button");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setPrefHeight(48);

        // Quick login triggers
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

        // Submit action
        signInBtn.setOnAction(e -> {
            String userId = userIdField.getText().trim();
            if (userId.isEmpty()) {
                showError(errorLabel, "Please enter a valid User ID.");
                return;
            }
            try {
                facade.login(userId);
                onLoginSuccess.run();
            } catch (Exception ex) {
                showError(errorLabel, "Authentication failed. Try ADM001, INS001, or MEM001.");
            }
        });

        // Enter key submits form
        userIdField.setOnAction(e -> signInBtn.fire());

        // Help hint
        Label helpHint = new Label("💡 Tip: Click any quick button above to sign in with one click.");
        helpHint.getStyleClass().add("login-hint-text");

        rightForm.getChildren().addAll(formHeader, quickSection, inputSection, errorLabel, signInBtn, helpHint);

        mainCard.getChildren().addAll(leftBrand, rightForm);

        // -------------------------------------------------------------
        // Floating Top-Right Theme Toggle
        // -------------------------------------------------------------
        Button themeToggle = new Button(ThemeManager.getInstance().isDark() ? "🌙" : "☀️");
        themeToggle.getStyleClass().add("theme-toggle-floating");
        themeToggle.setOnAction(e -> {
            ThemeManager tm = ThemeManager.getInstance();
            tm.toggle();
            tm.applyTheme(getScene());
            themeToggle.setText(tm.isDark() ? "🌙" : "☀️");
        });

        HBox topBar = new HBox(themeToggle);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(20));
        topBar.setPickOnBounds(false);
        StackPane.setAlignment(topBar, Pos.TOP_RIGHT);

        getChildren().addAll(mainCard, topBar);

        // Smooth Entrance Animation (Never hides content if skipped)
        playEntranceAnimation(mainCard);
    }

    private HBox createFeatureRow(String icon, String text) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 15px;");

        Label textLbl = new Label(text);
        textLbl.getStyleClass().add("login-feature-text");

        row.getChildren().addAll(iconLbl, textLbl);
        return row;
    }

    private void playEntranceAnimation(javafx.scene.Node target) {
        FadeTransition ft = new FadeTransition(Duration.millis(350), target);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(350), target);
        st.setFromX(0.97);
        st.setFromY(0.97);
        st.setToX(1.0);
        st.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
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

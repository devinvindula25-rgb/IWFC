package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.ui.components.CyberGridCanvas;
import com.iwfc.ui.components.CyberThemeToggle;
import com.iwfc.ui.components.LiveHeartbeatView;
import com.iwfc.model.User;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.InputStream;

/**
 * Redesigned High-Tech Cyber Login View for IWFC.
 * Features:
 * - Living animated ECG heartbeat monitor with real-time telemetry and cardiac contractions
 * - Integrated geometric eagle crest logo alongside the heavy "IW FC" logotype
 * - Instant 3-theme switcher: Light (white & lime), Dark (black & lime), Dark Blue (navy & lime)
 * - Beveled/chamfered cyber form card with lime accent top bar and cut-corner "ENTER PORTAL" button
 * - Role selector tiles with solid lime active fill and auto-filling demo credentials
 * - Cyberpunk CAD grid background with HUD corner brackets and reactive animations
 */
public class LoginView extends StackPane {

    private final IWFCSystemFacade facade;
    private final Runnable onLoginSuccess;

    // Components that react to theme changes
    private CyberGridCanvas gridCanvas;
    private LiveHeartbeatView heartbeatView;
    private Text textIW;
    private Text textFC;
    private Label hudTagLabel;
    private Label onFloorNumLabel;
    private Label checkInsNumLabel;
    private Label uptimeNumLabel;
    private Label onFloorTagLabel;
    private Label checkInsTagLabel;
    private Label uptimeTagLabel;
    private Label sloganTrain;
    private Label sloganTrack;
    private Label sloganTransform;
    private HBox sloganTrackBadge;
    private Label secureAccessTag;
    private Label signInTitle;
    private Label signInSubtitle;
    private Label roleSectionLabel;
    private Label credSectionLabel;
    private TextField userIdField;
    private Button enterPortalBtn;
    private Label demoIdsLabel;
    private StackPane cardOuterContainer;
    private Canvas cardBorderCanvas;
    // Step Navigation & State
    private VBox step1View;
    private VBox step2View;
    private Label step1ErrorLabel;
    private User targetUser;
    private Circle step2Avatar;
    private Label step2AvatarInitials;
    private Label step2NameLabel;
    private Label step2RolePill;
    private PasswordField passInputField;
    private TextField passVisibleField;
    private boolean isUpdatingPassField = false;
    private HBox step2ErrorBox;
    private Label step2ErrorLabel;

    // Role Tiles
    private Button adminTile;
    private Button trainerTile;
    private Button memberTile;
    private String selectedRole = "ADMIN";

    // 3D Cyber Theme Toggle
    private CyberThemeToggle themeToggle;

    public LoginView(IWFCSystemFacade facade, Runnable onLoginSuccess) {
        this.facade = facade;
        this.onLoginSuccess = onLoginSuccess;

        getStyleClass().add("cyber-login-root");
        try {
            String css = getClass().getResource("/com/iwfc/css/login.css").toExternalForm();
            getStylesheets().add(css);
        } catch (Exception ignored) {}

        setAlignment(Pos.CENTER);

        buildUI();
        updateThemeStyles();
    }

    private void buildUI() {
        // -------------------------------------------------------------
        // 1. Dynamic Cyber Grid Background
        // -------------------------------------------------------------
        gridCanvas = new CyberGridCanvas();
        gridCanvas.widthProperty().bind(widthProperty());
        gridCanvas.heightProperty().bind(heightProperty());
        getChildren().add(gridCanvas);

        // -------------------------------------------------------------
        // 2. Centered Dual-Pane Workspace
        // -------------------------------------------------------------
        HBox mainLayout = new HBox(44);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setMaxWidth(960);
        mainLayout.setPrefWidth(940);
        mainLayout.setMaxHeight(600);
        mainLayout.setPickOnBounds(false);

        // Build Left Branding Pane & Right Form Pane
        VBox leftBrandPane = buildLeftBrandPane();
        VBox rightFormPane = buildRightFormCard();

        mainLayout.getChildren().addAll(leftBrandPane, rightFormPane);
        getChildren().add(mainLayout);

        // -------------------------------------------------------------
        // 3. Floating Top-Right 3D Theme Switcher
        // -------------------------------------------------------------
        HBox topBar = buildThemeSwitcherBar();
        topBar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(topBar, Pos.TOP_RIGHT);
        StackPane.setMargin(topBar, new Insets(24, 32, 0, 0));
        getChildren().add(topBar);

        // Smooth Entrance Transition
        playEntranceAnimation(mainLayout);
    }

    // =========================================================================
    // LEFT BRANDING PANE
    // =========================================================================
    private VBox buildLeftBrandPane() {
        VBox pane = new VBox(18);
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.setPrefWidth(470);
        pane.setMinWidth(420);
        pane.setPickOnBounds(false);

        // HUD Top Tag
        hudTagLabel = new Label("— ENTERPRISE FITNESS OS");
        hudTagLabel.getStyleClass().add("login-hud-tag");

        // -------------------------------------------------------------
        // Brand Header: Eagle Logo + Heavy IWFC Logotype
        // -------------------------------------------------------------
        HBox brandHeader = new HBox(18);
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        // Geometric Eagle Crest Logo (from user upload)
        ImageView eagleView = createEagleLogoView();

        // Heavy "IW" & "FC" Logotype
        VBox logotypeBox = new VBox(-16);
        logotypeBox.setAlignment(Pos.CENTER_LEFT);

        Font heavyFont = resolveHeavyFont(76);

        textIW = new Text("IW");
        textIW.setFont(heavyFont);

        textFC = new Text("FC");
        textFC.setFont(heavyFont);

        logotypeBox.getChildren().addAll(textIW, textFC);
        brandHeader.getChildren().addAll(eagleView, logotypeBox);

        // -------------------------------------------------------------
        // Alive Heartbeat (ECG) Monitor
        // -------------------------------------------------------------
        heartbeatView = new LiveHeartbeatView();

        // -------------------------------------------------------------
        // Telemetry Stats Row
        // -------------------------------------------------------------
        HBox statsRow = new HBox(36);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setPadding(new Insets(6, 0, 8, 0));

        onFloorNumLabel = new Label("128");
        onFloorNumLabel.getStyleClass().add("stats-number");
        onFloorTagLabel = new Label("ON FLOOR");
        onFloorTagLabel.getStyleClass().add("stats-label");
        VBox colFloor = new VBox(2, onFloorTagLabel, onFloorNumLabel);

        checkInsNumLabel = new Label("412");
        checkInsNumLabel.getStyleClass().add("stats-number");
        checkInsTagLabel = new Label("CHECK-INS");
        checkInsTagLabel.getStyleClass().add("stats-label");
        VBox colCheckIns = new VBox(2, checkInsTagLabel, checkInsNumLabel);

        uptimeNumLabel = new Label("99.98%");
        uptimeNumLabel.getStyleClass().add("stats-number");
        uptimeTagLabel = new Label("UPTIME");
        uptimeTagLabel.getStyleClass().add("stats-label");
        VBox colUptime = new VBox(2, uptimeTagLabel, uptimeNumLabel);

        statsRow.getChildren().addAll(colFloor, colCheckIns, colUptime);

        // -------------------------------------------------------------
        // Slogan: TRAIN. TRACK. TRANSFORM.
        // -------------------------------------------------------------
        VBox sloganBox = new VBox(2);
        sloganBox.setAlignment(Pos.CENTER_LEFT);

        HBox sloganLine1 = new HBox(6);
        sloganLine1.setAlignment(Pos.CENTER_LEFT);

        sloganTrain = new Label("TRAIN.");
        sloganTrain.getStyleClass().addAll("slogan-text", "slogan-main");

        sloganTrack = new Label("TRACK.");
        sloganTrack.getStyleClass().addAll("slogan-text", "slogan-track");

        sloganTrackBadge = new HBox(sloganTrack);
        sloganTrackBadge.setAlignment(Pos.CENTER);

        sloganLine1.getChildren().addAll(sloganTrain, sloganTrackBadge);

        sloganTransform = new Label("TRANSFORM.");
        sloganTransform.getStyleClass().addAll("slogan-text", "slogan-main");

        sloganBox.getChildren().addAll(sloganLine1, sloganTransform);

        pane.getChildren().addAll(hudTagLabel, brandHeader, heartbeatView, statsRow, sloganBox);
        return pane;
    }

    private ImageView createEagleLogoView() {
        ImageView eagleView = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream("/com/iwfc/images/iwfc-logo.png");
            if (is != null) {
                Image img = new Image(is);
                eagleView.setImage(img);
            }
        } catch (Exception ignored) {}

        eagleView.setFitHeight(98);
        eagleView.setPreserveRatio(true);
        eagleView.setSmooth(true);

        // Soft ambient blue aura glow matching the crystalline facets
        DropShadow aura = new DropShadow(20, Color.rgb(56, 189, 248, 0.45));
        eagleView.setEffect(aura);

        // Subtle idle breathing scale transition
        ScaleTransition breathe = new ScaleTransition(Duration.millis(2400), eagleView);
        breathe.setFromX(1.0);
        breathe.setFromY(1.0);
        breathe.setToX(1.035);
        breathe.setToY(1.035);
        breathe.setAutoReverse(true);
        breathe.setCycleCount(Animation.INDEFINITE);
        breathe.play();

        return eagleView;
    }

    // =========================================================================
    // RIGHT FORM CARD (CHAMFERED CYBER CARD)
    // =========================================================================
    private VBox buildRightFormCard() {
        cardOuterContainer = new StackPane();
        cardOuterContainer.setPrefWidth(420);
        cardOuterContainer.setMaxWidth(440);
        cardOuterContainer.setPrefHeight(550);

        // Clipped card body with top-right & bottom-left chamfer
        StackPane cardBody = new StackPane();
        cardBody.setPadding(new Insets(34, 36, 32, 36));

        // Reactive chamfer clip
        double chamfer = 28.0;
        Polygon clipPolygon = new Polygon();
        cardBody.setClip(clipPolygon);

        cardBody.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (newB != null && newB.getWidth() > 0 && newB.getHeight() > 0) {
                double w = newB.getWidth();
                double h = newB.getHeight();
                clipPolygon.getPoints().setAll(
                        0.0, 0.0,
                        w - chamfer, 0.0,
                        w, chamfer,
                        w, h,
                        chamfer, h,
                        0.0, h - chamfer
                );
            }
        });

        // Top Lime Accent Bar & Chamfered Border Overlay Canvas
        cardBorderCanvas = new Canvas();
        cardBorderCanvas.setMouseTransparent(true);
        cardBorderCanvas.widthProperty().bind(cardOuterContainer.widthProperty());
        cardBorderCanvas.heightProperty().bind(cardOuterContainer.heightProperty());
        cardBorderCanvas.widthProperty().addListener(e -> redrawCardBorder());
        cardBorderCanvas.heightProperty().addListener(e -> redrawCardBorder());

        // -------------------------------------------------------------
        // Multi-Step Form Layout
        // -------------------------------------------------------------
        step1View = buildStep1View();
        step2View = buildStep2View();
        step2View.setVisible(false);
        step2View.setManaged(false);

        StackPane stepsContainer = new StackPane(step1View, step2View);
        stepsContainer.setAlignment(Pos.CENTER);

        cardBody.getChildren().add(stepsContainer);
        cardOuterContainer.getChildren().addAll(cardBody, cardBorderCanvas);

        VBox rightWrapper = new VBox(cardOuterContainer);
        rightWrapper.setAlignment(Pos.CENTER);
        return rightWrapper;
    }

    private VBox buildStep1View() {
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER_LEFT);

        // Header
        secureAccessTag = new Label("// STEP 01 - IDENTITY VERIFICATION");
        secureAccessTag.getStyleClass().add("form-secure-tag");

        signInTitle = new Label("SIGN IN");
        signInTitle.getStyleClass().add("form-sign-in-title");

        signInSubtitle = new Label("Select your role and enter your Credential ID.");
        signInSubtitle.getStyleClass().add("form-subtitle-desc");

        VBox headerBox = new VBox(3, secureAccessTag, signInTitle, signInSubtitle);

        // Role Selector Section
        roleSectionLabel = new Label("ROLE");
        roleSectionLabel.getStyleClass().add("form-field-label");

        HBox roleTiles = new HBox(10);
        roleTiles.setAlignment(Pos.CENTER_LEFT);

        adminTile = createRoleTile("👑", "ADMIN", "ADM001");
        trainerTile = createRoleTile("🏋️", "TRAINER", "INS001");
        memberTile = createRoleTile("👤", "MEMBER", "MEM001");

        roleTiles.getChildren().addAll(adminTile, trainerTile, memberTile);
        selectRole("ADMIN", "ADM001");

        VBox roleSection = new VBox(8, roleSectionLabel, roleTiles);

        // Credential ID Section
        credSectionLabel = new Label("CREDENTIAL ID");
        credSectionLabel.getStyleClass().add("form-field-label");

        userIdField = new TextField("ADM001");
        userIdField.getStyleClass().add("cyber-input-field");
        userIdField.setPrefHeight(46);

        step1ErrorLabel = new Label("");
        step1ErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        step1ErrorLabel.setVisible(false);
        step1ErrorLabel.setManaged(false);

        // Auto-select role tile when user types standard prefixes
        userIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (step1ErrorLabel != null) {
                step1ErrorLabel.setVisible(false);
                step1ErrorLabel.setManaged(false);
            }
            userIdField.setStyle("");
            if (newVal != null) {
                String val = newVal.trim().toUpperCase();
                if (val.startsWith("ADM") && !"ADMIN".equals(selectedRole)) {
                    selectRole("ADMIN", null);
                } else if (val.startsWith("INS") && !"TRAINER".equals(selectedRole)) {
                    selectRole("TRAINER", null);
                } else if (val.startsWith("MEM") && !"MEMBER".equals(selectedRole)) {
                    selectRole("MEMBER", null);
                }
            }
        });

        VBox credSection = new VBox(6, credSectionLabel, userIdField, step1ErrorLabel);

        // Action Button: "CONTINUE TO PASSCODE   →"
        enterPortalBtn = new Button("CONTINUE TO PASSCODE  →");
        enterPortalBtn.getStyleClass().add("cyber-enter-btn");
        enterPortalBtn.setMaxWidth(Double.MAX_VALUE);
        enterPortalBtn.setPrefHeight(48);

        applyCutCornerClip(enterPortalBtn);

        enterPortalBtn.setOnAction(e -> handleStep1Continue());
        userIdField.setOnAction(e -> handleStep1Continue());

        // Demo IDs Line
        HBox demoIdsBox = new HBox(6);
        demoIdsBox.setAlignment(Pos.CENTER);

        demoIdsLabel = new Label("DEMO IDS");
        demoIdsLabel.getStyleClass().add("demo-ids-text");

        Label chipAdm = createClickableDemoId("ADM001", "ADMIN");
        Label chipDot1 = new Label("·");
        chipDot1.getStyleClass().add("demo-ids-text");

        Label chipIns = createClickableDemoId("INS001", "TRAINER");
        Label chipDot2 = new Label("·");
        chipDot2.getStyleClass().add("demo-ids-text");

        Label chipMem = createClickableDemoId("MEM001", "MEMBER");

        demoIdsBox.getChildren().addAll(demoIdsLabel, chipAdm, chipDot1, chipIns, chipDot2, chipMem);

        // Card Footer
        HBox footerBox = buildCardFooter();

        content.getChildren().addAll(
                headerBox,
                roleSection,
                credSection,
                enterPortalBtn,
                demoIdsBox,
                footerBox
        );
        return content;
    }

    private VBox buildStep2View() {
        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER_LEFT);

        // Header
        Label step2Tag = new Label("// STEP 02 - SECURITY AUTHENTICATION");
        step2Tag.getStyleClass().add("form-secure-tag");

        Label step2Title = new Label("ENTER PASSCODE");
        step2Title.getStyleClass().add("form-sign-in-title");

        Label step2Subtitle = new Label("Verify your profile credentials to access the portal.");
        step2Subtitle.getStyleClass().add("form-subtitle-desc");

        VBox headerBox = new VBox(3, step2Tag, step2Title, step2Subtitle);

        // Identified User Profile Card
        HBox profileCard = new HBox(12);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 8px; -fx-padding: 10 14; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 8px;");

        step2Avatar = new Circle(18, Color.web("#ccff00"));
        step2AvatarInitials = new Label("AD");
        step2AvatarInitials.setStyle("-fx-text-fill: #000000; -fx-font-weight: 900; -fx-font-size: 11px;");
        StackPane avatarPane = new StackPane(step2Avatar, step2AvatarInitials);

        VBox profileInfo = new VBox(2);
        step2NameLabel = new Label("Sarah Johnson");
        step2NameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 14px;");

        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        step2RolePill = new Label("ADM001 • SYSTEM ADMINISTRATOR");
        step2RolePill.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");
        metaRow.getChildren().add(step2RolePill);

        profileInfo.getChildren().addAll(step2NameLabel, metaRow);

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        Label verifiedBadge = new Label("✓ ID FOUND");
        verifiedBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); -fx-text-fill: #22c55e; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 3 8; -fx-background-radius: 10px; -fx-border-color: rgba(34, 197, 94, 0.3); -fx-border-radius: 10px;");

        profileCard.getChildren().addAll(avatarPane, profileInfo, pSpacer, verifiedBadge);

        // Passcode Section
        Label passSectionLabel = new Label("SECURITY PASSCODE");
        passSectionLabel.getStyleClass().add("form-field-label");

        // Password input row with show/hide toggle
        StackPane passInputStack = new StackPane();
        passInputField = new PasswordField();
        passInputField.setPromptText("••••");
        passInputField.getStyleClass().add("cyber-input-field");
        passInputField.setPrefHeight(46);
        passInputField.setStyle("-fx-font-size: 18px; -fx-padding: 8 44 8 16;");

        passVisibleField = new TextField();
        passVisibleField.setPromptText("0000");
        passVisibleField.getStyleClass().add("cyber-input-field");
        passVisibleField.setPrefHeight(46);
        passVisibleField.setStyle("-fx-font-size: 18px; -fx-padding: 8 44 8 16;");
        passVisibleField.setVisible(false);
        passVisibleField.setManaged(false);

        // Sync inputs between masked and plain fields
        passInputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingPassField && passVisibleField.isVisible()) {
                isUpdatingPassField = true;
                passVisibleField.setText(newVal);
                isUpdatingPassField = false;
            }
            hideStep2Error();
        });
        passVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingPassField && !passVisibleField.isVisible()) {
                isUpdatingPassField = true;
                passInputField.setText(newVal);
                isUpdatingPassField = false;
            }
            hideStep2Error();
        });

        Button toggleEyeBtn = new Button("👁");
        toggleEyeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 10;");
        toggleEyeBtn.setOnAction(e -> {
            boolean show = !passVisibleField.isVisible();
            if (show) {
                isUpdatingPassField = true;
                passVisibleField.setText(passInputField.getText());
                isUpdatingPassField = false;
                passInputField.setVisible(false);
                passInputField.setManaged(false);
                passVisibleField.setVisible(true);
                passVisibleField.setManaged(true);
                toggleEyeBtn.setText("🙈");
                toggleEyeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccff00; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 10;");
                passVisibleField.requestFocus();
                passVisibleField.positionCaret(passVisibleField.getText().length());
            } else {
                isUpdatingPassField = true;
                passInputField.setText(passVisibleField.getText());
                isUpdatingPassField = false;
                passVisibleField.setVisible(false);
                passVisibleField.setManaged(false);
                passInputField.setVisible(true);
                passInputField.setManaged(true);
                toggleEyeBtn.setText("👁");
                toggleEyeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 10;");
                passInputField.requestFocus();
                passInputField.positionCaret(passInputField.getText().length());
            }
        });

        StackPane.setAlignment(toggleEyeBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleEyeBtn, new Insets(0, 6, 0, 0));
        passInputStack.getChildren().addAll(passInputField, passVisibleField, toggleEyeBtn);

        // Clickable Default Passcode Helper Chip
        HBox helperRow = new HBox(8);
        helperRow.setAlignment(Pos.CENTER_LEFT);

        Label hintChip = new Label("🔒 Default Passcode: 0000");
        hintChip.setStyle("-fx-background-color: rgba(204, 255, 0, 0.12); -fx-text-fill: #ccff00; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-cursor: hand; -fx-border-color: rgba(204, 255, 0, 0.25); -fx-border-radius: 4px;");
        hintChip.setOnMouseClicked(e -> {
            passInputField.setText("0000");
            passVisibleField.setText("0000");
            hideStep2Error();
        });

        Label clickHint = new Label("(click to fill)");
        clickHint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        helperRow.getChildren().addAll(hintChip, clickHint);

        // Step 2 Error alert box
        step2ErrorBox = new HBox(6);
        step2ErrorBox.setAlignment(Pos.CENTER_LEFT);
        step2ErrorBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8 12;");
        step2ErrorBox.setVisible(false);
        step2ErrorBox.setManaged(false);

        Label errIcon = new Label("⚠️");
        step2ErrorLabel = new Label("ACCESS DENIED: Invalid passcode. Default passcode is 0000.");
        step2ErrorLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        step2ErrorLabel.setWrapText(true);
        step2ErrorBox.getChildren().addAll(errIcon, step2ErrorLabel);

        VBox passSection = new VBox(6, passSectionLabel, passInputStack, helperRow, step2ErrorBox);

        // Action Buttons: UNLOCK PORTAL + BACK
        Button unlockPortalBtn = new Button("⚡ UNLOCK & ENTER PORTAL  →");
        unlockPortalBtn.getStyleClass().add("cyber-enter-btn");
        unlockPortalBtn.setMaxWidth(Double.MAX_VALUE);
        unlockPortalBtn.setPrefHeight(48);
        applyCutCornerClip(unlockPortalBtn);

        unlockPortalBtn.setOnAction(e -> doLoginWithPasscode());
        passInputField.setOnAction(e -> doLoginWithPasscode());
        passVisibleField.setOnAction(e -> doLoginWithPasscode());

        Button backBtn = new Button("← Back to Change ID");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 8;");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setAlignment(Pos.CENTER);
        backBtn.setOnAction(e -> transitionToStep1());

        // Card Footer
        HBox footerBox = buildCardFooter();

        content.getChildren().addAll(
                headerBox,
                profileCard,
                passSection,
                unlockPortalBtn,
                backBtn,
                footerBox
        );
        return content;
    }

    private void applyCutCornerClip(Button btn) {
        Polygon btnClip = new Polygon();
        btn.setClip(btnClip);
        btn.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (newB != null && newB.getWidth() > 0) {
                double bw = newB.getWidth();
                double bh = newB.getHeight();
                double c = 14.0;
                btnClip.getPoints().setAll(
                        0.0, 0.0,
                        bw, 0.0,
                        bw, bh - c,
                        bw - c, bh,
                        0.0, bh
                );
            }
        });
    }

    private HBox buildCardFooter() {
        HBox footerBox = new HBox();
        footerBox.setAlignment(Pos.CENTER_LEFT);
        footerBox.setPadding(new Insets(10, 0, 0, 0));

        // Pulsing green dot + SYSTEM ONLINE
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 10px;");
        FadeTransition pulseDot = new FadeTransition(Duration.millis(900), dot);
        pulseDot.setFromValue(1.0);
        pulseDot.setToValue(0.35);
        pulseDot.setAutoReverse(true);
        pulseDot.setCycleCount(Animation.INDEFINITE);
        pulseDot.play();

        Label statusLbl = new Label(" SYSTEM ONLINE");
        statusLbl.getStyleClass().add("online-status-text");

        HBox statusLeft = new HBox(4, dot, statusLbl);
        statusLeft.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label vLbl = new Label("V2.4 ENT");
        vLbl.getStyleClass().add("version-text");

        footerBox.getChildren().addAll(statusLeft, spacer, vLbl);
        return footerBox;
    }

    private Button createRoleTile(String icon, String roleName, String demoId) {
        Button btn = new Button();
        btn.getStyleClass().addAll("role-tile-btn", "role-tile-inactive");
        btn.setPrefWidth(110);
        btn.setPrefHeight(72);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px;");

        Label nameLbl = new Label(roleName);
        nameLbl.getStyleClass().add("role-tile-label-inactive");

        VBox content = new VBox(4, iconLbl, nameLbl);
        content.setAlignment(Pos.CENTER);
        btn.setGraphic(content);

        btn.setOnAction(e -> {
            selectRole(roleName, demoId);
            playTileClickAnimation(btn);
        });

        return btn;
    }

    private void selectRole(String roleName, String autofillId) {
        this.selectedRole = roleName;

        updateRoleTileVisual(adminTile, "ADMIN".equals(roleName));
        updateRoleTileVisual(trainerTile, "TRAINER".equals(roleName));
        updateRoleTileVisual(memberTile, "MEMBER".equals(roleName));

        if (autofillId != null && userIdField != null) {
            userIdField.setText(autofillId);
            if (step1ErrorLabel != null) {
                step1ErrorLabel.setVisible(false);
                step1ErrorLabel.setManaged(false);
            }
            userIdField.setStyle("");
        }
    }

    private void updateRoleTileVisual(Button btn, boolean isActive) {
        VBox content = (VBox) btn.getGraphic();
        if (content == null || content.getChildren().size() < 2) return;

        Label nameLbl = (Label) content.getChildren().get(1);

        btn.getStyleClass().removeAll("role-tile-active", "role-tile-inactive");
        nameLbl.getStyleClass().removeAll("role-tile-label-active", "role-tile-label-inactive");

        if (isActive) {
            btn.getStyleClass().add("role-tile-active");
            nameLbl.getStyleClass().add("role-tile-label-active");
        } else {
            btn.getStyleClass().add("role-tile-inactive");
            nameLbl.getStyleClass().add("role-tile-label-inactive");
        }
    }

    private Label createClickableDemoId(String id, String role) {
        Label chip = new Label(id);
        chip.getStyleClass().addAll("demo-ids-text", "demo-id-chip");
        chip.setOnMouseClicked(e -> {
            selectRole(role, id);
            userIdField.setText(id);
            if (step1ErrorLabel != null) {
                step1ErrorLabel.setVisible(false);
                step1ErrorLabel.setManaged(false);
            }
            userIdField.setStyle("");
        });
        return chip;
    }

    private void handleStep1Continue() {
        String userId = userIdField.getText().trim();
        if (userId.isEmpty()) {
            step1ErrorLabel.setText("⚠️ Please enter a credential ID.");
            step1ErrorLabel.setVisible(true);
            step1ErrorLabel.setManaged(true);
            playShakeAnimation(userIdField);
            return;
        }

        User found = com.iwfc.pattern.DataStore.getInstance().getUsers().findById(userId).orElse(null);
        if (found == null) {
            found = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll().stream()
                    .filter(u -> u.getUserId().equalsIgnoreCase(userId))
                    .findFirst()
                    .orElse(null);
        }

        if (found == null) {
            step1ErrorLabel.setText("⚠️ User ID '" + userId + "' not found. Check ID or pick a role.");
            step1ErrorLabel.setVisible(true);
            step1ErrorLabel.setManaged(true);
            userIdField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
            playShakeAnimation(userIdField);
            return;
        }

        step1ErrorLabel.setVisible(false);
        step1ErrorLabel.setManaged(false);
        userIdField.setStyle("");
        transitionToStep2(found);
    }

    private void transitionToStep2(User user) {
        this.targetUser = user;

        step2NameLabel.setText(user.getName());
        step2RolePill.setText(user.getUserId() + " • " + user.getRole().getDisplayName().toUpperCase());

        String initials = getInitials(user.getName());
        step2AvatarInitials.setText(initials);

        Color roleColor;
        switch (user.getRole()) {
            case ADMIN -> roleColor = Color.web("#f59e0b");
            case INSTRUCTOR -> roleColor = Color.web("#06b6d4");
            default -> roleColor = Color.web("#10b981");
        }
        step2Avatar.setFill(roleColor);
        if (roleColor == Color.web("#f59e0b")) {
            step2AvatarInitials.setStyle("-fx-text-fill: black; -fx-font-weight: 900; -fx-font-size: 11px;");
        } else {
            step2AvatarInitials.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 11px;");
        }

        passInputField.clear();
        passVisibleField.clear();
        hideStep2Error();

        step1View.setVisible(false);
        step1View.setManaged(false);

        step2View.setVisible(true);
        step2View.setManaged(true);
        step2View.setOpacity(0.0);
        step2View.setTranslateX(30.0);

        FadeTransition ft = new FadeTransition(Duration.millis(220), step2View);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), step2View);
        tt.setToX(0.0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> passInputField.requestFocus());
        pt.play();
    }

    private void transitionToStep1() {
        step2View.setVisible(false);
        step2View.setManaged(false);

        step1View.setVisible(true);
        step1View.setManaged(true);
        step1View.setOpacity(0.0);
        step1View.setTranslateX(-30.0);

        FadeTransition ft = new FadeTransition(Duration.millis(220), step1View);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), step1View);
        tt.setToX(0.0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> userIdField.requestFocus());
        pt.play();
    }

    private void doLoginWithPasscode() {
        if (targetUser == null) {
            transitionToStep1();
            return;
        }

        String enteredPass = passInputField.isVisible() ? passInputField.getText() : passVisibleField.getText();
        if (enteredPass == null || enteredPass.trim().isEmpty()) {
            showStep2Error("Please enter your security passcode.");
            playShakeAnimation(passInputField.isVisible() ? passInputField : passVisibleField);
            return;
        }

        try {
            facade.login(targetUser.getUserId(), enteredPass.trim());

            step2ErrorBox.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); -fx-border-color: #22c55e; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8 12;");
            step2ErrorBox.getChildren().clear();
            Label succIcon = new Label("✓");
            succIcon.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: 900;");
            Label succMsg = new Label("IDENTITY CONFIRMED — ACCESS GRANTED");
            succMsg.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: 900; -fx-font-size: 11.5px;");
            step2ErrorBox.getChildren().addAll(succIcon, succMsg);
            step2ErrorBox.setVisible(true);
            step2ErrorBox.setManaged(true);

            PauseTransition pause = new PauseTransition(Duration.millis(350));
            pause.setOnFinished(e -> {
                if (onLoginSuccess != null) {
                    onLoginSuccess.run();
                }
            });
            pause.play();
        } catch (Exception ex) {
            showStep2Error(ex.getMessage() != null && ex.getMessage().contains("Invalid password")
                    ? "ACCESS DENIED: Invalid passcode. Default passcode is 0000."
                    : ex.getMessage());
            playShakeAnimation(passInputField.isVisible() ? passInputField : passVisibleField);
            passInputField.selectAll();
            passVisibleField.selectAll();
        }
    }

    private void showStep2Error(String msg) {
        step2ErrorBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8 12;");
        step2ErrorBox.getChildren().clear();
        Label errIcon = new Label("⚠️");
        Label errMsg = new Label(msg);
        errMsg.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        errMsg.setWrapText(true);
        step2ErrorBox.getChildren().addAll(errIcon, errMsg);
        step2ErrorBox.setVisible(true);
        step2ErrorBox.setManaged(true);

        if (passInputField.isVisible()) {
            passInputField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-font-size: 18px; -fx-padding: 8 44 8 16;");
        } else {
            passVisibleField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-font-size: 18px; -fx-padding: 8 44 8 16;");
        }
    }

    private void hideStep2Error() {
        if (step2ErrorBox != null) {
            step2ErrorBox.setVisible(false);
            step2ErrorBox.setManaged(false);
        }
        if (passInputField != null) {
            passInputField.setStyle("-fx-font-size: 18px; -fx-padding: 8 44 8 16;");
        }
        if (passVisibleField != null) {
            passVisibleField.setStyle("-fx-font-size: 18px; -fx-padding: 8 44 8 16;");
        }
    }

    private void playShakeAnimation(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "US";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    // =========================================================================
    // THEME SWITCHER BAR (FLOATING TOP-RIGHT 3D CAPSULE)
    // =========================================================================
    private HBox buildThemeSwitcherBar() {
        HBox container = new HBox(12);
        container.getStyleClass().add("theme-switcher-container");
        container.setAlignment(Pos.CENTER_RIGHT);
        container.setPickOnBounds(false);

        Label themeHudTag = new Label("THEME // MODE");
        themeHudTag.getStyleClass().add("theme-hud-label");

        themeToggle = new CyberThemeToggle(this::applyTheme);

        container.getChildren().addAll(themeHudTag, themeToggle);
        return container;
    }

    private void applyTheme(ThemeManager.Theme newTheme) {
        ThemeManager tm = ThemeManager.getInstance();
        tm.setTheme(newTheme);
        tm.applyTheme(getScene());
        updateThemeStyles();
    }

    /**
     * Recomputes all visual styles for the active theme:
     * - Background Grid and glow
     * - "IW" and "FC" colors (solid lime with black outline in light theme)
     * - Animated ECG line colors
     * - Slogan "TRACK." highlight
     * - Form panel background and borders
     * - Theme switcher pill states
     */
    private void updateThemeStyles() {
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        // 1. Root theme class
        getStyleClass().removeAll("theme-dark", "theme-light", "theme-navy");
        if (theme == ThemeManager.Theme.LIGHT) {
            getStyleClass().add("theme-light");
        } else if (theme == ThemeManager.Theme.DARK_BLUE) {
            getStyleClass().add("theme-navy");
        } else {
            getStyleClass().add("theme-dark");
        }

        // 2. Redraw grid canvas & ECG
        if (gridCanvas != null) gridCanvas.redraw();
        if (heartbeatView != null) heartbeatView.updateTheme();

        // 3. Logotype: "IW" & "FC"
        if (textIW != null && textFC != null) {
            if (theme == ThemeManager.Theme.LIGHT) {
                // "the 'FC' logotype is solid lime with a black outline"
                textIW.setFill(Color.BLACK);
                textIW.setStroke(null);

                textFC.setFill(Color.web("#ccff00"));
                textFC.setStroke(Color.BLACK);
                textFC.setStrokeWidth(3.0);
                textFC.setStrokeType(StrokeType.OUTSIDE);
            } else {
                // Dark / Navy: "IW" is solid white, "FC" is transparent with neon lime outline
                textIW.setFill(Color.WHITE);
                textIW.setStroke(null);

                textFC.setFill(Color.TRANSPARENT);
                textFC.setStroke(Color.web("#ccff00"));
                textFC.setStrokeWidth(3.4);
                textFC.setStrokeType(StrokeType.CENTERED);
            }
        }

        // 4. Slogan "TRACK." Highlight Badge
        if (sloganTrackBadge != null && sloganTrack != null) {
            sloganTrackBadge.getStyleClass().remove("slogan-track-badge");
            if (theme == ThemeManager.Theme.LIGHT) {
                // "a highlight on 'TRACK.'"
                sloganTrackBadge.getStyleClass().add("slogan-track-badge");
                sloganTrack.setStyle("-fx-text-fill: #000000;");
            } else {
                sloganTrack.setStyle("-fx-text-fill: #ccff00;");
            }
        }

        // 5. Card background & shadow
        if (cardOuterContainer != null && !cardOuterContainer.getChildren().isEmpty()) {
            StackPane cardBody = (StackPane) cardOuterContainer.getChildren().get(0);
            if (theme == ThemeManager.Theme.LIGHT) {
                cardBody.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 1;");
                cardOuterContainer.setEffect(new DropShadow(32, 0, 10, Color.rgb(0, 0, 0, 0.08)));
            } else if (theme == ThemeManager.Theme.DARK_BLUE) {
                cardBody.setStyle("-fx-background-color: #07162c; -fx-border-color: rgba(59, 130, 246, 0.22); -fx-border-width: 1;");
                cardOuterContainer.setEffect(new DropShadow(36, 0, 12, Color.rgb(2, 8, 23, 0.8)));
            } else {
                cardBody.setStyle("-fx-background-color: #111614; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-width: 1;");
                cardOuterContainer.setEffect(new DropShadow(36, 0, 12, Color.rgb(0, 0, 0, 0.7)));
            }
        }

        // 6. Redraw chamfered card border with lime top accent
        redrawCardBorder();

        // 7. Update 3D Theme Switcher Toggle
        if (themeToggle != null) {
            themeToggle.syncWithCurrentTheme(true);
        }

        // 8. Re-sync role tiles
        if (adminTile != null) updateRoleTileVisual(adminTile, "ADMIN".equals(selectedRole));
        if (trainerTile != null) updateRoleTileVisual(trainerTile, "TRAINER".equals(selectedRole));
        if (memberTile != null) updateRoleTileVisual(memberTile, "MEMBER".equals(selectedRole));
    }

    private void redrawCardBorder() {
        if (cardBorderCanvas == null) return;
        double w = cardBorderCanvas.getWidth();
        double h = cardBorderCanvas.getHeight();
        if (w <= 10 || h <= 10) return;

        javafx.scene.canvas.GraphicsContext gc = cardBorderCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double c = 28.0;
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        // 1. Subtle outline around the whole chamfered polygon
        Color outlineColor;
        if (theme == ThemeManager.Theme.LIGHT) {
            outlineColor = Color.web("#e2e8f0");
        } else if (theme == ThemeManager.Theme.DARK_BLUE) {
            outlineColor = Color.rgb(59, 130, 246, 0.28);
        } else {
            outlineColor = Color.rgb(255, 255, 255, 0.10);
        }

        gc.setLineWidth(1.0);
        gc.setStroke(outlineColor);
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(w - c, 0);
        gc.lineTo(w, c);
        gc.lineTo(w, h);
        gc.lineTo(c, h);
        gc.lineTo(0, h - c);
        gc.closePath();
        gc.stroke();

        // 2. Thick neon lime accent line along the top edge AND down the top-right chamfer
        gc.setLineWidth(3.6);
        gc.setStroke(Color.web("#ccff00"));
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(w - c, 0);
        gc.lineTo(w, c);
        gc.stroke();
    }



    private Font resolveHeavyFont(double size) {
        // Look for heavy, condensed athletic grotesque fonts
        String[] fontCandidates = {"Impact", "Arial Black", "Segoe UI Black", "Trebuchet MS"};
        for (String candidate : fontCandidates) {
            Font font = Font.font(candidate, FontWeight.BLACK, size);
            if (font != null && font.getFamily().equalsIgnoreCase(candidate)) {
                return font;
            }
        }
        return Font.font("System", FontWeight.EXTRA_BOLD, size);
    }

    private void playEntranceAnimation(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0.6);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(400), node);
        st.setFromX(0.97);
        st.setFromY(0.97);
        st.setToX(1.0);
        st.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
    }

    private void playTileClickAnimation(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(0.96);
        st.setFromY(0.96);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
}

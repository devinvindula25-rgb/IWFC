package com.iwfc.ui.components;

import com.iwfc.ui.ThemeManager;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * CyberThemeToggle: Ultra-Premium 3D Segmented Capsule Theme Switcher.
 * Inspired by modern 3D physical tactile toggles:
 * - 3D extruded base with realistic physical elevation and drop shadows
 * - Inset stadium/pill track with dynamic theme transitions
 * - Vibrant royal cobalt blue sliding circular thumb with soft glow
 * - Custom vector SVG iconography for Sun (Light), Moon (Dark), and Cosmos (Navy)
 * - Zero Unicode emoji dependencies to prevent missing glyph boxes on Windows
 * - Fluid spring-eased TranslateTransition physics with tactile squish feedback
 * - Full interactive drag, click-to-slot, and click-to-cycle support
 */
public class CyberThemeToggle extends StackPane {

    private static final double TRACK_WIDTH = 126.0;
    private static final double TRACK_HEIGHT = 40.0;
    private static final double CORNER_RADIUS = 20.0;
    private static final double THUMB_DIAMETER = 32.0;
    private static final double THUMB_RADIUS = THUMB_DIAMETER / 2.0;
    private static final double THUMB_Y = (TRACK_HEIGHT - THUMB_DIAMETER) / 2.0; // 4.0

    // X positions for the 3 slots: Slot 0 (Light), Slot 1 (Dark), Slot 2 (Navy)
    private static final double SLOT_0_X = 4.0;
    private static final double SLOT_1_X = 47.0;
    private static final double SLOT_2_X = 90.0;

    private final Consumer<ThemeManager.Theme> onThemeChanged;

    // Visual Nodes
    private Rectangle baseExtrusion;
    private Rectangle trackSurface;
    private Pane trackContentPane;
    private StackPane thumbNode;
    private DropShadow thumbGlow;

    // Slot Inactive Icons (on track)
    private Node sunInactiveIcon;
    private Node moonInactiveIcon;
    private Node cosmosInactiveIcon;

    // Active Icons (inside sliding thumb)
    private Node sunActiveIcon;
    private Node moonActiveIcon;
    private Node cosmosActiveIcon;
    private StackPane thumbIconContainer;

    // Animation state
    private Animation activeTransition;
    private boolean isAnimating = false;
    private double dragStartX;
    private double thumbStartX;

    public CyberThemeToggle(Consumer<ThemeManager.Theme> onThemeChanged) {
        this.onThemeChanged = onThemeChanged;

        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT + 6.0);
        setMinSize(TRACK_WIDTH, TRACK_HEIGHT + 6.0);
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT + 6.0);
        setAlignment(Pos.TOP_LEFT);
        setCursor(Cursor.HAND);

        buildComponents();
        setupInteractivity();
        syncWithCurrentTheme(false);

        // Listen for external theme changes
        ThemeManager.getInstance().currentThemeProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                syncWithCurrentTheme(true);
            }
        });
    }

    private void buildComponents() {
        // -------------------------------------------------------------
        // 1. 3D Bottom Extrusion / Base Shadow (Physical depth layer)
        // -------------------------------------------------------------
        baseExtrusion = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
        baseExtrusion.setArcWidth(CORNER_RADIUS * 2.0);
        baseExtrusion.setArcHeight(CORNER_RADIUS * 2.0);
        baseExtrusion.setTranslateY(4.5); // 3D bevel offset downward

        DropShadow baseShadow = new DropShadow(14.0, 0, 6.0, Color.rgb(0, 0, 0, 0.35));
        baseExtrusion.setEffect(baseShadow);

        // -------------------------------------------------------------
        // 2. Main Track Surface (Top face of capsule)
        // -------------------------------------------------------------
        trackSurface = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
        trackSurface.setArcWidth(CORNER_RADIUS * 2.0);
        trackSurface.setArcHeight(CORNER_RADIUS * 2.0);
        trackSurface.setStrokeWidth(1.2);

        // -------------------------------------------------------------
        // 3. Track Slot Content (Inactive Vector Icons)
        // -------------------------------------------------------------
        trackContentPane = new Pane();
        trackContentPane.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        trackContentPane.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        sunInactiveIcon = createSunIcon(false);
        positionCenter(sunInactiveIcon, SLOT_0_X + THUMB_RADIUS, TRACK_HEIGHT / 2.0);

        moonInactiveIcon = createMoonIcon(false);
        positionCenter(moonInactiveIcon, SLOT_1_X + THUMB_RADIUS, TRACK_HEIGHT / 2.0);

        cosmosInactiveIcon = createCosmosIcon(false);
        positionCenter(cosmosInactiveIcon, SLOT_2_X + THUMB_RADIUS, TRACK_HEIGHT / 2.0);

        trackContentPane.getChildren().addAll(sunInactiveIcon, moonInactiveIcon, cosmosInactiveIcon);

        // -------------------------------------------------------------
        // 4. Sliding Thumb (Royal Cobalt Blue circular knob)
        // -------------------------------------------------------------
        thumbNode = new StackPane();
        thumbNode.setPrefSize(THUMB_DIAMETER, THUMB_DIAMETER);
        thumbNode.setMinSize(THUMB_DIAMETER, THUMB_DIAMETER);
        thumbNode.setMaxSize(THUMB_DIAMETER, THUMB_DIAMETER);

        Circle thumbBg = new Circle(THUMB_RADIUS);
        LinearGradient blueGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#3b82f6")),
                new Stop(1.0, Color.web("#1d4ed8"))
        );
        thumbBg.setFill(blueGrad);
        thumbBg.setStroke(Color.web("#60a5fa", 0.6));
        thumbBg.setStrokeWidth(1.0);

        thumbGlow = new DropShadow(10.0, 0, 3.0, Color.rgb(37, 99, 235, 0.55));
        thumbNode.setEffect(thumbGlow);

        // Active Icons inside Thumb
        sunActiveIcon = createSunIcon(true);
        moonActiveIcon = createMoonIcon(true);
        cosmosActiveIcon = createCosmosIcon(true);

        thumbIconContainer = new StackPane();
        thumbIconContainer.getChildren().addAll(sunActiveIcon, moonActiveIcon, cosmosActiveIcon);

        thumbNode.getChildren().addAll(thumbBg, thumbIconContainer);
        thumbNode.setTranslateY(THUMB_Y);
        thumbNode.setTranslateX(SLOT_1_X); // Default Dark (Center)

        trackContentPane.getChildren().add(thumbNode);

        // Assemble Layers
        getChildren().addAll(baseExtrusion, trackSurface, trackContentPane);

        // Add Tooltips for accessibility
        Tooltip.install(this, new Tooltip("Switch Theme: Sun (Light) · Moon (Dark) · Cosmos (Navy)"));
    }

    private void positionCenter(Node node, double cx, double cy) {
        node.setLayoutX(cx);
        node.setLayoutY(cy);
    }

    // =========================================================================
    // VECTOR ICON BUILDERS (Pixel-perfect & Unicode-safe)
    // =========================================================================

    /**
     * Creates a crisp vector Sun icon with 8 rays and a center core.
     */
    private Node createSunIcon(boolean isActive) {
        Group group = new Group();

        Color color = isActive ? Color.WHITE : Color.web("#94a3b8");

        Circle core = new Circle(0, 0, 4.2);
        core.setFill(color);

        group.getChildren().add(core);

        // 8 radiating rays
        double rInner = 6.8;
        double rOuter = 9.8;
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            double x1 = Math.cos(angle) * rInner;
            double y1 = Math.sin(angle) * rInner;
            double x2 = Math.cos(angle) * rOuter;
            double y2 = Math.sin(angle) * rOuter;

            Line ray = new Line(x1, y1, x2, y2);
            ray.setStroke(color);
            ray.setStrokeWidth(1.6);
            ray.setStrokeLineCap(StrokeLineCap.ROUND);
            group.getChildren().add(ray);
        }

        return group;
    }

    /**
     * Creates a crisp crescent Moon vector icon.
     */
    private Node createMoonIcon(boolean isActive) {
        Color color = isActive ? Color.WHITE : Color.web("#94a3b8");

        SVGPath moon = new SVGPath();
        // Crisp 16x16 crescent moon path
        moon.setContent("M 16.5 12.5 C 16.5 17.5 12.5 21 7.5 21 C 5.5 21 3.5 20.3 2 19 C 5.8 18.2 8.8 15 8.8 11 C 8.8 7 5.8 3.8 2 3 C 3.5 1.7 5.5 1 7.5 1 C 12.5 1 16.5 4.5 16.5 12.5 Z");
        moon.setFill(color);

        moon.setScaleX(0.72);
        moon.setScaleY(0.72);
        moon.setTranslateX(-9.0);
        moon.setTranslateY(-11.0);

        Group group = new Group(moon);
        return group;
    }

    /**
     * Creates a high-tech 4-point radiant Cosmos / Nebula star with satellite sparkle.
     */
    private Node createCosmosIcon(boolean isActive) {
        Color color = isActive ? Color.WHITE : Color.web("#94a3b8");

        SVGPath star = new SVGPath();
        // 4-point sparkle star + mini secondary sparkle
        star.setContent("M 10 2 C 10 6 6 10 2 10 C 6 10 10 14 10 18 C 10 14 14 10 18 10 C 14 10 10 6 10 2 Z M 16 14 C 16 16.2 14.2 18 12 18 C 14.2 18 16 19.8 16 22 C 16 19.8 17.8 18 20 18 C 17.8 18 16 16.2 16 14 Z");
        star.setFill(color);

        star.setScaleX(0.75);
        star.setScaleY(0.75);
        star.setTranslateX(-11.0);
        star.setTranslateY(-12.0);

        Group group = new Group(star);
        return group;
    }

    // =========================================================================
    // INTERACTIVITY (Click, Drag, and Micro-interactions)
    // =========================================================================

    private void setupInteractivity() {
        // Click on specific slots or cycle
        setOnMouseClicked(e -> {
            if (isAnimating) return;

            double clickX = e.getX();
            if (clickX < 42.0) {
                selectTheme(ThemeManager.Theme.LIGHT);
            } else if (clickX > 84.0) {
                selectTheme(ThemeManager.Theme.DARK_BLUE);
            } else {
                selectTheme(ThemeManager.Theme.DARK);
            }
        });

        // Hover Elevation Micro-animation
        setOnMouseEntered(e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(140), this);
            lift.setToY(-2.0);
            lift.play();

            baseExtrusion.setEffect(new DropShadow(18.0, 0, 9.0, Color.rgb(0, 0, 0, 0.45)));
        });

        setOnMouseExited(e -> {
            TranslateTransition drop = new TranslateTransition(Duration.millis(140), this);
            drop.setToY(0.0);
            drop.play();

            baseExtrusion.setEffect(new DropShadow(14.0, 0, 6.0, Color.rgb(0, 0, 0, 0.35)));
        });

        // Tactile Press Micro-interaction
        setOnMousePressed(e -> {
            setTranslateY(1.0);
        });

        setOnMouseReleased(e -> {
            setTranslateY(0.0);
        });

        // Drag Thumb gesture
        thumbNode.setOnMousePressed(e -> {
            dragStartX = e.getSceneX();
            thumbStartX = thumbNode.getTranslateX();
            e.consume();
        });

        thumbNode.setOnMouseDragged(e -> {
            double deltaX = e.getSceneX() - dragStartX;
            double targetX = Math.max(SLOT_0_X, Math.min(SLOT_2_X, thumbStartX + deltaX));
            thumbNode.setTranslateX(targetX);
            e.consume();
        });

        thumbNode.setOnMouseReleased(e -> {
            double finalX = thumbNode.getTranslateX();
            double d0 = Math.abs(finalX - SLOT_0_X);
            double d1 = Math.abs(finalX - SLOT_1_X);
            double d2 = Math.abs(finalX - SLOT_2_X);

            if (d0 <= d1 && d0 <= d2) {
                selectTheme(ThemeManager.Theme.LIGHT);
            } else if (d2 <= d1 && d2 <= d0) {
                selectTheme(ThemeManager.Theme.DARK_BLUE);
            } else {
                selectTheme(ThemeManager.Theme.DARK);
            }
            e.consume();
        });
    }

    private void selectTheme(ThemeManager.Theme theme) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm.getCurrentTheme() != theme) {
            if (onThemeChanged != null) {
                onThemeChanged.accept(theme);
            } else {
                tm.setTheme(theme);
                tm.applyTheme(getScene());
            }
        }
        syncWithCurrentTheme(true);
    }

    /**
     * Synchronizes the visual state of the 3D capsule with the current theme.
     */
    public void syncWithCurrentTheme(boolean animate) {
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();

        double targetX;
        if (theme == ThemeManager.Theme.LIGHT) {
            targetX = SLOT_0_X;
        } else if (theme == ThemeManager.Theme.DARK_BLUE) {
            targetX = SLOT_2_X;
        } else {
            targetX = SLOT_1_X;
        }

        // 1. Update Inset Track Colors & Base 3D Extrusion
        updateMaterials(theme);

        // 2. Active/Inactive Icons Visibility
        updateIconVisibility(theme);

        // 3. Thumb Sliding Transition
        if (animate && Math.abs(thumbNode.getTranslateX() - targetX) > 1.0) {
            if (activeTransition != null) {
                activeTransition.stop();
                activeTransition = null;
            }

            isAnimating = true;

            TranslateTransition slide = new TranslateTransition(Duration.millis(240), thumbNode);
            slide.setToX(targetX);
            slide.setInterpolator(Interpolator.SPLINE(0.22, 1.0, 0.36, 1.0));

            // Tactile Squish / Stretch Animation on thumb
            ScaleTransition squish = new ScaleTransition(Duration.millis(120), thumbNode);
            squish.setToX(1.12);
            squish.setToY(0.90);
            squish.setAutoReverse(true);
            squish.setCycleCount(2);

            ParallelTransition pt = new ParallelTransition(slide, squish);
            pt.setOnFinished(e -> {
                isAnimating = false;
                activeTransition = null;
            });
            activeTransition = pt;
            pt.play();
        } else {
            if (activeTransition != null) {
                activeTransition.stop();
                activeTransition = null;
            }
            thumbNode.setTranslateX(targetX);
        }
    }

    private void updateMaterials(ThemeManager.Theme theme) {
        if (theme == ThemeManager.Theme.LIGHT) {
            // Track: Pure clean white surface with subtle crisp border (matching Image 3)
            trackSurface.setFill(Color.web("#ffffff"));
            trackSurface.setStroke(Color.web("#cbd5e1"));

            // 3D Base: Soft slate drop bevel layer beneath white pill
            baseExtrusion.setFill(Color.web("#94a3b8"));
            baseExtrusion.setEffect(new DropShadow(10.0, 0, 4.0, Color.rgb(0, 0, 0, 0.15)));

            // Inactive icon colors for light mode
            setGroupColor(sunInactiveIcon, Color.web("#94a3b8"));
            setGroupColor(moonInactiveIcon, Color.web("#64748b"));
            setGroupColor(cosmosInactiveIcon, Color.web("#64748b"));

        } else if (theme == ThemeManager.Theme.DARK_BLUE) {
            // Track: Deep space midnight blue surface with electric border
            trackSurface.setFill(Color.web("#0a1931"));
            trackSurface.setStroke(Color.web("#2563eb", 0.5));

            // 3D Base: Deep midnight black extrusion with blue aura
            baseExtrusion.setFill(Color.web("#030814"));
            baseExtrusion.setEffect(new DropShadow(14.0, 0, 6.0, Color.rgb(2, 6, 23, 0.7)));

            // Inactive icon colors for navy mode
            setGroupColor(sunInactiveIcon, Color.web("#60a5fa", 0.7));
            setGroupColor(moonInactiveIcon, Color.web("#60a5fa", 0.7));
            setGroupColor(cosmosInactiveIcon, Color.web("#93c5fd"));

        } else {
            // Dark (Tactical Black): Sleek dark carbon matte surface (matching Image 3)
            trackSurface.setFill(Color.web("#1e293b"));
            trackSurface.setStroke(Color.web("#334155", 0.6));

            // 3D Base: Deep pitch-black base extrusion
            baseExtrusion.setFill(Color.web("#090b0e"));
            baseExtrusion.setEffect(new DropShadow(14.0, 0, 6.0, Color.rgb(0, 0, 0, 0.65)));

            // Inactive icon colors for dark mode
            setGroupColor(sunInactiveIcon, Color.web("#94a3b8"));
            setGroupColor(moonInactiveIcon, Color.web("#94a3b8"));
            setGroupColor(cosmosInactiveIcon, Color.web("#94a3b8"));
        }
    }

    private void updateIconVisibility(ThemeManager.Theme theme) {
        sunActiveIcon.setVisible(theme == ThemeManager.Theme.LIGHT);
        moonActiveIcon.setVisible(theme == ThemeManager.Theme.DARK);
        cosmosActiveIcon.setVisible(theme == ThemeManager.Theme.DARK_BLUE);

        // Hide the inactive icon underneath the thumb, keep the others visible
        sunInactiveIcon.setOpacity(theme == ThemeManager.Theme.LIGHT ? 0.0 : 0.65);
        moonInactiveIcon.setOpacity(theme == ThemeManager.Theme.DARK ? 0.0 : 0.65);
        cosmosInactiveIcon.setOpacity(theme == ThemeManager.Theme.DARK_BLUE ? 0.0 : 0.65);
    }

    private void setGroupColor(Node node, Color color) {
        if (node instanceof Group g) {
            for (Node child : g.getChildren()) {
                if (child instanceof Shape s) {
                    if (s instanceof Line) {
                        s.setStroke(color);
                    } else if (s.getFill() != null && !Color.TRANSPARENT.equals(s.getFill())) {
                        s.setFill(color);
                    } else {
                        s.setStroke(color);
                    }
                }
            }
        }
    }
}

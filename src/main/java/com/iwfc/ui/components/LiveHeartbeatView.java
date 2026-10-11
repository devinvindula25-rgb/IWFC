package com.iwfc.ui.components;

import com.iwfc.ui.ThemeManager;
import javafx.animation.AnimationTimer;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Live Heartbeat (ECG) View.
 * Renders an authentic biological ECG waveform that is truly ALIVE and working:
 * - Real-time electrical impulse traveling across the waveform at ~72 BPM
 * - High-speed phosphor decay trail trailing the pulse head
 * - Cardiac contraction flash on R-wave peak
 * - Heart rate variability (HRV) telemetry readout
 * - Exact theme matching:
 *     Dark/Navy: Neon lime (#CCFF00) with glowing lime halo
 *     Light: Crisp black core line (#0A0D0A) with electric lime halo (#CCFF00)
 */
public class LiveHeartbeatView extends VBox {

    private final Canvas canvas;
    private final Label telemetryBadge;
    private final Label heartIcon;

    private AnimationTimer timer;
    private long lastTimeNanos = 0;
    private double pulseProgress = 0.0; // 0.0 to 1.0
    private double currentBpm = 74.0;
    private double targetBpm = 74.0;
    private double rPeakFlashOpacity = 0.0;
    private boolean hasTriggeredBeatThisCycle = false;

    private static class Point {
        final double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }

    private final List<Point> wavePoints = new ArrayList<>();

    public LiveHeartbeatView() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        setMaxWidth(Double.MAX_VALUE);

        // Telemetry readout
        heartIcon = new Label("♥");
        heartIcon.setStyle("-fx-text-fill: #ccff00; -fx-font-size: 13px; -fx-font-weight: bold;");

        telemetryBadge = new Label("74 BPM  ·  LIVE ECG TELEMETRY");
        telemetryBadge.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1.2px;");

        javafx.scene.layout.HBox telemetryBox = new javafx.scene.layout.HBox(6, heartIcon, telemetryBadge);
        telemetryBox.setAlignment(Pos.CENTER_LEFT);

        canvas = new Canvas(480, 110);
        getChildren().addAll(telemetryBox, canvas);

        // Auto resize canvas width to match parent
        widthProperty().addListener((obs, oldW, newW) -> {
            if (newW != null && newW.doubleValue() > 100) {
                canvas.setWidth(newW.doubleValue());
                recomputeWavePoints();
            }
        });

        recomputeWavePoints();
        setupAnimation();

        // Pause timer when detached from scene
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                if (timer != null) timer.start();
            } else {
                if (timer != null) timer.stop();
            }
        });
    }

    /**
     * Updates colors when theme changes.
     */
    public void updateTheme() {
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
        if (theme == ThemeManager.Theme.LIGHT) {
            heartIcon.setStyle("-fx-text-fill: #3f6212; -fx-font-size: 13px; -fx-font-weight: bold;");
            telemetryBadge.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3f6212;");
        } else {
            heartIcon.setStyle("-fx-text-fill: #ccff00; -fx-font-size: 13px; -fx-font-weight: bold;");
            telemetryBadge.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        }
        drawFrame();
    }

    private void recomputeWavePoints() {
        wavePoints.clear();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 10) w = 480;
        if (h <= 10) h = 110;

        double baselineY = h * 0.58;

        // Construct high-fidelity ECG waveform matching the user's mockup:
        // Left baseline -> QRS Complex 1 -> Minor Complex 2 -> Right baseline
        double startX = 0;
        double endX = w;

        // We place the primary cardiac complex around 20% to 55% of the total width
        double c1Start = Math.max(40, w * 0.18);
        double c1Width = 140;

        // Sample points at 2px increments
        for (double x = startX; x <= endX; x += 2.0) {
            double yOffset = 0.0;

            if (x >= c1Start && x < c1Start + c1Width) {
                double u = x - c1Start;
                // Authentic P-Q-R-S-T wave curve
                if (u < 22) {
                    // P wave (atrial depolarization)
                    yOffset = -8.0 * Math.sin(Math.PI * u / 22.0);
                } else if (u < 38) {
                    // PR segment
                    yOffset = 0.0;
                } else if (u < 48) {
                    // Q wave (initial dip)
                    double t = (u - 38) / 10.0;
                    yOffset = 8.0 * t;
                } else if (u < 62) {
                    // R wave (dramatic ventricular spike UP!)
                    double t = (u - 48) / 14.0;
                    yOffset = 8.0 - 58.0 * Math.sin(Math.PI * t);
                } else if (u < 76) {
                    // S wave (deep plunge DOWN below baseline!)
                    double t = (u - 62) / 14.0;
                    yOffset = 24.0 * Math.sin(Math.PI * t);
                } else if (u < 92) {
                    // ST segment recovery
                    double t = (u - 76) / 16.0;
                    yOffset = 24.0 * (1.0 - t);
                } else if (u < 135) {
                    // T wave (ventricular repolarization)
                    double t = (u - 92) / 43.0;
                    yOffset = -15.0 * Math.sin(Math.PI * t);
                } else {
                    yOffset = 0.0;
                }
            } else if (x >= c1Start + c1Width + 20 && x < c1Start + c1Width + 120 && (c1Start + c1Width + 120 < w)) {
                // Secondary smaller complex for extended width
                double u = x - (c1Start + c1Width + 20);
                if (u < 15) {
                    yOffset = -5.0 * Math.sin(Math.PI * u / 15.0);
                } else if (u < 25) {
                    yOffset = 5.0 * (u - 15) / 10.0;
                } else if (u < 38) {
                    double t = (u - 25) / 13.0;
                    yOffset = 5.0 - 45.0 * Math.sin(Math.PI * t);
                } else if (u < 50) {
                    double t = (u - 38) / 12.0;
                    yOffset = 18.0 * Math.sin(Math.PI * t);
                } else if (u < 62) {
                    double t = (u - 50) / 12.0;
                    yOffset = 18.0 * (1.0 - t);
                } else if (u < 95) {
                    double t = (u - 62) / 33.0;
                    yOffset = -12.0 * Math.sin(Math.PI * t);
                }
            }

            wavePoints.add(new Point(x, baselineY + yOffset));
        }
    }

    private void setupAnimation() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTimeNanos == 0) {
                    lastTimeNanos = now;
                    return;
                }

                double dt = (now - lastTimeNanos) / 1_000_000_000.0;
                lastTimeNanos = now;

                // Speed of pulse: ~1.25 seconds per full cycle (~72 BPM)
                double cycleDuration = 60.0 / currentBpm;
                pulseProgress += dt / cycleDuration;
                if (pulseProgress >= 1.0) {
                    pulseProgress -= 1.0;
                    hasTriggeredBeatThisCycle = false;

                    // Micro-variation in BPM (HRV simulation between 71 and 76)
                    targetBpm = 72.0 + (Math.random() * 5.0);
                }

                // Smoothly approach target BPM
                currentBpm += (targetBpm - currentBpm) * (dt * 2.0);

                // Fade R-peak flash
                if (rPeakFlashOpacity > 0.0) {
                    rPeakFlashOpacity = Math.max(0.0, rPeakFlashOpacity - dt * 3.5);
                }

                // Trigger beat when pulse hits R-wave (~35% of cycle)
                if (pulseProgress >= 0.32 && !hasTriggeredBeatThisCycle) {
                    hasTriggeredBeatThisCycle = true;
                    triggerCardiacThrob();
                }

                drawFrame();
            }
        };
        timer.start();
    }

    private void triggerCardiacThrob() {
        rPeakFlashOpacity = 1.0;

        // Visual throb on heart icon
        ScaleTransition st = new ScaleTransition(Duration.millis(140), heartIcon);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.35);
        st.setToY(1.35);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        telemetryBadge.setText(String.format("%d BPM  ·  LIVE ECG TELEMETRY", Math.round(currentBpm)));
    }

    private void drawFrame() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);

        if (wavePoints.size() < 2) return;

        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
        boolean isLight = (theme == ThemeManager.Theme.LIGHT);

        // Find the current pulse head location along the wave points
        int pulseIndex = (int) (pulseProgress * (wavePoints.size() - 1));
        pulseIndex = Math.max(0, Math.min(pulseIndex, wavePoints.size() - 1));
        Point currentPulsePoint = wavePoints.get(pulseIndex);

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // -------------------------------------------------------------
        // PASS 1: Base Line Rendering
        // -------------------------------------------------------------
        if (isLight) {
            // Light Theme:
            // "the heartbeat line is black with a lime halo"

            // 1. Lime halo stroke underneath
            gc.setStroke(Color.web("#ccff00", 0.55));
            gc.setLineWidth(8.5);
            strokeWavePath(gc);

            // 2. Crisp black core line on top
            gc.setStroke(Color.web("#0a0d0a", 0.95));
            gc.setLineWidth(2.6);
            strokeWavePath(gc);

        } else {
            // Dark / Navy Theme:
            // "The lime accents and heartbeat line are unchanged"

            // 1. Broad soft ambient halo
            gc.setStroke(Color.web("#ccff00", 0.18));
            gc.setLineWidth(14.0);
            strokeWavePath(gc);

            // 2. Focused vibrant inner halo
            gc.setStroke(Color.web("#ccff00", 0.55));
            gc.setLineWidth(5.5);
            strokeWavePath(gc);

            // 3. Crisp neon lime core
            gc.setStroke(Color.web("#ccff00", 1.0));
            gc.setLineWidth(2.8);
            strokeWavePath(gc);
        }

        // -------------------------------------------------------------
        // PASS 2: Trailing Phosphor Glow behind the pulse head
        // -------------------------------------------------------------
        int trailLength = Math.min(45, pulseIndex);
        if (trailLength > 2) {
            for (int i = 0; i < trailLength; i++) {
                int pIdx = pulseIndex - i;
                if (pIdx < 1) break;
                Point p1 = wavePoints.get(pIdx - 1);
                Point p2 = wavePoints.get(pIdx);

                double ratio = 1.0 - ((double) i / trailLength);
                double alpha = isLight ? (ratio * 0.75) : (ratio * 0.95);

                gc.setStroke(isLight ? Color.web("#ccff00", alpha) : Color.web("#e6ff66", alpha));
                gc.setLineWidth(isLight ? (2.8 + ratio * 4.0) : (3.0 + ratio * 6.0));
                gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // -------------------------------------------------------------
        // PASS 3: R-Peak Cardiac Contraction Flash Burst
        // -------------------------------------------------------------
        if (rPeakFlashOpacity > 0.01) {
            // Find R-peak point (minimum Y coordinate in wave)
            Point rPeak = wavePoints.get(0);
            for (Point p : wavePoints) {
                if (p.y < rPeak.y) rPeak = p;
            }

            double flashRadius = 18.0 * (1.0 + (1.0 - rPeakFlashOpacity) * 0.6);
            RadialGradient burstGrad = new RadialGradient(
                    0, 0, rPeak.x, rPeak.y, flashRadius, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, isLight ? Color.web("#ccff00", rPeakFlashOpacity * 0.8) : Color.web("#ffffff", rPeakFlashOpacity * 0.9)),
                    new Stop(0.5, Color.web("#ccff00", rPeakFlashOpacity * 0.5)),
                    new Stop(1.0, Color.web("#ccff00", 0.0))
            );
            gc.setFill(burstGrad);
            gc.fillOval(rPeak.x - flashRadius, rPeak.y - flashRadius, flashRadius * 2, flashRadius * 2);
        }

        // -------------------------------------------------------------
        // PASS 4: Traveling Pulse Photon Spark Head
        // -------------------------------------------------------------
        double sparkRadius = 10.0;
        RadialGradient sparkGrad = new RadialGradient(
                0, 0, currentPulsePoint.x, currentPulsePoint.y, sparkRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, isLight ? Color.web("#ffffff", 0.95) : Color.web("#ffffff", 1.0)),
                new Stop(0.4, Color.web("#ccff00", 0.8)),
                new Stop(1.0, Color.web("#ccff00", 0.0))
        );
        gc.setFill(sparkGrad);
        gc.fillOval(currentPulsePoint.x - sparkRadius, currentPulsePoint.y - sparkRadius, sparkRadius * 2, sparkRadius * 2);

        // Core dot of the traveling photon
        gc.setFill(isLight ? Color.web("#0a0d0a") : Color.web("#ffffff"));
        gc.fillOval(currentPulsePoint.x - 2.5, currentPulsePoint.y - 2.5, 5.0, 5.0);
    }

    private void strokeWavePath(GraphicsContext gc) {
        gc.beginPath();
        Point p0 = wavePoints.get(0);
        gc.moveTo(p0.x, p0.y);
        for (int i = 1; i < wavePoints.size(); i++) {
            Point p = wavePoints.get(i);
            gc.lineTo(p.x, p.y);
        }
        gc.stroke();
    }
}

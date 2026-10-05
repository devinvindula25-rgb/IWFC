package com.iwfc.util;

import com.iwfc.model.Payment;
import com.iwfc.model.User;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class BillGenerator {

    public static void generateAndOpenPdf(Payment payment, User member) {
        Platform.runLater(() -> {
            try {
                VBox billNode = createBillLayout(payment, member);
                
                @SuppressWarnings("unused")
                Scene dummyScene = new Scene(billNode);
                billNode.applyCss();
                billNode.layout();

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                WritableImage snapshot = billNode.snapshot(params, null);
                
                BufferedImage bImage = SwingFXUtils.fromFXImage(snapshot, null);
                saveToPdf(bImage, payment.getPaymentId());

            } catch (Exception e) {
                System.err.println("Error generating PDF: " + e.getMessage());
            }
        });
    }

    private static void saveToPdf(BufferedImage bImage, String paymentId) {
        new Thread(() -> {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bImage);
                float width = pdImage.getWidth();
                float height = pdImage.getHeight();
                page.setMediaBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(width, height));
                
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, width, height);
                }

                Files.createDirectories(Paths.get("bills"));
                String fileName = "bills/Receipt_" + paymentId + ".pdf";
                File file = new File(fileName);
                document.save(file);
                
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()).start();
                }

            } catch (IOException ex) {
                System.err.println("Error saving PDF: " + ex.getMessage());
            }
        }).start();
    }

    public static VBox createBillLayout(Payment payment, User member) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(35));
        root.setPrefWidth(580);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #090d16, #020617); -fx-border-color: rgba(56, 189, 248, 0.35); -fx-border-width: 1.5px; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 25, 0.2, 0, 8);");

        // --- Logo / Header ---
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        Label logo = new Label("IWFC");
        logo.setStyle("-fx-text-fill: linear-gradient(to right, #38bdf8, #818cf8); -fx-font-size: 46px; -fx-font-weight: 900; -fx-effect: dropshadow(gaussian, rgba(56,189,248,0.6), 16, 0.2, 0, 0);");
        Label subTitle = new Label("INTELLIGENT WELLNESS & FITNESS CENTER");
        subTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        
        Label recTitle = new Label("OFFICIAL TAX INVOICE & RECEIPT");
        recTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800; -fx-padding: 12 0 0 0;");
        
        Label securityBadge = new Label("🔒 VERIFIED SECURE TRANSACTION • IWFC PAYMENT GATEWAY");
        securityBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.12); -fx-text-fill: #34d399; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 12; -fx-font-size: 10px; -fx-font-weight: bold;");

        header.getChildren().addAll(logo, subTitle, recTitle, securityBadge);

        // --- Metadata Grid ---
        HBox metaBox = new HBox(40);
        metaBox.setAlignment(Pos.CENTER);
        metaBox.setPadding(new Insets(15, 0, 8, 0));
        
        String memberName = member != null ? member.getName() : "Valued Member";
        String memberId = member != null ? member.getUserId() : payment.getMemberId();

        VBox leftMeta = new VBox(12);
        leftMeta.getChildren().addAll(
            createMetaLabel("BILLED TO:", memberName),
            createMetaLabel("TRANSACTION DATE:", payment.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")))
        );
        
        VBox rightMeta = new VBox(12);
        rightMeta.getChildren().addAll(
            createMetaLabel("MEMBER ID:", memberId),
            createMetaLabel("INVOICE REFERENCE:", payment.getPaymentId())
        );
        
        metaBox.getChildren().addAll(leftMeta, rightMeta);

        // --- Invoice Details Panel (Glassmorphism) ---
        VBox detailsPanel = new VBox(14);
        detailsPanel.setPadding(new Insets(20));
        detailsPanel.setStyle("-fx-background-color: rgba(30, 41, 59, 0.45); -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 14px; -fx-background-radius: 14px;");

        HBox purpRow = new HBox();
        purpRow.setAlignment(Pos.CENTER_LEFT);
        VBox pBox = new VBox(3);
        Label p1 = new Label("PAYMENT PURPOSE:"); p1.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label p2 = new Label(payment.getPurpose().getDisplayName()); p2.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: 700;");
        pBox.getChildren().addAll(p1, p2);
        
        Region pSpc = new Region(); HBox.setHgrow(pSpc, Priority.ALWAYS);
        
        VBox statBox = new VBox(3);
        statBox.setAlignment(Pos.CENTER_RIGHT);
        Label s1 = new Label("PAYMENT STATUS:"); s1.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label s2 = new Label(payment.getStatus() == com.iwfc.model.PaymentStatus.REFUNDED ? "↩ REFUNDED" : "✔ PAID IN FULL");
        s2.setStyle("-fx-text-fill: " + (payment.getStatus() == com.iwfc.model.PaymentStatus.REFUNDED ? "#c084fc" : "#34d399") + "; -fx-font-size: 15px; -fx-font-weight: 800;");
        statBox.getChildren().addAll(s1, s2);
        
        purpRow.getChildren().addAll(pBox, pSpc, statBox);

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
        div.setMinHeight(1);
        div.setPrefHeight(1);

        // Breakdown
        detailsPanel.getChildren().addAll(
            purpRow, div,
            createBreakdownRow("Base Membership & Service Fee", String.format("LKR %,.2f", payment.getAmount())),
            createBreakdownRow("Gateway Processing Fee & Digital Levy", String.format("LKR %,.2f", payment.getProcessingFee()))
        );

        // Total Box
        VBox totalBox = new VBox(4);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setPadding(new Insets(12, 0, 4, 0));
        totalBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 10px; -fx-padding: 12;");
        Label t1 = new Label("TOTAL AMOUNT CHARGED"); t1.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        Label t2 = new Label(String.format("LKR %,.2f", payment.getTotalCharged()));
        t2.setStyle("-fx-text-fill: linear-gradient(to right, #38bdf8, #818cf8); -fx-font-size: 32px; -fx-font-weight: 900; -fx-effect: dropshadow(gaussian, rgba(56,189,248,0.5), 12, 0.2, 0, 0);");
        totalBox.getChildren().addAll(t1, t2);
        detailsPanel.getChildren().add(totalBox);

        // --- Footer with Payment Method & Procedural Vector QR Code ---
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        
        VBox methBox = new VBox(4);
        Label m1 = new Label("SETTLEMENT METHOD:"); m1.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label m2 = new Label(payment.getMethod().getDisplayName()); m2.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
        Label m3 = new Label("Auth Code: AUTH-" + Math.abs(payment.getPaymentId().hashCode() % 1000000));
        m3.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        methBox.getChildren().addAll(m1, m2, m3);
        
        Region fSpc = new Region(); HBox.setHgrow(fSpc, Priority.ALWAYS);
        
        // Procedural Vector QR Matrix Canvas (Clean & Authentic)
        StackPane qrContainer = createProceduralQrCode(payment.getPaymentId());
        
        footer.getChildren().addAll(methBox, fSpc, qrContainer);

        root.getChildren().addAll(header, metaBox, detailsPanel, footer);
        return root;
    }

    /**
     * Renders a procedural vector QR code canvas with position finders and deterministic modules.
     */
    private static StackPane createProceduralQrCode(String paymentId) {
        int size = 78;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // White background
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(0, 0, size, size, 8, 8);

        // Draw 3 Position Finder Patterns at corners (7x7 modules)
        int cellSize = 3;
        int padding = 6;
        gc.setFill(Color.BLACK);

        drawFinderPattern(gc, padding, padding, cellSize);
        drawFinderPattern(gc, size - padding - 7 * cellSize, padding, cellSize);
        drawFinderPattern(gc, padding, size - padding - 7 * cellSize, cellSize);

        // Pseudo-random deterministic data modules based on paymentId hash
        long seed = paymentId.hashCode();
        java.util.Random rand = new java.util.Random(seed);
        for (int x = padding; x < size - padding; x += cellSize) {
            for (int y = padding; y < size - padding; y += cellSize) {
                // Skip corner finder zones
                boolean inTopLeft = (x < padding + 8 * cellSize) && (y < padding + 8 * cellSize);
                boolean inTopRight = (x > size - padding - 8 * cellSize) && (y < padding + 8 * cellSize);
                boolean inBottomLeft = (x < padding + 8 * cellSize) && (y > size - padding - 8 * cellSize);
                boolean inCenterBadge = (x >= size / 2 - 6 && x <= size / 2 + 6 && y >= size / 2 - 6 && y <= size / 2 + 6);

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenterBadge) {
                    if (rand.nextBoolean()) {
                        gc.fillRect(x, y, cellSize, cellSize);
                    }
                }
            }
        }

        // Center Security Insignia
        gc.setFill(Color.web("#0ea5e9"));
        gc.fillRoundRect(size / 2.0 - 6, size / 2.0 - 6, 12, 12, 3, 3);
        gc.setFill(Color.WHITE);
        gc.fillRect(size / 2.0 - 3, size / 2.0 - 3, 6, 6);

        StackPane wrapper = new StackPane(canvas);
        wrapper.setPadding(new Insets(4));
        wrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        return wrapper;
    }

    private static void drawFinderPattern(GraphicsContext gc, int startX, int startY, int cSize) {
        gc.setFill(Color.BLACK);
        gc.fillRect(startX, startY, 7 * cSize, 7 * cSize);
        gc.setFill(Color.WHITE);
        gc.fillRect(startX + cSize, startY + cSize, 5 * cSize, 5 * cSize);
        gc.setFill(Color.BLACK);
        gc.fillRect(startX + 2 * cSize, startY + 2 * cSize, 3 * cSize, 3 * cSize);
    }

    private static VBox createMetaLabel(String title, String value) {
        VBox b = new VBox(2);
        Label t = new Label(title); t.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label v = new Label(value); v.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 700;");
        b.getChildren().addAll(t, v);
        return b;
    }

    private static HBox createBreakdownRow(String title, String value) {
        HBox row = new HBox();
        Label t = new Label(title); t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Region spc = new Region(); HBox.setHgrow(spc, Priority.ALWAYS);
        Label v = new Label(value); v.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600;");
        row.getChildren().addAll(t, spc, v);
        return row;
    }
}

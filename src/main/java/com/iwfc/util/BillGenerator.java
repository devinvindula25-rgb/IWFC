package com.iwfc.util;

import com.iwfc.model.Payment;
import com.iwfc.model.User;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
        // Run the UI snapshot generation on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                VBox billNode = createBillLayout(payment, member);
                
                // We must wrap it in a Scene to calculate CSS and layout properly before snapshot
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
        // Run IO off the JavaFX thread
        new Thread(() -> {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(); // Default A4 size usually, but we will size it to the image
                
                // Convert BufferedImage to PDImageXObject
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bImage);
                
                // Calculate scale to fit page if needed, or set page size to image size
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
                
                // Open the PDF on Windows
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()).start();
                }

            } catch (IOException ex) {
                System.err.println("Error saving PDF: " + ex.getMessage());
            }
        }).start();
    }

    public static VBox createBillLayout(Payment payment, User member) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setPrefWidth(600);
        // Premium Dark Background with glow effect
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #020617); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 2px; -fx-border-radius: 20px;");

        // --- Logo / Header ---
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        Label logo = new Label("IWFC");
        logo.setStyle("-fx-text-fill: linear-gradient(to right, #38bdf8, #818cf8); -fx-font-size: 52px; -fx-font-weight: 900; -fx-effect: dropshadow(gaussian, rgba(56,189,248,0.5), 15, 0, 0, 0);");
        Label subTitle = new Label("IRON WORKS FITNESS CENTER");
        subTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-letter-spacing: 2px;");
        Label recTitle = new Label("MEMBER RECEIPT");
        recTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 20 0 0 0;");
        header.getChildren().addAll(logo, subTitle, recTitle);

        // --- Metadata Grid ---
        HBox metaBox = new HBox(50);
        metaBox.setAlignment(Pos.CENTER);
        metaBox.setPadding(new Insets(20, 0, 10, 0));
        
        VBox leftMeta = new VBox(15);
        leftMeta.getChildren().addAll(
            createMetaLabel("MEMBER NAME:", member.getName()),
            createMetaLabel("DATE:", payment.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
        );
        
        VBox rightMeta = new VBox(15);
        rightMeta.getChildren().addAll(
            createMetaLabel("MEMBER ID:", member.getUserId()),
            createMetaLabel("RECEIPT NO:", payment.getPaymentId())
        );
        
        metaBox.getChildren().addAll(leftMeta, rightMeta);

        // --- Invoice Details Panel (Glassmorphism) ---
        VBox detailsPanel = new VBox(15);
        detailsPanel.setPadding(new Insets(25));
        detailsPanel.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 16px; -fx-background-radius: 16px;");

        HBox purpRow = new HBox();
        VBox pBox = new VBox(5);
        Label p1 = new Label("PAYMENT PURPOSE:"); p1.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        Label p2 = new Label(payment.getPurpose().getDisplayName()); p2.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        pBox.getChildren().addAll(p1, p2);
        
        Region pSpc = new Region(); HBox.setHgrow(pSpc, Priority.ALWAYS);
        
        VBox statBox = new VBox(5);
        statBox.setAlignment(Pos.CENTER_RIGHT);
        Label s1 = new Label("STATUS:"); s1.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        Label s2 = new Label("✔ PAID"); s2.setStyle("-fx-text-fill: #34d399; -fx-font-size: 18px; -fx-font-weight: bold;");
        statBox.getChildren().addAll(s1, s2);
        
        purpRow.getChildren().addAll(pBox, pSpc, statBox);

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        div.setMinHeight(1);
        div.setPrefHeight(1);

        // Breakdown
        detailsPanel.getChildren().addAll(
            purpRow, div,
            createBreakdownRow("Base Amount", String.format("LKR %,.2f", payment.getAmount())),
            createBreakdownRow("Processing Fee", String.format("LKR %,.2f", payment.getProcessingFee()))
        );

        // Total
        VBox totalBox = new VBox(5);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setPadding(new Insets(20, 0, 0, 0));
        Label t1 = new Label("TOTAL AMOUNT"); t1.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
        Label t2 = new Label(String.format("LKR %,.2f", payment.getTotalCharged()));
        t2.setStyle("-fx-text-fill: linear-gradient(to right, #38bdf8, #818cf8); -fx-font-size: 36px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(56,189,248,0.4), 10, 0, 0, 0);");
        totalBox.getChildren().addAll(t1, t2);
        detailsPanel.getChildren().add(totalBox);

        // --- Footer ---
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(20, 0, 0, 0));
        
        VBox methBox = new VBox(5);
        Label m1 = new Label("PAYMENT METHOD:"); m1.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        Label m2 = new Label(payment.getMethod().getDisplayName()); m2.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        methBox.getChildren().addAll(m1, m2);
        
        Region fSpc = new Region(); HBox.setHgrow(fSpc, Priority.ALWAYS);
        
        // Mock QR Code Box
        StackPane qrBox = new StackPane();
        qrBox.setStyle("-fx-background-color: white; -fx-padding: 5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        qrBox.setPrefSize(80, 80);
        Label qrLbl = new Label("QR\nVERIFY");
        qrLbl.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");
        qrLbl.setAlignment(Pos.CENTER);
        qrBox.getChildren().add(qrLbl);
        
        footer.getChildren().addAll(methBox, fSpc, qrBox);

        root.getChildren().addAll(header, metaBox, detailsPanel, footer);
        return root;
    }

    private static VBox createMetaLabel(String title, String value) {
        VBox b = new VBox(2);
        Label t = new Label(title); t.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        Label v = new Label(value); v.setStyle("-fx-text-fill: #f3f4f6; -fx-font-size: 16px;");
        b.getChildren().addAll(t, v);
        return b;
    }

    private static HBox createBreakdownRow(String title, String value) {
        HBox row = new HBox();
        Label t = new Label(title); t.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 15px;");
        Region spc = new Region(); HBox.setHgrow(spc, Priority.ALWAYS);
        Label v = new Label(value); v.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
        row.getChildren().addAll(t, spc, v);
        return row;
    }
}

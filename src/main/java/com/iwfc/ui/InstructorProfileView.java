package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Enrollment;
import com.iwfc.model.Instructor;
import com.iwfc.model.Member;
import com.iwfc.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.List;

public class InstructorProfileView extends VBox {

    private final IWFCSystemFacade facade;
    private final Instructor instructor;

    public InstructorProfileView(IWFCSystemFacade facade, Instructor instructor) {
        this.facade = facade;
        this.instructor = instructor;

        setSpacing(20);
        
        buildInsights();
    }

    private void buildInsights() {
        // 1. Stats Row
        HBox statsRow = new HBox(15);
        
        VBox impactCard = new VBox(10);
        impactCard.getStyleClass().add("glass-card");
        impactCard.setPadding(new Insets(20));
        impactCard.setAlignment(Pos.CENTER);
        Label impactTitle = new Label("Impact Score (IIS)");
        impactTitle.getStyleClass().add("detail-label-title");
        
        // Radial Gauge simulation
        StackPane gauge = new StackPane();
        Circle bgCircle = new Circle(40);
        bgCircle.setFill(Color.TRANSPARENT);
        bgCircle.setStroke(Color.web("rgba(255,255,255,0.1)"));
        bgCircle.setStrokeWidth(8);
        
        Arc progressArc = new Arc(0, 0, 40, 40, 90, 0); // Start at 0
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.web("#8b5cf6"));
        progressArc.setStrokeWidth(8);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);
        progressArc.setEffect(new DropShadow(15, Color.web("#8b5cf6")));
        
        Label scoreLbl = new Label(String.format("%.1f", instructor.getPerformanceScore()));
        scoreLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Animate the gauge filling up
        double targetAngle = -360 * (instructor.getPerformanceScore() / 100.0);
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(progressArc.lengthProperty(), 0)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(progressArc.lengthProperty(), targetAngle, javafx.animation.Interpolator.EASE_OUT))
        );
        timeline.play();
        
        gauge.getChildren().addAll(bgCircle, progressArc, scoreLbl);
        impactCard.getChildren().addAll(impactTitle, gauge);
        HBox.setHgrow(impactCard, Priority.ALWAYS);

        VBox attCard = createStatBox("Attendance Rate", instructor.getAttendanceRate() + "%", "rgba(34, 197, 94, 0.15)", "#22c55e");
        attCard.getStyleClass().add("glass-card");
        
        statsRow.getChildren().addAll(impactCard, attCard);

        // 2. Trainee Roster with Avatar Stacks
        VBox rosterCard = new VBox(15);
        rosterCard.getStyleClass().add("glass-card");
        rosterCard.setPadding(new Insets(20));
        
        Label rosterTitle = new Label("Enrolled Trainees");
        rosterTitle.getStyleClass().add("h2");
        
        HBox avatarStack = new HBox();
        avatarStack.setAlignment(Pos.CENTER_LEFT);
        avatarStack.setSpacing(-15); // Negative spacing for overlap

        List<Enrollment> enrollments = facade.getEnrollmentsForInstructor(instructor.getUserId());
        
        if (enrollments != null && !enrollments.isEmpty()) {
            int count = 0;
            for (Enrollment enr : enrollments) {
                if (count >= 5) break; // show max 5 avatars in stack
                try {
                    List<User> members = facade.getUsersByRole(com.iwfc.model.Role.MEMBER);
                    Member trainee = (Member) members.stream().filter(m -> m.getUserId().equals(enr.getMemberId())).findFirst().orElse(null);
                    if (trainee != null) {
                        ImageView tAvatar = createAvatar(trainee.getProfileImageUrl(), 40);
                        
                        // Add stroke for the stack effect
                        Circle clip = new Circle(20, 20, 20);
                        tAvatar.setClip(clip);
                        
                        // Wrap in StackPane to add a border
                        StackPane borderPane = new StackPane(tAvatar);
                        Circle border = new Circle(22);
                        border.setFill(Color.TRANSPARENT);
                        border.setStroke(Color.web("#1e1e2d")); // Match background color roughly
                        border.setStrokeWidth(3);
                        borderPane.getChildren().add(border);
                        
                        avatarStack.getChildren().add(borderPane);
                        count++;
                    }
                } catch (com.iwfc.exception.UnauthorizedAccessException ex) {
                    // Ignored
                }
            }
            if (enrollments.size() > 5) {
                Label moreLbl = new Label("+" + (enrollments.size() - 5));
                moreLbl.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 20; -fx-font-weight: bold;");
                avatarStack.getChildren().add(moreLbl);
            }
        } else {
            avatarStack.getChildren().add(new Label("No trainees assigned."));
        }
        
        // Detailed Trainee List
        VBox detailedList = new VBox(5);
        if (enrollments != null) {
            for (Enrollment enr : enrollments) {
                try {
                    List<User> members = facade.getUsersByRole(com.iwfc.model.Role.MEMBER);
                    Member trainee = (Member) members.stream().filter(m -> m.getUserId().equals(enr.getMemberId())).findFirst().orElse(null);
                    if (trainee != null) {
                        HBox traineeRow = new HBox(10);
                        traineeRow.setAlignment(Pos.CENTER_LEFT);
                        traineeRow.setPadding(new Insets(5));
                        traineeRow.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");
                        
                        Label tName = new Label(trainee.getName());
                        tName.setStyle("-fx-font-weight: bold;");
                        
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        
                        Label planLbl = new Label("Plan: " + enr.getPlanId());
                        planLbl.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");
                        
                        traineeRow.getChildren().addAll(tName, spacer, planLbl);
                        detailedList.getChildren().add(traineeRow);
                    }
                } catch (com.iwfc.exception.UnauthorizedAccessException ex) {}
            }
        }

        ScrollPane rosterScroll = new ScrollPane(detailedList);
        rosterScroll.setPrefHeight(150);
        rosterScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        rosterScroll.setFitToWidth(true);
        
        rosterCard.getChildren().addAll(rosterTitle, avatarStack, new Separator(), rosterScroll);

        // 3. Feedback and bio
        VBox feedCard = new VBox(15);
        feedCard.getStyleClass().add("glass-card");
        feedCard.setPadding(new Insets(20));
        
        Label bioTitle = new Label("Bio & Expertise");
        bioTitle.getStyleClass().add("h2");
        Label bioDesc = new Label(instructor.getBio() != null ? instructor.getBio() : instructor.getSpecialization());
        bioDesc.setWrapText(true);
        bioDesc.setStyle("-fx-opacity: 0.8;");

        Label feedTitle = new Label("Recent Feedback");
        feedTitle.getStyleClass().add("h2");
        
        VBox feedbackList = new VBox(10);
        if (instructor.getFeedbacks() != null && !instructor.getFeedbacks().isEmpty()) {
            for (String f : instructor.getFeedbacks()) {
                Label fLbl = new Label("\"" + f + "\"");
                fLbl.setWrapText(true);
                fLbl.getStyleClass().add("feedback-quote");
                feedbackList.getChildren().add(fLbl);
            }
        } else {
            feedbackList.getChildren().add(new Label("No feedback available."));
        }
        
        feedCard.getChildren().addAll(bioTitle, bioDesc, new Separator(), feedTitle, feedbackList);

        // 4. Line Chart
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("glass-card");
        chartCard.setPadding(new Insets(20));
        Label chartTitle = new Label("Performance Trend");
        chartTitle.getStyleClass().add("h2");
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setPrefHeight(200);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Jan", 80));
        series.getData().add(new XYChart.Data<>("Feb", 85));
        series.getData().add(new XYChart.Data<>("Mar", instructor.getPerformanceScore() - 5));
        series.getData().add(new XYChart.Data<>("Apr", instructor.getPerformanceScore()));
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.getStyleClass().add("premium-line-chart");
        
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);
        chartCard.getChildren().addAll(chartTitle, chart);

        getChildren().addAll(statsRow, rosterCard, feedCard, chartCard);
    }
    
    private VBox createStatBox(String title, String value, String bg, String color) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12px;");
        HBox.setHgrow(box, Priority.ALWAYS);
        
        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        
        Label vLbl = new Label(value);
        vLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        box.getChildren().addAll(tLbl, vLbl);
        return box;
    }

    private ImageView createAvatar(String url, double size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        
        if (url != null && !url.isEmpty()) {
            try {
                view.setImage(new Image(url, true));
            } catch (Exception e) {
                // Ignore fallback
            }
        }
        
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        view.setClip(clip);
        return view;
    }
}

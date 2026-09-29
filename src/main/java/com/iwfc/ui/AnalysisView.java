package com.iwfc.ui;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Instructor;
import com.iwfc.model.Member;
import com.iwfc.model.Role;
import com.iwfc.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


import java.util.List;
import java.util.stream.Collectors;

/**
 * Top-level view for the Analysis tab.
 */
public class AnalysisView extends VBox {

    private final IWFCSystemFacade facade;
    private final VBox contentArea;

    public AnalysisView(IWFCSystemFacade facade) {
        this.facade = facade;

        getStyleClass().add("analysis-view");
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_LEFT);

        contentArea = new VBox();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        buildUI();
    }

    private void buildUI() {
        getChildren().clear();
        
        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Performance Reports");
        title.getStyleClass().add("h1");

        header.getChildren().add(title);

        User currentUser = facade.getCurrentUser();

        if (currentUser.getRole() == Role.MEMBER) {
            // Member sees their own analysis directly
            getChildren().addAll(header, contentArea);
            showAnalysisFor((Member) currentUser);
        } else {
            // Instructor or Admin sees a grid of member cards
            Label selectLabel = new Label("Select a member to view their report:");
            selectLabel.getStyleClass().add("view-subtitle");
            selectLabel.setStyle("-fx-font-weight: bold;");

            javafx.scene.layout.FlowPane cardsPane = new javafx.scene.layout.FlowPane();
            cardsPane.setHgap(20);
            cardsPane.setVgap(20);
            cardsPane.setPadding(new Insets(10, 0, 0, 0));

            List<User> allUsers;
            if (currentUser.getRole() == Role.ADMIN) {
                allUsers = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll();
            } else {
                // INSTRUCTOR
                Instructor instructor = (Instructor) currentUser;
                allUsers = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll().stream()
                        .filter(u -> instructor.getTraineeIds().contains(u.getUserId()))
                        .collect(Collectors.toList());
            }

            List<Member> members = allUsers.stream()
                    .filter(u -> u instanceof Member)
                    .map(u -> (Member) u)
                    .collect(Collectors.toList());

            for (Member m : members) {
                VBox card = createMemberCard(m);
                cardsPane.getChildren().add(card);
            }

            if (members.isEmpty()) {
                Label placeholder = new Label("No members found.");
                placeholder.getStyleClass().add("empty-placeholder");
                cardsPane.getChildren().add(placeholder);
            }
            
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(cardsPane);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);

            contentArea.getChildren().addAll(selectLabel, scroll);
            getChildren().addAll(header, contentArea);
        }
    }

    private VBox createMemberCard(Member m) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefSize(180, 160);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);");

        Label icon = new Label("👤");
        icon.setStyle("-fx-font-size: 40px;");
        
        Label name = new Label(m.getName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label id = new Label("ID: " + m.getUserId());
        id.setStyle("-fx-font-size: 12px; -fx-opacity: 0.6;");

        card.getChildren().addAll(icon, name, id);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5); -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5); -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));

        card.setOnMouseClicked(e -> showAnalysisFor(m));

        return card;
    }

    private void showAnalysisFor(Member member) {
        contentArea.getChildren().clear();
        
        if (facade.getCurrentUser().getRole() != Role.MEMBER) {
            javafx.scene.control.Button backBtn = new javafx.scene.control.Button("← Back to Members");
            backBtn.getStyleClass().add("action-button");
            backBtn.setOnAction(e -> {
                contentArea.getChildren().clear();
                buildUI();
            });
            contentArea.getChildren().add(backBtn);
        }
        
        MeasurementAnalyticsView analyticsView = new MeasurementAnalyticsView(facade, member);
        VBox.setVgrow(analyticsView, Priority.ALWAYS);
        contentArea.getChildren().add(analyticsView);
        
        // Add fade-in
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), analyticsView);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}

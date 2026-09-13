package com.iwfc;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.ui.LoginView;
import com.iwfc.ui.MainLayout;
import com.iwfc.ui.ThemeManager;
import com.iwfc.ui.components.ToastNotification;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Main entry point for the IWFC JavaFX application.
 * Initializes the Facade, registers Observer listeners, and launches the UI.
 */
public class IWFCApp extends Application {

    private IWFCSystemFacade facade;
    private Scene scene;
    private StackPane rootContainer;

    @Override
    public void start(Stage primaryStage) {
        facade = new IWFCSystemFacade();
        rootContainer = new StackPane();

        scene = new Scene(rootContainer, 1200, 750);
        ThemeManager.getInstance().applyTheme(scene);

        primaryStage.setTitle("IWFC — Intelligent Wellness & Fitness Center");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        showLoginView();

        primaryStage.show();
    }

    /**
     * Shows the login screen.
     */
    private void showLoginView() {
        rootContainer.getChildren().clear();
        LoginView loginView = new LoginView(facade, this::onLoginSuccess);
        rootContainer.getChildren().add(loginView);
    }

    /**
     * Called after successful login. Sets up the main layout with Observer listeners.
     */
    private void onLoginSuccess() {
        rootContainer.getChildren().clear();

        // Create toast container (overlay on top of content)
        StackPane toastContainer = new StackPane();

        // Register toast notification as an Observer listener
        ToastNotification toastListener = new ToastNotification(toastContainer);
        facade.getNotificationService().subscribe("BOOKING_CONFIRMED", toastListener);
        facade.getNotificationService().subscribe("MAINTENANCE_STATUS_CHANGED", toastListener);
        facade.getNotificationService().subscribe("MAINTENANCE_ALERT", toastListener);
        facade.getNotificationService().subscribe("EQUIPMENT_THRESHOLD", toastListener);

        MainLayout mainLayout = new MainLayout(facade, toastContainer, this::showLoginView);
        rootContainer.getChildren().add(mainLayout);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

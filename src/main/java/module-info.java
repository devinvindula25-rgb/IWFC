module com.iwfc {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;

    opens com.iwfc to javafx.fxml;
    opens com.iwfc.ui to javafx.fxml;

    exports com.iwfc;
    exports com.iwfc.model;
    exports com.iwfc.service;
    exports com.iwfc.facade;
    exports com.iwfc.pattern;
    exports com.iwfc.exception;
    exports com.iwfc.ui;
    exports com.iwfc.ui.components;
}

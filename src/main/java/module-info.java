module com.iwfc {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.desktop;
    requires javafx.swing;
    requires org.apache.pdfbox;

    opens com.iwfc to javafx.fxml;
    opens com.iwfc.ui to javafx.fxml;

    exports com.iwfc;
    exports com.iwfc.model;
    exports com.iwfc.service;
    exports com.iwfc.facade;
    exports com.iwfc.pattern;
    exports com.iwfc.pattern.payment;
    exports com.iwfc.pattern.plan;
    exports com.iwfc.exception;
    exports com.iwfc.ui;
    exports com.iwfc.ui.components;
}

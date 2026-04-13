package com.powerloom.ui;
import javafx.scene.control.TabPane;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class DashboardController implements Initializable {

    @FXML
    private TabPane tabbedPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Additional tabs can be added here later:
        // tabbedPane.getTabs().add(new Tab("Another Tool", node));
    }
}
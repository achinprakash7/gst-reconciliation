package com.powerloom;
import com.powerloom.javafx.DashboardApp;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PowerloomApplication {
    public static void main(String[] args) {
        // Launch JavaFX (which internally starts Spring via FxWeaver)
        Application.launch(DashboardApp.class, args);
    }
}
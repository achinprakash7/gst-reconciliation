package com.powerloom.config;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

public class StageReadyEvent extends ApplicationEvent {

    private final Stage stage;

    public StageReadyEvent(Stage stage) {
        super(stage); // ✅ REQUIRED
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}
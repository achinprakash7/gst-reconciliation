package com.powerloom.config;

import com.powerloom.controller.GSTReconciliationController;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final FxWeaver fxWeaver;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        Scene scene = new Scene(
                fxWeaver.loadView(GSTReconciliationController.class), 1200, 750
        );
        scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );
        stage.setTitle("PL Handloom Tools");
        stage.setScene(scene);
        stage.show();
    }
}

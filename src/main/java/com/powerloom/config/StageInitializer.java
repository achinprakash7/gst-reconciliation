package com.powerloom.config;

import com.powerloom.StageReadyEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext applicationContext;

    public StageInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            Stage stage = event.getStage();

            // ── load FXML ──────────────────────────────────────────────
            // DO NOT use AppConfig's shared FXMLLoader bean here.
            // FXMLLoader is stateful — create a fresh one each time.
            FXMLLoader loader = new FXMLLoader();
            loader.setControllerFactory(applicationContext::getBean);

            URL fxmlUrl = getClass().getResource("/fxml/dashboard.fxml");

            // Fail fast with a clear message if the file is missing
            if (fxmlUrl == null) {
                throw new IllegalStateException(
                        "Cannot find /fxml/dashboard.fxml on the classpath.\n" +
                                "Make sure the file is at:\n" +
                                "  src/main/resources/fxml/dashboard.fxml\n" +
                                "and that Maven has copied it to target/classes/fxml/"
                );
            }

            loader.setLocation(fxmlUrl);
            Parent root = loader.load();

            // ── scene & stage ─────────────────────────────────────────
            Scene scene = new Scene(root, 1200, 750);
            stage.setTitle("PL Handloom Tools");
            stage.setScene(scene);
            stage.show();
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "FXML parse error — check dashboard.fxml or reconciliation.fxml\n" +
                            "Cause: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load dashboard FXML: " + e.getMessage(), e);
        }
    }

}
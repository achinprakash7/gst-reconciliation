package com.powerloom.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ToastNotification {

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    public static void show(Stage owner, String message, Type type) {
        Popup popup = new Popup();
        popup.setAutoFix(true);

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 12, 20));
        box.setMaxWidth(420);
        box.setMinHeight(48);

        String bg, border, fg, icon;
        switch (type) {
            case SUCCESS -> { bg="#EAF3DE"; border="#639922"; fg="#27500A"; icon="✓"; }
            case ERROR   -> { bg="#FCEBEB"; border="#E24B4A"; fg="#791F1F"; icon="✕"; }
            case WARNING -> { bg="#FAEEDA"; border="#EF9F27"; fg="#633806"; icon="!"; }
            default      -> { bg="#E6F1FB"; border="#378ADD"; fg="#0C447C"; icon="i"; }
        }

        box.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:0 0 0 4;" +
                        "-fx-border-radius:0 10 10 0;" +
                        "-fx-background-radius:10;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);"
        );

        Label iconLbl = new Label(icon);
        iconLbl.setStyle(
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + fg + ";" +
                        "-fx-background-color:" + border + ";" +
                        "-fx-background-radius:50%;-fx-min-width:24px;-fx-min-height:24px;" +
                        "-fx-alignment:center;"
        );

        Label msgLbl = new Label(message);
        msgLbl.setStyle(
                "-fx-font-size:13px;-fx-text-fill:" + fg + ";-fx-wrap-text:true;" +
                        "-fx-font-family:'Segoe UI',sans-serif;"
        );
        msgLbl.setMaxWidth(340);

        box.getChildren().addAll(iconLbl, msgLbl);
        popup.getContent().add(box);

        // position bottom-right of owner
        double x = owner.getX() + owner.getWidth()  - 450;
        double y = owner.getY() + owner.getHeight()  - 80;
        popup.show(owner, x, y);

        // slide in
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(280), box);
        slideIn.setFromX(60);
        slideIn.setToX(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), box);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        new ParallelTransition(slideIn, fadeIn).play();

        // auto-dismiss after 3s
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), box);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> popup.hide());
            fadeOut.play();
        });
        pause.play();
    }
}
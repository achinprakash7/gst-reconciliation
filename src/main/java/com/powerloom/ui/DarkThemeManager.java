package com.powerloom.ui;

import javafx.scene.Scene;

public class DarkThemeManager {

    private static final String LIGHT_CSS = "/css/app.css";
    private static final String DARK_CSS  = "/css/dark.css";

    private boolean dark = false;

    public void toggle(Scene scene) {
        if (dark) {
            scene.getStylesheets().remove(
                    getClass().getResource(DARK_CSS).toExternalForm());
            dark = false;
        } else {
            scene.getStylesheets().add(
                    getClass().getResource(DARK_CSS).toExternalForm());
            dark = true;
        }
    }

    public boolean isDark() { return dark; }
}
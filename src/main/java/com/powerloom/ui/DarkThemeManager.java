package com.powerloom.ui;

import javafx.scene.Scene;

public class DarkThemeManager {

    private static final String DARK_CSS = "/css/dark.css";
    private boolean dark = false;

    public void toggle(Scene scene) {
        String url = getClass().getResource(DARK_CSS).toExternalForm();
        if (dark) {
            scene.getStylesheets().remove(url);
            dark = false;
        } else {
            if (!scene.getStylesheets().contains(url))
                scene.getStylesheets().add(url);
            dark = true;
        }
    }

    public boolean isDark() { return dark; }
}
package com.powerloom.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class KeyboardShortcutManager {

    public static void register(Scene scene,
                                Runnable onCompare,
                                Runnable onExport,
                                Runnable onReset,
                                Runnable onBrowseB2B,
                                Runnable onBrowseGST) {

        // Ctrl+Enter → Compare
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                onCompare
        );

        // Ctrl+E → Export
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN),
                onExport
        );

        // Ctrl+R → Reset
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
                onReset
        );

        // Ctrl+1 → Browse B2B
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN),
                onBrowseB2B
        );

        // Ctrl+2 → Browse GST
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN),
                onBrowseGST
        );
    }
}
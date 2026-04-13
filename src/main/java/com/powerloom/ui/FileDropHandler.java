package com.powerloom.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class FileDropHandler {

    private static final String STYLE_DRAG_OVER =
            "-fx-background-color:#EEEDFE;" +
                    "-fx-background-radius:8;" +
                    "-fx-border-color:#534AB7;" +
                    "-fx-border-width:2;" +
                    "-fx-border-radius:8;" +
                    "-fx-border-style:dashed;";

    private static final String STYLE_DRAG_ACCEPTED =
            "-fx-background-color:#EAF3DE;" +
                    "-fx-background-radius:8;" +
                    "-fx-border-color:#639922;" +
                    "-fx-border-width:2;" +
                    "-fx-border-radius:8;" +
                    "-fx-border-style:dashed;";

    private static final String STYLE_DRAG_REJECTED =
            "-fx-background-color:#FCEBEB;" +
                    "-fx-background-radius:8;" +
                    "-fx-border-color:#E24B4A;" +
                    "-fx-border-width:2;" +
                    "-fx-border-radius:8;" +
                    "-fx-border-style:dashed;";

    /**
     * Attaches drag-and-drop handlers to the given container HBox.
     * When a valid .xlsx or .xls file is dropped, the path is set
     * into the TextField and the onFileAccepted callback is called.
     *
     * @param container       the HBox row to receive drops
     * @param field           the TextField to populate with file path
     * @param originalStyle   the normal style to restore after drag exits
     * @param onFileAccepted  callback called with the accepted File
     */
    public static void attach(HBox container,
                              TextField field,
                              String originalStyle,
                              Consumer<File> onFileAccepted) {

        // ── dragOver ──────────────────────────────────────────────────────────
        container.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() && isExcel(db.getFiles())) {
                e.acceptTransferModes(TransferMode.COPY);
                container.setStyle(STYLE_DRAG_ACCEPTED);
            } else if (db.hasFiles()) {
                container.setStyle(STYLE_DRAG_REJECTED);
            }
            e.consume();
        });

        // ── dragEntered ───────────────────────────────────────────────────────
        container.setOnDragEntered(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                container.setStyle(isExcel(db.getFiles())
                        ? STYLE_DRAG_OVER
                        : STYLE_DRAG_REJECTED);
            }
            e.consume();
        });

        // ── dragExited ────────────────────────────────────────────────────────
        container.setOnDragExited(e -> {
            container.setStyle(originalStyle);
            e.consume();
        });

        // ── dragDropped ───────────────────────────────────────────────────────
        container.setOnDragDropped(e -> {
            Dragboard db     = e.getDragboard();
            boolean   success = false;

            if (db.hasFiles()) {
                File first = db.getFiles().stream()
                        .filter(FileDropHandler::isExcelFile)
                        .findFirst()
                        .orElse(null);

                if (first != null) {
                    field.setText(first.getAbsolutePath());
                    container.setStyle(originalStyle);
                    if (onFileAccepted != null) onFileAccepted.accept(first);
                    success = true;
                } else {
                    // briefly flash red then restore
                    container.setStyle(STYLE_DRAG_REJECTED);
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(
                                    javafx.util.Duration.millis(800));
                    pause.setOnFinished(ev -> container.setStyle(originalStyle));
                    pause.play();
                }
            }

            e.setDropCompleted(success);
            e.consume();
        });

        // ── also attach to the TextField itself ───────────────────────────────
        field.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles() &&
                    isExcel(e.getDragboard().getFiles()))
                e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });

        field.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File first = db.getFiles().stream()
                        .filter(FileDropHandler::isExcelFile)
                        .findFirst()
                        .orElse(null);
                if (first != null) {
                    field.setText(first.getAbsolutePath());
                    if (onFileAccepted != null) onFileAccepted.accept(first);
                    success = true;
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static boolean isExcel(List<File> files) {
        return files.stream().anyMatch(FileDropHandler::isExcelFile);
    }

    public static boolean isExcelFile(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".xlsx") || name.endsWith(".xls");
    }
}
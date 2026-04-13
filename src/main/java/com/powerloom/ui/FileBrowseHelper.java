package com.powerloom.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.function.Consumer;

public class FileBrowseHelper {

    private static final FileChooser.ExtensionFilter EXCEL_FILTER =
            new FileChooser.ExtensionFilter(
                    "Excel files (*.xlsx, *.xls)", "*.xlsx", "*.xls");

    /**
     * Opens a FileChooser filtered to Excel files.
     * If the user selects a file, sets the path into the field
     * and calls the callback.
     *
     * @param owner          window owner for the dialog
     * @param field          TextField to populate
     * @param initialDir     last used directory (can be null)
     * @param onFileSelected callback with the selected File
     * @return the selected File, or null if cancelled
     */
    public static File browse(Window owner,
                              TextField field,
                              File initialDir,
                              Consumer<File> onFileSelected) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Excel File");
        chooser.getExtensionFilters().add(EXCEL_FILTER);

        // restore last directory
        if (initialDir != null && initialDir.exists()) {
            chooser.setInitialDirectory(
                    initialDir.isDirectory() ? initialDir : initialDir.getParentFile());
        }

        File selected = chooser.showOpenDialog(owner);
        if (selected != null) {
            field.setText(selected.getAbsolutePath());
            if (onFileSelected != null) onFileSelected.accept(selected);
        }
        return selected;
    }

    /**
     * Convenience overload — derives directory from the current field value.
     */
    public static File browse(Window owner,
                              TextField field,
                              Consumer<File> onFileSelected) {
        File currentFile = field.getText() != null && !field.getText().isBlank()
                ? new File(field.getText()) : null;
        return browse(owner, field, currentFile, onFileSelected);
    }
    /**
     * Opens a save dialog. If the user confirms, calls the callback with the
     * chosen file. The directory memory is shared with browse().
     */
    public static void saveDialog(Window owner,
                                  String initialFileName,
                                  String description,
                                  String extension,
                                  Consumer<File> onFileChosen) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File");
        chooser.setInitialFileName(initialFileName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, extension));

        File out = chooser.showSaveDialog(owner);
        if (out != null && onFileChosen != null) {
            onFileChosen.accept(out);
        }
    }
}
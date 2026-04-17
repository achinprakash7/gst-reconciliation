package com.powerloom.ui;

import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.RowDataEntity;
import com.powerloom.service.ExcelExportService;
import com.powerloom.service.ExportPdfService;
import com.powerloom.service.GSTReconciliationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class ReconciliationController implements Initializable {



    // ── FXML — file rows ──────────────────────────────────────────────────────
    @FXML private HBox      topSection;
    @FXML private HBox      b2bDropZone;
    @FXML private HBox      gstDropZone;
    @FXML private Label     b2bDropHint;
    @FXML private Label     gstDropHint;
    @FXML private TextField b2bPathField;
    @FXML private TextField gstPathField;
    @FXML private TextField b2bStartRow;
    @FXML private TextField gstStartRow;

    // ── FXML — search / month / year ──────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> monthBox;
    @FXML private TextField        yearField;

    // ── FXML — filter chips ───────────────────────────────────────────────────
    @FXML private ToggleButton chip1;
    @FXML private ToggleButton chip2;
    @FXML private ToggleButton chip3;
    @FXML private ToggleButton chip4;
    @FXML private ToggleButton chip5;
    @FXML private Button       toggleTopBtn;

    // ── FXML — table ──────────────────────────────────────────────────────────
    @FXML private TableView<ResultRow>           tableView;
    @FXML private TableColumn<ResultRow, String> typeCol;
    @FXML private TableColumn<ResultRow, String> gstinCol;
    @FXML private TableColumn<ResultRow, String> nameCol;
    @FXML private TableColumn<ResultRow, String> invoiceCol;
    @FXML private TableColumn<ResultRow, String> dateCol;
    @FXML private TableColumn<ResultRow, Double> taxableCol;
    @FXML private TableColumn<ResultRow, Double> igstCol;
    @FXML private TableColumn<ResultRow, Double> cgstCol;
    @FXML private TableColumn<ResultRow, Double> sgstCol;
    @FXML private TableColumn<ResultRow, Double> cessCol;

    // ── FXML — summary labels ─────────────────────────────────────────────────
    @FXML private VBox summaryVertical;
    @FXML private HBox summaryHorizontal;
    @FXML private Label totalB2BVal;
    @FXML private Label totalGSTVal;
    @FXML private Label matchVal;
    @FXML private Label mismatchVal;
    @FXML private Label missingGSTVal;
    @FXML private Label missingB2BVal;


    @FXML private Label monthPill;
    @FXML private Label onlinePill;
    @FXML private Label tallyPill;

    @FXML private Label totalB2BValH;
    @FXML private Label totalGSTValH;
    @FXML private Label matchValH;
    @FXML private Label mismatchValH;
    @FXML private Label missingGSTValH;
    @FXML private Label missingB2BValH;

    // ── FXML — progress + dark mode ───────────────────────────────────────────
    @FXML private ProgressBar progressBar;
    @FXML private Label       progressLabel;
    @FXML private Button      darkToggleBtn;

    // ── Spring services ───────────────────────────────────────────────────────
    private final GSTReconciliationService reconciliationService;
    private final ExcelExportService       excelExportService;
    private final ExportPdfService         pdfExportService;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<ReconciliationResult>      cachedResults = new ArrayList<>();
    private final ObservableList<ResultRow> sourceList    =
            FXCollections.observableArrayList();
    private FilteredResultRow               filteredWrapper;
    private final DarkThemeManager          darkTheme     = new DarkThemeManager();
    private int                             activeChip    = 1;
    private File                            lastDirectory = null;

    // ── Chip styles ───────────────────────────────────────────────────────────
    private static final String CHIP_ACTIVE =
            "-fx-background-color:#534AB7;-fx-text-fill:#EEEDFE;" +
                    "-fx-background-radius:20;-fx-border-radius:20;" +
                    "-fx-border-width:0;-fx-padding:5 14 5 14;" +
                    "-fx-font-size:12px;-fx-cursor:hand;";

    private static final String CHIP_INACTIVE =
            "-fx-background-color:#F1EFE8;-fx-text-fill:#888780;" +
                    "-fx-background-radius:20;-fx-border-color:#2C2C2A;" +
                    "-fx-border-width:0.5;-fx-border-radius:20;" +
                    "-fx-padding:5 14 5 14;-fx-font-size:12px;-fx-cursor:hand;";

    // ── File row styles ───────────────────────────────────────────────────────
    private static final String B2B_NORMAL =
            "-fx-background-color:#F4F8FD;-fx-background-radius:8;" +
                    "-fx-border-color:#2C2C2A;-fx-border-width:1;" +
                    "-fx-border-radius:8;-fx-padding:8 10 8 10;";

    private static final String GST_NORMAL =
            "-fx-background-color:#F6F4FE;-fx-background-radius:8;" +
                    "-fx-border-color:#2C2C2A;-fx-border-width:1;" +
                    "-fx-border-radius:8;-fx-padding:8 10 8 10;";

    private static final String B2B_LOADED =
            "-fx-background-color:#EAF3DE;-fx-background-radius:8;" +
                    "-fx-border-color:#639922;-fx-border-width:2;" +
                    "-fx-border-radius:8;-fx-padding:8 10 8 10;";

    private static final String GST_LOADED =
            "-fx-background-color:#E1F5EE;-fx-background-radius:8;" +
                    "-fx-border-color:#1D9E75;-fx-border-width:2;" +
                    "-fx-border-radius:8;-fx-padding:8 10 8 10;";

    private static final String[] MONTHS = {
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
    };

    // ── Constructor ───────────────────────────────────────────────────────────
    public ReconciliationController(GSTReconciliationService reconciliationService,
                                    ExcelExportService excelExportService,
                                    ExportPdfService pdfExportService) {
        this.reconciliationService = reconciliationService;
        this.excelExportService    = excelExportService;
        this.pdfExportService      = pdfExportService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ── filtered list ─────────────────────────────────────────────────────
        filteredWrapper = new FilteredResultRow(sourceList);
        tableView.setItems(filteredWrapper.getFilteredList());
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ── search ────────────────────────────────────────────────────────────
        searchField.textProperty().addListener((o, ov, nv) ->
                filteredWrapper.filter(nv));

        // ── column value factories ────────────────────────────────────────────
        typeCol   .setCellValueFactory(new PropertyValueFactory<>("type"));
        gstinCol  .setCellValueFactory(new PropertyValueFactory<>("gstin"));
        nameCol   .setCellValueFactory(new PropertyValueFactory<>("name"));
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("invoice"));
        dateCol   .setCellValueFactory(new PropertyValueFactory<>("date"));
        taxableCol.setCellValueFactory(cd -> cd.getValue().taxableProperty());
        igstCol   .setCellValueFactory(cd -> cd.getValue().igstProperty());
        cgstCol   .setCellValueFactory(cd -> cd.getValue().cgstProperty());
        sgstCol   .setCellValueFactory(cd -> cd.getValue().sgstProperty());
        cessCol   .setCellValueFactory(cd -> cd.getValue().cessProperty());

        // ── cell factories ────────────────────────────────────────────────────
        typeCol   .setCellFactory(col -> styledStringCell(true));
        gstinCol  .setCellFactory(col -> styledStringCell(false));
        nameCol   .setCellFactory(col -> styledStringCell(false));
        invoiceCol.setCellFactory(col -> styledStringCell(false));
        dateCol   .setCellFactory(col -> styledStringCell(false));

        for (TableColumn<ResultRow, Double> col :
                List.of(taxableCol, igstCol, cgstCol, sgstCol, cessCol)) {
            col.setCellFactory(c -> styledDoubleCell());
        }

        // ── row factory ───────────────────────────────────────────────────────
        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ResultRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null
                        || item.getType() == null
                        || item.getType().isBlank()) {
                    setStyle("-fx-background-color:#E8E8E8;" +
                            "-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:0 0 1 0;" +
                            "-fx-pref-height:5;");
                    return;
                }
                String t = item.getType();
                if      (t.startsWith("ONLINE"))
                    setStyle("-fx-background-color:#F4F8FD;" +
                            "-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:2 0 0 0;");
                else if (t.startsWith("TALLY"))
                    setStyle("-fx-background-color:#F6F4FE;" +
                            "-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:0 0 0 0;");
                else if ("DIFF".equals(t))
                    setStyle(item.hasDiff()
                            ? "-fx-background-color:#FF9696;" +
                            "-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:0.5 0 2 0;" +
                            "-fx-font-weight:bold;"
                            : "-fx-background-color:#C8F0C8;" +
                            "-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:0.5 0 2 0;" +
                            "-fx-font-weight:bold;");
                else if (t.startsWith("TOTAL"))
                    setStyle("-fx-background-color:#FFFBF4;" +
                            "-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:2 0 2 0;" +
                            "-fx-font-weight:bold;");
                else
                    setStyle("-fx-border-color:#2C2C2A;" +
                            "-fx-border-width:0 0 0.5 0;");
            }
        });

        // ── month / year defaults ─────────────────────────────────────────────
        monthBox.getItems().addAll(MONTHS);
        LocalDate prev = LocalDate.now().minusMonths(1);
        monthBox.getSelectionModel().select(prev.getMonthValue() - 1);
        yearField.setText(String.valueOf(prev.getYear()));

        monthBox.getSelectionModel().selectedIndexProperty()
                .addListener((o, ov, nv) -> { updatePills(); renderTable(); });
        yearField.textProperty()
                .addListener((o, ov, nv) -> { updatePills(); renderTable(); });

        // ── keyboard shortcuts ────────────────────────────────────────────────
        tableView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null)
                KeyboardShortcutManager.register(
                        newScene,
                        this::onCompare,
                        this::onExportExcel,
                        this::onReset,
                        this::onBrowseB2B,
                        this::onBrowseGST);
        });

        // ── progress hidden ───────────────────────────────────────────────────
        progressBar.setVisible(false);
        progressLabel.setVisible(false);

        // ── chips + pills + file handlers ─────────────────────────────────────
        setActiveChip(1);
        updatePills();
        initFileHandlers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILE HANDLERS — drag & drop + browse
    // ─────────────────────────────────────────────────────────────────────────

    private void initFileHandlers() {

        // ── B2B drag & drop ───────────────────────────────────────────────────
        FileDropHandler.attach(b2bDropZone, b2bPathField, B2B_NORMAL, file -> {
            lastDirectory = file.getParentFile();
            updateDropHint(b2bDropHint, file.getName());
            b2bDropZone.setStyle(B2B_LOADED);
            toast("B2B file loaded: " + file.getName(),
                    ToastNotification.Type.SUCCESS);
        });

        // ── GST drag & drop ───────────────────────────────────────────────────
        FileDropHandler.attach(gstDropZone, gstPathField, GST_NORMAL, file -> {
            lastDirectory = file.getParentFile();
            updateDropHint(gstDropHint, file.getName());
            gstDropZone.setStyle(GST_LOADED);
            toast("GST file loaded: " + file.getName(),
                    ToastNotification.Type.SUCCESS);
        });

        // ── text field listeners (manual path entry) ──────────────────────────
        b2bPathField.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.isBlank())
                updateDropHint(b2bDropHint, new File(nv).getName());
            else
                resetDropHint(b2bDropHint);
        });

        gstPathField.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.isBlank())
                updateDropHint(gstDropHint, new File(nv).getName());
            else
                resetDropHint(gstDropHint);
        });
    }

    @FXML
    private void onBrowseB2B() {
        FileBrowseHelper.browse(getStage(), b2bPathField, lastDirectory, file -> {
            lastDirectory = file.getParentFile();
            updateDropHint(b2bDropHint, file.getName());
            b2bDropZone.setStyle(B2B_LOADED);
            toast("B2B file loaded: " + file.getName(),
                    ToastNotification.Type.SUCCESS);
        });
    }

    @FXML
    private void onBrowseGST() {
        FileBrowseHelper.browse(getStage(), gstPathField, lastDirectory, file -> {
            lastDirectory = file.getParentFile();
            updateDropHint(gstDropHint, file.getName());
            gstDropZone.setStyle(GST_LOADED);
            toast("GST file loaded: " + file.getName(),
                    ToastNotification.Type.SUCCESS);
        });
    }

    // ── drop hint helpers ─────────────────────────────────────────────────────

    private void updateDropHint(Label hint, String fileName) {
        hint.setText(fileName);
        hint.setStyle("-fx-text-fill:#27500A;-fx-font-size:11px;" +
                "-fx-font-weight:bold;-fx-font-style:normal;");
    }

    private void resetDropHint(Label hint) {
        hint.setText("Drop .xlsx here or browse");
        hint.setStyle("-fx-text-fill:#B4B2A9;-fx-font-size:11px;" +
                "-fx-font-style:italic;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CELL FACTORIES
    // ─────────────────────────────────────────────────────────────────────────

    private TableCell<ResultRow, String> styledStringCell(boolean isBadge) {
        return new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(null);
                applyBorderStyle(this);
                if (empty || value == null || value.isBlank()) return;
                if (isBadge) {
                    badge.setText(value);
                    badge.setStyle(badgeStyle(value));
                    setGraphic(badge);
                } else {
                    setText(value);
                }
            }
        };
    }

    private TableCell<ResultRow, Double> styledDoubleCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                applyBorderStyle(this);
                if (empty || value == null) { setText(null); return; }
                setText(String.format("%.2f", value));
                String colour;
                if      (value < 0) colour = "-fx-text-fill:#A32D2D;-fx-font-weight:bold;";
                else if (value > 0) colour = "-fx-text-fill:#3B6D11;";
                else                colour = "-fx-text-fill:#888780;";
                setStyle(getStyle() + colour);
            }
        };
    }

    private void applyBorderStyle(TableCell<?, ?> cell) {
        TableRow<?> row = cell.getTableRow();
        if (row == null || !(row.getItem() instanceof ResultRow r)
                || r.getType() == null || r.getType().isBlank()) {
            cell.setStyle(""); return;
        }
        String border =
                "-fx-border-color:transparent #2C2C2A transparent transparent;" +
                        "-fx-border-width:0 1 0 0;";
        String t = r.getType();
        if      (t.startsWith("TOTAL"))
            cell.setStyle(border + "-fx-font-weight:bold;");
        else if ("DIFF".equals(t))
            cell.setStyle(border + "-fx-font-weight:bold;");
        else
            cell.setStyle(border);
    }

    private String badgeStyle(String value) {
        String colour;
        if      (value.startsWith("ONLINE"))     colour = "#E6F1FB;-fx-text-fill:#185FA5;";
        else if (value.startsWith("TALLY"))      colour = "#EEEDFE;-fx-text-fill:#3C3489;";
        else if ("DIFF".equals(value))           colour = "#FCEBEB;-fx-text-fill:#791F1F;";
        else if (value.startsWith("TOTAL DIFF")) colour = "#FCEBEB;-fx-text-fill:#791F1F;";
        else                                     colour = "#FAEEDA;-fx-text-fill:#633806;";
        return "-fx-background-color:" + colour +
                "-fx-background-radius:10;-fx-font-size:10px;" +
                "-fx-font-weight:bold;-fx-padding:2 7 2 7;";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHIP HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void onChip1() { setActiveChip(1); renderTable(); }
    @FXML private void onChip2() { setActiveChip(2); renderTable(); }
    @FXML private void onChip3() { setActiveChip(3); renderTable(); }
    @FXML private void onChip4() { setActiveChip(4); renderTable(); }
    @FXML private void onChip5() { setActiveChip(5); renderTable(); }

    @FXML
    private void onToggleTopSection() {
        boolean isVisible = topSection.isVisible();

        // Toggle top section
        topSection.setVisible(!isVisible);
        topSection.setManaged(!isVisible);

        // Toggle summary layout
        summaryVertical.setVisible(!isVisible);
        summaryVertical.setManaged(!isVisible);

        summaryHorizontal.setVisible(isVisible);
        summaryHorizontal.setManaged(isVisible);

        toggleTopBtn.setText(isVisible ? "Show Option" : "Hide Option");

        syncHorizontalSummary();
    }

    private void syncHorizontalSummary() {
        totalB2BValH.setText(totalB2BVal.getText());
        totalGSTValH.setText(totalGSTVal.getText());
        matchValH.setText(matchVal.getText());
        mismatchValH.setText(mismatchVal.getText());
        missingGSTValH.setText(missingGSTVal.getText());
        missingB2BValH.setText(missingB2BVal.getText());
    }

    private void setActiveChip(int n) {
        activeChip = n;
        chip1.setStyle(CHIP_INACTIVE);
        chip2.setStyle(CHIP_INACTIVE);
        chip3.setStyle(CHIP_INACTIVE);
        chip4.setStyle(CHIP_INACTIVE);
        chip5.setStyle(CHIP_INACTIVE);
        getChip(n).setStyle(CHIP_ACTIVE);
        updateChipLabels();
    }

    private ToggleButton getChip(int n) {
        return switch (n) {
            case 2  -> chip2; case 3 -> chip3;
            case 4  -> chip4; case 5 -> chip5;
            default -> chip1;
        };
    }

    private void updateChipLabels() {
        if (monthBox.getValue() == null) return;
        String label = monthBox.getValue() + " " + yearField.getText();
        chip2.setText("Online – Prev extra (before " + label + ")");
        chip3.setText("Online – Recon month (" + label + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML ACTIONS
    // ─────────────────────────────────────────────────────────────────────────



    @FXML
    private void onCompare() {
        if (b2bPathField.getText().isBlank() || gstPathField.getText().isBlank()) {
            toast("Please select both files before comparing.",
                    ToastNotification.Type.WARNING);
            return;
        }

        File b2bFile = new File(b2bPathField.getText().trim());
        File gstFile = new File(gstPathField.getText().trim());
        int  b2bStart, gstStart;

        try {
            b2bStart = Integer.parseInt(b2bStartRow.getText().trim());
            gstStart = Integer.parseInt(gstStartRow.getText().trim());
        } catch (NumberFormatException e) {
            toast("Start row must be a valid number.",
                    ToastNotification.Type.ERROR);
            return;
        }

        Task<List<ReconciliationResult>> task = new Task<>() {
            @Override
            protected List<ReconciliationResult> call() throws Exception {
                updateMessage("Reading files…");
                updateProgress(0.2, 1.0);
                List<ReconciliationResult> result =
                        reconciliationService.compare(
                                b2bFile, gstFile, b2bStart, gstStart);
                updateProgress(1.0, 1.0);
                return result;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        progressBar.setVisible(true);
        progressLabel.setVisible(true);

        task.setOnSucceeded(e -> {
            cachedResults = task.getValue();
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            monthBox.setDisable(true);
            yearField.setEditable(false);
            renderTable();
            updateSummary();
            updatePills();

            topSection.setVisible(false);
            topSection.setManaged(false);
            toggleTopBtn.setText("Show Option");

            summaryHorizontal.setVisible(true);
            summaryHorizontal.setManaged(true);

            syncHorizontalSummary();

            toast("Compare complete — " + cachedResults.size() + " records.",
                    ToastNotification.Type.SUCCESS);
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            Throwable ex = task.getException();
            toast("Compare failed: " +
                            (ex != null ? ex.getMessage() : "unknown error"),
                    ToastNotification.Type.ERROR);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onReset() {
        cachedResults.clear();
        sourceList.clear();
        searchField.clear();

        b2bPathField.clear();
        gstPathField.clear();
        resetDropHint(b2bDropHint);
        resetDropHint(gstDropHint);
        b2bDropZone.setStyle(B2B_NORMAL);
        gstDropZone.setStyle(GST_NORMAL);

        monthBox.setDisable(false);
        yearField.setEditable(true);
        resetSummary();
        updatePills();
        toast("Reset complete.", ToastNotification.Type.INFO);
    }

    @FXML
    private void onExportExcel() {
        List<ReconciliationResult> filtered = getFilteredResults();
        if (filtered.isEmpty()) {
            toast("Nothing to export — run Compare first.",
                    ToastNotification.Type.WARNING);
            return;
        }
        FileBrowseHelper.saveDialog(getStage(), buildFileName("xlsx"),
                "Excel files", "*.xlsx", file -> {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        excelExportService.export(filtered, activeMode(), fos);
                        toast("Excel saved: " + file.getName(),
                                ToastNotification.Type.SUCCESS);
                    } catch (Exception ex) {
                        toast("Export failed: " + ex.getMessage(),
                                ToastNotification.Type.ERROR);
                    }
                });
    }

    @FXML
    private void onExportPdf() {
        List<ReconciliationResult> filtered = getFilteredResults();
        if (filtered.isEmpty()) {
            toast("Nothing to export — run Compare first.",
                    ToastNotification.Type.WARNING);
            return;
        }
        FileBrowseHelper.saveDialog(getStage(), buildFileName("pdf"),
                "PDF files", "*.pdf", file -> {
                    try {
                        pdfExportService.export(filtered, activeMode(), file);
                        toast("PDF saved: " + file.getName(),
                                ToastNotification.Type.SUCCESS);
                    } catch (Exception ex) {
                        toast("PDF failed: " + ex.getMessage(),
                                ToastNotification.Type.ERROR);
                    }
                });
    }

    @FXML
    private void onToggleDark() {
        darkTheme.toggle(tableView.getScene());
        darkToggleBtn.setText(darkTheme.isDark() ? "Light mode" : "Dark mode");
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER TABLE
    // ─────────────────────────────────────────────────────────────────────────

    private void renderTable() {
        sourceList.clear();
        if (cachedResults.isEmpty()) return;

        int month = monthBox.getSelectionModel().getSelectedIndex() + 1;
        int year  = parseYear();
        List<ReconciliationResult> filtered = new ArrayList<>();

        for (ReconciliationResult r : cachedResults) {
            if (!include(r, month, year)) continue;
            filtered.add(r);
            if (isMatchView()) {
                addRowToTable("ONLINE", r.getB2bRow());
                addRowToTable("TALLY",  r.getGstRow());
                addDiffToTable(r.getB2bRow(), r.getGstRow());
                sourceList.add(new ResultRow());
            } else {
                RowDataEntity d = r.getB2bRow() != null
                        ? r.getB2bRow() : r.getGstRow();
                addRowToTable(r.getStatus(), d);
                sortRowDateWise();
            }
        }

        if (!filtered.isEmpty()) {
            if (isMatchView()) addMatchMismatchTotals(filtered);
            else               addSimpleTotal(filtered);
        }
    }

    private void sortRowDateWise(){
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        FXCollections.sort(sourceList, (r1, r2) -> {
            try {
                LocalDate d1 = LocalDate.parse(r1.getDate(), fmt);
                LocalDate d2 = LocalDate.parse(r2.getDate(), fmt);
                return d1.compareTo(d2);
            } catch (Exception e) {
                return 0; // fallback (keeps original order)
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROW BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    private void addRowToTable(String type, RowDataEntity d) {
        if (d == null) return;
        sourceList.add(new ResultRow(
                type + " [ " + d.getRowNo() + " ]",
                nvl(d.getGstin()),
                nvl(d.getTradeOrLegalName()),
                nvl(d.getInvoiceNo()),
                d.getDate() != null ? d.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "",
                d.getTaxable(), d.getIgst(),
                d.getCgst(),    d.getSgst(), d.getCess()
        ));
    }

    private void addDiffToTable(RowDataEntity b, RowDataEntity g) {
        if (b == null || g == null) return;
        sourceList.add(new ResultRow(
                "DIFF", "", "", "",
                b.getDate().isEqual(g.getDate()) ? "" : "NOT_MATCH",
                round(b.getTaxable() - g.getTaxable()),
                round(b.getIgst()    - g.getIgst()),
                round(b.getCgst()    - g.getCgst()),
                round(b.getSgst()    - g.getSgst()),
                round(b.getCess()    - g.getCess())
        ));
    }

    private void addSimpleTotal(List<ReconciliationResult> f) {
        double t=0,ig=0,cg=0,sg=0,ce=0;
        for (ReconciliationResult r : f) {
            RowDataEntity d = r.getB2bRow()!=null?r.getB2bRow():r.getGstRow();
            if (d==null) continue;
            t+=d.getTaxable(); ig+=d.getIgst();
            cg+=d.getCgst();   sg+=d.getSgst(); ce+=d.getCess();
        }
        sourceList.add(new ResultRow(
                "TOTAL","COUNT "+f.size(),"","","",
                round(t),round(ig),round(cg),round(sg),round(ce)));
    }

    private void addMatchMismatchTotals(List<ReconciliationResult> f) {
        double bt=0,bi=0,bc=0,bs=0,be=0;
        double gt=0,gi=0,gc=0,gs=0,ge=0;
        for (ReconciliationResult r : f) {
            RowDataEntity b=r.getB2bRow(), g=r.getGstRow();
            if(b!=null){bt+=b.getTaxable();bi+=b.getIgst();bc+=b.getCgst();bs+=b.getSgst();be+=b.getCess();}
            if(g!=null){gt+=g.getTaxable();gi+=g.getIgst();gc+=g.getCgst();gs+=g.getSgst();ge+=g.getCess();}
        }
        sourceList.add(new ResultRow(
                "TOTAL ONLINE","COUNT "+f.size(),"","","",
                round(bt),round(bi),round(bc),round(bs),round(be)));
        sourceList.add(new ResultRow(
                "TOTAL TALLY","COUNT "+f.size(),"","","",
                round(gt),round(gi),round(gc),round(gs),round(ge)));
        sourceList.add(new ResultRow(
                "TOTAL DIFF","","","","",
                round(bt-gt),round(bi-gi),
                round(bc-gc),round(bs-gs),round(be-ge)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER
    // ─────────────────────────────────────────────────────────────────────────

    private boolean include(ReconciliationResult r, int month, int year) {
        String    s = r.getStatus();
        LocalDate d = effectiveDate(r);
        return switch (activeChip) {
            case 1  -> "MATCH".equals(s) || "MISMATCH".equals(s);
            case 2  -> "MISSING_IN_TALLY".equals(s)
                    && (d==null||d.getYear()!=year||d.getMonthValue()!=month);
            case 3  -> "MISSING_IN_TALLY".equals(s)
                    && d!=null&&d.getYear()==year&&d.getMonthValue()==month;
            case 4  -> "MISSING_IN_ONLINE".equals(s);
            case 5  -> "MISMATCH".equals(s);
            default -> false;
        };
    }

    private List<ReconciliationResult> getFilteredResults() {
        int month = monthBox.getSelectionModel().getSelectedIndex() + 1;
        int year  = parseYear();
        List<ReconciliationResult> out = new ArrayList<>();
        for (ReconciliationResult r : cachedResults)
            if (include(r, month, year)) out.add(r);
        return out;
    }

    private boolean isMatchView() {
        return activeChip == 1 || activeChip == 5;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY & PILLS
    // ─────────────────────────────────────────────────────────────────────────

    private void updateSummary() {
        int b2b=0,gst=0,match=0,mismatch=0,missingGST=0,missingB2B=0;
        for (ReconciliationResult r : cachedResults) {
            if (r.getB2bRow()!=null) b2b++;
            if (r.getGstRow()!=null) gst++;
            switch (nvl(r.getStatus())) {
                case "MATCH"             -> match++;
                case "MISMATCH"          -> mismatch++;
                case "MISSING_IN_TALLY"  -> missingGST++;
                case "MISSING_IN_ONLINE" -> missingB2B++;
            }
        }
        totalB2BVal  .setText(String.valueOf(b2b));
        totalGSTVal  .setText(String.valueOf(gst));
        matchVal     .setText(String.valueOf(match));
        mismatchVal  .setText(String.valueOf(mismatch));
        missingGSTVal.setText(String.valueOf(missingGST));
        missingB2BVal.setText(String.valueOf(missingB2B));
        onlinePill.setText(b2b + " online");
        tallyPill .setText(gst + " tally");
    }

    private void resetSummary() {
        totalB2BVal.setText("0");   totalGSTVal.setText("0");
        matchVal.setText("0");      mismatchVal.setText("0");
        missingGSTVal.setText("0"); missingB2BVal.setText("0");
        onlinePill.setText("0 online");
        tallyPill .setText("0 tally");
    }

    private void updatePills() {
        if (monthBox.getValue() == null) return;
        monthPill.setText(monthBox.getValue() + " " + yearField.getText());
        updateChipLabels();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private String activeMode() {
        return switch (activeChip) {
            case 2  -> "ONLINE_PREV";
            case 3  -> "ONLINE_CURRENT";
            case 4  -> "TALLY_EXTRA";
            case 5  -> "ONLY_MISMATCH";
            default -> "MATCH_MISMATCH";
        };
    }

    private String buildFileName(String ext) {
        return "GST_Reco_"
                + monthBox.getValue() + "_"
                + yearField.getText() + "_"
                + activeMode() + "." + ext;
    }

    private LocalDate effectiveDate(ReconciliationResult r) {
        if (r.getB2bRow()!=null && r.getB2bRow().getDate()!=null)
            return r.getB2bRow().getDate();
        if (r.getGstRow()!=null) return r.getGstRow().getDate();
        return null;
    }

    private Stage getStage() {
        return (Stage) tableView.getScene().getWindow();
    }

    private void toast(String msg, ToastNotification.Type type) {
        ToastNotification.show(getStage(), msg, type);
    }

    private int parseYear() {
        try { return Integer.parseInt(yearField.getText().trim()); }
        catch (NumberFormatException e) { return LocalDate.now().getYear(); }
    }

    private double round(double v)      { return Math.round(v * 100.0) / 100.0; }
    //private Double nullIfZero(double v) { return v == 0.0 ? null : v; }
    private String nvl(String s)        { return s == null ? "" : s; }
}
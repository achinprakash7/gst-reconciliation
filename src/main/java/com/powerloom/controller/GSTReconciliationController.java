package com.powerloom.controller;

import com.powerloom.dto.ReconDisplayRow;
import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.RowDataEntity;
import com.powerloom.service.GSTReconciliationService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@FxmlView("/fxml/reconciliation.fxml")
public class GSTReconciliationController {

    // ================= INPUT =================
    @FXML private TextField b2bFileField;
    @FXML private TextField gstFileField;
    @FXML private TextField b2bStartRowField;
    @FXML private TextField gstStartRowField;
    @FXML private ComboBox<String> monthBox;
    @FXML private TextField yearField;

    // ================= SUMMARY =================
    @FXML private Label totalB2BLabel;
    @FXML private Label totalGSTLabel;
    @FXML private Label matchedLabel;
    @FXML private Label mismatchLabel;
    @FXML private Label missingTallyLabel;
    @FXML private Label missingOnlineLabel;

    // ================= TABLE =================
    @FXML private TableView<ReconDisplayRow> resultTable;

    @FXML private TableColumn<ReconDisplayRow, String> colType;
    @FXML private TableColumn<ReconDisplayRow, String> colGstin;
    @FXML private TableColumn<ReconDisplayRow, String> colName;
    @FXML private TableColumn<ReconDisplayRow, String> colInvoice;
    @FXML private TableColumn<ReconDisplayRow, String> colDate;
    @FXML private TableColumn<ReconDisplayRow, Double> colTaxable;
    @FXML private TableColumn<ReconDisplayRow, Double> colIgst;
    @FXML private TableColumn<ReconDisplayRow, Double> colCgst;
    @FXML private TableColumn<ReconDisplayRow, Double> colSgst;
    @FXML private TableColumn<ReconDisplayRow, Double> colCess;

    @Autowired
    private GSTReconciliationService service;

    private File b2bFile;
    private File gstFile;

    // ================= INIT =================
    @FXML
    public void initialize() {

        monthBox.setItems(FXCollections.observableArrayList(
                "JAN","FEB","MAR","APR","MAY","JUN",
                "JUL","AUG","SEP","OCT","NOV","DEC"
        ));

        // ===== COLUMN BINDING =====
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colGstin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGstin()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colInvoice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInvoice()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate()));

        colTaxable.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTaxable()));
        colIgst.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getIgst()));
        colCgst.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCgst()));
        colSgst.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSgst()));
        colCess.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCess()));

        // ===== ROW COLOR =====
        resultTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ReconDisplayRow item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                    return;
                }

                if ("DIFF".equals(item.getType())) {
                    setStyle("-fx-background-color: #f8d7da; -fx-font-weight: bold;");
                } else if (item.getType() != null && item.getType().startsWith("ONLINE")) {
                    setStyle("-fx-background-color: #f1f1f1;");
                } else if (item.getType() != null && item.getType().startsWith("TALLY")) {
                    setStyle("-fx-background-color: #e9ecef;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    // ================= BUILD DISPLAY =================
    private List<ReconDisplayRow> buildDisplay(List<ReconciliationResult> results) {

        List<ReconDisplayRow> list = new ArrayList<>();

        for (ReconciliationResult r : results) {

            RowDataEntity b = r.getB2bRow();
            RowDataEntity g = r.getGstRow();

            // ===== ONLINE =====
            if (b != null) {
                ReconDisplayRow online = new ReconDisplayRow();

                online.setType("ONLINE [" + b.getRowNo() + "]");
                online.setGstin(b.getGstin());
                online.setName(b.getTradeOrLegalName());
                online.setInvoice(b.getInvoiceNo());
                online.setDate(String.valueOf(b.getDate()));

                online.setTaxable(b.getTaxable());
                online.setIgst(b.getIgst());
                online.setCgst(b.getCgst());
                online.setSgst(b.getSgst());
                online.setCess(b.getCess());

                list.add(online);
            }

            // ===== TALLY =====
            if (g != null) {
                ReconDisplayRow tally = new ReconDisplayRow();

                tally.setType("TALLY [" + g.getRowNo() + "]");
                tally.setGstin(g.getGstin());
                tally.setName(g.getTradeOrLegalName());
                tally.setInvoice(g.getInvoiceNo());
                tally.setDate(String.valueOf(g.getDate()));

                tally.setTaxable(g.getTaxable());
                tally.setIgst(g.getIgst());
                tally.setCgst(g.getCgst());
                tally.setSgst(g.getSgst());
                tally.setCess(g.getCess());

                list.add(tally);
            }

            // ===== DIFF =====
            if (b != null && g != null) {
                ReconDisplayRow diff = new ReconDisplayRow();

                diff.setType("DIFF");
                diff.setTaxable(b.getTaxable() - g.getTaxable());
                diff.setIgst(b.getIgst() - g.getIgst());
                diff.setCgst(b.getCgst() - g.getCgst());
                diff.setSgst(b.getSgst() - g.getSgst());
                diff.setCess(b.getCess() - g.getCess());

                list.add(diff);
            }

            list.add(new ReconDisplayRow()); // gap
        }

        return list;
    }

    // ================= ACTIONS =================

    @FXML
    private void onBrowseB2B() {
        FileChooser chooser = new FileChooser();
        b2bFile = chooser.showOpenDialog(null);
        if (b2bFile != null) {
            b2bFileField.setText(b2bFile.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseGST() {
        FileChooser chooser = new FileChooser();
        gstFile = chooser.showOpenDialog(null);
        if (gstFile != null) {
            gstFileField.setText(gstFile.getAbsolutePath());
        }
    }

    @FXML
    private void onCompare() {
        try {
            List<ReconciliationResult> results = service.compare(
                    b2bFile,
                    gstFile,
                    Integer.parseInt(b2bStartRowField.getText()),
                    Integer.parseInt(gstStartRowField.getText()),
                    monthBox.getValue(),
                    monthBox.getSelectionModel().getSelectedIndex() + 1,
                    Integer.parseInt(yearField.getText())
            );

            List<ReconDisplayRow> display = buildDisplay(results);
            resultTable.setItems(FXCollections.observableArrayList(display));

            updateSummary(results);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSummary(List<ReconciliationResult> results) {

        long match = results.stream().filter(r -> "MATCH".equals(r.getStatus())).count();
        long mismatch = results.stream().filter(r -> "MISMATCH".equals(r.getStatus())).count();
        long missingTally = results.stream().filter(r -> "MISSING_IN_TALLY".equals(r.getStatus())).count();
        long missingOnline = results.stream().filter(r -> "MISSING_IN_ONLINE".equals(r.getStatus())).count();

        totalB2BLabel.setText("ONLINE ENTRY: " + results.size());
        totalGSTLabel.setText("TALLY ENTRY: " + results.size());
        matchedLabel.setText("MATCHED ENTRY: " + match);
        mismatchLabel.setText("MISMATCH ENTRY: " + mismatch);
        missingTallyLabel.setText("MISSING TALLY ENTRY: " + missingTally);
        missingOnlineLabel.setText("MISSING ONLINE ENTRY: " + missingOnline);
    }

    @FXML
    private void onReset() {
        resultTable.getItems().clear();
    }

    @FXML
    private void onExportExcel() {
        System.out.println("Export coming next...");
    }

    @FXML
    private void onFilterChanged() {
        System.out.println("Filter changed...");
    }
}
package com.powerloom.service;

import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.RowDataEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param data   filtered list of results to export
     * @param mode   "MATCH_MISMATCH" | "ONLY_MISMATCH" | "ONLINE_PREV" |
     *               "ONLINE_CURRENT" | "TALLY_EXTRA"
     * @param out    output stream (FileOutputStream from controller)
     */
    public void export(List<ReconciliationResult> data,
                       String mode,
                       OutputStream out) throws Exception {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("GST Reconciliation");

            // ── styles ──
            CellStyle headerStyle = boldStyle(wb, IndexedColors.WHITE);
            CellStyle greyStyle   = colorStyle(wb, IndexedColors.GREY_25_PERCENT, false);
            CellStyle redStyle    = colorStyle(wb, IndexedColors.ROSE,            false);
            CellStyle totalStyle  = colorStyle(wb, IndexedColors.PALE_BLUE,       true);

            // header row background
            CellStyle headerBg = wb.createCellStyle();
            headerBg.cloneStyleFrom(headerStyle);
            headerBg.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerBg.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── header ──
            String[] cols = {"Type", "GSTIN", "Name", "Invoice", "Date",
                    "Taxable", "IGST", "CGST", "SGST", "CESS"};
            int rowNum = 0;
            Row header = sheet.createRow(rowNum++);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerBg);
            }

            boolean isMatchView = "MATCH_MISMATCH".equals(mode) || "ONLY_MISMATCH".equals(mode);

            // ── data rows ──
            for (ReconciliationResult r : data) {

                if (isMatchView) {
                    RowDataEntity b = r.getB2bRow();
                    RowDataEntity g = r.getGstRow();

                    if (b != null)
                        addRow(sheet, rowNum++,
                                "ONLINE [" + b.getRowNo() + "]", b, greyStyle);
                    if (g != null)
                        addRow(sheet, rowNum++,
                                "TALLY  [" + g.getRowNo() + "]", g, greyStyle);

                    rowNum = addDiffRow(sheet, rowNum, b, g, redStyle);
                    rowNum++; // blank spacer

                } else {
                    RowDataEntity d = r.getB2bRow() != null ? r.getB2bRow() : r.getGstRow();
                    addRow(sheet, rowNum++, r.getStatus(), d, null);
                }
            }

            // ── totals ──
            if (!data.isEmpty()) {
                rowNum++;
                if (isMatchView)
                    addMatchMismatchTotals(sheet, rowNum, data, totalStyle);
                else
                    addSimpleTotal(sheet, rowNum, data, totalStyle);
            }

            // ── auto-size ──
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — ROW WRITERS
    // ─────────────────────────────────────────────────────────────────────────

    private void addRow(Sheet sheet, int rowNum, String type,
                        RowDataEntity d, CellStyle style) {
        if (d == null) return;
        Row row = sheet.createRow(rowNum);
        Object[] vals = {
                type,
                d.getGstin(),
                d.getTradeOrLegalName(),
                d.getInvoiceNo(),
                d.getDate() != null ? d.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "",
                d.getTaxable(),
                d.getIgst(),
                d.getCgst(),
                d.getSgst(),
                d.getCess()
        };
        for (int i = 0; i < vals.length; i++) {
            Cell c = row.createCell(i);
            if (vals[i] instanceof Number)
                c.setCellValue(((Number) vals[i]).doubleValue());
            else
                c.setCellValue(vals[i] != null ? vals[i].toString() : "");
            if (style != null) c.setCellStyle(style);
        }
    }

    private int addDiffRow(Sheet sheet, int rowNum,
                           RowDataEntity b, RowDataEntity g, CellStyle redStyle) {
        if (b == null || g == null) return rowNum;

        double[] diffs = {
                round(b.getTaxable() - g.getTaxable()),
                round(b.getIgst()    - g.getIgst()),
                round(b.getCgst()    - g.getCgst()),
                round(b.getSgst()    - g.getSgst()),
                round(b.getCess()    - g.getCess())
        };

        boolean hasDiff = false;
        for (double d : diffs) if (d != 0) { hasDiff = true; break; }

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue("DIFF");
        for (int i = 0; i < diffs.length; i++) {
            Cell c = row.createCell(i + 5);
            c.setCellValue(diffs[i]);
            if (hasDiff) c.setCellStyle(redStyle);
        }
        return rowNum;
    }

    private void addSimpleTotal(Sheet sheet, int rowNum,
                                List<ReconciliationResult> data,
                                CellStyle style) {
        double t = 0, ig = 0, cg = 0, sg = 0, ce = 0;
        for (ReconciliationResult r : data) {
            RowDataEntity d = r.getB2bRow() != null ? r.getB2bRow() : r.getGstRow();
            if (d == null) continue;
            t += d.getTaxable(); ig += d.getIgst();
            cg += d.getCgst();   sg += d.getSgst(); ce += d.getCess();
        }
        writeTotalRow(sheet, rowNum, "TOTAL",
                "COUNT " + data.size(), t, ig, cg, sg, ce, style);
    }

    private void addMatchMismatchTotals(Sheet sheet, int rowNum,
                                        List<ReconciliationResult> data,
                                        CellStyle style) {
        double bt = 0, bi = 0, bc = 0, bs = 0, be = 0;
        double gt = 0, gi = 0, gc = 0, gs = 0, ge = 0;

        for (ReconciliationResult r : data) {
            RowDataEntity b = r.getB2bRow();
            RowDataEntity g = r.getGstRow();
            if (b != null) {
                bt += b.getTaxable(); bi += b.getIgst();
                bc += b.getCgst();   bs += b.getSgst(); be += b.getCess();
            }
            if (g != null) {
                gt += g.getTaxable(); gi += g.getIgst();
                gc += g.getCgst();   gs += g.getSgst(); ge += g.getCess();
            }
        }

        rowNum = writeTotalRow(sheet, rowNum, "TOTAL ONLINE",
                "COUNT " + data.size(), bt, bi, bc, bs, be, style);
        rowNum = writeTotalRow(sheet, rowNum, "TOTAL TALLY",
                "COUNT " + data.size(), gt, gi, gc, gs, ge, style);
        writeTotalRow(sheet, rowNum, "TOTAL DIFF", "",
                bt - gt, bi - gi, bc - gc, bs - gs, be - ge, style);
    }

    private int writeTotalRow(Sheet sheet, int rowNum, String label,
                              String countLabel,
                              double t, double ig, double cg, double sg, double ce,
                              CellStyle style) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(countLabel);
        double[] vals = {t, ig, cg, sg, ce};
        for (int i = 0; i < vals.length; i++) {
            Cell c = row.createCell(i + 5);
            c.setCellValue(round(vals[i]));
            if (style != null) c.setCellStyle(style);
        }
        return rowNum;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — STYLE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private CellStyle boldStyle(Workbook wb, IndexedColors color) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle s = wb.createCellStyle();
        s.setFont(font);
        return s;
    }

    private CellStyle colorStyle(Workbook wb, IndexedColors color, boolean bold) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (bold) {
            Font f = wb.createFont();
            f.setBold(true);
            s.setFont(f);
        }
        return s;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
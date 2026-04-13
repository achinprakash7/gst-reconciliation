package com.powerloom.service;

import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.RowDataEntity;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class ExportPdfService {

    // ── page layout ───────────────────────────────────────────────────────────
    private static final PDRectangle PAGE_SIZE   = PDRectangle.A3;   // wider = all 10 cols fit
    private static final float       PAGE_W      = PAGE_SIZE.getWidth();
    private static final float       PAGE_H      = PAGE_SIZE.getHeight();
    private static final float       MARGIN      = 28f;
    private static final float       TABLE_W     = PAGE_W - 2 * MARGIN;
    private static final float       ROW_H       = 16f;
    private static final float       HEADER_H    = 20f;
    private static final float       TITLE_H     = 30f;
    private static final float       CELL_PAD    = 4f;

    // ── column definitions ────────────────────────────────────────────────────
    // 10 columns — widths must sum to TABLE_W
    // A3 landscape TABLE_W ≈ 1134 - 56 = 1134px at 72dpi → use A3 portrait width
    // A3 portrait = 841.89 × 1190.55  → TABLE_W = 841.89 - 56 = 785.89
    private static final String[] COL_NAMES = {
            "Type [Row]", "GSTIN", "Name", "Invoice", "Date",
            "Taxable", "IGST", "CGST", "SGST", "CESS"
    };

    // proportional widths — must sum to 1.0
    private static final float[] COL_RATIOS = {
            0.11f,  // Type
            0.11f,  // GSTIN
            0.13f,  // Name
            0.10f,  // Invoice
            0.08f,  // Date
            0.10f,  // Taxable
            0.09f,  // IGST
            0.09f,  // CGST
            0.09f,  // SGST
            0.10f   // CESS
    };

    // ── colours ───────────────────────────────────────────────────────────────
    private static final Color COL_HEADER_BG = new Color(0x2C2C2A);
    private static final Color COL_HEADER_FG = Color.WHITE;
    private static final Color ONLINE_BG     = new Color(0xE6F1FB);
    private static final Color TALLY_BG      = new Color(0xEEEDFE);
    private static final Color DIFF_RED_BG   = new Color(0xFF9696);
    private static final Color DIFF_GREEN_BG = new Color(0xC8F0C8);
    private static final Color TOTAL_BG      = new Color(0xFFFBF4);
    private static final Color SPACER_BG     = new Color(0xE8E8E8);
    private static final Color MISSING_BG    = new Color(0xFAEEDA);
    private static final Color BORDER_COLOR  = new Color(0x2C2C2A);
    private static final Color NEG_COLOR     = new Color(0xA32D2D);
    private static final Color POS_COLOR     = new Color(0x3B6D11);
    private static final Color MUTED_COLOR   = new Color(0x888780);

    // ── fonts ─────────────────────────────────────────────────────────────────
    private PDFont fontRegular;
    private PDFont fontBold;

    // ── computed column x-positions ───────────────────────────────────────────
    private float[] colX;
    private float[] colW;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public void export(List<ReconciliationResult> data,
                       String mode, File out) throws Exception {

        try (PDDocument doc = new PDDocument()) {

            // init fonts
            fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            // compute column positions
            computeColumns();

            // document info
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle("GST Reconciliation — " + mode);
            info.setAuthor("PL Handloom Tools");

            boolean matchView = "MATCH_MISMATCH".equals(mode)
                    || "ONLY_MISMATCH".equals(mode);

            // ── first page ────────────────────────────────────────────────────
            PageWriter pw = new PageWriter(doc);
            pw.drawTitle("GST Reconciliation — " + mode);
            pw.drawColumnHeaders();

            // ── data rows ─────────────────────────────────────────────────────
            for (ReconciliationResult r : data) {

                if (matchView) {
                    // ensure 3 rows + spacer fit — otherwise start new page
                    if (!pw.canFit(ROW_H * 3 + 4)) {
                        pw.finish();
                        pw = new PageWriter(doc);
                        pw.drawColumnHeaders();
                    }
                    pw.drawDataRow(buildOnlineLabel(r), r.getB2bRow(), ONLINE_BG, false);
                    pw.drawDataRow(buildTallyLabel(r),  r.getGstRow(),  TALLY_BG,  false);
                    pw.drawDiffRow(r.getB2bRow(), r.getGstRow());
                    pw.drawSpacer();

                } else {
                    if (!pw.canFit(ROW_H)) {
                        pw.finish();
                        pw = new PageWriter(doc);
                        pw.drawColumnHeaders();
                    }
                    RowDataEntity d = r.getB2bRow() != null
                            ? r.getB2bRow() : r.getGstRow();
                    String label = buildStatusLabel(r);
                    pw.drawDataRow(label, d, MISSING_BG, false);
                }
            }

            // ── totals ────────────────────────────────────────────────────────
            if (!data.isEmpty()) {
                if (matchView) {
                    if (!pw.canFit(ROW_H * 3)) {
                        pw.finish();
                        pw = new PageWriter(doc);
                        pw.drawColumnHeaders();
                    }
                    drawMatchMismatchTotals(pw, data);
                } else {
                    if (!pw.canFit(ROW_H)) {
                        pw.finish();
                        pw = new PageWriter(doc);
                        pw.drawColumnHeaders();
                    }
                    drawSimpleTotal(pw, data);
                }
            }

            pw.finish();
            doc.save(out);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLUMN SETUP
    // ─────────────────────────────────────────────────────────────────────────

    private void computeColumns() {
        colW = new float[COL_RATIOS.length];
        colX = new float[COL_RATIOS.length];
        float x = MARGIN;
        for (int i = 0; i < COL_RATIOS.length; i++) {
            colW[i] = TABLE_W * COL_RATIOS[i];
            colX[i] = x;
            x += colW[i];
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOTAL ROW HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void drawSimpleTotal(PageWriter pw,
                                 List<ReconciliationResult> data)
            throws IOException {
        double t=0, ig=0, cg=0, sg=0, ce=0;
        for (ReconciliationResult r : data) {
            RowDataEntity d = r.getB2bRow() != null
                    ? r.getB2bRow() : r.getGstRow();
            if (d == null) continue;
            t+=d.getTaxable(); ig+=d.getIgst();
            cg+=d.getCgst();   sg+=d.getSgst(); ce+=d.getCess();
        }
        pw.drawTotalRow("TOTAL  [COUNT " + data.size() + "]",
                t, ig, cg, sg, ce, TOTAL_BG);
    }

    private void drawMatchMismatchTotals(PageWriter pw,
                                         List<ReconciliationResult> data)
            throws IOException {
        double bt=0,bi=0,bc=0,bs=0,be=0;
        double gt=0,gi=0,gc=0,gs=0,ge=0;
        for (ReconciliationResult r : data) {
            RowDataEntity b = r.getB2bRow();
            RowDataEntity g = r.getGstRow();
            if (b!=null){bt+=b.getTaxable();bi+=b.getIgst();bc+=b.getCgst();bs+=b.getSgst();be+=b.getCess();}
            if (g!=null){gt+=g.getTaxable();gi+=g.getIgst();gc+=g.getCgst();gs+=g.getSgst();ge+=g.getCess();}
        }
        pw.drawTotalRow("TOTAL ONLINE  [COUNT " + data.size() + "]",
                bt, bi, bc, bs, be, new Color(0xE6F1FB));
        pw.drawTotalRow("TOTAL TALLY   [COUNT " + data.size() + "]",
                gt, gi, gc, gs, ge, new Color(0xEEEDFE));
        pw.drawTotalRow("TOTAL DIFF",
                bt-gt, bi-gi, bc-gc, bs-gs, be-ge, TOTAL_BG);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LABEL BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    private String buildOnlineLabel(ReconciliationResult r) {
        return r.getB2bRow() != null
                ? "ONLINE [" + r.getB2bRow().getRowNo() + "]" : "ONLINE";
    }

    private String buildTallyLabel(ReconciliationResult r) {
        return r.getGstRow() != null
                ? "TALLY  [" + r.getGstRow().getRowNo() + "]" : "TALLY";
    }

    private String buildStatusLabel(ReconciliationResult r) {
        RowDataEntity d = r.getB2bRow() != null
                ? r.getB2bRow() : r.getGstRow();
        long rowNo = d != null ? d.getRowNo() : 0;
        return r.getStatus() + " [" + rowNo + "]";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private String fmt(double v) {
        return v == 0.0 ? "-" : String.format("%.2f", v);
    }

    private String fmtDiff(double v) {
        return v == 0.0 ? "-" : String.format("%.2f", v);
    }

    private String truncate(String s, float maxWidth, PDFont font,
                            float fontSize) throws IOException {
        if (s == null) return "";
        while (s.length() > 1) {
            float w = font.getStringWidth(s) / 1000f * fontSize;
            if (w <= maxWidth) return s;
            s = s.substring(0, s.length() - 2) + "…";
        }
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE WRITER — inner class manages one page at a time
    // ─────────────────────────────────────────────────────────────────────────

    private class PageWriter {

        private final PDDocument           doc;
        private final PDPage               page;
        private       PDPageContentStream  cs;
        private       float                curY;

        PageWriter(PDDocument doc) throws IOException {
            this.doc  = doc;
            this.page = new PDPage(PAGE_SIZE);
            doc.addPage(page);
            cs   = new PDPageContentStream(doc, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);
            curY = PAGE_H - MARGIN;
        }

        void finish() throws IOException { cs.close(); }

        boolean canFit(float height) { return curY - height >= MARGIN; }

        // ── title ─────────────────────────────────────────────────────────────
        void drawTitle(String text) throws IOException {
            fillRect(MARGIN, curY - TITLE_H, TABLE_W, TITLE_H,
                    new Color(0x534AB7));
            drawText(text, MARGIN + CELL_PAD,
                    curY - TITLE_H / 2 - 5,
                    fontBold, 11, Color.WHITE);
            curY -= TITLE_H;
        }

        // ── column header row ─────────────────────────────────────────────────
        void drawColumnHeaders() throws IOException {
            fillRect(MARGIN, curY - HEADER_H, TABLE_W, HEADER_H,
                    COL_HEADER_BG);

            for (int i = 0; i < COL_NAMES.length; i++) {
                String text = truncate(COL_NAMES[i], colW[i] - CELL_PAD * 2,
                        fontBold, 7.5f);
                drawText(text, colX[i] + CELL_PAD,
                        curY - HEADER_H / 2 - 3,
                        fontBold, 7.5f, COL_HEADER_FG);
                // right border
                drawLine(colX[i] + colW[i], curY,
                        colX[i] + colW[i], curY - HEADER_H,
                        BORDER_COLOR, 0.3f);
            }

            // outer border
            drawRect(MARGIN, curY - HEADER_H, TABLE_W, HEADER_H,
                    BORDER_COLOR, 0.5f);
            curY -= HEADER_H;
        }

        // ── data row ──────────────────────────────────────────────────────────
        void drawDataRow(String typeLabel, RowDataEntity d,
                         Color bg, boolean bold) throws IOException {
            fillRect(MARGIN, curY - ROW_H, TABLE_W, ROW_H, bg);
            drawRowBorders();

            float fontSize = 7f;
            PDFont font    = bold ? fontBold : fontRegular;

            if (d == null) {
                String tl = truncate(typeLabel, colW[0] - CELL_PAD * 2, font, fontSize);
                drawText(tl, colX[0] + CELL_PAD, textY(), font, fontSize, Color.BLACK);
                curY -= ROW_H;
                return;
            }

            String[] cells = {
                    typeLabel,
                    nvl(d.getGstin()),
                    nvl(d.getTradeOrLegalName()),
                    nvl(d.getInvoiceNo()),
                    d.getDate() != null ? d.getDate().toString() : "",
                    fmt(d.getTaxable()),
                    fmt(d.getIgst()),
                    fmt(d.getCgst()),
                    fmt(d.getSgst()),
                    fmt(d.getCess())
            };

            for (int i = 0; i < cells.length; i++) {
                String text = truncate(cells[i],
                        colW[i] - CELL_PAD * 2, font, fontSize);
                // right-align numeric columns (5-9)
                float tx = i >= 5
                        ? colX[i] + colW[i] - CELL_PAD
                        - textWidth(text, font, fontSize)
                        : colX[i] + CELL_PAD;
                drawText(text, tx, textY(), font, fontSize, Color.BLACK);
            }

            curY -= ROW_H;
        }

        // ── diff row ──────────────────────────────────────────────────────────
        void drawDiffRow(RowDataEntity b, RowDataEntity g) throws IOException {
            if (b == null || g == null) {
                fillRect(MARGIN, curY - ROW_H, TABLE_W, ROW_H, SPACER_BG);
                drawRowBorders();
                drawText("DIFF", colX[0] + CELL_PAD, textY(),
                        fontBold, 7f, MUTED_COLOR);
                curY -= ROW_H;
                return;
            }

            double[] diffs = {
                    round(b.getTaxable() - g.getTaxable()),
                    round(b.getIgst()    - g.getIgst()),
                    round(b.getCgst()    - g.getCgst()),
                    round(b.getSgst()    - g.getSgst()),
                    round(b.getCess()    - g.getCess())
            };

            boolean hasDiff = false;
            for (double d : diffs) if (d != 0) { hasDiff = true; break; }

            Color bg = hasDiff ? DIFF_RED_BG : DIFF_GREEN_BG;
            fillRect(MARGIN, curY - ROW_H, TABLE_W, ROW_H, bg);

            // thick bottom border to close the group
            drawLine(MARGIN, curY - ROW_H,
                    MARGIN + TABLE_W, curY - ROW_H,
                    BORDER_COLOR, 1.2f);
            drawRowBorders();

            drawText("DIFF", colX[0] + CELL_PAD, textY(),
                    fontBold, 7f, Color.BLACK);

            // numeric diff cells (cols 5-9)
            for (int i = 0; i < diffs.length; i++) {
                int col = i + 5;
                String text = fmtDiff(diffs[i]);
                Color  fg   = diffs[i] < 0 ? NEG_COLOR
                        : diffs[i] > 0 ? POS_COLOR
                        : MUTED_COLOR;
                float tx = colX[col] + colW[col] - CELL_PAD
                        - textWidth(text, fontBold, 7f);
                drawText(text, tx, textY(), fontBold, 7f, fg);
            }

            curY -= ROW_H;
        }

        // ── total row ─────────────────────────────────────────────────────────
        void drawTotalRow(String label,
                          double t, double ig, double cg,
                          double sg, double ce,
                          Color bg) throws IOException {
            fillRect(MARGIN, curY - ROW_H, TABLE_W, ROW_H, bg);

            // thick top + bottom border
            drawLine(MARGIN, curY,
                    MARGIN + TABLE_W, curY,
                    BORDER_COLOR, 1.2f);
            drawLine(MARGIN, curY - ROW_H,
                    MARGIN + TABLE_W, curY - ROW_H,
                    BORDER_COLOR, 1.2f);
            drawRowBorders();

            // label spans first 5 columns
            String lbl = truncate(label,
                    (colX[4] + colW[4]) - colX[0] - CELL_PAD * 2,
                    fontBold, 7f);
            drawText(lbl, colX[0] + CELL_PAD, textY(), fontBold, 7f, Color.BLACK);

            // numeric totals (cols 5-9)
            double[] nums = {t, ig, cg, sg, ce};
            for (int i = 0; i < nums.length; i++) {
                int    col  = i + 5;
                String text = String.format("%.2f", round(nums[i]));
                Color  fg   = nums[i] < 0 ? NEG_COLOR
                        : nums[i] > 0 ? POS_COLOR
                        : Color.BLACK;
                float tx = colX[col] + colW[col] - CELL_PAD
                        - textWidth(text, fontBold, 7f);
                drawText(text, tx, textY(), fontBold, 7f, fg);
            }

            curY -= ROW_H;
        }

        // ── spacer ────────────────────────────────────────────────────────────
        void drawSpacer() throws IOException {
            fillRect(MARGIN, curY - 5, TABLE_W, 5, SPACER_BG);
            curY -= 5;
        }

        // ── drawing primitives ────────────────────────────────────────────────

        private float textY() { return curY - ROW_H / 2 - 2.5f; }

        private void drawRowBorders() throws IOException {
            // outer rect
            drawRect(MARGIN, curY - ROW_H, TABLE_W, ROW_H, BORDER_COLOR, 0.3f);
            // column separators
            for (int i = 1; i < COL_NAMES.length; i++) {
                drawLine(colX[i], curY,
                        colX[i], curY - ROW_H,
                        BORDER_COLOR, 0.3f);
            }
        }

        private void fillRect(float x, float y, float w, float h,
                              Color c) throws IOException {
            cs.setNonStrokingColor(c);
            cs.addRect(x, y, w, h);
            cs.fill();
        }

        private void drawRect(float x, float y, float w, float h,
                              Color c, float lineW) throws IOException {
            cs.setStrokingColor(c);
            cs.setLineWidth(lineW);
            cs.addRect(x, y, w, h);
            cs.stroke();
        }

        private void drawLine(float x1, float y1, float x2, float y2,
                              Color c, float lineW) throws IOException {
            cs.setStrokingColor(c);
            cs.setLineWidth(lineW);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private void drawText(String text, float x, float y,
                              PDFont font, float size,
                              Color c) throws IOException {
            if (text == null || text.isEmpty()) return;
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(c);
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
        }

        private float textWidth(String text, PDFont font,
                                float size) throws IOException {
            if (text == null || text.isEmpty()) return 0;
            return font.getStringWidth(text) / 1000f * size;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String nvl(String s) { return s == null ? "" : s; }
}
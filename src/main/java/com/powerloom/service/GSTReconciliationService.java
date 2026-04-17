package com.powerloom.service;

import com.powerloom.entity.ReconciliationResponse;
import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.RowDataEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GSTReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(GSTReconciliationService.class);

    private static final String INVOICE_ENTRY_MATCH     = "INVOICE_ENTRY_MATCH";
    private static final String INVOICE_ENTRY_NOT_MATCH = "INVOICE_ENTRY_NOT_MATCH";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public List<ReconciliationResult> compare(File b2bFile, File gstFile,
                                              int b2bStart, int gstStart) throws Exception {

        // ── DEBUG: print all headers found in both files ──────────────────────
        debugHeaders(b2bFile, b2bStart, "B2B");
        debugHeaders(gstFile, gstStart, "GST");

        Map<String, Set<String>> gstnDocNoMap = buildGstDocNoMap(gstFile, gstStart);
        ReconciliationResponse   gstData      = loadGST(gstFile, gstStart);
        ReconciliationResponse   b2bData      = loadB2B(b2bFile, b2bStart, gstnDocNoMap);

        Map<String, RowDataEntity> b2bMap  = b2bData.getDataMap();
        Map<String, RowDataEntity> gstMap  = gstData.getDataMap();
        List<RowDataEntity>        b2bList = b2bData.getList();
        List<RowDataEntity>        gstList = gstData.getList();

        List<ReconciliationResult> results = new ArrayList<>();

        for (RowDataEntity b2bRow : b2bList) {
            ReconciliationResult r = new ReconciliationResult();
            r.setB2bRow(b2bRow);
            if (INVOICE_ENTRY_MATCH.equals(b2bRow.getStatus())) {
                RowDataEntity gstRow = gstMap.get(b2bRow.key());
                r.setGstRow(gstRow);
                r.setStatus(b2bRow.matches(gstRow) ? "MATCH" : "MISMATCH");
            } else {
                r.setStatus("MISSING_IN_TALLY");
            }
            results.add(r);
        }

        for (RowDataEntity gstRow : gstList) {
            if (!b2bMap.containsKey(gstRow.key())) {
                ReconciliationResult r = new ReconciliationResult();
                r.setGstRow(gstRow);
                r.setStatus("MISSING_IN_ONLINE");
                results.add(r);
            }
        }

        log.info("Online count : {}", b2bList.size());
        log.info("Tally count  : {}", gstList.size());
        results.stream()
                .collect(Collectors.groupingBy(ReconciliationResult::getStatus,
                        Collectors.counting()))
                .forEach((s, c) -> log.info("{} -> {}", s, c));

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEBUG HELPER — prints every header found so you can see the exact strings
    // ─────────────────────────────────────────────────────────────────────────

    private void debugHeaders(File file, int startRow, String label) {
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);
            // try header row at startRow-3, startRow-2, startRow-1
            for (int offset = 1; offset <= 3; offset++) {
                int rowIdx = startRow - offset;
                if (rowIdx < 0) continue;
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                List<String> headers = new ArrayList<>();
                for (Cell c : row) {
                    String val = c.toString().trim();
                    if (!val.isEmpty()) headers.add("[" + val + "]");
                }
                if (!headers.isEmpty()) {
                    log.info("=== {} headers at Excel row {} (0-indexed {}) ===",
                            label, rowIdx + 1, rowIdx);
                    log.info("{}", String.join(" | ", headers));
                }
            }
        } catch (Exception e) {
            log.error("debugHeaders failed for {}: {}", label, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD B2B
    // ─────────────────────────────────────────────────────────────────────────

    private ReconciliationResponse loadB2B(File file, int startRow,
                                           Map<String, Set<String>> gstnDocNoMap)
            throws Exception {

        ReconciliationResponse response = new ReconciliationResponse();
        Map<String, RowDataEntity>   map      = new LinkedHashMap<>();

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);

            // ── resolve header row ────────────────────────────────────────────
            // GSTR-2B files put the column header 3 rows above the first data row.
            // We try offsets 3, 2, 1 and pick the row that has the most cells.
            Map<String, Integer> h = bestHeaderRow(sheet, startRow, "gstin of supplier",
                    "invoice number", "taxable value");

            log.info("B2B header map: {}", h);

            // ── data rows ─────────────────────────────────────────────────────
            for (int i = startRow - 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                RowDataEntity d = new RowDataEntity();
                d.setRowNo(i + 1);
                d.setGstin(str(row, h.get("gstin of supplier")));
                d.setInvoiceNo(str(row, h.get("invoice number")));
                d.setTradeOrLegalName(str(row, h.get("trade/legal name")));
                d.setDate(date(row, h.get("invoice date")));

                // ── numeric columns — use fuzzy key lookup ────────────────────
                d.setTaxable(numByKey(row, h, "taxable value"));
                d.setIgst(numByKey(row, h, "integrated tax"));
                d.setCgst(numByKey(row, h, "central tax"));
                d.setSgst(numByKey(row, h, "state/ut tax"));
                d.setCess(numByKey(row, h, "cess"));

                // log first data row so you can verify values
                if (i == startRow - 1) {
                    log.info("B2B first data row [{}}]: gstin={} taxable={} igst={} cgst={} sgst={} cess={}",
                            i + 1, d.getGstin(), d.getTaxable(),
                            d.getIgst(), d.getCgst(), d.getSgst(), d.getCess());
                }

                // ── match invoice to tally doc no ─────────────────────────────
                Set<String> gstDocNoSet = gstnDocNoMap.get(d.getGstin());
                if (gstDocNoSet == null) {
                    d.setCoreDocNo(d.getInvoiceNo());
                    d.setStatus(INVOICE_ENTRY_NOT_MATCH);
                } else {
                    Optional<String> match = gstDocNoSet.stream()
                            .filter(docNo -> d.getInvoiceNo() != null
                                    && d.getInvoiceNo().contains(docNo))
                            .findFirst();
                    if (match.isPresent()) {
                        d.setCoreDocNo(match.get());
                        d.setStatus(INVOICE_ENTRY_MATCH);
                    } else {
                        d.setCoreDocNo(d.getInvoiceNo());
                        d.setStatus(INVOICE_ENTRY_NOT_MATCH);
                        log.info("No match: invoice={} gstin={} available={}",
                                d.getInvoiceNo(), d.getGstin(), gstDocNoSet);
                    }
                }

                map.put(d.key(), d);
                response.getList().add(d);
            }
        }

        response.setDataMap(map);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD GST (TALLY)
    // ─────────────────────────────────────────────────────────────────────────

    private ReconciliationResponse loadGST(File file, int startRow) throws Exception {

        ReconciliationResponse response = new ReconciliationResponse();
        Map<String, RowDataEntity>   map      = new LinkedHashMap<>();

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> h = bestHeaderRow(sheet, startRow,
                    "party gstin/uin", "doc no.", "taxable");

            log.info("GST header map: {}", h);

            for (int i = startRow - 1; i < sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                RowDataEntity d = new RowDataEntity();
                d.setRowNo(i + 1);
                d.setGstin(str(row, h.get("party gstin/uin")));
                d.setInvoiceNo(str(row, h.get("doc no.")));
                d.setCoreDocNo(
                        d.getInvoiceNo() != null
                                ? (
                                d.getInvoiceNo().contains("/")
                                        ? d.getInvoiceNo().split("/")[0]
                                        : d.getInvoiceNo()
                        ).replaceFirst("^0+", "")
                                : null
                );
                d.setTradeOrLegalName(str(row, h.get("particulars")));
                d.setDate(date(row, h.get("date")));
                d.setTaxable(numByKey(row, h, "taxable"));
                d.setIgst(numByKey(row, h, "igst"));
                d.setCgst(numByKey(row, h, "cgst"));
                d.setSgst(numByKey(row, h, "sgst"));
                d.setCess(numByKey(row, h, "cess"));

                RowDataEntity existing = map.put(d.key(), d);
                if (existing != null) log.warn("Duplicate GST key: {}", d.key());
                response.getList().add(d);
            }
        }

        response.setDataMap(map);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD GSTN → DOC-NO MAP
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Set<String>> buildGstDocNoMap(File file, int startRow)
            throws Exception {

        Map<String, Set<String>> gstDocMap = new HashMap<>();

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> h = bestHeaderRow(sheet, startRow,
                    "party gstin/uin", "doc no.", "taxable");

            for (int i = startRow - 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String gstin     = str(row, h.get("party gstin/uin"));
                String invoiceNo = str(row, h.get("doc no."));
                if (gstin.isEmpty() || invoiceNo.isEmpty()) continue;

                String coreInvoice = invoiceNo.contains("/")
                        ? invoiceNo.split("/")[0] : invoiceNo;
                coreInvoice = coreInvoice.replaceFirst("^0+", "");

                gstDocMap.computeIfAbsent(gstin, k -> new HashSet<>()).add(coreInvoice);
            }
        }

        return gstDocMap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HEADER ROW RESOLVER
    // Tries offsets 3, 2, 1 above startRow; picks the row whose normalised
    // headers contain the most of the required keys.
    // This handles files where headers are 2 rows above instead of 3.
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Integer> bestHeaderRow(Sheet sheet, int startRow,
                                               String... requiredKeys) {
        Map<String, Integer> best = Collections.emptyMap();
        int bestScore = -1;

        for (int offset = 1; offset <= 4; offset++) {
            int rowIdx = startRow - offset - 1; // convert to 0-based
            if (rowIdx < 0) continue;
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            Map<String, Integer> candidate = header(row);
            int score = 0;
            for (String key : requiredKeys) {
                // fuzzy: check if any header key contains the required key
                for (String h : candidate.keySet()) {
                    if (h.contains(key)) { score++; break; }
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POI HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build header map: normalised cell text → column index.
     * Normalisation: trim + lowercase + collapse multiple spaces + strip ₹ symbol.
     */
    private Map<String, Integer> header(Row r) {
        Map<String, Integer> m = new HashMap<>();
        if (r == null) return m;
        for (Cell c : r) {
            String raw = c.toString();
            String key = normaliseHeader(raw);
            if (!key.isEmpty()) {
                m.put(key, c.getColumnIndex());
            }
        }
        return m;
    }

    private String normaliseHeader(String raw) {
        return raw
                .trim()
                .toLowerCase()
                .replace("₹", "")        // strip rupee symbol
                .replace("\u20b9", "")   // strip ₹ as unicode escape
                .replaceAll("\\(\\s*\\)", "") // strip empty parens
                .replaceAll("\\s+", " ") // collapse spaces
                .trim();
    }

    /**
     * Fuzzy numeric lookup by partial key match.
     * e.g. key="taxable value" matches header "taxable value (₹)"
     * or "taxable value(rs)" or "taxable value"
     */
    private double numByKey(Row row, Map<String, Integer> headerMap, String partialKey) {
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            if (entry.getKey().contains(partialKey)) {
                return num(row, entry.getValue());
            }
        }
        log.warn("No column found for key '{}' in row {}", partialKey, row.getRowNum() + 1);
        return 0;
    }

    private String str(Row r, Integer c) {
        if (c == null) return "";
        Cell cell = r.getCell(c);
        if (cell == null) return "";
        return cell.toString().trim().toUpperCase();
    }

    /**
     * Robust number reader — handles NUMERIC, STRING, FORMULA, and BLANK cell types.
     */
    private double num(Row r, Integer c) {
        if (c == null) return 0;
        Cell cell = r.getCell(c);
        if (cell == null) return 0;

        switch (cell.getCellType()) {

            case NUMERIC:
                return cell.getNumericCellValue();

            case FORMULA:
                // evaluate formula result
                try {
                    CellType cached = cell.getCachedFormulaResultType();
                    if (cached == CellType.NUMERIC)
                        return cell.getNumericCellValue();
                    if (cached == CellType.STRING)
                        return parseDouble(cell.getStringCellValue());
                } catch (Exception e) {
                    log.warn("Formula eval failed at col {}: {}", c, e.getMessage());
                }
                return 0;

            case STRING:
                return parseDouble(cell.getStringCellValue());

            case BLANK:
            default:
                return 0;
        }
    }

    /** Parse a string that may contain commas, spaces, or be empty. */
    private double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            return Double.parseDouble(
                    raw.trim()
                            .replace(",", "")
                            .replace(" ", "")
            );
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDate date(Row r, Integer c) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell))
                        return cell.getDateCellValue()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    break;
                case STRING:
                    return parseDate(cell.getStringCellValue().trim());
                default:
                    break;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy")}) {
            try { return LocalDate.parse(value, fmt); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !cell.toString().trim().isEmpty()) return false;
        }
        return true;
    }
}
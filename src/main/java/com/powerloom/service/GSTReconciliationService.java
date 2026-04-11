package com.powerloom.service;

import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.ReconciliationSession;
import com.powerloom.entity.RowDataEntity;
import com.powerloom.repository.SessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GSTReconciliationService {

    private final SessionRepository sessionRepository;

    // ===================== MAIN =====================
    @Transactional
    public List<ReconciliationResult> compare(
            File b2bFile,
            File gstFile,
            int b2bStart,
            int gstStart,
            String monthName,
            int month,
            int year
    ) throws Exception {

        // 🔥 STEP 1: CREATE SESSION FIRST
        ReconciliationSession session = new ReconciliationSession();
        session.setMonthName(monthName);
        session.setMonthValue(month);
        session.setYearValue(year);
        session.setB2bFileName(b2bFile.getName());
        session.setGstFileName(gstFile.getName());

        // 🔥 STEP 2: LOAD ROWS WITH SESSION
        List<RowDataEntity> b2bList = loadRows(b2bFile, b2bStart, "B2B", session);
        List<RowDataEntity> gstList = loadRows(gstFile, gstStart, "GST", session);

        // 🔥 MAPS FOR MATCHING
        Map<String, RowDataEntity> gstMap = new HashMap<>();
        for (RowDataEntity g : gstList) {
            gstMap.put(g.key(), g);
        }

        Map<String, RowDataEntity> b2bMap = new HashMap<>();
        for (RowDataEntity b : b2bList) {
            b2bMap.put(b.key(), b);
        }

        List<ReconciliationResult> results = new ArrayList<>();

        // ================= MATCH LOGIC =================
        for (RowDataEntity b : b2bList) {

            ReconciliationResult r = new ReconciliationResult();
            r.setSession(session);
            r.setB2bRow(b);

            RowDataEntity g = gstMap.get(b.key());

            if (g != null) {
                r.setGstRow(g);
                r.setStatus(b.matches(g) ? "MATCH" : "MISMATCH");
            } else {
                r.setStatus("MISSING_IN_TALLY");
            }

            results.add(r);
        }

        // ================= MISSING ONLINE =================
        for (RowDataEntity g : gstList) {
            if (!b2bMap.containsKey(g.key())) {

                ReconciliationResult r = new ReconciliationResult();
                r.setSession(session);
                r.setGstRow(g);
                r.setStatus("MISSING_IN_ONLINE");

                results.add(r);
            }
        }

        // ================= ATTACH TO SESSION =================
        session.getRows().addAll(b2bList);
        session.getRows().addAll(gstList);
        session.getResults().addAll(results);

        // 🔥 SAVE (CASCADE WILL HANDLE ALL)
        sessionRepository.save(session);

        log.info("Reconciliation Done: {} records", results.size());

        return results;
    }

    // ===================== LOAD EXCEL =====================
    private List<RowDataEntity> loadRows(
            File file,
            int startRow,
            String source,
            ReconciliationSession session
    ) throws Exception {

        List<RowDataEntity> list = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {

            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> header = header(sheet.getRow(startRow - 3));

            for (int i = startRow - 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null || isEmpty(row)) continue;

                RowDataEntity d = new RowDataEntity();

                // 🔥 VERY IMPORTANT
                d.setSession(session);

                d.setSource(source);
                d.setRowNo(i + 1);

                if (source.equals("B2B")) {
                    d.setGstin(str(row, header.get("gstin of supplier")));
                    d.setInvoiceNo(str(row, header.get("invoice number")));
                    d.setCoreDocNo(d.getInvoiceNo());
                } else {
                    d.setGstin(str(row, header.get("party gstin/uin")));
                    d.setInvoiceNo(str(row, header.get("doc no.")));
                    d.setCoreDocNo(d.getInvoiceNo().split("/")[0]);
                }

                d.setTradeOrLegalName(str(row, header.get("trade/legal name")));

                d.setDate(date(row, header.get(source.equals("B2B") ? "invoice date" : "date")));
                d.setTaxable(num(row, header.get(source.equals("B2B") ? "taxable value (₹)" : "taxable")));
                d.setIgst(num(row, header.get(source.equals("B2B") ? "integrated tax(₹)" : "igst")));
                d.setCgst(num(row, header.get(source.equals("B2B") ? "central tax(₹)" : "cgst")));
                d.setSgst(num(row, header.get(source.equals("B2B") ? "state/ut tax(₹)" : "sgst/")));
                d.setCess(num(row, header.get("cess")));

                list.add(d);
            }
        }

        return list;
    }

    // ===================== UTIL =====================
    private Map<String, Integer> header(Row r) {
        Map<String, Integer> map = new HashMap<>();
        if (r == null) return map;

        for (Cell c : r) {
            if (c.getCellType() == CellType.STRING) {
                map.put(c.getStringCellValue().trim().toLowerCase(), c.getColumnIndex());
            }
        }
        return map;
    }

    private String str(Row r, Integer c) {
        if (c == null) return "";
        Cell cell = r.getCell(c);
        if (cell == null) return "";
        return cell.toString().trim();
    }

    private double num(Row r, Integer c) {
        if (c == null) return 0;
        Cell cell = r.getCell(c);
        if (cell == null) return 0;

        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            if (cell.getCellType() == CellType.STRING) return Double.parseDouble(cell.getStringCellValue());
        } catch (Exception ignored) {
        }
        return 0;
    }

    private LocalDate date(Row r, Integer c) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                return parseDate(cell.getStringCellValue());
            }
        } catch (Exception ignored) {
        }

        return null;
    }
    private LocalDate parseDate(String value) {
        DateTimeFormatter[] formats = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter f : formats) {
            try {
                return LocalDate.parse(value, f);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    private boolean isEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !cell.toString().trim().isEmpty()) return false;
        }
        return true;
    }
}
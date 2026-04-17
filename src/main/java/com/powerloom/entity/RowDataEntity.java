package com.powerloom.entity;

import java.time.LocalDate;

public class RowDataEntity {
    private String gstin, tradeOrLegalName, invoiceNo, coreDocNo, status;
    private LocalDate date;
    private double taxable, igst, cgst, sgst, cess;
    private long rowNo;

    // --- key used for map lookups ---
    public String key() { return gstin + "|" + coreDocNo; }

    public boolean matches(RowDataEntity o) {
        if (o == null) return false;
        return eq(this.taxable, o.taxable)
                && eq(this.igst, o.igst)
                && eq(this.cgst, o.cgst)
                && eq(this.sgst, o.sgst)
                && this.date.isEqual(o.date);

    }

    private boolean eq(double a, double b) { return a - b == 0; }

    // --- getters & setters (all fields) ---
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getTradeOrLegalName() { return tradeOrLegalName; }
    public void setTradeOrLegalName(String v) { this.tradeOrLegalName = v; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getCoreDocNo() { return coreDocNo; }
    public void setCoreDocNo(String v) { this.coreDocNo = v; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public double getTaxable() { return taxable; }
    public void setTaxable(double v) { this.taxable = v; }
    public double getIgst() { return igst; }
    public void setIgst(double v) { this.igst = v; }
    public double getCgst() { return cgst; }
    public void setCgst(double v) { this.cgst = v; }
    public double getSgst() { return sgst; }
    public void setSgst(double v) { this.sgst = v; }
    public double getCess() { return cess; }
    public void setCess(double v) { this.cess = v; }
    public long getRowNo() { return rowNo; }
    public void setRowNo(long rowNo) { this.rowNo = rowNo; }
}
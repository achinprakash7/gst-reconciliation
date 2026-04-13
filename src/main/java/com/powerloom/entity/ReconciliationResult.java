package com.powerloom.entity;

public class ReconciliationResult {
    private RowDataEntity b2bRow;
    private RowDataEntity gstRow;
    private String status;   // MATCH | MISMATCH | MISSING_IN_TALLY | MISSING_IN_ONLINE

    public RowDataEntity getB2bRow() { return b2bRow; }
    public void setB2bRow(RowDataEntity b2bRow) { this.b2bRow = b2bRow; }
    public RowDataEntity getGstRow() { return gstRow; }
    public void setGstRow(RowDataEntity gstRow) { this.gstRow = gstRow; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
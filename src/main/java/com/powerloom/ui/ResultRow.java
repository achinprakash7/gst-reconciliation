package com.powerloom.ui;

import javafx.beans.property.*;
import org.springframework.util.StringUtils;

/**
 * ResultRow — one display row in the reconciliation TableView.
 *
 * FIXES vs uploaded version:
 *
 * 1. NUMERIC FIELDS ARE NOW ObjectProperty<Double> (nullable), NOT DoubleProperty.
 *    DoubleProperty always holds a primitive double — its default is 0.0 and it
 *    can NEVER be null. So spacer rows (new ResultRow()) rendered "0.0" in every
 *    numeric cell because the TableView cell factory received 0.0, not null.
 *    With ObjectProperty<Double>, spacer rows store null, and the cell factory
 *    correctly renders blank when value == null.
 *
 * 2. NULLIFZERO SUPPORT.
 *    The controller's addDiffToTable() calls nullIfZero(double) — returning null
 *    for zero diffs so those cells render blank (not "0.00"). This only works
 *    when the property type can hold null, i.e. ObjectProperty<Double>.
 *
 * 3. NO-ARG CONSTRUCTOR leaves all numeric properties null → blank cells.
 *
 * 4. hasDiff() updated to null-safe check.
 *
 * 5. Column type in controller must be TableColumn<ResultRow, Double>
 *    (Object works too but Double is cleaner since the cell factory casts to Double).
 *    The cell factory already handles null → blank.
 */
public class ResultRow {

    // String properties — unchanged
    private final StringProperty type    = new SimpleStringProperty("");
    private final StringProperty gstin   = new SimpleStringProperty("");
    private final StringProperty name    = new SimpleStringProperty("");
    private final StringProperty invoice = new SimpleStringProperty("");
    private final StringProperty date    = new SimpleStringProperty("");

    // FIX: ObjectProperty<Double> instead of DoubleProperty
    // Allows null (→ blank cell) vs 0.0 (→ "0.00" cell). These are different.
    private final ObjectProperty<Double> taxable = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> igst    = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> cgst    = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> sgst    = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> cess    = new SimpleObjectProperty<>(null);

    // ── No-arg constructor — spacer row, all numeric = null = blank ──────────
    public ResultRow() {}

    // ── Full constructor — used for ONLINE, TALLY, TOTAL rows ────────────────
    public ResultRow(String type, String gstin, String name, String invoice,
                     String date, Double taxable, Double igst,
                     Double cgst, Double sgst, Double cess) {
        this.type   .set(type);
        this.gstin  .set(gstin);
        this.name   .set(name);
        this.invoice.set(invoice);
        this.date   .set(date);
        this.taxable.set(taxable);   // null is valid — renders blank
        this.igst   .set(igst);
        this.cgst   .set(cgst);
        this.sgst   .set(sgst);
        this.cess   .set(cess);
    }

    // ── hasDiff — true when DIFF row has at least one non-zero amount ─────────
    public boolean hasDiff() {
        return isNonZero(taxable.get())
                || isNonZero(igst.get())
                || isNonZero(cgst.get())
                || isNonZero(sgst.get())
                || isNonZero(cess.get())
                || StringUtils.hasText(date.get());
    }

    private boolean isNonZero(Double v) {
        return v != null && v != 0.0;
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public StringProperty typeProperty()            { return type; }
    public String         getType()                 { return type.get(); }
    public void           setType(String v)         { type.set(v); }

    public StringProperty gstinProperty()           { return gstin; }
    public String         getGstin()                { return gstin.get(); }
    public void           setGstin(String v)        { gstin.set(v); }

    public StringProperty nameProperty()            { return name; }
    public String         getName()                 { return name.get(); }
    public void           setName(String v)         { name.set(v); }

    public StringProperty invoiceProperty()         { return invoice; }
    public String         getInvoice()              { return invoice.get(); }
    public void           setInvoice(String v)      { invoice.set(v); }

    public StringProperty dateProperty()            { return date; }
    public String         getDate()                 { return date.get(); }
    public void           setDate(String v)         { date.set(v); }

    // FIX: return ObjectProperty<Double> not DoubleProperty
    public ObjectProperty<Double> taxableProperty() { return taxable; }
    public Double                 getTaxable()       { return taxable.get(); }
    public void                   setTaxable(Double v){ taxable.set(v); }

    public ObjectProperty<Double> igstProperty()    { return igst; }
    public Double                 getIgst()          { return igst.get(); }
    public void                   setIgst(Double v)  { igst.set(v); }

    public ObjectProperty<Double> cgstProperty()    { return cgst; }
    public Double                 getCgst()          { return cgst.get(); }
    public void                   setCgst(Double v)  { cgst.set(v); }

    public ObjectProperty<Double> sgstProperty()    { return sgst; }
    public Double                 getSgst()          { return sgst.get(); }
    public void                   setSgst(Double v)  { sgst.set(v); }

    public ObjectProperty<Double> cessProperty()    { return cess; }
    public Double                 getCess()          { return cess.get(); }
    public void                   setCess(Double v)  { cess.set(v); }
}
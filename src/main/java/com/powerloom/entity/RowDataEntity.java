package com.powerloom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "row_data_entity",
        indexes = {
                @Index(name = "idx_row_session",   columnList = "session_id"),
                @Index(name = "idx_row_gstin_doc", columnList = "gstin, core_doc_no"),
                @Index(name = "idx_row_source",    columnList = "source")
        }
)
@Getter                 // explicit @Getter — generates every getXxx() method
@Setter                 // explicit @Setter — generates every setXxx() method
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "session")
@EqualsAndHashCode(exclude = "session")
public class RowDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ReconciliationSession session;

    @Column(name = "source", nullable = false, length = 10)
    private String source;          // "B2B" or "GST"

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Column(name = "trade_or_legal_name", length = 255)
    private String tradeOrLegalName;

    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;

    @Column(name = "core_doc_no", length = 100)
    private String coreDocNo;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "taxable")
    private double taxable;

    @Column(name = "igst")
    private double igst;

    @Column(name = "cgst")
    private double cgst;

    @Column(name = "sgst")
    private double sgst;

    @Column(name = "cess")
    private double cess;

    @Column(name = "row_no")
    private long rowNo;

    @Column(name = "status", length = 50)
    private String status;

    // ── Original RowData methods preserved verbatim ──────────────────────────

    public String key() {
        return gstin + "|" + coreDocNo;
    }

    public boolean matches(RowDataEntity o) {
        if (o == null) return false;
        return eq(this.taxable, o.taxable)
                && eq(this.igst,    o.igst)
                && eq(this.cgst,    o.cgst)
                && eq(this.sgst,    o.sgst);
    }

    private boolean eq(double a, double b) {
        return a - b == 0;
    }
}

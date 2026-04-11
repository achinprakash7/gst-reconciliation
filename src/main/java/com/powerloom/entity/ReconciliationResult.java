package com.powerloom.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "reconciliation_result",
        indexes = {
                @Index(name = "idx_result_session", columnList = "session_id"),
                @Index(name = "idx_result_status",  columnList = "status")
        }
)
@Getter                 // generates getB2bRow(), getGstRow(), getStatus() etc.
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"session", "b2bRow", "gstRow"})
@EqualsAndHashCode(exclude = {"session", "b2bRow", "gstRow"})
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ReconciliationSession session;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "b2b_row_id" )
    private RowDataEntity b2bRow;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "gst_row_id")
    private RowDataEntity gstRow;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
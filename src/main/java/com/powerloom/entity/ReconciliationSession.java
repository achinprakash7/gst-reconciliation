package com.powerloom.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "reconciliation_session",
        indexes = {
                @Index(name = "idx_session_month_year", columnList = "month_value, year_value"),
                @Index(name = "idx_session_created",    columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"rows", "results"})
@EqualsAndHashCode(exclude = {"rows", "results"})
public class ReconciliationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_name", length = 10)
    private String monthName;

    @Column(name = "month_value")
    private int monthValue;

    @Column(name = "year_value")
    private int yearValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "b2b_file_name", length = 255)
    private String b2bFileName;

    @Column(name = "gst_file_name", length = 255)
    private String gstFileName;

    @Column(name = "total_b2b_count")
    private int totalB2bCount;

    @Column(name = "total_gst_count")
    private int totalGstCount;

    @Column(name = "match_count")
    private int matchCount;

    @Column(name = "mismatch_count")
    private int mismatchCount;

    @Column(name = "missing_in_tally_count")
    private int missingInTallyCount;

    @Column(name = "missing_in_online_count")
    private int missingInOnlineCount;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RowDataEntity> rows = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReconciliationResult> results = new ArrayList<>();
}
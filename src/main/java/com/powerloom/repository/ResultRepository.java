package com.powerloom.repository;

import com.powerloom.entity.ReconciliationResult;
import com.powerloom.entity.ReconciliationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<ReconciliationResult, Long> {

    List<ReconciliationResult> findBySession(ReconciliationSession session);

    List<ReconciliationResult> findBySessionAndStatus(ReconciliationSession session, String status);

    @Query("SELECT r FROM ReconciliationResult r " +
            "LEFT JOIN FETCH r.b2bRow " +
            "LEFT JOIN FETCH r.gstRow " +
            "WHERE r.session = :session")
    List<ReconciliationResult> findBySessionWithRows(@Param("session") ReconciliationSession session);
}
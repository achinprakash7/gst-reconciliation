package com.powerloom.repository;

import com.powerloom.entity.ReconciliationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<ReconciliationSession, Long> {
    List<ReconciliationSession> findAllByOrderByCreatedAtDesc();
    List<ReconciliationSession> findByMonthValueAndYearValue(int month, int year);
}
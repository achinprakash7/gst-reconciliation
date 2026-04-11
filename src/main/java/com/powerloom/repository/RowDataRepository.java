package com.powerloom.repository;

import com.powerloom.entity.ReconciliationSession;
import com.powerloom.entity.RowDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RowDataRepository extends JpaRepository<RowDataEntity, Long> {
    List<RowDataEntity> findBySessionAndSource(ReconciliationSession session, String source);
    List<RowDataEntity> findBySession(ReconciliationSession session);
}
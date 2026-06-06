package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.LogisticsTaskDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LogisticsTaskRepository extends JpaRepository<LogisticsTaskDoc, String> {

    List<LogisticsTaskDoc> findByFactoryIdOrderByCreatedAtDesc(String factoryId);

    List<LogisticsTaskDoc> findByProductBatchOrderByCreatedAtDesc(String productBatch);

    List<LogisticsTaskDoc> findByStatusOrderByCreatedAtDesc(String status);

    long deleteByStatusAndCompletedAtBefore(String status, Instant cutoff);
}

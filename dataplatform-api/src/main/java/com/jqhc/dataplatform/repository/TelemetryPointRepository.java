package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.TelemetryPointDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TelemetryPointRepository extends JpaRepository<TelemetryPointDoc, String> {

    List<TelemetryPointDoc> findByProductLineIdAndFieldIdInAndTimestampBetweenOrderByTimestampAsc(
            String productLineId, List<String> fieldIds, Instant start, Instant end);

    List<TelemetryPointDoc> findByProductBatchAndTimestampBetweenOrderByTimestampAsc(
            String productBatch, Instant start, Instant end);

    List<TelemetryPointDoc> findByProductLineIdAndProductBatchAndProcessStepId(
            String productLineId, String productBatch, String processStepId);

    long deleteByTimestampBefore(java.time.Instant cutoff);
}

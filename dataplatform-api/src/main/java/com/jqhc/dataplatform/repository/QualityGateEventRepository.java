package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.QualityGateEventDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface QualityGateEventRepository extends JpaRepository<QualityGateEventDoc, String> {

    List<QualityGateEventDoc> findByFactoryIdOrderByDecidedAtDesc(String factoryId);

    List<QualityGateEventDoc> findByBatchIdOrderByDecidedAtDesc(String batchId);

    List<QualityGateEventDoc> findByDecisionOrderByDecidedAtDesc(String decision);

    long deleteByDecidedAtBefore(Instant cutoff);
}

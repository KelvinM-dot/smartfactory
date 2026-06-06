package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.MaterialEventDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MaterialEventRepository extends JpaRepository<MaterialEventDoc, String> {

    List<MaterialEventDoc> findByProductLineIdOrderByTimestampDesc(String productLineId);

    List<MaterialEventDoc> findByMaterialBatchOrderByTimestampAsc(String materialBatch);

    long deleteByTimestampBefore(Instant cutoff);
}

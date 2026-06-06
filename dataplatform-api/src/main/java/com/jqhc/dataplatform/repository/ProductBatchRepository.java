package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.ProductBatchDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductBatchRepository extends JpaRepository<ProductBatchDoc, String> {

    List<ProductBatchDoc> findByProductLineIdOrderByStartedAtDesc(String productLineId);

    List<ProductBatchDoc> findByProductionOrderId(String productionOrderId);

    Optional<ProductBatchDoc> findFirstByProductLineIdAndStatusOrderByStartedAtDesc(
            String productLineId, String status);
}

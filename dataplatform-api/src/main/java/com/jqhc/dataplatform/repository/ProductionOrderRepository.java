package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.ProductionOrderDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrderDoc, String> {

    List<ProductionOrderDoc> findByFactoryIdOrderByDueDateAsc(String factoryId);

    List<ProductionOrderDoc> findByStatusOrderByDueDateAsc(String status);
}

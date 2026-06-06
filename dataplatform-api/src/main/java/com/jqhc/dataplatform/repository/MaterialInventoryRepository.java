package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.MaterialInventoryDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialInventoryRepository extends JpaRepository<MaterialInventoryDoc, String> {

    List<MaterialInventoryDoc> findByInventoryClass(String inventoryClass);

    List<MaterialInventoryDoc> findByProductLineId(String productLineId);
}

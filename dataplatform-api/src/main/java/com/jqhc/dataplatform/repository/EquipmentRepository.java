package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.EquipmentDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<EquipmentDoc, String> {

    List<EquipmentDoc> findByProductLineId(String productLineId);

    List<EquipmentDoc> findByProductLineIdAndProcessStepId(String productLineId, String processStepId);
}

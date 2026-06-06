package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.LatestStateDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LatestStateRepository extends JpaRepository<LatestStateDoc, String> {

    List<LatestStateDoc> findByProductLineId(String productLineId);

    List<LatestStateDoc> findByEquipmentId(String equipmentId);
}

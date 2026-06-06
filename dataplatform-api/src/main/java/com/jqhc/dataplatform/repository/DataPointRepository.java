package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.DataPointDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataPointRepository extends JpaRepository<DataPointDoc, String> {

    List<DataPointDoc> findByEquipmentId(String equipmentId);

    Optional<DataPointDoc> findByDataPointId(String dataPointId);

    List<DataPointDoc> findByProcessStepId(String processStepId);
}

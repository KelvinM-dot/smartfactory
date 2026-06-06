package com.jqhc.dataplatform.repository;

import com.jqhc.dataplatform.domain.AlarmEventDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AlarmEventRepository extends JpaRepository<AlarmEventDoc, String> {

    List<AlarmEventDoc> findByProductLineIdOrderByTriggeredAtDesc(String productLineId);

    List<AlarmEventDoc> findByProductLineIdAndHandleStatusOrderByTriggeredAtDesc(
            String productLineId, String handleStatus);

    long countByProductLineIdAndHandleStatus(String productLineId, String handleStatus);

    long deleteByHandleStatusAndResolvedAtBefore(String handleStatus, Instant cutoff);
}

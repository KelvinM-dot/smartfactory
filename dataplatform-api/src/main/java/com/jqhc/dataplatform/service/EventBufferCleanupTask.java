package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.repository.AlarmEventRepository;
import com.jqhc.dataplatform.repository.LogisticsTaskRepository;
import com.jqhc.dataplatform.repository.MaterialEventRepository;
import com.jqhc.dataplatform.repository.QualityGateEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 定期裁剪物料/物流/质门/报警等事件表的历史数据。
 */
@Component
public class EventBufferCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(EventBufferCleanupTask.class);

    private final MaterialEventRepository materialEventRepository;
    private final LogisticsTaskRepository logisticsTaskRepository;
    private final QualityGateEventRepository qualityGateEventRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final JqhcProperties properties;

    public EventBufferCleanupTask(
            MaterialEventRepository materialEventRepository,
            LogisticsTaskRepository logisticsTaskRepository,
            QualityGateEventRepository qualityGateEventRepository,
            AlarmEventRepository alarmEventRepository,
            JqhcProperties properties) {
        this.materialEventRepository = materialEventRepository;
        this.logisticsTaskRepository = logisticsTaskRepository;
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jqhc.event-cleanup-interval-ms:300000}")
    @Transactional
    public void purgeExpiredEvents() {
        Instant now = Instant.now();

        Instant materialCutoff = now.minus(properties.getMaterialEventRetentionHours(), ChronoUnit.HOURS);
        long materialDeleted = materialEventRepository.deleteByTimestampBefore(materialCutoff);
        if (materialDeleted > 0) {
            log.info("Purged {} material event(s) older than {}", materialDeleted, materialCutoff);
        }

        Instant logisticsCutoff = now.minus(properties.getCompletedLogisticsRetentionHours(), ChronoUnit.HOURS);
        long logisticsDeleted = logisticsTaskRepository.deleteByStatusAndCompletedAtBefore(
                "completed", logisticsCutoff);
        if (logisticsDeleted > 0) {
            log.info("Purged {} completed logistics task(s) older than {}", logisticsDeleted, logisticsCutoff);
        }

        Instant qualityCutoff = now.minus(properties.getQualityGateRetentionHours(), ChronoUnit.HOURS);
        long qualityDeleted = qualityGateEventRepository.deleteByDecidedAtBefore(qualityCutoff);
        if (qualityDeleted > 0) {
            log.info("Purged {} quality gate event(s) older than {}", qualityDeleted, qualityCutoff);
        }

        Instant alarmCutoff = now.minus(properties.getResolvedAlarmRetentionHours(), ChronoUnit.HOURS);
        long alarmDeleted = alarmEventRepository.deleteByHandleStatusAndResolvedAtBefore(
                "resolved", alarmCutoff);
        if (alarmDeleted > 0) {
            log.info("Purged {} resolved alarm(s) older than {}", alarmDeleted, alarmCutoff);
        }
    }
}

package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.repository.TelemetryPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 定期裁剪 telemetry_points 缓冲窗口外的历史遥测。
 */
@Component
public class TelemetryBufferCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(TelemetryBufferCleanupTask.class);

    private final TelemetryPointRepository telemetryPointRepository;
    private final JqhcProperties properties;

    public TelemetryBufferCleanupTask(
            TelemetryPointRepository telemetryPointRepository,
            JqhcProperties properties) {
        this.telemetryPointRepository = telemetryPointRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jqhc.telemetry-cleanup-interval-ms:300000}")
    @Transactional
    public void purgeExpiredTelemetry() {
        Instant cutoff = Instant.now().minus(properties.getBufferWindowHours(), ChronoUnit.HOURS);
        long deleted = telemetryPointRepository.deleteByTimestampBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} telemetry point(s) older than {}", deleted, cutoff);
        }
    }
}

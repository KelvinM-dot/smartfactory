package com.jqhc.dataplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jqhc")
public class JqhcProperties {

    private String domainConfigPath = "../schemas/智造数据台/presets/jqhc-manufacturing-config.json";
    private String masterDataPath = "../schemas/智造数据台/presets/jqhc-factory-master-data.json";
    private String sqliteDatabasePath = "./data/jqhc_dataplatform.db";
    private boolean resetDbOnStartup = true;
    private long telemetryCleanupIntervalMs = 60_000L;
    private long eventCleanupIntervalMs = 300_000L;
    private int bufferWindowHours = 2;
    private int materialEventRetentionHours = 72;
    private int qualityGateRetentionHours = 72;
    private int completedLogisticsRetentionHours = 24;
    private int resolvedAlarmRetentionHours = 168;
    private int heartbeatTimeoutSec = 30;
    private String simulatorUrl = "http://127.0.0.1:3002";

    public String getDomainConfigPath() {
        return domainConfigPath;
    }

    public void setDomainConfigPath(String domainConfigPath) {
        this.domainConfigPath = domainConfigPath;
    }

    public String getMasterDataPath() {
        return masterDataPath;
    }

    public void setMasterDataPath(String masterDataPath) {
        this.masterDataPath = masterDataPath;
    }

    public String getSqliteDatabasePath() {
        return sqliteDatabasePath;
    }

    public void setSqliteDatabasePath(String sqliteDatabasePath) {
        this.sqliteDatabasePath = sqliteDatabasePath;
    }

    public boolean isResetDbOnStartup() {
        return resetDbOnStartup;
    }

    public void setResetDbOnStartup(boolean resetDbOnStartup) {
        this.resetDbOnStartup = resetDbOnStartup;
    }

    public int getBufferWindowHours() {
        return bufferWindowHours;
    }

    public void setBufferWindowHours(int bufferWindowHours) {
        this.bufferWindowHours = bufferWindowHours;
    }

    public int getHeartbeatTimeoutSec() {
        return heartbeatTimeoutSec;
    }

    public void setHeartbeatTimeoutSec(int heartbeatTimeoutSec) {
        this.heartbeatTimeoutSec = heartbeatTimeoutSec;
    }

    public long getTelemetryCleanupIntervalMs() {
        return telemetryCleanupIntervalMs;
    }

    public void setTelemetryCleanupIntervalMs(long telemetryCleanupIntervalMs) {
        this.telemetryCleanupIntervalMs = telemetryCleanupIntervalMs;
    }

    public long getEventCleanupIntervalMs() {
        return eventCleanupIntervalMs;
    }

    public void setEventCleanupIntervalMs(long eventCleanupIntervalMs) {
        this.eventCleanupIntervalMs = eventCleanupIntervalMs;
    }

    public int getMaterialEventRetentionHours() {
        return materialEventRetentionHours;
    }

    public void setMaterialEventRetentionHours(int materialEventRetentionHours) {
        this.materialEventRetentionHours = materialEventRetentionHours;
    }

    public int getQualityGateRetentionHours() {
        return qualityGateRetentionHours;
    }

    public void setQualityGateRetentionHours(int qualityGateRetentionHours) {
        this.qualityGateRetentionHours = qualityGateRetentionHours;
    }

    public int getCompletedLogisticsRetentionHours() {
        return completedLogisticsRetentionHours;
    }

    public void setCompletedLogisticsRetentionHours(int completedLogisticsRetentionHours) {
        this.completedLogisticsRetentionHours = completedLogisticsRetentionHours;
    }

    public int getResolvedAlarmRetentionHours() {
        return resolvedAlarmRetentionHours;
    }

    public void setResolvedAlarmRetentionHours(int resolvedAlarmRetentionHours) {
        this.resolvedAlarmRetentionHours = resolvedAlarmRetentionHours;
    }

    public String getSimulatorUrl() {
        return simulatorUrl;
    }

    public void setSimulatorUrl(String simulatorUrl) {
        this.simulatorUrl = simulatorUrl;
    }
}

package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "source_status")
public class SourceStatusDoc {

    @Id
    @Column(name = "id")
    private String id;
    private String sourceInstance;
    private String source;
    private String productLineId;
    private Instant lastHeartbeatAt;
    private String scenarioId;
    private Double speedMultiplier;
    private Boolean connected;
    private String dwellMode;
    private String dwellReason;
    private Double activePowerKw;
    private Integer runningEquipmentCount;
    private String currentStep;
    private Boolean rawMaterialLow;

    public static String key(String sourceInstance, String productLineId) {
        return sourceInstance + "::" + productLineId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceInstance() { return sourceInstance; }
    public void setSourceInstance(String sourceInstance) { this.sourceInstance = sourceInstance; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public Double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(Double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public Boolean getConnected() { return connected; }
    public void setConnected(Boolean connected) { this.connected = connected; }
    public String getDwellMode() { return dwellMode; }
    public void setDwellMode(String dwellMode) { this.dwellMode = dwellMode; }
    public String getDwellReason() { return dwellReason; }
    public void setDwellReason(String dwellReason) { this.dwellReason = dwellReason; }
    public Double getActivePowerKw() { return activePowerKw; }
    public void setActivePowerKw(Double activePowerKw) { this.activePowerKw = activePowerKw; }
    public Integer getRunningEquipmentCount() { return runningEquipmentCount; }
    public void setRunningEquipmentCount(Integer runningEquipmentCount) { this.runningEquipmentCount = runningEquipmentCount; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public Boolean getRawMaterialLow() { return rawMaterialLow; }
    public void setRawMaterialLow(Boolean rawMaterialLow) { this.rawMaterialLow = rawMaterialLow; }
}

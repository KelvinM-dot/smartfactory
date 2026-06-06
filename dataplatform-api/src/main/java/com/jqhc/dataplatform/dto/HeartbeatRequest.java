package com.jqhc.dataplatform.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class HeartbeatRequest {

    private String source;
    private String sourceInstance;
    private Instant timestamp;
    private String productLineId;
    private String scenarioId;
    private Double speedMultiplier;
    private Map<String, Object> runtime;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceInstance() { return sourceInstance; }
    public void setSourceInstance(String sourceInstance) { this.sourceInstance = sourceInstance; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public Double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(Double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public Map<String, Object> getRuntime() { return runtime; }
    public void setRuntime(Map<String, Object> runtime) { this.runtime = runtime; }
}

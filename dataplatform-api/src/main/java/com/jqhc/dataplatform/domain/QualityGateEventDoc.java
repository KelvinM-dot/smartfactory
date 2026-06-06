package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "quality_gate_events")
public class QualityGateEventDoc {

    @Id
    @Column(name = "gate_event_id")
    private String gateEventId;
    private String factoryId;
    private String workshopId;
    private String batchId;
    private String processStepId;
    private String inspectionId;
    private String gateType;
    private String decision;
    private String reasonCode;
    private String reasonText;
    private String decidedBy;
    private Instant decidedAt;
    private String traceId;

    public String getGateEventId() { return gateEventId; }
    public void setGateEventId(String gateEventId) { this.gateEventId = gateEventId; }
    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getWorkshopId() { return workshopId; }
    public void setWorkshopId(String workshopId) { this.workshopId = workshopId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getProcessStepId() { return processStepId; }
    public void setProcessStepId(String processStepId) { this.processStepId = processStepId; }
    public String getInspectionId() { return inspectionId; }
    public void setInspectionId(String inspectionId) { this.inspectionId = inspectionId; }
    public String getGateType() { return gateType; }
    public void setGateType(String gateType) { this.gateType = gateType; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}

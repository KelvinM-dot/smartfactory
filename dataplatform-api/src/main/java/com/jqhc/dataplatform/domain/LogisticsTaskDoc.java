package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "logistics_tasks")
public class LogisticsTaskDoc {

    @Id
    @Column(name = "task_id")
    private String taskId;
    private String factoryId;
    private String workshopId;
    private String taskType;
    private String materialBatch;
    private String productBatch;
    private String productionOrderId;
    private String sourceLocationId;
    private String targetLocationId;
    private Double quantityKg;
    private String agvId;
    private String priority;
    private Instant createdAt;
    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;
    private String status;
    private String failureReason;
    private String traceId;
    private String correlationId;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getWorkshopId() { return workshopId; }
    public void setWorkshopId(String workshopId) { this.workshopId = workshopId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getMaterialBatch() { return materialBatch; }
    public void setMaterialBatch(String materialBatch) { this.materialBatch = materialBatch; }
    public String getProductBatch() { return productBatch; }
    public void setProductBatch(String productBatch) { this.productBatch = productBatch; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getSourceLocationId() { return sourceLocationId; }
    public void setSourceLocationId(String sourceLocationId) { this.sourceLocationId = sourceLocationId; }
    public String getTargetLocationId() { return targetLocationId; }
    public void setTargetLocationId(String targetLocationId) { this.targetLocationId = targetLocationId; }
    public Double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(Double quantityKg) { this.quantityKg = quantityKg; }
    public String getAgvId() { return agvId; }
    public void setAgvId(String agvId) { this.agvId = agvId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}

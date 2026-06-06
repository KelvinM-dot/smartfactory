package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "material_events")
public class MaterialEventDoc {

    @Id
    @Column(name = "event_id")
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String materialBatch;
    private String productType;
    private String productCategory;
    private String productLineId;
    private Double quantityKg;
    private String quantityUnit;
    private String location;
    private String fromLocation;
    private String toLocation;
    private String processStepId;
    private String agvId;
    private String remark;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getMaterialBatch() { return materialBatch; }
    public void setMaterialBatch(String materialBatch) { this.materialBatch = materialBatch; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public Double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(Double quantityKg) { this.quantityKg = quantityKg; }
    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }
    public String getProcessStepId() { return processStepId; }
    public void setProcessStepId(String processStepId) { this.processStepId = processStepId; }
    public String getAgvId() { return agvId; }
    public void setAgvId(String agvId) { this.agvId = agvId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

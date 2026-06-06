package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "material_inventory")
public class MaterialInventoryDoc {

    @Id
    @Column(name = "inventory_id")
    private String inventoryId;
    private String inventoryClass;
    private String sku;
    private String displayName;
    private String batchId;
    private String productBatch;
    private String processStepId;
    private String productLineId;
    private Double quantityKg;
    private String location;
    private String status;

    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }
    public String getInventoryClass() { return inventoryClass; }
    public void setInventoryClass(String inventoryClass) { this.inventoryClass = inventoryClass; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getProductBatch() { return productBatch; }
    public void setProductBatch(String productBatch) { this.productBatch = productBatch; }
    public String getProcessStepId() { return processStepId; }
    public void setProcessStepId(String processStepId) { this.processStepId = processStepId; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public Double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(Double quantityKg) { this.quantityKg = quantityKg; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

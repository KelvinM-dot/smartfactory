package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "product_batches")
public class ProductBatchDoc {

    @Id
    @Column(name = "batch_id")
    private String batchId;
    private String factoryId;
    private String workshopId;
    private String productLineId;
    private String productionOrderId;
    private String productCategory;
    private String recipeId;
    private String grade;
    private String shift;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> parentBatches;
    private Double quantityKg;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> qualityResult;

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getWorkshopId() { return workshopId; }
    public void setWorkshopId(String workshopId) { this.workshopId = workshopId; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public List<String> getParentBatches() { return parentBatches; }
    public void setParentBatches(List<String> parentBatches) { this.parentBatches = parentBatches; }
    public Double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(Double quantityKg) { this.quantityKg = quantityKg; }
    public Map<String, Object> getQualityResult() { return qualityResult; }
    public void setQualityResult(Map<String, Object> qualityResult) { this.qualityResult = qualityResult; }
}

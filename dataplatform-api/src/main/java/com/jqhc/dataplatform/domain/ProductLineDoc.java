package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "product_lines")
public class ProductLineDoc {

    @Id
    @Column(name = "product_line_id")
    private String productLineId;
    private String factoryId;
    private String workshopId;
    private String name;
    private String productCategory;
    private String templateId;
    private String configId;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> processSteps;
    private String status;
    private Boolean simulationEnabled;
    private String detailLevel;
    private Boolean twin3dReady;
    private Double designCapacityTPerYear;
    private Double designCapacityTPerDay;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> plannedShiftPattern;

    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getWorkshopId() { return workshopId; }
    public void setWorkshopId(String workshopId) { this.workshopId = workshopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
    public List<String> getProcessSteps() { return processSteps; }
    public void setProcessSteps(List<String> processSteps) { this.processSteps = processSteps; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getSimulationEnabled() { return simulationEnabled; }
    public void setSimulationEnabled(Boolean simulationEnabled) { this.simulationEnabled = simulationEnabled; }
    public String getDetailLevel() { return detailLevel; }
    public void setDetailLevel(String detailLevel) { this.detailLevel = detailLevel; }
    public Boolean getTwin3dReady() { return twin3dReady; }
    public void setTwin3dReady(Boolean twin3dReady) { this.twin3dReady = twin3dReady; }
    public Double getDesignCapacityTPerYear() { return designCapacityTPerYear; }
    public void setDesignCapacityTPerYear(Double designCapacityTPerYear) { this.designCapacityTPerYear = designCapacityTPerYear; }
    public Double getDesignCapacityTPerDay() { return designCapacityTPerDay; }
    public void setDesignCapacityTPerDay(Double designCapacityTPerDay) { this.designCapacityTPerDay = designCapacityTPerDay; }
    public List<String> getPlannedShiftPattern() { return plannedShiftPattern; }
    public void setPlannedShiftPattern(List<String> plannedShiftPattern) { this.plannedShiftPattern = plannedShiftPattern; }
}

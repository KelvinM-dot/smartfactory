package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "data_points")
public class DataPointDoc {

    @Id
    @Column(name = "data_point_id")
    private String dataPointId;
    private String fieldId;
    private String equipmentId;
    private String displayName;
    private String dataCategory;
    private String dataType;
    private String unit;
    private String processStepId;
    private String samplingMethod;
    private Double samplingFreqHz;
    private String priority;
    private String mandatoryFlag;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> kpiTags;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> specLimits;
    private Boolean enabled;

    public String getDataPointId() { return dataPointId; }
    public void setDataPointId(String dataPointId) { this.dataPointId = dataPointId; }
    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDataCategory() { return dataCategory; }
    public void setDataCategory(String dataCategory) { this.dataCategory = dataCategory; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getProcessStepId() { return processStepId; }
    public void setProcessStepId(String processStepId) { this.processStepId = processStepId; }
    public String getSamplingMethod() { return samplingMethod; }
    public void setSamplingMethod(String samplingMethod) { this.samplingMethod = samplingMethod; }
    public Double getSamplingFreqHz() { return samplingFreqHz; }
    public void setSamplingFreqHz(Double samplingFreqHz) { this.samplingFreqHz = samplingFreqHz; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getMandatoryFlag() { return mandatoryFlag; }
    public void setMandatoryFlag(String mandatoryFlag) { this.mandatoryFlag = mandatoryFlag; }
    public List<String> getKpiTags() { return kpiTags; }
    public void setKpiTags(List<String> kpiTags) { this.kpiTags = kpiTags; }
    public Map<String, Double> getSpecLimits() { return specLimits; }
    public void setSpecLimits(Map<String, Double> specLimits) { this.specLimits = specLimits; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

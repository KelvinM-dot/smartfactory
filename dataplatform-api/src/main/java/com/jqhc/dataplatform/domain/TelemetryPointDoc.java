package com.jqhc.dataplatform.domain;

import com.jqhc.dataplatform.config.JsonValueConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "telemetry_points")
public class TelemetryPointDoc {

    @Id
    @Column(name = "id")
    private String id;
    private String productLineId;
    private String equipmentId;
    private String processStepId;
    private String fieldId;
    private String productBatch;
    private Instant timestamp;
    @Convert(converter = JsonValueConverter.class)
    @Column(columnDefinition = "TEXT")
    private Object value;
    private String quality;
    private String dataSource;
    private String recordType;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getProcessStepId() { return processStepId; }
    public void setProcessStepId(String processStepId) { this.processStepId = processStepId; }
    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }
    public String getProductBatch() { return productBatch; }
    public void setProductBatch(String productBatch) { this.productBatch = productBatch; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
}

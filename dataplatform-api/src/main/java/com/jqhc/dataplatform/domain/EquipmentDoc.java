package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "equipment")
public class EquipmentDoc {

    @Id
    @Column(name = "equipment_id")
    private String equipmentId;
    private String factoryId;
    private String workshopId;
    private String name;
    private String equipmentType;
    private String productLineId;
    private String processStepId;
    private String manufacturer;
    private String protocol;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> connection;
    private Double ratedCapacityPerHour;
    private String capacityUnit;
    private String energyMeterId;
    private Boolean enabled;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getWorkshopId() { return workshopId; }
    public void setWorkshopId(String workshopId) { this.workshopId = workshopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public String getProcessStepId() { return processStepId; }
    public void setProcessStepId(String processStepId) { this.processStepId = processStepId; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public Map<String, Object> getConnection() { return connection; }
    public void setConnection(Map<String, Object> connection) { this.connection = connection; }
    public Double getRatedCapacityPerHour() { return ratedCapacityPerHour; }
    public void setRatedCapacityPerHour(Double ratedCapacityPerHour) { this.ratedCapacityPerHour = ratedCapacityPerHour; }
    public String getCapacityUnit() { return capacityUnit; }
    public void setCapacityUnit(String capacityUnit) { this.capacityUnit = capacityUnit; }
    public String getEnergyMeterId() { return energyMeterId; }
    public void setEnergyMeterId(String energyMeterId) { this.energyMeterId = energyMeterId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

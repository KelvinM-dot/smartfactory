package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "factories")
public class FactoryDoc {

    @Id
    @Column(name = "factory_id")
    private String factoryId;
    private String factoryName;
    private String configId;

    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getFactoryName() { return factoryName; }
    public void setFactoryName(String factoryName) { this.factoryName = factoryName; }
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
}

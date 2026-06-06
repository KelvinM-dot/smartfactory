package com.jqhc.dataplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "production_orders")
public class ProductionOrderDoc {

    @Id
    @Column(name = "production_order_id")
    private String productionOrderId;
    private String factoryId;
    private String customerOrderId;
    private String productId;
    private String productCategory;
    private String grade;
    private String recipeId;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> assignedLineIds;
    private String priority;
    private Double plannedQuantityT;
    private Double releasedQuantityT;
    private Instant dueDate;
    private String status;
    private String remark;
    private String orderType;
    private Integer deliverySlaDays;
    private String customerSegment;
    private Boolean isExport;

    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getFactoryId() { return factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    public String getCustomerOrderId() { return customerOrderId; }
    public void setCustomerOrderId(String customerOrderId) { this.customerOrderId = customerOrderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public List<String> getAssignedLineIds() { return assignedLineIds; }
    public void setAssignedLineIds(List<String> assignedLineIds) { this.assignedLineIds = assignedLineIds; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Double getPlannedQuantityT() { return plannedQuantityT; }
    public void setPlannedQuantityT(Double plannedQuantityT) { this.plannedQuantityT = plannedQuantityT; }
    public Double getReleasedQuantityT() { return releasedQuantityT; }
    public void setReleasedQuantityT(Double releasedQuantityT) { this.releasedQuantityT = releasedQuantityT; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public Integer getDeliverySlaDays() { return deliverySlaDays; }
    public void setDeliverySlaDays(Integer deliverySlaDays) { this.deliverySlaDays = deliverySlaDays; }
    public String getCustomerSegment() { return customerSegment; }
    public void setCustomerSegment(String customerSegment) { this.customerSegment = customerSegment; }
    public Boolean getIsExport() { return isExport; }
    public void setIsExport(Boolean isExport) { this.isExport = isExport; }
}

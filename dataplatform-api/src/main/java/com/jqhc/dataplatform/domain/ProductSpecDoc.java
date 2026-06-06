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
@Table(name = "products")
public class ProductSpecDoc {

    @Id
    @Column(name = "product_id")
    private String productId;
    private String displayName;
    private String productCategory;
    private String grade;
    private String spec;
    private String packageSpec;
    private String qualityStandard;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> defaultRecipeIds;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> allowedLineIds;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> mechanicalSpec;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getPackageSpec() { return packageSpec; }
    public void setPackageSpec(String packageSpec) { this.packageSpec = packageSpec; }
    public String getQualityStandard() { return qualityStandard; }
    public void setQualityStandard(String qualityStandard) { this.qualityStandard = qualityStandard; }
    public List<String> getDefaultRecipeIds() { return defaultRecipeIds; }
    public void setDefaultRecipeIds(List<String> defaultRecipeIds) { this.defaultRecipeIds = defaultRecipeIds; }
    public List<String> getAllowedLineIds() { return allowedLineIds; }
    public void setAllowedLineIds(List<String> allowedLineIds) { this.allowedLineIds = allowedLineIds; }
    public Map<String, Object> getMechanicalSpec() { return mechanicalSpec; }
    public void setMechanicalSpec(Map<String, Object> mechanicalSpec) { this.mechanicalSpec = mechanicalSpec; }
}

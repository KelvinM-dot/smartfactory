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
@Table(name = "recipes")
public class RecipeDoc {

    @Id
    @Column(name = "recipe_id")
    private String recipeId;
    private String displayName;
    private String productCategory;
    private String grade;
    private String productLineId;
    private String version;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> targetParameters;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> rawMaterialRefs;

    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Map<String, Object> getTargetParameters() { return targetParameters; }
    public void setTargetParameters(Map<String, Object> targetParameters) { this.targetParameters = targetParameters; }
    public List<Map<String, Object>> getRawMaterialRefs() { return rawMaterialRefs; }
    public void setRawMaterialRefs(List<Map<String, Object>> rawMaterialRefs) { this.rawMaterialRefs = rawMaterialRefs; }
}

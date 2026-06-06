package com.jqhc.dataplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jqhc.dataplatform.config.JqhcProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FactoryMasterDataService {

    private static final Logger log = LoggerFactory.getLogger(FactoryMasterDataService.class);

    private final JqhcProperties properties;
    private final ObjectMapper objectMapper;

    private Map<String, Object> masterData = Map.of();

    public FactoryMasterDataService(JqhcProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        reload();
    }

    /** 从磁盘重新加载 jqhc-factory-master-data.json（reseed 前必须调用）. */
    public void reload() {
        try {
            Path path = Path.of(properties.getMasterDataPath()).toAbsolutePath().normalize();
            File file = path.toFile();
            if (!file.exists()) {
                log.warn("Factory master data not found at {}", path);
                masterData = Map.of();
                return;
            }
            masterData = objectMapper.readValue(file, new TypeReference<>() {});
            log.info("Loaded factory master data from {} ({} lines, {} batches, {} equipment)",
                    path,
                    getProductLines().size(),
                    getProductBatches().size(),
                    getEquipment().size());
        } catch (Exception e) {
            log.error("Failed to load factory master data", e);
        }
    }

    public Map<String, Object> getMasterData() {
        return masterData;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProductLines() {
        Object lines = masterData.get("product_lines");
        if (lines instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEquipment() {
        Object equipment = masterData.get("equipment");
        if (equipment instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDataPoints() {
        Object points = masterData.get("data_points");
        if (points instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecipes() {
        Object recipes = masterData.get("recipes");
        if (recipes instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProducts() {
        Object products = masterData.get("products");
        if (products instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProductBatches() {
        Object batches = masterData.get("product_batches");
        if (batches instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProductionOrders() {
        Object orders = masterData.get("production_orders");
        if (orders instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMaterialBatches() {
        Object batches = masterData.get("material_batches");
        if (batches instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getInventorySnapshots() {
        Object snapshots = masterData.get("inventory_snapshots");
        if (snapshots instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSimulationDefaults() {
        Object defaults = masterData.get("simulation_defaults");
        if (defaults instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getEquipmentTelemetryProfiles() {
        Object profiles = masterData.get("equipment_telemetry_profiles");
        if (profiles instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTwinLayouts() {
        Object layouts = masterData.get("twin_layouts");
        if (layouts instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTwinFieldLabels() {
        Object labels = masterData.get("twin_field_labels");
        if (labels instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFactoryProfile() {
        Object profile = masterData.get("factory_profile");
        if (profile instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProductCatalog() {
        Object catalog = masterData.get("product_catalog");
        if (catalog instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLineRegistry() {
        Object registry = masterData.get("line_registry");
        if (registry instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return getProductLines();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAnnualProductionPlan() {
        Object plan = masterData.get("annual_production_plan");
        if (plan instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEnergyAssets() {
        Object assets = masterData.get("energy_assets");
        if (assets instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }
}

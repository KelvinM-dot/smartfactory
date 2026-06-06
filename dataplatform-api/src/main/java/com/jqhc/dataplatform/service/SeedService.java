package com.jqhc.dataplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.util.NumberUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final JqhcProperties properties;
    private final FactoryMasterDataService masterDataService;
    private final FactoryRepository factoryRepository;
    private final ProductLineRepository productLineRepository;
    private final EquipmentRepository equipmentRepository;
    private final DataPointRepository dataPointRepository;
    private final ProductBatchRepository productBatchRepository;
    private final ProductSpecRepository productSpecRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final RecipeRepository recipeRepository;
    private final MaterialInventoryRepository materialInventoryRepository;
    private final LatestStateRepository latestStateRepository;
    private final DomainConfigService domainConfigService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final List<String> CLEAR_TABLE_ORDER = List.of(
            "telemetry_points",
            "latest_state",
            "alarm_events",
            "material_events",
            "source_status",
            "quality_gate_events",
            "logistics_tasks",
            "production_orders",
            "products",
            "product_batches",
            "material_inventory",
            "data_points",
            "equipment",
            "recipes",
            "product_lines",
            "factories"
    );

    public SeedService(
            JqhcProperties properties,
            FactoryMasterDataService masterDataService,
            DomainConfigService domainConfigService,
            JdbcTemplate jdbcTemplate,
            FactoryRepository factoryRepository,
            ProductLineRepository productLineRepository,
            EquipmentRepository equipmentRepository,
            DataPointRepository dataPointRepository,
            ProductBatchRepository productBatchRepository,
            ProductSpecRepository productSpecRepository,
            ProductionOrderRepository productionOrderRepository,
            RecipeRepository recipeRepository,
            MaterialInventoryRepository materialInventoryRepository,
            LatestStateRepository latestStateRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.masterDataService = masterDataService;
        this.domainConfigService = domainConfigService;
        this.jdbcTemplate = jdbcTemplate;
        this.factoryRepository = factoryRepository;
        this.productLineRepository = productLineRepository;
        this.equipmentRepository = equipmentRepository;
        this.dataPointRepository = dataPointRepository;
        this.productBatchRepository = productBatchRepository;
        this.productSpecRepository = productSpecRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.recipeRepository = recipeRepository;
        this.materialInventoryRepository = materialInventoryRepository;
        this.latestStateRepository = latestStateRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void seedIfEmpty() {
        if (properties.isResetDbOnStartup()) {
            log.info("POC mode: resetting SQLite database (simulation — historical data discarded)");
            reseedFromMasterData();
        } else if (productLineRepository.count() > 0) {
            return;
        } else {
            log.info("Seeding SQLite from jqhc-factory-master-data.json...");
            seedFromMasterData();
        }
    }

    /**
     * 清空运行时与主数据集合，再从 jqhc-factory-master-data.json 全量灌库。
     * 供模拟器或运维接口触发「全新加载」。
     */
    public Map<String, Object> reseedFromMasterData() {
        masterDataService.reload();
        clearAll();
        seedFromMasterData();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("factory_id", factoryRepository.findAll().stream().findFirst()
                .map(FactoryDoc::getFactoryId).orElse(null));
        summary.put("product_lines", productLineRepository.count());
        summary.put("equipment", equipmentRepository.count());
        summary.put("data_points", dataPointRepository.count());
        summary.put("recipes", recipeRepository.count());
        summary.put("product_batches", productBatchRepository.count());
        summary.put("latest_state", latestStateRepository.count());
        summary.put("material_inventory", materialInventoryRepository.count());
        log.info("Reseed complete: {} lines, {} equipment, {} batches",
                summary.get("product_lines"), summary.get("equipment"), summary.get("product_batches"));
        return summary;
    }

    @Transactional
    void clearAll() {
        jdbcTemplate.execute("PRAGMA foreign_keys = OFF");
        for (String table : CLEAR_TABLE_ORDER) {
            jdbcTemplate.execute("DELETE FROM " + table);
        }
        jdbcTemplate.execute("PRAGMA foreign_keys = ON");
        log.info("Cleared {} SQLite tables via bulk DELETE", CLEAR_TABLE_ORDER.size());
    }

    private void seedFromMasterData() {
        Map<String, Object> raw = masterDataService.getMasterData();
        if (raw.isEmpty()) {
            log.warn("Master data empty, skipping seed");
            return;
        }

        FactoryDoc factory = new FactoryDoc();
        factory.setFactoryId(NumberUtils.str(raw.get("factory_id")));
        factory.setFactoryName(NumberUtils.str(raw.get("factory_name")));
        factory.setConfigId(NumberUtils.str(raw.get("config_id")));
        factoryRepository.save(factory);

        for (Map<String, Object> lineMap : masterDataService.getProductLines()) {
            ProductLineDoc line = objectMapper.convertValue(lineMap, ProductLineDoc.class);
            if (line.getProductLineId() == null) {
                line.setProductLineId(NumberUtils.str(lineMap.get("product_line_id")));
            }
            if (lineMap.get("simulation_enabled") != null) {
                line.setSimulationEnabled(Boolean.TRUE.equals(lineMap.get("simulation_enabled")));
            }
            if (lineMap.get("detail_level") != null) {
                line.setDetailLevel(NumberUtils.str(lineMap.get("detail_level")));
            }
            validateLineProcessSteps(line);
            productLineRepository.save(line);
        }

        for (Map<String, Object> eqMap : masterDataService.getEquipment()) {
            EquipmentDoc eq = objectMapper.convertValue(eqMap, EquipmentDoc.class);
            if (eq.getEquipmentId() == null) {
                eq.setEquipmentId(NumberUtils.str(eqMap.get("equipment_id")));
            }
            if (eq.getManufacturer() == null) {
                eq.setManufacturer("internal");
            }
            equipmentRepository.save(eq);
        }

        for (Map<String, Object> dpMap : masterDataService.getDataPoints()) {
            dataPointRepository.save(mapDataPoint(dpMap));
        }

        for (Map<String, Object> recipeMap : masterDataService.getRecipes()) {
            RecipeDoc recipe = objectMapper.convertValue(recipeMap, RecipeDoc.class);
            if (recipe.getRecipeId() == null) {
                recipe.setRecipeId(NumberUtils.str(recipeMap.get("recipe_id")));
            }
            recipeRepository.save(recipe);
        }

        for (Map<String, Object> productMap : masterDataService.getProducts()) {
            ProductSpecDoc product = objectMapper.convertValue(productMap, ProductSpecDoc.class);
            if (product.getProductId() == null) {
                product.setProductId(NumberUtils.str(productMap.get("product_id")));
            }
            if (productMap.get("mechanical_spec") instanceof Map<?, ?> spec) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mechanical = (Map<String, Object>) spec;
                product.setMechanicalSpec(mechanical);
            }
            productSpecRepository.save(product);
        }

        for (Map<String, Object> orderMap : masterDataService.getProductionOrders()) {
            ProductionOrderDoc order = objectMapper.convertValue(orderMap, ProductionOrderDoc.class);
            if (order.getProductionOrderId() == null) {
                order.setProductionOrderId(NumberUtils.str(orderMap.get("production_order_id")));
            }
            if (order.getDueDate() == null && orderMap.get("due_date") != null) {
                order.setDueDate(Instant.parse(NumberUtils.str(orderMap.get("due_date"))));
            }
            if (orderMap.get("order_type") != null) {
                order.setOrderType(NumberUtils.str(orderMap.get("order_type")));
            }
            if (orderMap.get("delivery_sla_days") instanceof Number n) {
                order.setDeliverySlaDays(n.intValue());
            }
            if (orderMap.get("customer_segment") != null) {
                order.setCustomerSegment(NumberUtils.str(orderMap.get("customer_segment")));
            }
            if (orderMap.get("is_export") != null) {
                order.setIsExport(Boolean.TRUE.equals(orderMap.get("is_export")));
            }
            productionOrderRepository.save(order);
        }

        for (Map<String, Object> batchMap : masterDataService.getProductBatches()) {
            ProductBatchDoc batch = objectMapper.convertValue(batchMap, ProductBatchDoc.class);
            if (batch.getBatchId() == null) {
                batch.setBatchId(NumberUtils.str(batchMap.get("batch_id")));
            }
            if (batch.getStartedAt() == null && batchMap.get("started_at") != null) {
                batch.setStartedAt(Instant.parse(NumberUtils.str(batchMap.get("started_at"))));
            }
            if (batch.getEndedAt() == null && batchMap.get("ended_at") != null) {
                batch.setEndedAt(Instant.parse(NumberUtils.str(batchMap.get("ended_at"))));
            }
            productBatchRepository.save(batch);
        }

        seedInventorySnapshots();
        seedInitialLatestState();
    }

    @SuppressWarnings("unchecked")
    private void seedInitialLatestState() {
        Map<String, Object> profiles = masterDataService.getEquipmentTelemetryProfiles();
        if (profiles.isEmpty()) {
            return;
        }

        Map<String, String> eqToLine = new HashMap<>();
        Map<String, String> eqToStep = new HashMap<>();
        for (Map<String, Object> eq : masterDataService.getEquipment()) {
            String eqId = NumberUtils.str(eq.get("equipment_id"));
            eqToLine.put(eqId, NumberUtils.str(eq.get("product_line_id")));
            eqToStep.put(eqId, NumberUtils.str(eq.get("process_step_id")));
        }

        Map<String, String> lineStatus = new HashMap<>();
        for (Map<String, Object> line : masterDataService.getProductLines()) {
            lineStatus.put(NumberUtils.str(line.get("product_line_id")), NumberUtils.str(line.get("status")));
        }

        Map<String, String> lineToBatch = new HashMap<>();
        for (Map<String, Object> batch : masterDataService.getProductBatches()) {
            if ("in_progress".equals(batch.get("status"))) {
                lineToBatch.put(NumberUtils.str(batch.get("product_line_id")), NumberUtils.str(batch.get("batch_id")));
            }
        }

        Instant now = Instant.now();
        for (Map.Entry<String, Object> entry : profiles.entrySet()) {
            String eqId = entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> profileMap)) {
                continue;
            }
            Map<String, Object> profile = (Map<String, Object>) profileMap;
            String lineId = eqToLine.get(eqId);
            String stepId = NumberUtils.str(profile.get("process_step_id"));
            if (stepId == null) {
                stepId = eqToStep.get(eqId);
            }
            String batchId = lineToBatch.get(lineId);
            String lineOperationalStatus = lineStatus.getOrDefault(lineId, "active");

            Object fieldsObj = profile.get("fields");
            if (!(fieldsObj instanceof Map<?, ?> fieldsMap)) {
                continue;
            }
            for (Map.Entry<?, ?> fieldEntry : fieldsMap.entrySet()) {
                String fieldId = NumberUtils.str(fieldEntry.getKey());
                Object spec = fieldEntry.getValue();
                Object value;
                if ("status".equals(fieldId)) {
                    value = switch (lineOperationalStatus) {
                        case "inactive" -> "STOPPED";
                        case "maintenance" -> "MANUAL";
                        default -> spec instanceof String s ? s : "RUNNING";
                    };
                } else if (spec instanceof Map<?, ?> specMap) {
                    value = specMap.get("target");
                    if (value == null && specMap.containsKey("range")) {
                        value = specMap.get("range");
                    }
                } else {
                    value = spec;
                }
                if (value == null) {
                    continue;
                }

                LatestStateDoc latest = new LatestStateDoc();
                latest.setId(LatestStateDoc.key(eqId, fieldId));
                latest.setProductLineId(lineId);
                latest.setEquipmentId(eqId);
                latest.setProcessStepId(stepId);
                latest.setFieldId(fieldId);
                latest.setProductBatch(batchId);
                latest.setTimestamp(now);
                latest.setValue(value);
                latest.setQuality("good");
                latest.setDataSource("seed");
                latestStateRepository.save(latest);
            }
        }
        log.info("Seeded initial latest_state for {} equipment profile(s)", profiles.size());
    }

    @SuppressWarnings("unchecked")
    private void seedInventorySnapshots() {
        Map<String, Object> snapshots = masterDataService.getInventorySnapshots();
        int idx = 0;

        Object rawList = snapshots.get("raw");
        if (rawList instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    MaterialInventoryDoc inv = new MaterialInventoryDoc();
                    inv.setInventoryId("raw-" + idx++);
                    inv.setInventoryClass("raw");
                    inv.setSku(NumberUtils.str(map.get("sku")));
                    inv.setDisplayName(NumberUtils.str(map.get("display_name")));
                    inv.setQuantityKg(num(map.get("total_kg")));
                    inv.setLocation(NumberUtils.str(map.get("location")));
                    inv.setStatus("available");
                    materialInventoryRepository.save(inv);
                }
            }
        }

        Object wipList = snapshots.get("wip");
        if (wipList instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    MaterialInventoryDoc inv = new MaterialInventoryDoc();
                    inv.setInventoryId(NumberUtils.str(map.get("wip_id")));
                    inv.setInventoryClass("wip");
                    inv.setProductBatch(NumberUtils.str(map.get("product_batch")));
                    inv.setProcessStepId(NumberUtils.str(map.get("process_step_id")));
                    inv.setQuantityKg(num(map.get("quantity_kg")));
                    inv.setLocation(NumberUtils.str(map.get("location")));
                    inv.setProductLineId(extractLineFromLocation(NumberUtils.str(map.get("location"))));
                    inv.setStatus("in_process");
                    materialInventoryRepository.save(inv);
                }
            }
        }

        Object finishedList = snapshots.get("finished");
        if (finishedList instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    MaterialInventoryDoc inv = new MaterialInventoryDoc();
                    inv.setInventoryId("fg-" + NumberUtils.str(map.get("batch_id")));
                    inv.setInventoryClass("finished");
                    inv.setBatchId(NumberUtils.str(map.get("batch_id")));
                    inv.setProductBatch(NumberUtils.str(map.get("batch_id")));
                    inv.setDisplayName(NumberUtils.str(map.get("grade")));
                    inv.setQuantityKg(num(map.get("quantity_kg")));
                    inv.setLocation(NumberUtils.str(map.get("location")));
                    inv.setStatus(NumberUtils.str(map.get("status")));
                    materialInventoryRepository.save(inv);
                }
            }
        }
    }

    private DataPointDoc mapDataPoint(Map<String, Object> dpMap) {
        DataPointDoc dp = new DataPointDoc();
        dp.setDataPointId(NumberUtils.str(dpMap.get("data_point_id")));
        dp.setFieldId(NumberUtils.str(dpMap.getOrDefault("field_id", dpMap.get("data_point_id"))));
        dp.setEquipmentId(NumberUtils.str(dpMap.get("equipment_id")));
        dp.setDisplayName(NumberUtils.str(dpMap.get("display_name")));
        dp.setDataCategory(NumberUtils.str(dpMap.get("data_category")));
        dp.setDataType(NumberUtils.str(dpMap.getOrDefault("data_type", "float")));
        dp.setUnit(NumberUtils.str(dpMap.get("unit")));
        dp.setProcessStepId(NumberUtils.str(dpMap.get("process_step_id")));
        dp.setSamplingMethod(NumberUtils.str(dpMap.get("sampling_method")));
        if (dpMap.get("sampling_freq_hz") instanceof Number n) {
            dp.setSamplingFreqHz(n.doubleValue());
        }
        dp.setPriority(NumberUtils.str(dpMap.get("priority")));
        dp.setMandatoryFlag(NumberUtils.str(dpMap.get("mandatory_flag")));
        if (dpMap.get("kpi_tags") instanceof List<?> tags) {
            dp.setKpiTags(tags.stream().map(String::valueOf).toList());
        }
        dp.setSpecLimits(normalizeSpecLimits(dpMap.get("spec_limits")));
        dp.setEnabled(dpMap.get("enabled") == null || Boolean.TRUE.equals(dpMap.get("enabled")));
        return dp;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> normalizeSpecLimits(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = NumberUtils.str(entry.getKey());
            Object val = entry.getValue();
            if (key == null || val == null) {
                continue;
            }
            if ("range".equals(key) && val instanceof List<?> list && list.size() >= 2) {
                if (list.get(0) instanceof Number n0) {
                    result.put("lsl", n0.doubleValue());
                }
                if (list.get(1) instanceof Number n1) {
                    result.put("usl", n1.doubleValue());
                }
            } else if (val instanceof Number n) {
                result.put(key, n.doubleValue());
            }
        }
        return result.isEmpty() ? null : result;
    }

    private void validateLineProcessSteps(ProductLineDoc line) {
        String templateId = line.getTemplateId();
        if (templateId == null || templateId.isBlank()) {
            return;
        }
        List<String> templateSteps = domainConfigService.getTemplateStepIds(templateId);
        if (templateSteps.isEmpty()) {
            log.warn("产线 {} 模板 {} 在域配置中未找到工序链", line.getProductLineId(), templateId);
            return;
        }
        List<String> processSteps = line.getProcessSteps() != null ? line.getProcessSteps() : List.of();
        Set<String> allowed = new LinkedHashSet<>(templateSteps);
        List<String> extra = processSteps.stream().filter(s -> !allowed.contains(s)).toList();
        if (!extra.isEmpty()) {
            log.error("产线 {} process_steps 含模板 {} 未声明工序: {}",
                    line.getProductLineId(), templateId, extra);
        }
        if (!new LinkedHashSet<>(processSteps).equals(allowed) && extra.isEmpty()) {
            List<String> missing = templateSteps.stream()
                    .filter(s -> !processSteps.contains(s))
                    .toList();
            if (!missing.isEmpty()) {
                log.warn("产线 {} 工序链缺少模板 {} 步骤: {}",
                        line.getProductLineId(), templateId, missing);
            }
        }
    }

    private static String extractLineFromLocation(String location) {
        if (location == null || !location.contains("/")) {
            return null;
        }
        return location.split("/")[0];
    }

    private static Double num(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }
}

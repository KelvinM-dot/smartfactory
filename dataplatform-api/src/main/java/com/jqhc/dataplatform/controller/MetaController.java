package com.jqhc.dataplatform.controller;

import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.service.DomainConfigService;
import com.jqhc.dataplatform.service.FactoryMasterDataService;
import com.jqhc.dataplatform.service.ProductionOrderCommandService;
import com.jqhc.dataplatform.service.TwinLayoutService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/meta")
public class MetaController {

    private final DomainConfigService domainConfigService;
    private final FactoryMasterDataService masterDataService;
    private final TwinLayoutService twinLayoutService;
    private final FactoryRepository factoryRepository;
    private final ProductLineRepository productLineRepository;
    private final EquipmentRepository equipmentRepository;
    private final DataPointRepository dataPointRepository;
    private final ProductBatchRepository productBatchRepository;
    private final ProductSpecRepository productSpecRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final RecipeRepository recipeRepository;
    private final ProductionOrderCommandService productionOrderCommandService;
    private final MaterialInventoryRepository materialInventoryRepository;

    public MetaController(
            DomainConfigService domainConfigService,
            FactoryMasterDataService masterDataService,
            TwinLayoutService twinLayoutService,
            FactoryRepository factoryRepository,
            ProductLineRepository productLineRepository,
            EquipmentRepository equipmentRepository,
            DataPointRepository dataPointRepository,
            ProductBatchRepository productBatchRepository,
            ProductSpecRepository productSpecRepository,
            ProductionOrderRepository productionOrderRepository,
            RecipeRepository recipeRepository,
            ProductionOrderCommandService productionOrderCommandService,
            MaterialInventoryRepository materialInventoryRepository) {
        this.domainConfigService = domainConfigService;
        this.masterDataService = masterDataService;
        this.twinLayoutService = twinLayoutService;
        this.factoryRepository = factoryRepository;
        this.productLineRepository = productLineRepository;
        this.equipmentRepository = equipmentRepository;
        this.dataPointRepository = dataPointRepository;
        this.productBatchRepository = productBatchRepository;
        this.productSpecRepository = productSpecRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.recipeRepository = recipeRepository;
        this.productionOrderCommandService = productionOrderCommandService;
        this.materialInventoryRepository = materialInventoryRepository;
    }

    @GetMapping("/domain-config")
    public Map<String, Object> domainConfig() {
        return domainConfigService.getConfig();
    }

    @GetMapping("/factory")
    public Map<String, Object> factory() {
        Map<String, Object> result = new LinkedHashMap<>();
        factoryRepository.findAll().stream().findFirst().ifPresentOrElse(f -> {
            result.put("factory_id", f.getFactoryId());
            result.put("factory_name", f.getFactoryName());
            result.put("config_id", f.getConfigId());
        }, () -> {
            Map<String, Object> raw = masterDataService.getMasterData();
            result.put("factory_id", raw.get("factory_id"));
            result.put("factory_name", raw.get("factory_name"));
            result.put("config_id", raw.get("config_id"));
        });
        result.put("product_lines", productLineRepository.findAll());
        result.put("products", productSpecRepository.findAll());
        result.put("production_orders", productionOrderRepository.findAll());
        result.put("simulation_defaults", masterDataService.getSimulationDefaults());
        result.put("factory_profile", masterDataService.getFactoryProfile());
        result.put("product_catalog", masterDataService.getProductCatalog());
        result.put("line_registry", masterDataService.getLineRegistry());
        result.put("annual_production_plan", masterDataService.getAnnualProductionPlan());
        result.put("energy_assets", masterDataService.getEnergyAssets());
        result.put("registry_line_count", masterDataService.getLineRegistry().size());
        result.put("simulation_line_count", masterDataService.getProductLines().stream()
                .filter(l -> Boolean.TRUE.equals(l.get("simulation_enabled"))).count());
        return result;
    }

    @GetMapping("/products")
    public List<ProductSpecDoc> products() {
        return productSpecRepository.findAll();
    }

    @GetMapping("/orders")
    public List<ProductionOrderDoc> orders() {
        return productionOrderRepository.findAll();
    }

    @PostMapping("/orders")
    public ProductionOrderDoc createOrder(@RequestBody Map<String, Object> payload) {
        return productionOrderCommandService.createOrder(payload);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/orders/{orderId}")
    public ProductionOrderDoc patchOrder(
            @org.springframework.web.bind.annotation.PathVariable String orderId,
            @RequestBody Map<String, Object> payload) {
        return productionOrderCommandService.patchOrderStatus(orderId, payload);
    }

    @GetMapping("/lines")
    public List<ProductLineDoc> lines() {
        return productLineRepository.findAll();
    }

    @GetMapping("/lines/{lineId}/equipment")
    public List<EquipmentDoc> equipment(@PathVariable String lineId) {
        return equipmentRepository.findByProductLineId(lineId);
    }

    @GetMapping("/datapoints")
    public List<DataPointDoc> dataPoints(@RequestParam(value = "line_id", required = false) String lineId) {
        if (lineId == null) {
            return dataPointRepository.findAll();
        }
        return equipmentRepository.findByProductLineId(lineId).stream()
                .flatMap(eq -> dataPointRepository.findByEquipmentId(eq.getEquipmentId()).stream())
                .toList();
    }

    @GetMapping("/batches")
    public List<ProductBatchDoc> batches(@RequestParam(value = "line_id", required = false) String lineId) {
        if (lineId == null || lineId.isBlank()) {
            return productBatchRepository.findAll().stream()
                    .sorted(java.util.Comparator.comparing(ProductBatchDoc::getStartedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .toList();
        }
        return productBatchRepository.findByProductLineIdOrderByStartedAtDesc(lineId);
    }

    @GetMapping("/recipes")
    public List<RecipeDoc> recipes(@RequestParam(value = "line_id", required = false) String lineId) {
        if (lineId == null) {
            return recipeRepository.findAll();
        }
        return recipeRepository.findByProductLineId(lineId);
    }

    @GetMapping("/inventory")
    public Map<String, Object> inventory(
            @RequestParam(value = "line_id", required = false) String lineId) {
        List<MaterialInventoryDoc> all = materialInventoryRepository.findAll();
        List<MaterialInventoryDoc> filtered = lineId == null ? all : all.stream()
                .filter(i -> lineId.equals(i.getProductLineId()) || i.getProductLineId() == null)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("raw", filtered.stream().filter(i -> "raw".equals(i.getInventoryClass())).toList());
        result.put("wip", filtered.stream().filter(i -> "wip".equals(i.getInventoryClass())).toList());
        result.put("finished", filtered.stream().filter(i -> "finished".equals(i.getInventoryClass())).toList());
        result.put("material_batches", masterDataService.getMaterialBatches());
        return result;
    }

    @GetMapping("/material-batches")
    public List<Map<String, Object>> materialBatches() {
        return masterDataService.getMaterialBatches();
    }

    /** 全部产线 2D/3D 孪生布局（与 master-data twin_layouts 一致） */
    @GetMapping("/twin-layouts")
    public Map<String, Object> twinLayouts() {
        return twinLayoutService.getAllLayoutsForFrontend();
    }

    /** 单产线孪生布局 */
    @GetMapping("/lines/{lineId}/twin-layout")
    public Map<String, Object> twinLayout(@PathVariable String lineId) {
        Map<String, Object> layout = twinLayoutService.getLayoutForLine(lineId);
        if (layout.isEmpty()) {
            return Map.of();
        }
        return layout;
    }
}

package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 工厂驾驶舱聚合查询：单次请求替代前端 120+ 次 HTTP 拼装。
 */
@Service
public class FactoryDashboardService {

    private final FactoryRepository factoryRepository;
    private final ProductLineRepository productLineRepository;
    private final EquipmentRepository equipmentRepository;
    private final DataPointRepository dataPointRepository;
    private final FactoryMasterDataService masterDataService;
    private final ComputeService computeService;

    public FactoryDashboardService(
            FactoryRepository factoryRepository,
            ProductLineRepository productLineRepository,
            EquipmentRepository equipmentRepository,
            DataPointRepository dataPointRepository,
            FactoryMasterDataService masterDataService,
            ComputeService computeService) {
        this.factoryRepository = factoryRepository;
        this.productLineRepository = productLineRepository;
        this.equipmentRepository = equipmentRepository;
        this.dataPointRepository = dataPointRepository;
        this.masterDataService = masterDataService;
        this.computeService = computeService;
    }

    public Map<String, Object> getDashboard(String factoryId) {
        String fid = NumberUtils.resolveFactoryId(factoryId, "JQHC-PLANT-01");
        Map<String, Object> factory = buildFactoryHeader(fid);
        List<ProductLineDoc> lines = productLineRepository.findAll();

        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        Map<String, Object> kpis = computeService.getFactoryKpis(fid);
        List<Map<String, Object>> orderRisk = computeService.getOrderRiskList(fid);
        List<Map<String, Object>> logisticsTasks = computeService.getLogisticsTasks(fid, null);
        List<Map<String, Object>> qualityGates = computeService.getQualityGateEvents(fid, null, null);

        List<Map<String, Object>> lineCards = new ArrayList<>();
        for (ProductLineDoc line : lines) {
            String lineId = line.getProductLineId();
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("line", line);
            try {
                card.put("overview", computeService.getLineOverview(lineId));
                card.put("equipment", buildEquipmentWithLatest(lineId));
                card.put("dataPoints", buildDataPoints(lineId));
                card.put("error", null);
            } catch (Exception e) {
                card.put("overview", null);
                card.put("equipment", List.of());
                card.put("dataPoints", List.of());
                card.put("error", e.getMessage() != null ? e.getMessage() : "overview_unavailable");
            }
            lineCards.add(card);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory", factory);
        result.put("lines", lineCards);
        result.put("energy", energy);
        result.put("kpis", kpis);
        result.put("orderRisk", orderRisk);
        result.put("logisticsTasks", logisticsTasks);
        result.put("qualityGates", qualityGates);
        result.put("computed_at", Instant.now());
        return result;
    }

    private Map<String, Object> buildFactoryHeader(String factoryId) {
        Map<String, Object> result = new LinkedHashMap<>();
        factoryRepository.findAll().stream().findFirst().ifPresentOrElse(f -> {
            result.put("factory_id", f.getFactoryId());
            result.put("factory_name", f.getFactoryName());
            result.put("config_id", f.getConfigId());
        }, () -> {
            Map<String, Object> raw = masterDataService.getMasterData();
            result.put("factory_id", raw.getOrDefault("factory_id", factoryId));
            result.put("factory_name", raw.getOrDefault("factory_name", "金桥焊材科技公司"));
            result.put("config_id", raw.get("config_id"));
        });
        result.put("product_lines", productLineRepository.findAll());
        result.put("factory_profile", masterDataService.getFactoryProfile());
        result.put("simulation_defaults", masterDataService.getSimulationDefaults());
        result.put("registry_line_count", masterDataService.getLineRegistry().size());
        result.put("simulation_line_count", masterDataService.getProductLines().stream()
                .filter(l -> Boolean.TRUE.equals(l.get("simulation_enabled"))).count());
        return result;
    }

    private List<Map<String, Object>> buildEquipmentWithLatest(String lineId) {
        List<EquipmentDoc> equipment = equipmentRepository.findByProductLineId(lineId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EquipmentDoc eq : equipment) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("equipment_id", eq.getEquipmentId());
            row.put("name", eq.getName());
            row.put("product_line_id", eq.getProductLineId());
            row.put("process_step_id", eq.getProcessStepId());
            row.put("equipment_type", eq.getEquipmentType());
            row.put("latest", computeService.getEquipmentLatest(eq.getEquipmentId()));
            rows.add(row);
        }
        return rows;
    }

    private List<DataPointDoc> buildDataPoints(String lineId) {
        return equipmentRepository.findByProductLineId(lineId).stream()
                .flatMap(eq -> dataPointRepository.findByEquipmentId(eq.getEquipmentId()).stream())
                .toList();
    }
}

package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.service.optimization.OptimizationBalanceService;
import com.jqhc.dataplatform.service.optimization.OptimizationContextService;
import com.jqhc.dataplatform.service.optimization.OptimizationEnergyCarbonService;
import com.jqhc.dataplatform.service.optimization.OptimizationLineContext;
import com.jqhc.dataplatform.service.optimization.OptimizationModelEngine;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智优决策中心 — 全厂 42 条产线优化模型编排层。
 * 上下文与模型计算分别委托 {@link OptimizationContextService}、{@link OptimizationModelEngine}。
 */
@Service
public class OptimizationService {

    private static final Set<String> KNOWN_WORKSHOPS = Set.of("WS-WIRE-01", "WS-ROD-01");

    private final OptimizationContextService contextService;
    private final OptimizationModelEngine modelEngine;
    private final OptimizationBalanceService balanceService;
    private final OptimizationEnergyCarbonService energyCarbonService;
    private final FactoryMasterDataService masterDataService;
    private final ComputeService computeService;
    private final SimulatorProxyService simulatorProxyService;

    public OptimizationService(
            OptimizationContextService contextService,
            OptimizationModelEngine modelEngine,
            OptimizationBalanceService balanceService,
            OptimizationEnergyCarbonService energyCarbonService,
            FactoryMasterDataService masterDataService,
            ComputeService computeService,
            SimulatorProxyService simulatorProxyService) {
        this.contextService = contextService;
        this.modelEngine = modelEngine;
        this.balanceService = balanceService;
        this.energyCarbonService = energyCarbonService;
        this.masterDataService = masterDataService;
        this.computeService = computeService;
        this.simulatorProxyService = simulatorProxyService;
    }

    // ── 公开入口 ──────────────────────────────────────────────

    public Map<String, Object> getOverview(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> profile = masterDataService.getFactoryProfile();
        Map<String, Object> kpis = computeService.getFactoryKpis(fid);
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);

        Map<String, Object> efficiency = modelEngine.buildEfficiencyModels(lines, fid);
        Map<String, Object> cost = modelEngine.buildCostModels(lines, fid, energy);
        Map<String, Object> quality = modelEngine.buildQualityModels(lines, fid);

        double effIdx = contextService.compositeIndex(efficiency);
        double costIdx = contextService.compositeIndex(cost);
        double qualIdx = contextService.compositeIndex(quality);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("factory_name", masterDataService.getMasterData().getOrDefault("factory_name", "金桥焊材科技公司"));
        result.put("computed_at", Instant.now());
        result.put("plant_layout", contextService.buildPlantLayout(profile, lines));
        result.put("three_extremes", Map.of(
                "efficiency_index", NumberUtils.round1(effIdx),
                "cost_index", NumberUtils.round1(costIdx),
                "quality_index", NumberUtils.round1(qualIdx),
                "efficiency_label", contextService.indexLabel(effIdx),
                "cost_label", contextService.indexLabel(costIdx),
                "quality_label", contextService.indexLabel(qualIdx)
        ));
        result.put("headline_kpis", contextService.buildHeadlineKpis(kpis, energy, lines));
        result.put("workshops", contextService.summarizeWorkshops(lines));
        result.put("models", contextService.listModelStatus(efficiency, cost, quality));
        result.put("recommendations", contextService.collectRecommendations(efficiency, cost, quality, 8));
        long telemetryCount = lines.stream().filter(OptimizationLineContext::telemetry).count();
        long registryCount = lines.size() - telemetryCount;
        String hint = registryCount > 0
                ? "全厂 " + lines.size() + " 线（仿真 " + telemetryCount + " · 估算 " + registryCount + "）；登记线结论标注为估算。"
                : "全厂 " + lines.size() + " 条产线全链路仿真；拖动排产/批次/工艺杠杆时三极致指数此消彼长。";
        result.put("tradeoff_hint", hint);
        return result;
    }

    public Map<String, Object> getWorkshops(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("computed_at", Instant.now());
        result.put("plant_layout", contextService.buildPlantLayout(masterDataService.getFactoryProfile(), lines));
        result.put("workshops", lines.stream()
                .collect(Collectors.groupingBy(lc -> lc.line().getWorkshopId()))
                .entrySet().stream()
                .filter(e -> KNOWN_WORKSHOPS.contains(e.getKey()))
                .map(e -> contextService.buildWorkshopDetail(e.getKey(), e.getValue(), energy))
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("workshop_id"))))
                .toList());
        return result;
    }

    public Map<String, Object> getLineSnapshots(String factoryId, String workshopId) {
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        if (workshopId != null && !workshopId.isBlank()) {
            lines = lines.stream()
                    .filter(lc -> workshopId.equals(lc.line().getWorkshopId()))
                    .toList();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", contextService.resolveFactoryId(factoryId));
        result.put("workshop_id", workshopId);
        result.put("computed_at", Instant.now());
        result.put("line_count", lines.size());
        result.put("lines", lines.stream().map(contextService::buildLineSnapshot).toList());
        return result;
    }

    public Map<String, Object> getEfficiency(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> models = modelEngine.buildEfficiencyModels(lines, fid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("computed_at", Instant.now());
        result.put("dimension", "efficiency");
        result.put("plant_layout", contextService.buildPlantLayout(masterDataService.getFactoryProfile(), lines));
        result.put("line_summary", contextService.buildLineSummary(lines));
        result.put("workshops", contextService.summarizeWorkshops(lines));
        result.put("models", models);
        result.put("recommendations", contextService.collectRecommendations(models, Map.of(), Map.of(), 10));
        return result;
    }

    public Map<String, Object> getCost(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        Map<String, Object> models = modelEngine.buildCostModels(lines, fid, energy);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("computed_at", Instant.now());
        result.put("dimension", "cost");
        result.put("plant_layout", contextService.buildPlantLayout(masterDataService.getFactoryProfile(), lines));
        result.put("line_summary", contextService.buildLineSummary(lines));
        result.put("workshops", contextService.summarizeWorkshops(lines));
        result.put("models", models);
        result.put("recommendations", contextService.collectRecommendations(Map.of(), models, Map.of(), 10));
        return result;
    }

    public Map<String, Object> getQuality(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> models = modelEngine.buildQualityModels(lines, fid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("computed_at", Instant.now());
        result.put("dimension", "quality");
        result.put("plant_layout", contextService.buildPlantLayout(masterDataService.getFactoryProfile(), lines));
        result.put("line_summary", contextService.buildLineSummary(lines));
        result.put("workshops", contextService.summarizeWorkshops(lines));
        result.put("models", models);
        result.put("recommendations", contextService.collectRecommendations(Map.of(), Map.of(), models, 10));
        return result;
    }

    public List<Map<String, Object>> getScenarios() {
        Map<String, Object> defaults = masterDataService.getSimulationDefaults();
        Object scenarios = defaults.get("scenarios");
        if (scenarios instanceof List<?> list) {
            return list.stream()
                    .filter(o -> o instanceof Map<?, ?>)
                    .map(o -> {
                        Map<String, Object> m = new LinkedHashMap<>((Map<String, Object>) o);
                        m.put("simulator_ready", true);
                        return m;
                    })
                    .toList();
        }
        return List.of();
    }

    public List<Map<String, Object>> getRecommendations(String factoryId, int limit) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        return contextService.collectRecommendations(
                modelEngine.buildEfficiencyModels(lines, fid),
                modelEngine.buildCostModels(lines, fid, energy),
                modelEngine.buildQualityModels(lines, fid),
                limit);
    }

    public Map<String, Object> getKpiSnapshot(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> kpis = computeService.getFactoryKpis(fid);
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        Map<String, Object> efficiency = modelEngine.buildEfficiencyModels(lines, fid);
        Map<String, Object> cost = modelEngine.buildCostModels(lines, fid, energy);
        Map<String, Object> quality = modelEngine.buildQualityModels(lines, fid);
        double effIdx = contextService.compositeIndex(efficiency);
        double costIdx = contextService.compositeIndex(cost);
        double qualIdx = contextService.compositeIndex(quality);

        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("factory_id", fid);
        snap.put("captured_at", Instant.now());
        snap.put("three_extremes", Map.of(
                "efficiency_index", NumberUtils.round1(effIdx),
                "cost_index", NumberUtils.round1(costIdx),
                "quality_index", NumberUtils.round1(qualIdx)
        ));
        snap.put("headline_kpis", contextService.buildHeadlineKpis(kpis, energy, lines));
        snap.put("line_summary", contextService.buildLineSummary(lines));
        Map<String, Object> simStatus = simulatorProxyService.getStatus();
        if (simStatus.get("scenario_id") != null) {
            snap.put("scenario_id", simStatus.get("scenario_id"));
        }
        snap.put("simulator_running", Boolean.TRUE.equals(simStatus.get("running")));
        return snap;
    }

    public Map<String, Object> applySimulatorScenario(String scenarioId, String factoryId) {
        return applySimulatorScenario(scenarioId, factoryId, null);
    }

    public Map<String, Object> applySimulatorScenario(String scenarioId, String factoryId, Double greenShiftPct) {
        String fid = contextService.resolveFactoryId(factoryId);
        Map<String, Object> baseline = getKpiSnapshot(fid);
        Map<String, Object> simResult = simulatorProxyService.applyScenario(scenarioId, greenShiftPct);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("target_scenario_id", scenarioId);
        result.put("baseline", baseline);
        result.put("simulator", simResult);
        result.put("simulator_available", simulatorProxyService.isAvailable());
        result.put("poll_hint_sec", 4);
        result.put("message", "场景已切换，请等待 " + 4 + " 秒后拉取 /v1/opt/kpi-snapshot 对比 KPI");
        return result;
    }

    public Map<String, Object> compareSnapshots(Map<String, Object> baseline, Map<String, Object> after) {
        Map<String, Object> delta = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> bExt = baseline.get("three_extremes") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> aExt = after.get("three_extremes") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        for (String key : List.of("efficiency_index", "cost_index", "quality_index")) {
            delta.put(key, NumberUtils.round1(NumberUtils.toDouble(aExt.get(key)) - NumberUtils.toDouble(bExt.get(key))));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> bHead = baseline.get("headline_kpis") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> aHead = after.get("headline_kpis") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        Map<String, Object> headlineDelta = new LinkedHashMap<>();
        for (String key : List.of("avg_oee_pct", "quality_pass_rate_pct", "green_power_ratio_pct", "high_risk_orders", "active_lines", "telemetry_lines")) {
            if (bHead.containsKey(key) || aHead.containsKey(key)) {
                headlineDelta.put(key, NumberUtils.round1(NumberUtils.toDouble(aHead.get(key)) - NumberUtils.toDouble(bHead.get(key))));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseline", baseline);
        result.put("after", after);
        result.put("delta_extremes", delta);
        result.put("delta_headline", headlineDelta);
        result.put("scenario_from", baseline.get("scenario_id"));
        result.put("scenario_to", after.get("scenario_id"));
        result.put("computed_at", Instant.now());
        return result;
    }

    public Map<String, Object> getSimulatorStatus() {
        Map<String, Object> status = simulatorProxyService.getStatus();
        status.put("simulator_available", simulatorProxyService.isAvailable());
        return status;
    }

    public Map<String, Object> analyzeBalance(String factoryId, Map<String, Object> params) {
        String fid = contextService.resolveFactoryId(factoryId);
        Map<String, Object> snapshot = getKpiSnapshot(fid);
        Map<String, Object> result = balanceService.analyzeBalance(snapshot, params);
        result.put("factory_id", fid);
        return result;
    }

    public Map<String, Object> getEnergyCarbonOverview(String factoryId) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        Map<String, Object> m05 = modelEngine.model05Energy(lines, energy);
        Map<String, Object> lineSummary = contextService.buildLineSummary(lines);
        return energyCarbonService.buildOverview(fid, energy, m05, lines, lineSummary);
    }

    public Map<String, Object> projectEnergyCarbonGreenShift(String factoryId, double greenShiftPct) {
        String fid = contextService.resolveFactoryId(factoryId);
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        Map<String, Object> snapshot = getKpiSnapshot(fid);
        Map<String, Object> result = energyCarbonService.projectGreenShift(energy, snapshot, greenShiftPct);
        result.put("factory_id", fid);
        return result;
    }

    public Map<String, Object> getEnergyCarbonBatches(String factoryId, int limit) {
        String fid = contextService.resolveFactoryId(factoryId);
        List<OptimizationLineContext> lines = contextService.loadOptimizationLineContexts();
        Map<String, Object> energy = computeService.getFactoryEnergy(fid);
        Map<String, Object> footprints = energyCarbonService.buildBatchFootprints(energy, lines, limit);
        footprints.put("factory_id", fid);
        return footprints;
    }

    public Map<String, Object> applyEnergyCarbonScenario(
            String factoryId, String scenarioId, Double greenShiftPct) {
        String fid = contextService.resolveFactoryId(factoryId);
        String targetScenario = scenarioId != null && !scenarioId.isBlank()
                ? scenarioId
                : (greenShiftPct != null
                ? energyCarbonService.resolveGreenShiftScenarioId(greenShiftPct)
                : "plant_green_shift_80");

        Map<String, Object> baseline = getKpiSnapshot(fid);
        Map<String, Object> simResult = simulatorProxyService.applyScenario(targetScenario, greenShiftPct);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", fid);
        result.put("target_scenario_id", targetScenario);
        result.put("green_shift_pct", greenShiftPct);
        result.put("baseline", baseline);
        result.put("simulator", simResult);
        result.put("simulator_available", simulatorProxyService.isAvailable());
        result.put("poll_hint_sec", 4);
        result.put("message", "能碳场景已切换，请等待约 4 秒后刷新 KPI 或前往场景实验室对比");
        return result;
    }
}

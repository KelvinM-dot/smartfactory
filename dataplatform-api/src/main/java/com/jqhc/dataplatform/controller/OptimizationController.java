package com.jqhc.dataplatform.controller;

import com.jqhc.dataplatform.service.OptimizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智优决策中心 — 独立 API 命名空间，不侵入现有 /v1/state、/v1/compute 等接口。
 */
@RestController
@RequestMapping("/v1/opt")
public class OptimizationController {

    private final OptimizationService optimizationService;

    public OptimizationController(OptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getOverview(factoryId);
    }

    @GetMapping("/workshops")
    public Map<String, Object> workshops(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getWorkshops(factoryId);
    }

    @GetMapping("/lines")
    public Map<String, Object> lines(
            @RequestParam(value = "factory_id", required = false) String factoryId,
            @RequestParam(value = "workshop_id", required = false) String workshopId) {
        return optimizationService.getLineSnapshots(factoryId, workshopId);
    }

    @GetMapping("/efficiency")
    public Map<String, Object> efficiency(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getEfficiency(factoryId);
    }

    @GetMapping("/cost")
    public Map<String, Object> cost(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getCost(factoryId);
    }

    @GetMapping("/quality")
    public Map<String, Object> quality(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getQuality(factoryId);
    }

    @GetMapping("/scenarios")
    public List<Map<String, Object>> scenarios() {
        return optimizationService.getScenarios();
    }

    @GetMapping("/recommendations")
    public List<Map<String, Object>> recommendations(
            @RequestParam(value = "factory_id", required = false) String factoryId,
            @RequestParam(value = "limit", defaultValue = "15") int limit) {
        return optimizationService.getRecommendations(factoryId, limit);
    }

    @GetMapping("/kpi-snapshot")
    public Map<String, Object> kpiSnapshot(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getKpiSnapshot(factoryId);
    }

    @GetMapping("/sim/status")
    public Map<String, Object> simStatus() {
        return optimizationService.getSimulatorStatus();
    }

    @PostMapping("/sim/scenario")
    public Map<String, Object> applyScenario(
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        String scenarioId = String.valueOf(body.getOrDefault("scenario_id", "normal_shift"));
        Double greenShiftPct = null;
        if (body.get("green_shift_pct") != null) {
            greenShiftPct = Double.valueOf(String.valueOf(body.get("green_shift_pct")));
        }
        return optimizationService.applySimulatorScenario(scenarioId, factoryId, greenShiftPct);
    }

    @PostMapping("/scenario/compare")
    public Map<String, Object> compareScenario(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> baseline = body.get("baseline") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> after = body.get("after") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        return optimizationService.compareSnapshots(baseline, after);
    }

    @PostMapping("/balance")
    public Map<String, Object> balance(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.analyzeBalance(factoryId, body != null ? body : Map.of());
    }

    @GetMapping("/balance")
    public Map<String, Object> balanceDefault(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.analyzeBalance(factoryId, Map.of());
    }

    @GetMapping("/energy-carbon/overview")
    public Map<String, Object> energyCarbonOverview(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.getEnergyCarbonOverview(factoryId);
    }

    @GetMapping("/energy-carbon/green-shift")
    public Map<String, Object> energyCarbonGreenShift(
            @RequestParam(value = "green_shift_pct", defaultValue = "50") double greenShiftPct,
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return optimizationService.projectEnergyCarbonGreenShift(factoryId, greenShiftPct);
    }

    @GetMapping("/energy-carbon/batches")
    public Map<String, Object> energyCarbonBatches(
            @RequestParam(value = "factory_id", required = false) String factoryId,
            @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return optimizationService.getEnergyCarbonBatches(factoryId, limit);
    }

    @PostMapping("/energy-carbon/apply-scenario")
    public Map<String, Object> energyCarbonApplyScenario(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        String scenarioId = body != null ? String.valueOf(body.getOrDefault("scenario_id", "")) : "";
        Double greenShiftPct = null;
        if (body != null && body.get("green_shift_pct") != null) {
            greenShiftPct = Double.valueOf(String.valueOf(body.get("green_shift_pct")));
        }
        if (scenarioId.isBlank() && greenShiftPct == null) {
            scenarioId = "plant_green_shift_80";
        }
        return optimizationService.applyEnergyCarbonScenario(factoryId, scenarioId, greenShiftPct);
    }
}

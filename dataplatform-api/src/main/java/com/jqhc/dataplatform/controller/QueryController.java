package com.jqhc.dataplatform.controller;

import com.jqhc.dataplatform.service.ComputeService;
import com.jqhc.dataplatform.service.FactoryDashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class QueryController {

    private final ComputeService computeService;
    private final FactoryDashboardService factoryDashboardService;

    public QueryController(ComputeService computeService, FactoryDashboardService factoryDashboardService) {
        this.computeService = computeService;
        this.factoryDashboardService = factoryDashboardService;
    }

    @GetMapping("/state/lines/{lineId}/overview")
    public Map<String, Object> lineOverview(@PathVariable String lineId) {
        return computeService.getLineOverview(lineId);
    }

    @GetMapping("/state/lines/{lineId}/steps/{stepId}")
    public Map<String, Object> stepDetail(@PathVariable String lineId, @PathVariable String stepId) {
        return computeService.getStepDetail(lineId, stepId);
    }

    @GetMapping("/state/equipment/{equipmentId}/latest")
    public Map<String, Object> equipmentLatest(@PathVariable String equipmentId) {
        return computeService.getEquipmentLatest(equipmentId);
    }

    @GetMapping("/trends")
    public Map<String, Object> trends(
            @RequestParam("line_id") String lineId,
            @RequestParam(value = "field_ids", defaultValue = "fill_ratio_pct,coating_thickness_top_um") String fieldIds,
            @RequestParam(value = "range", defaultValue = "buffer") String range,
            @RequestParam(value = "batch_id", required = false) String batchId) {
        List<String> ids = Arrays.stream(fieldIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return computeService.getTrends(lineId, ids, range, batchId);
    }

    @GetMapping("/compute/batches/{batchId}/timeline")
    public Map<String, Object> batchTimeline(@PathVariable String batchId) {
        return computeService.getBatchTimeline(batchId);
    }

    @GetMapping("/alarms")
    public List<Map<String, Object>> alarms(
            @RequestParam(value = "line_id", required = false) String lineId,
            @RequestParam(value = "status", required = false) String status) {
        return computeService.getAlarms(lineId, status);
    }

    @PostMapping("/alarms/{alarmId}/acknowledge")
    public Map<String, Object> acknowledgeAlarm(
            @PathVariable String alarmId,
            @RequestBody(required = false) Map<String, Object> body) {
        String handler = body != null ? String.valueOf(body.getOrDefault("handler", "")) : "";
        String note = body != null ? String.valueOf(body.getOrDefault("handle_note", "")) : "";
        if ("null".equals(handler)) handler = "";
        if ("null".equals(note)) note = "";
        return computeService.acknowledgeAlarm(alarmId, handler, note);
    }

    @PostMapping("/alarms/{alarmId}/resolve")
    public Map<String, Object> resolveAlarm(
            @PathVariable String alarmId,
            @RequestBody(required = false) Map<String, Object> body) {
        String handler = body != null ? String.valueOf(body.getOrDefault("handler", "")) : "";
        String note = body != null ? String.valueOf(body.getOrDefault("handle_note", "")) : "";
        if ("null".equals(handler)) handler = "";
        if ("null".equals(note)) note = "";
        return computeService.resolveAlarm(alarmId, handler, note);
    }

    @GetMapping("/material-events")
    public List<Map<String, Object>> materialEvents(
            @RequestParam(value = "line_id", required = false) String lineId,
            @RequestParam(value = "batch_id", required = false) String batchId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return computeService.getMaterialEvents(lineId, batchId, limit);
    }

    @GetMapping("/logistics/tasks")
    public List<Map<String, Object>> logisticsTasks(
            @RequestParam(value = "factory_id", required = false) String factoryId,
            @RequestParam(value = "status", required = false) String status) {
        return computeService.getLogisticsTasks(factoryId, status);
    }

    @GetMapping("/quality/gates")
    public List<Map<String, Object>> qualityGates(
            @RequestParam(value = "factory_id", required = false) String factoryId,
            @RequestParam(value = "batch_id", required = false) String batchId,
            @RequestParam(value = "decision", required = false) String decision) {
        return computeService.getQualityGateEvents(factoryId, batchId, decision);
    }

    @GetMapping("/factory/dashboard")
    public Map<String, Object> factoryDashboard(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return factoryDashboardService.getDashboard(factoryId);
    }

    @GetMapping("/factory/energy")
    public Map<String, Object> factoryEnergy(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return computeService.getFactoryEnergy(factoryId);
    }

    @GetMapping("/factory/kpis")
    public Map<String, Object> factoryKpis(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return computeService.getFactoryKpis(factoryId);
    }

    @GetMapping("/orders/risk")
    public List<Map<String, Object>> orderRisk(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return computeService.getOrderRiskList(factoryId);
    }

    @GetMapping("/orders/summary")
    public Map<String, Object> orderSummary(
            @RequestParam(value = "factory_id", required = false) String factoryId) {
        return computeService.getOrderSummary(factoryId);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders(
            @RequestParam(value = "factory_id", required = false) String factoryId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "risk_level", required = false) String riskLevel) {
        return computeService.getOrderList(factoryId, status, riskLevel);
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> orderDetail(@PathVariable String orderId) {
        return computeService.getOrderDetail(orderId);
    }

    @GetMapping("/orders/{orderId}/timeline")
    public Map<String, Object> orderTimeline(@PathVariable String orderId) {
        return computeService.getOrderTimeline(orderId);
    }

    @GetMapping("/compute/oee")
    public Map<String, Object> oee(@RequestParam("line_id") String lineId) {
        Map<String, Object> overview = computeService.getLineOverview(lineId);
        double oee = 0;
        Object kpiBar = overview.get("kpi_bar");
        if (kpiBar instanceof Map<?, ?> kpi && kpi.get("oee_pct") instanceof Number n) {
            oee = n.doubleValue();
        }
        return Map.of("line_id", lineId, "oee_pct", oee);
    }
}

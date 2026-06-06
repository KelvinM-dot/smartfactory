package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FactoryMetricsService {

    private final ProductLineRepository productLineRepository;
    private final EquipmentRepository equipmentRepository;
    private final LatestStateRepository latestStateRepository;
    private final ProductBatchRepository productBatchRepository;
    private final LogisticsTaskRepository logisticsTaskRepository;
    private final QualityGateEventRepository qualityGateEventRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final FactoryMasterDataService masterDataService;
    private final JqhcProperties properties;
    private final LineOeeService lineOeeService;
    private final OrderAnalyticsService orderAnalyticsService;

    public FactoryMetricsService(
            ProductLineRepository productLineRepository,
            EquipmentRepository equipmentRepository,
            LatestStateRepository latestStateRepository,
            ProductBatchRepository productBatchRepository,
            LogisticsTaskRepository logisticsTaskRepository,
            QualityGateEventRepository qualityGateEventRepository,
            ProductionOrderRepository productionOrderRepository,
            FactoryMasterDataService masterDataService,
            JqhcProperties properties,
            LineOeeService lineOeeService,
            OrderAnalyticsService orderAnalyticsService) {
        this.productLineRepository = productLineRepository;
        this.equipmentRepository = equipmentRepository;
        this.latestStateRepository = latestStateRepository;
        this.productBatchRepository = productBatchRepository;
        this.logisticsTaskRepository = logisticsTaskRepository;
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.masterDataService = masterDataService;
        this.properties = properties;
        this.lineOeeService = lineOeeService;
        this.orderAnalyticsService = orderAnalyticsService;
    }

    public Map<String, Object> getFactoryEnergy(String factoryId) {
        List<ProductLineDoc> lines = productLineRepository.findAll();
        Instant cutoff = Instant.now().minus(properties.getBufferWindowHours(), ChronoUnit.HOURS);
        List<Map<String, Object>> lineBreakdown = new ArrayList<>();
        double totalConsumption = 0;
        double totalActivePowerKw = 0;
        int totalRunningEquipment = 0;
        int telemetryRunningEquipment = countTelemetryRunningEquipment();
        for (ProductLineDoc line : lines) {
            String lineId = line.getProductLineId();
            List<LatestStateDoc> states = latestStateRepository.findByProductLineId(lineId);
            double powerKwSum = states.stream()
                    .filter(s -> "power_kw".equals(s.getFieldId()))
                    .mapToDouble(s -> NumberUtils.toDouble(s.getValue()))
                    .sum();
            SourceStatusDoc source = lineOeeService.findLatestSource(lineId);
            int lineRunningEquipment = resolveLineRunningEquipment(line, states, source);
            double lineActivePowerKw = resolveLineActivePowerKw(line, states, source, lineRunningEquipment, powerKwSum);
            double lineConsumption = lineActivePowerKw > 0
                    ? Math.max(1.0, lineActivePowerKw * 0.25)
                    : Math.max(8.0, lineRunningEquipment * 3.5);
            totalConsumption += lineConsumption;
            totalActivePowerKw += lineActivePowerKw;
            totalRunningEquipment += lineRunningEquipment;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product_line_id", lineId);
            row.put("line_name", line.getName());
            row.put("line_status", line.getStatus());
            row.put("simulation_enabled", Boolean.TRUE.equals(line.getSimulationEnabled()));
            row.put("detail_level", line.getDetailLevel());
            row.put("consumption_kwh", Math.round(lineConsumption * 100.0) / 100.0);
            row.put("active_power_kw", Math.round(lineActivePowerKw * 100.0) / 100.0);
            row.put("running_equipment_count", lineRunningEquipment);
            row.put("telemetry_power_kw_sum", Math.round(powerKwSum * 100.0) / 100.0);
            row.put("specific_energy_kwh_per_t", Math.round((lineConsumption / Math.max(line.getDesignCapacityTPerYear() != null ? line.getDesignCapacityTPerYear() : 1.0, 0.1)) * 100.0) / 100.0);
            if (source != null) {
                row.put("dwell_mode", source.getDwellMode());
            }
            lineBreakdown.add(row);
        }
        double greenRatioPct = computeGreenPowerRatioPct();
        double greenPower = Math.round(totalConsumption * greenRatioPct) / 100.0;
        double gridPower = Math.round((totalConsumption - greenPower) * 100.0) / 100.0;
        double annualGen = masterDataService.getEnergyAssets().stream()
                .mapToDouble(a -> NumberUtils.toDouble(a.get("annual_generation_kwh"))).sum();
        double carbonReduction = masterDataService.getEnergyAssets().stream()
                .mapToDouble(a -> NumberUtils.toDouble(a.get("annual_carbon_reduction_t"))).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", factoryId != null && !factoryId.isBlank() ? factoryId : "JQHC-PLANT-01");
        result.put("computed_at", Instant.now());
        result.put("window_start", cutoff);
        result.put("window_end", Instant.now());
        result.put("total_consumption_kwh", Math.round(totalConsumption * 100.0) / 100.0);
        result.put("total_active_power_kw", Math.round(totalActivePowerKw * 100.0) / 100.0);
        Map<String, Object> profile = masterDataService.getFactoryProfile();
        int plantEquipmentCount = (int) NumberUtils.toDouble(profile.getOrDefault("equipment_count", equipmentRepository.count()));
        long activeLines = lines.stream().filter(l -> "active".equals(l.getStatus())).count();
        long simLines = lines.stream().filter(l -> Boolean.TRUE.equals(l.getSimulationEnabled())).count();
        result.put("total_running_equipment", totalRunningEquipment);
        result.put("telemetry_running_equipment", telemetryRunningEquipment);
        result.put("plant_equipment_count", plantEquipmentCount);
        result.put("active_line_count", activeLines);
        result.put("simulation_line_count", simLines);
        result.put("registry_line_count", lines.size());
        result.put("wire_line_count", NumberUtils.toDouble(profile.getOrDefault("wire_line_count", 22)));
        result.put("rod_line_count", NumberUtils.toDouble(profile.getOrDefault("rod_line_count", 20)));
        result.put("green_power_kwh", greenPower);
        result.put("grid_power_kwh", gridPower);
        result.put("green_power_ratio_pct", greenRatioPct);
        result.put("annual_clean_generation_kwh", annualGen);
        result.put("annual_carbon_reduction_t", carbonReduction);
        result.put("line_breakdown", lineBreakdown);
        return result;
    }

    public Map<String, Object> getFactoryKpis(String factoryId) {
        List<ProductLineDoc> lines = productLineRepository.findAll();
        List<ProductLineDoc> simLines = lines.stream()
                .filter(l -> Boolean.TRUE.equals(l.getSimulationEnabled()))
                .toList();
        double avgOee = simLines.isEmpty() ? 0.0 : simLines.stream()
                .mapToDouble(l -> lineOeeService.computeOeePct(l.getProductLineId()))
                .average()
                .orElse(0.0);

        List<QualityGateEventDoc> qualityEvents = qualityGateEventRepository.findAll();
        long passCount = qualityEvents.stream().filter(q -> "pass".equalsIgnoreCase(q.getDecision())).count();
        double qualityPassRate = qualityEvents.isEmpty() ? 100.0 : (passCount * 100.0 / qualityEvents.size());

        List<LogisticsTaskDoc> logisticsTasks = logisticsTaskRepository.findAll();
        long completedTasks = logisticsTasks.stream().filter(t -> "completed".equalsIgnoreCase(t.getStatus())).count();
        double logisticsCompletionRate = logisticsTasks.isEmpty() ? 100.0 : (completedTasks * 100.0 / logisticsTasks.size());

        List<ProductionOrderDoc> orders;
        if (factoryId != null && !factoryId.isBlank()) {
            orders = productionOrderRepository.findByFactoryIdOrderByDueDateAsc(factoryId);
        } else {
            orders = productionOrderRepository.findAll().stream()
                    .sorted(Comparator.comparing(ProductionOrderDoc::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }
        List<Map<String, Object>> orderRiskList = orderAnalyticsService.getOrderRiskList(factoryId);
        long highRiskOrders = orderRiskList.stream()
                .filter(o -> {
                    Object level = o.get("delivery_risk_level");
                    return "high".equals(level) || "critical".equals(level);
                })
                .count();
        long constrainedOrders = orderRiskList.stream()
                .filter(o -> {
                    Object constrained = o.get("delivery_blocked");
                    return constrained instanceof Boolean b && b;
                })
                .count();
        long blockedOrders = orderRiskList.stream()
                .filter(o -> "blocked".equals(String.valueOf(o.get("order_status"))))
                .count();
        long readyToShipOrders = orderRiskList.stream()
                .filter(o -> "ready_to_ship".equals(String.valueOf(o.get("order_status"))))
                .count();
        List<ProductBatchDoc> allBatches = productBatchRepository.findAll();
        long holdBatches = allBatches.stream().filter(b -> "hold".equalsIgnoreCase(b.getStatus())).count();
        long readyToShipBatches = allBatches.stream().filter(b -> "ready_to_ship".equalsIgnoreCase(b.getStatus())).count();
        double totalPlanned = orders.stream().map(ProductionOrderDoc::getPlannedQuantityT).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).sum();
        double totalReleased = orders.stream().map(ProductionOrderDoc::getReleasedQuantityT).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).sum();
        double releaseProgress = totalPlanned <= 0 ? 0.0 : (totalReleased * 100.0 / totalPlanned);

        Map<String, Object> energy = getFactoryEnergy(factoryId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", factoryId != null && !factoryId.isBlank() ? factoryId : "JQHC-PLANT-01");
        result.put("computed_at", Instant.now());
        result.put("avg_oee_pct", NumberUtils.round1(avgOee));
        result.put("quality_pass_rate_pct", NumberUtils.round1(qualityPassRate));
        result.put("logistics_completion_rate_pct", NumberUtils.round1(logisticsCompletionRate));
        result.put("release_progress_pct", NumberUtils.round1(releaseProgress));
        result.put("high_risk_orders", highRiskOrders);
        result.put("constrained_orders", constrainedOrders);
        result.put("blocked_orders", blockedOrders);
        result.put("ready_to_ship_orders", readyToShipOrders);
        result.put("hold_batches", holdBatches);
        result.put("ready_to_ship_batches", readyToShipBatches);
        result.put("green_power_ratio_pct", energy.get("green_power_ratio_pct"));
        result.put("total_orders", orders.size());
        Map<String, Object> profile = masterDataService.getFactoryProfile();
        Map<String, Object> targets2025 = profile.get("targets_2025") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        if (!targets2025.isEmpty()) {
            result.put("target_sales_t_2025", targets2025.get("sales_t"));
            result.put("target_fcw_sales_t_2025", targets2025.get("fcw_sales_t"));
            result.put("target_output_value_cny_2025", targets2025.get("output_value_cny"));
        }
        Map<String, Object> plan = masterDataService.getAnnualProductionPlan();
        if (!plan.isEmpty()) {
            result.put("plan_year", plan.get("plan_year"));
        }
        result.put("registry_line_count", masterDataService.getLineRegistry().size());
        result.put("total_production_line_count", NumberUtils.toDouble(profile.getOrDefault(
                "total_production_line_count",
                masterDataService.getLineRegistry().size())));
        result.put("wire_line_count", NumberUtils.toDouble(profile.getOrDefault("wire_line_count", 22)));
        result.put("rod_line_count", NumberUtils.toDouble(profile.getOrDefault("rod_line_count", 20)));
        result.put("plant_equipment_count", NumberUtils.toDouble(profile.getOrDefault("equipment_count", 0)));
        result.put("active_line_count", lines.stream().filter(l -> "active".equals(l.getStatus())).count());
        result.put("simulation_line_count", simLines.size());
        if (energy.get("telemetry_running_equipment") != null) {
            result.put("telemetry_running_equipment", energy.get("telemetry_running_equipment"));
        }
        if (energy.get("total_running_equipment") != null) {
            result.put("plant_running_equipment", energy.get("total_running_equipment"));
        }
        result.put("capacity_summary", buildCapacitySummary(lines, profile));
        long registryLines = lines.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getSimulationEnabled()))
                .count();
        result.put("registry_line_count_estimated", registryLines);
        result.put("telemetry_line_count", simLines.size());
        return result;
    }

    private Map<String, Object> buildCapacitySummary(List<ProductLineDoc> lines, Map<String, Object> profile) {
        double wireCap = 0.0;
        double rodCap = 0.0;
        for (ProductLineDoc line : lines) {
            double cap = line.getDesignCapacityTPerYear() != null ? line.getDesignCapacityTPerYear() : 0.0;
            String cat = line.getProductCategory() != null ? line.getProductCategory() : "";
            if ("welding_rod".equals(cat)) {
                rodCap += cap;
            } else if ("flux_core_wire".equals(cat) || "solid_wire".equals(cat) || "submerged_arc_wire".equals(cat)) {
                wireCap += cap;
            }
        }
        double targetWire = NumberUtils.toDouble(profile.getOrDefault("wire_annual_capacity_t", 100_000));
        double targetRod = NumberUtils.toDouble(profile.getOrDefault("rod_annual_capacity_t", 300_000));
        double targetTotal = NumberUtils.toDouble(profile.getOrDefault("annual_capacity_t", 400_000));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("design_wire_t_per_year", Math.round(wireCap));
        summary.put("design_rod_t_per_year", Math.round(rodCap));
        summary.put("design_total_t_per_year", Math.round(wireCap + rodCap));
        summary.put("target_wire_t_per_year", Math.round(targetWire));
        summary.put("target_rod_t_per_year", Math.round(targetRod));
        summary.put("target_total_t_per_year", Math.round(targetTotal));
        summary.put("wire_capacity_aligned", Math.abs(wireCap - targetWire) < 500);
        summary.put("rod_capacity_aligned", Math.abs(rodCap - targetRod) < 500);
        summary.put("total_capacity_aligned", Math.abs(wireCap + rodCap - targetTotal) < 1000);
        summary.put("capacity_note", profile.get("capacity_note"));
        return summary;
    }

    private int countTelemetryRunningEquipment() {
        return (int) latestStateRepository.findAll().stream()
                .filter(s -> "status".equals(s.getFieldId()))
                .filter(s -> "RUNNING".equals(String.valueOf(s.getValue())))
                .count();
    }

    private int equipmentSlotsPerLine(ProductLineDoc line) {
        Map<String, Object> defaults = masterDataService.getSimulationDefaults();
        Object mapObj = defaults.get("equipment_per_line_by_category");
        if (mapObj instanceof Map<?, ?> map && line.getProductCategory() != null) {
            Object slots = map.get(line.getProductCategory());
            if (slots instanceof Number n) {
                return n.intValue();
            }
        }
        return switch (String.valueOf(line.getProductCategory())) {
            case "flux_core_wire" -> 9;
            case "solid_wire", "submerged_arc_wire" -> 6;
            case "welding_rod" -> 7;
            default -> 8;
        };
    }

    private int resolveLineRunningEquipment(
            ProductLineDoc line,
            List<LatestStateDoc> states,
            SourceStatusDoc source) {
        if (Boolean.TRUE.equals(line.getSimulationEnabled())) {
            if (source != null && source.getRunningEquipmentCount() != null) {
                return source.getRunningEquipmentCount();
            }
            return (int) states.stream()
                    .filter(s -> "status".equals(s.getFieldId()))
                    .filter(s -> "RUNNING".equals(String.valueOf(s.getValue())))
                    .count();
        }
        int slots = equipmentSlotsPerLine(line);
        String status = line.getStatus() != null ? line.getStatus() : "active";
        return switch (status) {
            case "inactive" -> 0;
            case "maintenance" -> Math.max(1, (int) Math.round(slots * 0.35));
            default -> (int) Math.round(slots * 0.85);
        };
    }

    private double resolveLineActivePowerKw(
            ProductLineDoc line,
            List<LatestStateDoc> states,
            SourceStatusDoc source,
            int runningCount,
            double powerKwSum) {
        if (Boolean.TRUE.equals(line.getSimulationEnabled())) {
            if (source != null && source.getActivePowerKw() != null) {
                return source.getActivePowerKw();
            }
            if (powerKwSum > 0) {
                return powerKwSum;
            }
            return runningCount * 12.0;
        }
        double capDay = line.getDesignCapacityTPerDay() != null ? line.getDesignCapacityTPerDay() : 12.0;
        String status = line.getStatus() != null ? line.getStatus() : "active";
        double factor = switch (status) {
            case "inactive" -> 0.04;
            case "maintenance" -> 0.22;
            default -> 0.68;
        };
        return Math.round(capDay * 14.0 * factor * 100.0) / 100.0;
    }

    private double computeGreenPowerRatioPct() {
        Map<String, Object> defaults = masterDataService.getSimulationDefaults();
        double base = NumberUtils.toDouble(defaults.getOrDefault("default_green_power_ratio_pct", 60.0));
        List<Map<String, Object>> assets = masterDataService.getEnergyAssets();
        double pvKw = assets.stream().filter(a -> "pv".equals(a.get("asset_type")))
                .mapToDouble(a -> NumberUtils.toDouble(a.get("rated_power_kw"))).sum();
        double windKw = assets.stream().filter(a -> "wind".equals(a.get("asset_type")))
                .mapToDouble(a -> NumberUtils.toDouble(a.get("rated_power_kw"))).sum();
        double total = Math.max(pvKw + windKw, 1.0);
        int hour = (Instant.now().atZone(java.time.ZoneId.of("Asia/Shanghai")).getHour());
        double pvCurve = (hour >= 6 && hour <= 18)
                ? Math.pow(Math.cos((hour - 12.0) / 6.0 * Math.PI / 2.0), 2) : 0.0;
        double dynamic = base * 0.35 + (pvKw / total) * pvCurve * 100.0 + (windKw / total) * 18.0;
        return NumberUtils.round1(Math.min(92.0, Math.max(22.0, dynamic)));
    }
}

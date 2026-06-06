package com.jqhc.dataplatform.service.optimization;

import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.service.ComputeService;
import com.jqhc.dataplatform.service.DomainConfigService;
import com.jqhc.dataplatform.service.FactoryMasterDataService;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OptimizationModelEngine {

    private static final String WS_WIRE = "WS-WIRE-01";
    private static final String WS_ROD = "WS-ROD-01";

    private final QualityGateEventRepository qualityGateEventRepository;
    private final LogisticsTaskRepository logisticsTaskRepository;
    private final MaterialEventRepository materialEventRepository;
    private final ProductBatchRepository productBatchRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final TelemetryPointRepository telemetryPointRepository;
    private final FactoryMasterDataService masterDataService;
    private final ComputeService computeService;
    private final DomainConfigService domainConfigService;
    private final OptimizationContextService contextService;
    private final JqhcProperties properties;

    public OptimizationModelEngine(
            QualityGateEventRepository qualityGateEventRepository,
            LogisticsTaskRepository logisticsTaskRepository,
            MaterialEventRepository materialEventRepository,
            ProductBatchRepository productBatchRepository,
            ProductionOrderRepository productionOrderRepository,
            TelemetryPointRepository telemetryPointRepository,
            FactoryMasterDataService masterDataService,
            ComputeService computeService,
            DomainConfigService domainConfigService,
            OptimizationContextService contextService,
            JqhcProperties properties) {
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.logisticsTaskRepository = logisticsTaskRepository;
        this.materialEventRepository = materialEventRepository;
        this.productBatchRepository = productBatchRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.telemetryPointRepository = telemetryPointRepository;
        this.masterDataService = masterDataService;
        this.computeService = computeService;
        this.domainConfigService = domainConfigService;
        this.contextService = contextService;
        this.properties = properties;
    }

    // ── 模型 1–4：效率 ───────────────────────────────────────

    public Map<String, Object> buildEfficiencyModels(List<OptimizationLineContext> lines, String factoryId) {
        Map<String, Object> models = new LinkedHashMap<>();
        models.put("m01_bottleneck_flow", model01BottleneckFlow(lines));
        models.put("m02_scheduling", model02Scheduling(lines, factoryId));
        models.put("m03_oee_loss", model03OeeLoss(lines));
        models.put("m04_logistics", model04Logistics(factoryId, lines));
        return models;
    }

    public Map<String, Object> model01BottleneckFlow(List<OptimizationLineContext> lines) {
        List<Map<String, Object>> perLine = new ArrayList<>();
        Map<String, Integer> stepPressure = new HashMap<>();
        for (OptimizationLineContext lc : lines) {
            String bottleneck = contextService.resolveBottleneck(lc);
            stepPressure.merge(bottleneck, 1, Integer::sum);
            double flowEff = lc.telemetry()
                    ? contextService.estimateFlowEfficiencyTelemetry(lc)
                    : contextService.estimateFlowEfficiencyRegistry(lc);
            double wipDays = lc.telemetry() ? 1.2 + (100 - flowEff) * 0.04 : 2.5 + (100 - flowEff) * 0.06;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product_line_id", lc.lineId());
            row.put("line_name", lc.line().getName());
            row.put("workshop_id", lc.line().getWorkshopId());
            row.put("product_category", lc.line().getProductCategory());
            row.put("bottleneck_step", bottleneck);
            row.put("flow_efficiency_pct", NumberUtils.round1(flowEff));
            row.put("estimated_wip_days", NumberUtils.round1(wipDays));
            row.put("data_confidence", lc.telemetry() ? "telemetry" : "registry_estimate");
            row.put("status", lc.line().getStatus());
            perLine.add(row);
        }
        String plantBottleneck = stepPressure.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("filling_forming");
        double avgFlow = perLine.stream().mapToDouble(r -> NumberUtils.toDouble(r.get("flow_efficiency_pct"))).average().orElse(0);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m01");
        model.put("title", "产线约束与流动优化");
        model.put("plant_bottleneck_step", plantBottleneck);
        model.put("avg_flow_efficiency_pct", NumberUtils.round1(avgFlow));
        model.put("wire_shop_bottleneck", contextService.topBottleneckForWorkshop(lines, WS_WIRE));
        model.put("rod_shop_bottleneck", contextService.topBottleneckForWorkshop(lines, WS_ROD));
        model.put("line_breakdown", perLine);
        model.put("recommendations", List.of(
                contextService.rec("efficiency", "high", "释放全厂主约束「" + contextService.stepLabel(plantBottleneck) + "」",
                        "焊丝/焊条车间共 " + stepPressure.getOrDefault(plantBottleneck, 0) + " 条线受该工序制约",
                        plantBottleneck, null, "m01"),
                contextService.rec("efficiency", "medium", "焊条车间压涂-烘干汇合段 WIP 偏高",
                        "建议焊条线批次由 800kg 试降至 650kg，缩短汇合等待",
                        "coating", WS_ROD, "m01")
        ));
        return model;
    }

    public Map<String, Object> model02Scheduling(List<OptimizationLineContext> lines, String factoryId) {
        List<Map<String, Object>> orderRisk = computeService.getOrderRiskList(factoryId);
        List<ProductionOrderDoc> orders = productionOrderRepository.findByFactoryIdOrderByDueDateAsc(factoryId);
        if (orders.isEmpty()) {
            orders = productionOrderRepository.findAll();
        }

        List<Map<String, Object>> assignments = new ArrayList<>();
        for (ProductionOrderDoc order : orders.stream().limit(20).toList()) {
            String cat = order.getProductCategory() != null ? order.getProductCategory() : "flux_core_wire";
            List<OptimizationLineContext> candidates = lines.stream()
                    .filter(lc -> cat.equals(lc.line().getProductCategory()))
                    .filter(lc -> "active".equals(lc.line().getStatus()))
                    .sorted(Comparator.comparingDouble((OptimizationLineContext lc) -> -lc.utilizationHeadroom()))
                    .toList();
            if (candidates.isEmpty()) {
                candidates = lines.stream().filter(lc -> "active".equals(lc.line().getStatus())).toList();
            }
            OptimizationLineContext best = candidates.isEmpty() ? null : candidates.get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order_id", order.getProductionOrderId());
            row.put("product_category", cat);
            row.put("planned_quantity_t", order.getPlannedQuantityT());
            row.put("due_date", order.getDueDate());
            row.put("recommended_line_id", best != null ? best.lineId() : null);
            row.put("line_headroom_pct", best != null ? NumberUtils.round1(best.utilizationHeadroom()) : 0);
            assignments.add(row);
        }

        long highRisk = orderRisk.stream()
                .filter(o -> "high".equals(o.get("delivery_risk_level")) || "critical".equals(o.get("delivery_risk_level")))
                .count();
        Map<String, Object> sla = masterDataService.getFactoryProfile().get("delivery_sla") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m02");
        model.put("title", "多产线排产与订单分配");
        model.put("total_orders", orders.size());
        model.put("high_risk_orders", highRisk);
        model.put("sla_regular_days", sla.getOrDefault("regular_days_min", 15) + "–" + sla.getOrDefault("regular_days_max", 30));
        Map<String, Object> market = masterDataService.getFactoryProfile().get("market") instanceof Map<?, ?> mm
                ? (Map<String, Object>) mm : Map.of();
        model.put("export_share_pct", NumberUtils.toDouble(market.getOrDefault("export_share_pct", 40)));
        model.put("assignments", assignments);
        model.put("workshop_capacity", Map.of(
                WS_WIRE, contextService.capacityByWorkshop(lines, WS_WIRE),
                WS_ROD, contextService.capacityByWorkshop(lines, WS_ROD)
        ));
        String fcwPick = pickTopLineIds(lines, "flux_core_wire", 2);
        String swPick = pickTopLineIds(lines, "solid_wire", 2);
        String wrPick = pickTopLineIds(lines, "welding_rod", 3);
        model.put("recommendations", List.of(
                contextService.rec("efficiency", "high", "出口订单优先分配高余量仿真产线",
                        "推荐药芯 " + fcwPick + " · 实心 " + swPick + "（按 headroom 排序）", null, WS_WIRE, "m02"),
                contextService.rec("efficiency", "medium", "焊条旺季订单向多线均匀摊派",
                        "推荐 " + wrPick + " 等 " + lines.stream().filter(lc -> "welding_rod".equals(lc.line().getProductCategory())).count() + " 条焊条线，避免单线过载",
                        null, WS_ROD, "m02")
        ));
        return model;
    }

    private String pickTopLineIds(List<OptimizationLineContext> lines, String category, int limit) {
        return lines.stream()
                .filter(lc -> category.equals(lc.line().getProductCategory()))
                .filter(lc -> "active".equals(lc.line().getStatus()))
                .filter(OptimizationLineContext::telemetry)
                .sorted(Comparator.comparingDouble((OptimizationLineContext lc) -> -lc.utilizationHeadroom()))
                .limit(limit)
                .map(OptimizationLineContext::lineId)
                .collect(Collectors.joining(" / "));
    }

    public Map<String, Object> model03OeeLoss(List<OptimizationLineContext> lines) {
        List<Map<String, Object>> lossRanking = new ArrayList<>();
        for (OptimizationLineContext lc : lines) {
            double oee = lc.oeePct();
            double availability = lc.availabilityPct();
            double performance = lc.performancePct();
            double quality = lc.qualityPct();
            double lossValue = (100 - oee) * (lc.line().getDesignCapacityTPerYear() != null
                    ? lc.line().getDesignCapacityTPerYear() : 1000) * 0.8;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product_line_id", lc.lineId());
            row.put("line_name", lc.line().getName());
            row.put("workshop_id", lc.line().getWorkshopId());
            row.put("oee_pct", NumberUtils.round1(oee));
            row.put("availability_pct", NumberUtils.round1(availability));
            row.put("performance_pct", NumberUtils.round1(performance));
            row.put("quality_pct", NumberUtils.round1(quality));
            row.put("estimated_loss_t_per_year", NumberUtils.round1(lossValue / 1000));
            row.put("pending_alarms", lc.pendingAlarms());
            row.put("data_confidence", lc.telemetry() ? "telemetry" : "registry_estimate");
            lossRanking.add(row);
        }
        lossRanking.sort(Comparator.comparingDouble(r -> -NumberUtils.toDouble(r.get("estimated_loss_t_per_year"))));

        double plantOee = lines.stream().mapToDouble(lc -> lc.oeePct()).average().orElse(0);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m03");
        model.put("title", "OEE 损失归因与改善优先级");
        model.put("plant_avg_oee_pct", NumberUtils.round1(plantOee));
        model.put("wire_shop_oee_pct", NumberUtils.round1(contextService.workshopOee(lines, WS_WIRE)));
        model.put("rod_shop_oee_pct", NumberUtils.round1(contextService.workshopOee(lines, WS_ROD)));
        model.put("loss_ranking", lossRanking);
        model.put("loss_ranking_count", lossRanking.size());
        model.put("recommendations", List.of(
                contextService.rec("efficiency", "high", "优先处理镀铜/填充工序 Critical 报警",
                        "断丝与填充率漂移直接拖累性能率与质量率", "copper_plating", WS_WIRE, "m03")
        ));
        return model;
    }

    public Map<String, Object> model04Logistics(String factoryId, List<OptimizationLineContext> lines) {
        List<LogisticsTaskDoc> tasks = logisticsTaskRepository.findAll();
        long completed = tasks.stream().filter(t -> "completed".equalsIgnoreCase(t.getStatus())).count();
        long failed = tasks.stream().filter(t -> "failed".equalsIgnoreCase(t.getStatus())).count();
        long pending = tasks.stream().filter(t -> !"completed".equalsIgnoreCase(t.getStatus())
                && !"cancelled".equalsIgnoreCase(t.getStatus())).count();
        double completionRate = tasks.isEmpty() ? 99.9 : completed * 100.0 / tasks.size();

        double avgLeadMin = tasks.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCompletedAt() != null)
                .mapToDouble(t -> ChronoUnit.SECONDS.between(t.getCreatedAt(), t.getCompletedAt()) / 60.0)
                .average().orElse(3.0);

        long shortageLines = lines.stream().filter(lc -> lc.materialShortage()).count();
        Map<String, Object> sla = masterDataService.getFactoryProfile().get("delivery_sla") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m04");
        model.put("title", "厂内物流协同效率");
        model.put("task_count", tasks.size());
        model.put("completion_rate_pct", NumberUtils.round1(completionRate));
        model.put("target_accuracy_pct", sla.getOrDefault("agv_delivery_accuracy_pct", 99.9));
        model.put("avg_lead_time_min", NumberUtils.round1(avgLeadMin));
        model.put("pending_tasks", pending);
        model.put("failed_tasks", failed);
        model.put("lines_with_material_shortage", shortageLines);
        model.put("hidden_capacity_loss_t_per_month", NumberUtils.round1(shortageLines * 42.0));
        model.put("recommendations", List.of(
                contextService.rec("efficiency", "medium", "原料仓补货阈值由 150kg 提升至 220kg",
                        "降低焊丝线缺料等待造成的隐性停机", null, WS_WIRE, "m04")
        ));
        return model;
    }

    // ── 模型 5–8：成本 ───────────────────────────────────────

    public Map<String, Object> buildCostModels(List<OptimizationLineContext> lines, String factoryId, Map<String, Object> energy) {
        Map<String, Object> models = new LinkedHashMap<>();
        models.put("m05_energy", model05Energy(lines, energy));
        models.put("m06_recipe_yield", model06RecipeYield(lines));
        models.put("m07_coq", model07Coq(factoryId));
        models.put("m08_capacity_util", model08CapacityUtil(lines));
        return models;
    }

    public Map<String, Object> model05Energy(List<OptimizationLineContext> lines, Map<String, Object> energy) {
        double totalKwh = NumberUtils.toDouble(energy.get("total_consumption_kwh"));
        double greenPct = NumberUtils.toDouble(energy.get("green_power_ratio_pct"));
        int hour = Instant.now().atZone(ZoneId.of("Asia/Shanghai")).getHour();
        boolean noonWindow = hour >= 11 && hour <= 14;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = energy.get("line_breakdown") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        double wireKwh = breakdown.stream()
                .filter(r -> contextService.lineWorkshop(r.get("product_line_id"), lines).equals(WS_WIRE))
                .mapToDouble(r -> NumberUtils.toDouble(r.get("consumption_kwh"))).sum();
        double rodKwh = breakdown.stream()
                .filter(r -> contextService.lineWorkshop(r.get("product_line_id"), lines).equals(WS_ROD))
                .mapToDouble(r -> NumberUtils.toDouble(r.get("consumption_kwh"))).sum();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m05");
        model.put("title", "单吨综合能耗与绿电协同");
        model.put("total_consumption_kwh", NumberUtils.round1(totalKwh));
        model.put("green_power_ratio_pct", NumberUtils.round1(greenPct));
        model.put("grid_power_kwh", energy.get("grid_power_kwh"));
        model.put("wire_shop_kwh", NumberUtils.round1(wireKwh));
        model.put("rod_shop_kwh", NumberUtils.round1(rodKwh));
        model.put("annual_carbon_reduction_t", energy.get("annual_carbon_reduction_t"));
        model.put("green_window_active", noonWindow);
        model.put("green_window_hint", "11:00–14:00 光伏峰值，建议高耗能工序（烘干/拉拔）排入该窗口");
        model.put("line_breakdown", breakdown);
        model.put("recommendations", List.of(
                contextService.rec("cost", "high", noonWindow ? "当前处于绿电窗口，优先排烘干/拉拔" : "下一绿电窗口预排高耗能批次",
                        "可再降购电成本约 4–7%", "drying", null, "m05")
        ));
        return model;
    }

    public Map<String, Object> model06RecipeYield(List<OptimizationLineContext> lines) {
        List<Map<String, Object>> recipeRows = new ArrayList<>();
        List<Map<String, Object>> recipes = masterDataService.getRecipes();
        for (OptimizationLineContext lc : lines.stream().filter(l -> l.telemetry()).toList()) {
            recipes.stream()
                    .filter(r -> lc.lineId().equals(String.valueOf(r.get("product_line_id"))))
                    .findFirst()
                    .ifPresent(recipe -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("product_line_id", lc.lineId());
                        row.put("recipe_id", recipe.get("recipe_id"));
                        row.put("grade", recipe.get("grade"));
                        row.put("deviation_score", NumberUtils.round1(lc.recipeDeviationScore()));
                        row.put("yield_pct", NumberUtils.round1(lc.qualityPct()));
                        row.put("material_saving_potential_pct", NumberUtils.round1(Math.max(0, 3.5 - lc.recipeDeviationScore() * 0.4)));
                        recipeRows.add(row);
                    });
        }
        double avgDeviation = recipeRows.stream().mapToDouble(r -> NumberUtils.toDouble(r.get("deviation_score"))).average().orElse(1.2);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m06");
        model.put("title", "配方-原料成本与收率");
        model.put("telemetry_line_count", recipeRows.size());
        model.put("avg_recipe_deviation_score", NumberUtils.round1(avgDeviation));
        model.put("recipe_breakdown", recipeRows);
        model.put("recommendations", List.of(
                contextService.rec("cost", "medium", "药芯线填充率回归配方中心值",
                        "偏离 1σ 时吨原料成本上升约 1.8%", "filling_forming", WS_WIRE, "m06")
        ));
        return model;
    }

    public Map<String, Object> model07Coq(String factoryId) {
        List<QualityGateEventDoc> gates = qualityGateEventRepository.findAll();
        long pass = gates.stream().filter(g -> "pass".equalsIgnoreCase(g.getDecision())).count();
        long hold = gates.stream().filter(g -> "hold".equalsIgnoreCase(g.getDecision())).count();
        long rework = gates.stream().filter(g -> "rework".equalsIgnoreCase(g.getDecision())).count();
        long scrap = gates.stream().filter(g -> "scrap".equalsIgnoreCase(g.getDecision())).count();
        double failRate = gates.isEmpty() ? 0 : (hold + rework + scrap) * 100.0 / gates.size();

        double tonValue = 8500.0;
        double prevention = 120000;
        double appraisal = 45000;
        double internalFailure = (hold + rework) * 1.2 * tonValue;
        double externalFailure = scrap * 2.5 * tonValue;

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m07");
        model.put("title", "质量成本 COQ");
        model.put("gate_event_count", gates.size());
        model.put("first_pass_rate_pct", gates.isEmpty() ? 97.2 : NumberUtils.round1(pass * 100.0 / gates.size()));
        model.put("failure_rate_pct", NumberUtils.round1(failRate));
        model.put("coq_breakdown_cny", Map.of(
                "prevention", OptimizationContextService.round0(prevention),
                "appraisal", OptimizationContextService.round0(appraisal),
                "internal_failure", OptimizationContextService.round0(internalFailure),
                "external_failure", OptimizationContextService.round0(externalFailure),
                "total", OptimizationContextService.round0(prevention + appraisal + internalFailure + externalFailure)
        ));
        model.put("saving_per_0_1pct_yield_cny", OptimizationContextService.round0(tonValue * 120));
        model.put("recommendations", List.of(
                contextService.rec("cost", "high", "镀铜工序前置 SPC 可降低内部失败成本",
                        "每提升 0.1% 一次合格率约节省 ¥" + OptimizationContextService.round0(tonValue * 120), "copper_plating", WS_WIRE, "m07")
        ));
        return model;
    }

    public Map<String, Object> model08CapacityUtil(List<OptimizationLineContext> lines) {
        Map<String, Object> profile = masterDataService.getFactoryProfile();
        Map<String, Object> targets = profile.get("targets_2025") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        Map<String, Object> prod2024 = profile.get("production_2024") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        double wireCap = contextService.capacityByWorkshop(lines, WS_WIRE);
        double rodCap = contextService.capacityByWorkshop(lines, WS_ROD);
        double wire2024 = NumberUtils.toDouble(prod2024.getOrDefault("wire_t", 2964));
        double rod2024 = NumberUtils.toDouble(prod2024.getOrDefault("rod_t", 249100));
        double fcwTarget = NumberUtils.toDouble(targets.getOrDefault("fcw_sales_t", 25000));

        double wireUtil = wireCap > 0 ? wire2024 / wireCap * 100 : 0;
        double rodUtil = rodCap > 0 ? rod2024 / rodCap * 100 : 0;
        double fcwCap = lines.stream()
                .filter(lc -> "flux_core_wire".equals(lc.line().getProductCategory()))
                .mapToDouble(lc -> lc.line().getDesignCapacityTPerYear() != null ? lc.line().getDesignCapacityTPerYear() : 0)
                .sum();
        double fcwUtil = fcwCap > 0 ? fcwTarget / fcwCap * 100 : 0;

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m08");
        model.put("title", "产能利用率与边际投资");
        model.put("total_investment_cny", profile.get("total_investment_cny"));
        model.put("wire_design_capacity_t", OptimizationContextService.round0(wireCap));
        model.put("rod_design_capacity_t", OptimizationContextService.round0(rodCap));
        model.put("wire_utilization_2024_pct", NumberUtils.round1(wireUtil));
        model.put("rod_utilization_2024_pct", NumberUtils.round1(rodUtil));
        model.put("fcw_utilization_2025_target_pct", NumberUtils.round1(fcwUtil));
        model.put("marginal_cost_note", "焊丝产能远未饱和，药芯爬坡优先于新建产线");
        model.put("line_utilization", lines.stream().map(lc -> Map.of(
                "product_line_id", lc.lineId(),
                "workshop_id", lc.line().getWorkshopId(),
                "utilization_pct", NumberUtils.round1(lc.estimatedUtilizationPct()),
                "status", lc.line().getStatus()
        )).toList());
        long fcwSim = lines.stream()
                .filter(lc -> "flux_core_wire".equals(lc.line().getProductCategory()))
                .filter(OptimizationLineContext::telemetry)
                .count();
        model.put("recommendations", List.of(
                contextService.rec("cost", "high", "2025 药芯目标 2.5 万吨：优先提升 FCW 仿真线负荷而非资本开支",
                        fcwSim + " 条药芯仿真线设计产能 " + OptimizationContextService.round0(fcwCap) + " 吨/年，当前利用率约 " + NumberUtils.round1(fcwUtil) + "%",
                        null, WS_WIRE, "m08")
        ));
        return model;
    }

    // ── 模型 9–11：质量 ───────────────────────────────────────

    public Map<String, Object> buildQualityModels(List<OptimizationLineContext> lines, String factoryId) {
        Map<String, Object> models = new LinkedHashMap<>();
        models.put("m09_spc_predict", model09SpcPredict(lines));
        models.put("m10_trace", model10TraceIntegrity(lines));
        models.put("m11_grade_mix", model11GradeMix(lines, factoryId));
        return models;
    }

    public Map<String, Object> model09SpcPredict(List<OptimizationLineContext> lines) {
        List<String> criticalFields = List.of(
                "fill_ratio_pct", "coating_thickness_top_um", "drying_temp_C",
                "line_speed_m_per_min", "forming_pressure_MPa", "tension_kN"
        );
        Instant cutoff = Instant.now().minus(properties.getBufferWindowHours(), ChronoUnit.HOURS);
        List<Map<String, Object>> ctqRows = new ArrayList<>();

        for (OptimizationLineContext lc : lines.stream().filter(l -> l.telemetry()).toList()) {
            List<TelemetryPointDoc> allPoints = telemetryPointRepository
                    .findByProductLineIdAndFieldIdInAndTimestampBetweenOrderByTimestampAsc(
                            lc.lineId(), criticalFields, cutoff, Instant.now());
            Map<String, List<TelemetryPointDoc>> byField = allPoints.stream()
                    .collect(Collectors.groupingBy(TelemetryPointDoc::getFieldId));
            for (String fieldId : criticalFields) {
                List<TelemetryPointDoc> points = byField.getOrDefault(fieldId, List.of());
                if (points.isEmpty()) continue;
                List<Double> vals = points.stream()
                        .filter(p -> p.getValue() instanceof Number)
                        .map(p -> ((Number) p.getValue()).doubleValue())
                        .toList();
                if (vals.isEmpty()) continue;
                double mean = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double max = vals.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                Map<String, Double> limits = domainConfigService.getSpecLimits(fieldId);
                double compliance = 100.0;
                double holdRisk = 5.0;
                if (limits.containsKey("lsl") && limits.containsKey("usl")) {
                    long inSpec = vals.stream().filter(v -> v >= limits.get("lsl") && v <= limits.get("usl")).count();
                    compliance = vals.isEmpty() ? 0 : inSpec * 100.0 / vals.size();
                    double usl = limits.get("usl");
                    double lsl = limits.get("lsl");
                    double span = usl - lsl;
                    if (span > 0) {
                        double distToLimit = Math.min(Math.abs(mean - usl), Math.abs(mean - lsl));
                        holdRisk = Math.max(2, Math.min(85, 100 - (distToLimit / span) * 120));
                    }
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("product_line_id", lc.lineId());
                row.put("field_id", fieldId);
                row.put("display_name", domainConfigService.getFieldDef(fieldId) != null
                        ? domainConfigService.getFieldDef(fieldId).get("display_name") : fieldId);
                row.put("mean", NumberUtils.round1(mean));
                row.put("max", NumberUtils.round1(max));
                row.put("spec_compliance_pct", NumberUtils.round1(compliance));
                row.put("hold_risk_pct", NumberUtils.round1(holdRisk));
                row.put("data_confidence", "telemetry");
                ctqRows.add(row);
            }
        }

        for (OptimizationLineContext lc : lines.stream().filter(l -> !l.telemetry()).limit(10).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product_line_id", lc.lineId());
            row.put("field_id", contextService.defaultCtqForCategory(lc.line().getProductCategory()));
            row.put("spec_compliance_pct", NumberUtils.round1(96.5 + lc.qualityPct() * 0.03));
            row.put("hold_risk_pct", NumberUtils.round1(Math.max(3, 15 - lc.qualityPct() * 0.1)));
            row.put("data_confidence", "registry_estimate");
            ctqRows.add(row);
        }

        ctqRows.sort(Comparator.comparingDouble(r -> -NumberUtils.toDouble(r.get("hold_risk_pct"))));

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m09");
        model.put("title", "过程参数 SPC 与预测性质门");
        model.put("ctq_monitoring", ctqRows);
        model.put("ctq_monitoring_count", ctqRows.size());
        model.put("high_risk_count", ctqRows.stream().filter(r -> NumberUtils.toDouble(r.get("hold_risk_pct")) > 40).count());
        model.put("recommendations", List.of(
                contextService.rec("quality", "high", "填充率 CTQ 接近上限时提前降速 3%",
                        "可在终检前降低 68% Hold 概率", "filling_forming", WS_WIRE, "m09")
        ));
        return model;
    }

    public Map<String, Object> model10TraceIntegrity(List<OptimizationLineContext> lines) {
        List<MaterialEventDoc> events = materialEventRepository.findAll();
        List<ProductBatchDoc> batches = productBatchRepository.findAll();
        long complete = batches.stream().filter(b -> b.getParentBatches() != null && !b.getParentBatches().isEmpty()).count();
        long gap = batches.size() - complete;
        double integrityPct = batches.isEmpty() ? 100 : complete * 100.0 / batches.size();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m10");
        model.put("title", "批次追溯完整性");
        model.put("batch_count", batches.size());
        model.put("trace_complete_count", complete);
        model.put("trace_gap_count", gap);
        model.put("integrity_pct", NumberUtils.round1(integrityPct));
        model.put("material_event_count", events.size());
        model.put("export_risk_note", "出口占比 40%，追溯断点将放大合规风险");
        model.put("line_breakdown", lines.stream().filter(lc -> lc.telemetry()).map(lc -> Map.of(
                "product_line_id", lc.lineId(),
                "events", events.stream().filter(e -> lc.lineId().equals(e.getProductLineId())).count(),
                "integrity_pct", NumberUtils.round1(integrityPct)
        )).toList());
        model.put("recommendations", List.of(
                contextService.rec("quality", "medium", "新批次强制写入 parent_batches",
                        "消除 trace_gap 场景导致的追溯断点", null, null, "m10")
        ));
        return model;
    }

    public Map<String, Object> model11GradeMix(List<OptimizationLineContext> lines, String factoryId) {
        List<ProductBatchDoc> batches = productBatchRepository.findAll();
        Map<String, Long> gradeCount = batches.stream()
                .filter(b -> b.getGrade() != null)
                .collect(Collectors.groupingBy(ProductBatchDoc::getGrade, Collectors.counting()));

        List<Map<String, Object>> mixRows = new ArrayList<>();
        for (OptimizationLineContext lc : lines) {
            long changes = batches.stream()
                    .filter(b -> lc.lineId().equals(b.getProductLineId()))
                    .map(ProductBatchDoc::getGrade)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            double recoveryTicks = changes > 2 ? 25.0 + changes * 8 : 12.0;
            mixRows.add(Map.of(
                    "product_line_id", lc.lineId(),
                    "workshop_id", lc.line().getWorkshopId(),
                    "grade_variety", changes,
                    "quality_recovery_ticks", NumberUtils.round1(recoveryTicks),
                    "recommendation", changes > 3 ? "同类规格集中生产" : "换型节奏合理"
            ));
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("model_id", "m11");
        model.put("title", "多规格等级混合优化");
        model.put("grade_distribution", gradeCount);
        model.put("line_mix_analysis", mixRows);
        model.put("recommendations", List.of(
                contextService.rec("quality", "medium", "HY830 及以上等级相邻批次合并排产",
                        "减少换型后 SPC 恢复时间约 18 tick", null, WS_WIRE, "m11")
        ));
        return model;
    }
}

package com.jqhc.dataplatform.service.optimization;

import com.jqhc.dataplatform.domain.ProductLineDoc;
import com.jqhc.dataplatform.repository.AlarmEventRepository;
import com.jqhc.dataplatform.repository.ProductLineRepository;
import com.jqhc.dataplatform.service.ComputeService;
import com.jqhc.dataplatform.service.DomainConfigService;
import com.jqhc.dataplatform.service.FactoryMasterDataService;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OptimizationContextService {

    private static final String DEFAULT_FACTORY = "JQHC-PLANT-01";
    private static final String WS_WIRE = "WS-WIRE-01";
    private static final String WS_ROD = "WS-ROD-01";
    private static final Map<String, String> WORKSHOP_NAMES = Map.of(
            WS_WIRE, "焊丝智能制造车间",
            WS_ROD, "焊条智能制造车间"
    );
    private static final Map<String, String> BOTTLENECK_BY_CATEGORY = Map.of(
            "flux_core_wire", "filling_forming",
            "solid_wire", "copper_plating",
            "submerged_arc_wire", "rough_drawing",
            "welding_rod", "coating"
    );

    private final ProductLineRepository productLineRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final ComputeService computeService;
    private final DomainConfigService domainConfigService;
    private final FactoryMasterDataService masterDataService;

    public OptimizationContextService(
            ProductLineRepository productLineRepository,
            AlarmEventRepository alarmEventRepository,
            ComputeService computeService,
            DomainConfigService domainConfigService,
            FactoryMasterDataService masterDataService) {
        this.productLineRepository = productLineRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.computeService = computeService;
        this.domainConfigService = domainConfigService;
        this.masterDataService = masterDataService;
    }

    public List<OptimizationLineContext> loadOptimizationLineContexts() {
        return productLineRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductLineDoc::getProductLineId))
                .map(this::buildOptimizationLineContext)
                .toList();
    }

    private OptimizationLineContext buildOptimizationLineContext(ProductLineDoc line) {
        String lineId = line.getProductLineId();
        boolean telemetry = Boolean.TRUE.equals(line.getSimulationEnabled());
        Map<String, Object> overview = telemetry ? safeOverview(lineId) : Map.of();
        double oee = 0, avail = 0, perf = 0, qual = 0;
        if (overview.get("kpi_bar") instanceof Map<?, ?> kpi) {
            oee = NumberUtils.toDouble(kpi.get("oee_pct"));
            avail = NumberUtils.toDouble(kpi.get("availability_pct"));
            perf = NumberUtils.toDouble(kpi.get("performance_pct"));
            qual = NumberUtils.toDouble(kpi.get("quality_pct"));
        }
        if (!telemetry) {
            String status = line.getStatus() != null ? line.getStatus() : "active";
            avail = switch (status) {
                case "inactive" -> 12;
                case "maintenance" -> 55;
                default -> 82;
            };
            perf = status.equals("maintenance") ? 70 : 88;
            qual = 96.5;
            oee = NumberUtils.round1(avail * perf * qual / 10000.0);
        }
        long pending = alarmEventRepository.countByProductLineIdAndHandleStatus(lineId, "pending");
        boolean shortage = overview.get("data_source") instanceof Map<?, ?> ds
                && "material_shortage".equals(ds.get("dwell_mode"));
        double recipeDev = telemetry ? 1.5 + (100 - qual) * 0.08 : 2.0;
        return new OptimizationLineContext(line, lineId, telemetry, oee, avail, perf, qual, pending, shortage, recipeDev);
    }

    public Map<String, Object> safeOverview(String lineId) {
        try {
            return computeService.getLineOverview(lineId);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ── 辅助：聚合与展现 ─────────────────────────────────────

    public Map<String, Object> buildPlantLayout(Map<String, Object> profile, List<OptimizationLineContext> lines) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("building_note", "单体厂房 8 万㎡，焊丝车间与焊条车间同栋相邻，逻辑上为双车间一体工厂");
        layout.put("building_area_m2", profile.getOrDefault("building_area_m2", 83423.15));
        layout.put("single_workshop_area_m2", profile.getOrDefault("single_workshop_area_m2", 80000));
        layout.put("total_line_count", lines.size());
        layout.put("wire_line_count", lines.stream().filter(lc -> WS_WIRE.equals(lc.line().getWorkshopId())).count());
        layout.put("rod_line_count", lines.stream().filter(lc -> WS_ROD.equals(lc.line().getWorkshopId())).count());
        layout.put("telemetry_line_count", lines.stream().filter(lc -> lc.telemetry()).count());
        layout.put("registry_line_count", lines.stream().filter(lc -> !lc.telemetry()).count());
        return layout;
    }

    public List<Map<String, Object>> summarizeWorkshops(List<OptimizationLineContext> lines) {
        return List.of(
                workshopSummary(WS_WIRE, lines),
                workshopSummary(WS_ROD, lines)
        );
    }

    public Map<String, Object> workshopSummary(String workshopId, List<OptimizationLineContext> lines) {
        List<OptimizationLineContext> ws = lines.stream().filter(lc -> workshopId.equals(lc.line().getWorkshopId())).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("workshop_id", workshopId);
        m.put("workshop_name", WORKSHOP_NAMES.getOrDefault(workshopId, workshopId));
        m.put("line_count", ws.size());
        m.put("active_lines", ws.stream().filter(lc -> "active".equals(lc.line().getStatus())).count());
        m.put("telemetry_lines", ws.stream().filter(OptimizationLineContext::telemetry).count());
        m.put("simulation_lines", ws.stream().filter(OptimizationLineContext::telemetry).count());
        m.put("registry_lines", ws.stream().filter(lc -> !lc.telemetry()).count());
        m.put("avg_oee_pct", NumberUtils.round1(ws.stream().mapToDouble(lc -> lc.oeePct()).average().orElse(0)));
        m.put("design_capacity_t_per_year", round0(capacityByWorkshop(lines, workshopId)));
        m.put("bottleneck_step", topBottleneckForWorkshop(lines, workshopId));
        return m;
    }

    public Map<String, Object> buildWorkshopDetail(String workshopId, List<OptimizationLineContext> ws, Map<String, Object> energy) {
        Map<String, Object> detail = workshopSummary(workshopId, ws);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = energy.get("line_breakdown") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        double kwh = breakdown.stream()
                .filter(r -> ws.stream().anyMatch(lc -> lc.lineId().equals(r.get("product_line_id"))))
                .mapToDouble(r -> NumberUtils.toDouble(r.get("consumption_kwh"))).sum();
        detail.put("consumption_kwh", NumberUtils.round1(kwh));
        detail.put("lines", ws.stream().map(this::buildLineSnapshot).toList());
        return detail;
    }

    public Map<String, Object> buildLineSnapshot(OptimizationLineContext lc) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("product_line_id", lc.lineId());
        snap.put("line_name", lc.line().getName());
        snap.put("workshop_id", lc.line().getWorkshopId());
        snap.put("product_category", lc.line().getProductCategory());
        snap.put("status", lc.line().getStatus());
        snap.put("simulation_enabled", lc.telemetry());
        snap.put("detail_level", lc.line().getDetailLevel());
        snap.put("design_capacity_t_per_year", lc.line().getDesignCapacityTPerYear());
        snap.put("oee_pct", NumberUtils.round1(lc.oeePct()));
        snap.put("bottleneck_step", resolveBottleneck(lc));
        snap.put("utilization_pct", NumberUtils.round1(lc.estimatedUtilizationPct()));
        snap.put("pending_alarms", lc.pendingAlarms());
        snap.put("data_confidence", lc.telemetry() ? "telemetry" : "registry_estimate");
        return snap;
    }

    public Map<String, Object> buildLineSummary(List<OptimizationLineContext> lines) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total_lines", lines.size());
        s.put("telemetry_lines", lines.stream().filter(OptimizationLineContext::telemetry).count());
        s.put("registry_lines", lines.stream().filter(lc -> !lc.telemetry()).count());
        s.put("wire_lines", lines.stream().filter(lc -> WS_WIRE.equals(lc.line().getWorkshopId())).count());
        s.put("rod_lines", lines.stream().filter(lc -> WS_ROD.equals(lc.line().getWorkshopId())).count());
        s.put("active_lines", lines.stream().filter(lc -> "active".equals(lc.line().getStatus())).count());
        s.put("maintenance_lines", lines.stream().filter(lc -> "maintenance".equals(lc.line().getStatus())).count());
        s.put("inactive_lines", lines.stream().filter(lc -> "inactive".equals(lc.line().getStatus())).count());
        return s;
    }

    public Map<String, Object> buildHeadlineKpis(Map<String, Object> kpis, Map<String, Object> energy, List<OptimizationLineContext> lines) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("total_lines", lines.size());
        h.put("telemetry_lines", lines.stream().filter(OptimizationLineContext::telemetry).count());
        h.put("active_lines", lines.stream().filter(lc -> "active".equals(lc.line().getStatus())).count());
        h.put("avg_oee_pct", kpis.get("avg_oee_pct"));
        h.put("quality_pass_rate_pct", kpis.get("quality_pass_rate_pct"));
        h.put("green_power_ratio_pct", energy.get("green_power_ratio_pct"));
        h.put("high_risk_orders", kpis.get("high_risk_orders"));
        h.put("target_fcw_sales_t_2025", kpis.get("target_fcw_sales_t_2025"));
        return h;
    }

    public List<Map<String, Object>> listModelStatus(
            Map<String, Object> efficiency, Map<String, Object> cost, Map<String, Object> quality) {
        List<Map<String, Object>> all = new ArrayList<>();
        int idx = 1;
        for (Map.Entry<String, Object> e : efficiency.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> m) {
                all.add(modelCard((Map<String, Object>) m, "efficiency", idx++));
            }
        }
        for (Map.Entry<String, Object> e : cost.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> m) {
                all.add(modelCard((Map<String, Object>) m, "cost", idx++));
            }
        }
        for (Map.Entry<String, Object> e : quality.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> m) {
                all.add(modelCard((Map<String, Object>) m, "quality", idx++));
            }
        }
        return all;
    }

    public Map<String, Object> modelCard(Map<String, Object> model, String dimension, int order) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("order", order);
        card.put("model_id", model.get("model_id"));
        card.put("title", model.get("title"));
        card.put("dimension", dimension);
        card.put("route", "/optimization/" + dimension);
        return card;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> collectRecommendations(
            Map<String, Object> efficiency,
            Map<String, Object> cost,
            Map<String, Object> quality,
            int limit) {
        List<Map<String, Object>> all = new ArrayList<>();
        Stream.of(efficiency, cost, quality).forEach(models -> {
            for (Object v : models.values()) {
                if (v instanceof Map<?, ?> m && m.get("recommendations") instanceof List<?> recs) {
                    for (Object r : recs) {
                        if (r instanceof Map<?, ?>) {
                            all.add(new LinkedHashMap<>((Map<String, Object>) r));
                        }
                    }
                }
            }
        });
        Map<String, Integer> priority = Map.of("high", 0, "medium", 1, "low", 2);
        all.sort(Comparator.comparingInt(r -> priority.getOrDefault(String.valueOf(r.get("priority")), 9)));
        return all.stream().limit(limit).toList();
    }

    public Map<String, Object> rec(String dimension, String priority, String title, String detail,
                                    String stepId, String workshopId, String modelId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dimension", dimension);
        r.put("priority", priority);
        r.put("title", title);
        r.put("detail", detail);
        if (stepId != null) r.put("process_step_id", stepId);
        if (workshopId != null) r.put("workshop_id", workshopId);
        r.put("model_id", modelId);
        return r;
    }

    // ── 估算公式 ─────────────────────────────────────────────

    public String resolveBottleneck(OptimizationLineContext lc) {
        if (lc.telemetry()) {
            try {
                Map<String, Object> overview = computeService.getLineOverview(lc.lineId());
                if (overview.get("process_pipeline") instanceof List<?> pipeline) {
                    for (Object step : pipeline) {
                        if (step instanceof Map<?, ?> s) {
                            String status = String.valueOf(s.get("aggregate_status"));
                            if ("alarm".equals(status) || "warning".equals(status) || "blocked".equals(status)) {
                                Object sid = s.get("process_step_id");
                                if (sid != null) return String.valueOf(sid);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return BOTTLENECK_BY_CATEGORY.getOrDefault(
                lc.line().getProductCategory() != null ? lc.line().getProductCategory() : "",
                "packaging");
    }

    public double estimateFlowEfficiencyTelemetry(OptimizationLineContext lc) {
        return Math.min(95, Math.max(55, lc.oeePct() * 0.85 + lc.qualityPct() * 0.15));
    }

    public double estimateFlowEfficiencyRegistry(OptimizationLineContext lc) {
        String status = lc.line().getStatus() != null ? lc.line().getStatus() : "active";
        double base = switch (status) {
            case "inactive" -> 35;
            case "maintenance" -> 58;
            default -> 78;
        };
        return base;
    }

    public String topBottleneckForWorkshop(List<OptimizationLineContext> lines, String workshopId) {
        return lines.stream()
                .filter(lc -> workshopId.equals(lc.line().getWorkshopId()))
                .map(this::resolveBottleneck)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("packaging");
    }

    public double workshopOee(List<OptimizationLineContext> lines, String workshopId) {
        return lines.stream()
                .filter(lc -> workshopId.equals(lc.line().getWorkshopId()))
                .mapToDouble(lc -> lc.oeePct())
                .average().orElse(0);
    }

    public double capacityByWorkshop(List<OptimizationLineContext> lines, String workshopId) {
        return lines.stream()
                .filter(lc -> workshopId.equals(lc.line().getWorkshopId()))
                .mapToDouble(lc -> lc.line().getDesignCapacityTPerYear() != null ? lc.line().getDesignCapacityTPerYear() : 0)
                .sum();
    }

    public String lineWorkshop(Object lineId, List<OptimizationLineContext> lines) {
        if (lineId == null) return "";
        return lines.stream()
                .filter(lc -> lc.lineId().equals(String.valueOf(lineId)))
                .map(lc -> lc.line().getWorkshopId())
                .findFirst().orElse("");
    }

    public String defaultCtqForCategory(String category) {
        return switch (category != null ? category : "") {
            case "flux_core_wire" -> "fill_ratio_pct";
            case "solid_wire" -> "coating_thickness_top_um";
            case "welding_rod" -> "coating_thickness_top_um";
            default -> "line_speed_m_per_min";
        };
    }

    public String stepLabel(String stepId) {
        return domainConfigService.getStepDisplayName(stepId);
    }

    public double compositeIndex(Map<String, Object> models) {
        if (models.isEmpty()) return 70;
        double sum = 0;
        int count = 0;
        for (Object v : models.values()) {
            if (v instanceof Map<?, ?> m) {
                sum += scoreModel((Map<String, Object>) m);
                count++;
            }
        }
        return count == 0 ? 70 : sum / count;
    }

    public double scoreModel(Map<String, Object> model) {
        String id = String.valueOf(model.getOrDefault("model_id", ""));
        return switch (id) {
            case "m01" -> NumberUtils.toDouble(model.get("avg_flow_efficiency_pct"));
            case "m02" -> Math.max(50, 88 - NumberUtils.toDouble(model.get("high_risk_orders")) * 4);
            case "m03" -> NumberUtils.toDouble(model.get("plant_avg_oee_pct"));
            case "m04" -> NumberUtils.toDouble(model.get("completion_rate_pct"));
            case "m05" -> NumberUtils.toDouble(model.get("green_power_ratio_pct"));
            case "m06" -> Math.max(60, 98 - NumberUtils.toDouble(model.get("avg_recipe_deviation_score")) * 8);
            case "m07" -> NumberUtils.toDouble(model.get("first_pass_rate_pct"));
            case "m08" -> Math.min(95, 55 + NumberUtils.toDouble(model.get("rod_utilization_2024_pct")) * 0.15);
            case "m09" -> Math.max(55, 100 - NumberUtils.toDouble(model.get("high_risk_count")) * 6);
            case "m10" -> NumberUtils.toDouble(model.get("integrity_pct"));
            case "m11" -> 82;
            default -> 75;
        };
    }

    public String indexLabel(double index) {
        if (index >= 85) return "优秀";
        if (index >= 75) return "良好";
        if (index >= 65) return "待改善";
        return "需关注";
    }

    public String resolveFactoryId(String factoryId) {
        return NumberUtils.resolveFactoryId(factoryId, DEFAULT_FACTORY);
    }

    public static long round0(double v) {
        return Math.round(v);
    }
}

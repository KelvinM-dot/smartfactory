package com.jqhc.dataplatform.service.optimization;

import com.jqhc.dataplatform.domain.ProductBatchDoc;
import com.jqhc.dataplatform.repository.ProductBatchRepository;
import com.jqhc.dataplatform.service.FactoryMasterDataService;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/**
 * 智优能碳决策 — 能源结构、用能诊断、碳排核算与绿电协同建议（独立模块）。
 */
@Service
public class OptimizationEnergyCarbonService {

    private static final String WS_WIRE = "WS-WIRE-01";
    private static final String WS_ROD = "WS-ROD-01";
    private static final double DEFAULT_GRID_EMISSION_FACTOR = 0.5704;
    private static final double DEFAULT_GREEN_OFFSET_COEFF = 0.8;

    private static final List<Map<String, Object>> HIGH_ENERGY_PROCESSES = List.of(
            Map.of("step_id", "drying", "label", "烘干", "share_pct", 24.0, "green_window_recommended", true),
            Map.of("step_id", "copper_plating", "label", "镀铜", "share_pct", 18.0, "green_window_recommended", true),
            Map.of("step_id", "fine_drawing", "label", "精拉", "share_pct", 15.0, "green_window_recommended", true),
            Map.of("step_id", "rough_drawing", "label", "粗拉", "share_pct", 12.0, "green_window_recommended", false),
            Map.of("step_id", "filling_forming", "label", "药芯填充", "share_pct", 14.0, "green_window_recommended", false),
            Map.of("step_id", "coating", "label", "压涂", "share_pct", 12.0, "green_window_recommended", false),
            Map.of("step_id", "other", "label", "其他工序", "share_pct", 5.0, "green_window_recommended", false)
    );

    private static final Set<String> ENERGY_SCENARIO_IDS = Set.of(
            "clean_energy_noon", "plant_green_shift_60", "plant_green_shift_80"
    );

    private final FactoryMasterDataService masterDataService;
    private final ProductBatchRepository productBatchRepository;

    public OptimizationEnergyCarbonService(
            FactoryMasterDataService masterDataService,
            ProductBatchRepository productBatchRepository) {
        this.masterDataService = masterDataService;
        this.productBatchRepository = productBatchRepository;
    }

    public Map<String, Object> buildOverview(
            String factoryId,
            Map<String, Object> energy,
            Map<String, Object> m05,
            List<OptimizationLineContext> lines,
            Map<String, Object> lineSummary) {

        Map<String, Object> carbon = buildCarbon(energy, lines);
        Map<String, Object> headline = buildHeadline(energy, carbon, m05);
        Map<String, Object> supply = buildSupply(energy);
        Map<String, Object> demand = buildDemand(energy, lines);
        List<Map<String, Object>> recommendations = buildRecommendations(energy, carbon, m05, headline);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("factory_id", factoryId);
        result.put("computed_at", Instant.now());
        result.put("headline", headline);
        result.put("supply", supply);
        result.put("demand", demand);
        result.put("carbon", carbon);
        result.put("recommendations", recommendations);
        result.put("model_m05", m05 != null ? m05 : Map.of());
        result.put("line_summary", lineSummary != null ? lineSummary : Map.of());
        result.put("energy_scenarios", listEnergyScenarios());
        result.put("linkage", buildLinkageHints());
        result.put("methodology", Map.of(
                "carbon", "grid_factor_v1",
                "grid_emission_factor_kg_per_kwh", emissionFactor(),
                "green_offset_coefficient", greenOffsetCoeff(),
                "note", "Scope2 外购电力碳排；绿电抵扣为启发式估算，非 CBAM 认证口径"
        ));
        return result;
    }

    public Map<String, Object> projectGreenShift(
            Map<String, Object> energy,
            Map<String, Object> kpiSnapshot,
            double greenShiftPct) {

        double clamped = Math.max(0, Math.min(100, greenShiftPct));
        double totalKwh = NumberUtils.toDouble(energy.get("total_consumption_kwh"));
        double baseGreenPct = NumberUtils.toDouble(energy.get("green_power_ratio_pct"));
        double projectedGreenPct = NumberUtils.round1(Math.min(95, Math.max(18,
                baseGreenPct + (clamped - 50) * 0.22)));
        double greenKwh = totalKwh * projectedGreenPct / 100.0;
        double gridKwh = Math.max(0, totalKwh - greenKwh);

        double factor = emissionFactor();
        double coeff = greenOffsetCoeff();
        double scope2 = gridKwh * factor;
        double offset = greenKwh * factor * coeff;
        double netCarbon = Math.max(0, scope2 - offset);
        double baseGrid = NumberUtils.toDouble(energy.get("grid_power_kwh"));
        double baseScope2 = baseGrid * factor;
        double baseOffset = NumberUtils.toDouble(energy.get("green_power_kwh")) * factor * coeff;
        double baseNet = Math.max(0, baseScope2 - baseOffset);

        @SuppressWarnings("unchecked")
        Map<String, Object> extremes = kpiSnapshot.get("three_extremes") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        double baseEff = NumberUtils.toDouble(extremes.get("efficiency_index"));
        double baseCost = NumberUtils.toDouble(extremes.get("cost_index"));
        double baseQual = NumberUtils.toDouble(extremes.get("quality_index"));
        if (baseEff <= 0) baseEff = 75;
        if (baseCost <= 0) baseCost = 72;
        if (baseQual <= 0) baseQual = 78;

        double projCost = baseCost + (clamped - 50) * 0.18 - (projectedGreenPct - baseGreenPct) * 0.05;
        double projEff = baseEff + clamped * 0.02;
        double projQual = baseQual - clamped * 0.01;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("green_shift_pct", NumberUtils.round1(clamped));
        result.put("baseline", Map.of(
                "green_power_ratio_pct", NumberUtils.round1(baseGreenPct),
                "grid_power_kwh", NumberUtils.round1(baseGrid),
                "net_kg_co2e", NumberUtils.round1(baseNet),
                "cost_index", NumberUtils.round1(baseCost)
        ));
        result.put("projected", Map.of(
                "green_power_ratio_pct", projectedGreenPct,
                "grid_power_kwh", NumberUtils.round1(gridKwh),
                "net_kg_co2e", NumberUtils.round1(netCarbon),
                "cost_index", NumberUtils.round1(Math.min(98, Math.max(40, projCost))),
                "efficiency_index", NumberUtils.round1(Math.min(98, Math.max(40, projEff))),
                "quality_index", NumberUtils.round1(Math.min(98, Math.max(40, projQual)))
        ));
        result.put("delta", Map.of(
                "green_power_ratio_pct", NumberUtils.round1(projectedGreenPct - baseGreenPct),
                "net_kg_co2e", NumberUtils.round1(netCarbon - baseNet),
                "cost_index", NumberUtils.round1(projCost - baseCost)
        ));
        result.put("hint", clamped >= 70
                ? "高绿电偏移有利于降碳，需确认高耗能工序已排入 11:00–14:00 窗口"
                : "绿电偏移偏低，电网购电与碳排成本偏高");
        return result;
    }

    private Map<String, Object> buildHeadline(Map<String, Object> energy, Map<String, Object> carbon, Map<String, Object> m05) {
        double annualTarget = NumberUtils.toDouble(energy.get("annual_carbon_reduction_t"));
        if (annualTarget <= 0) annualTarget = 35000;
        int dayOfYear = Instant.now().atZone(ZoneId.of("Asia/Shanghai")).getDayOfYear();
        double progressPct = NumberUtils.round1(Math.min(100, dayOfYear / 365.0 * 100));

        Map<String, Object> h = new LinkedHashMap<>();
        h.put("total_consumption_kwh", energy.get("total_consumption_kwh"));
        h.put("total_active_power_kw", energy.get("total_active_power_kw"));
        h.put("green_power_kwh", energy.get("green_power_kwh"));
        h.put("grid_power_kwh", energy.get("grid_power_kwh"));
        h.put("green_power_ratio_pct", energy.get("green_power_ratio_pct"));
        h.put("carbon_intensity_kg_per_t", carbon.get("carbon_intensity_kg_per_t"));
        h.put("net_kg_co2e", carbon.get("net_kg_co2e"));
        h.put("annual_carbon_reduction_target_t", annualTarget);
        h.put("annual_carbon_reduction_progress_pct", progressPct);
        h.put("annual_carbon_reduction_ytd_t", NumberUtils.round1(annualTarget * progressPct / 100.0));
        h.put("green_window_active", m05 != null ? m05.get("green_window_active") : isGreenWindowActive());
        h.put("green_window_hint", m05 != null ? m05.get("green_window_hint")
                : "11:00–14:00 光伏峰值，建议高耗能工序排入该窗口");
        h.put("wire_shop_kwh", m05 != null ? m05.get("wire_shop_kwh") : null);
        h.put("rod_shop_kwh", m05 != null ? m05.get("rod_shop_kwh") : null);
        return h;
    }

    private Map<String, Object> buildSupply(Map<String, Object> energy) {
        List<Map<String, Object>> assets = masterDataService.getEnergyAssets().stream()
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>(a);
                    String type = String.valueOf(a.getOrDefault("asset_type", ""));
                    row.put("type_label", assetTypeLabel(type));
                    return row;
                }).toList();

        double totalKwh = NumberUtils.toDouble(energy.get("total_consumption_kwh"));
        double greenKwh = NumberUtils.toDouble(energy.get("green_power_kwh"));
        double gridKwh = NumberUtils.toDouble(energy.get("grid_power_kwh"));

        Map<String, Object> mix = new LinkedHashMap<>();
        mix.put("pv_kwh", NumberUtils.round1(greenKwh * 0.55));
        mix.put("wind_kwh", NumberUtils.round1(greenKwh * 0.25));
        mix.put("waste_heat_saving_kwh", NumberUtils.round1(greenKwh * 0.12));
        mix.put("grid_kwh", NumberUtils.round1(gridKwh));
        mix.put("total_kwh", NumberUtils.round1(totalKwh));

        Map<String, Object> supply = new LinkedHashMap<>();
        supply.put("assets", assets);
        supply.put("generation_mix", mix);
        supply.put("annual_clean_generation_kwh", energy.get("annual_clean_generation_kwh"));
        supply.put("hourly_green_curve", buildHourlyGreenCurve());
        return supply;
    }

    private Map<String, Object> buildDemand(Map<String, Object> energy, List<OptimizationLineContext> lines) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = energy.get("line_breakdown") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();

        double wireTotal = 0;
        double rodTotal = 0;
        List<Map<String, Object>> enrichedLines = new ArrayList<>();
        Map<String, Double> workshopSums = new HashMap<>();
        Map<String, Integer> workshopCounts = new HashMap<>();

        for (Map<String, Object> row : breakdown) {
            String lineId = String.valueOf(row.get("product_line_id"));
            String workshopId = resolveWorkshop(lineId, lines);
            double consumption = NumberUtils.toDouble(row.get("consumption_kwh"));
            workshopSums.merge(workshopId, consumption, Double::sum);
            workshopCounts.merge(workshopId, 1, Integer::sum);
            if (WS_WIRE.equals(workshopId)) wireTotal += consumption;
            if (WS_ROD.equals(workshopId)) rodTotal += consumption;

            Map<String, Object> enriched = new LinkedHashMap<>(row);
            enriched.put("workshop_id", workshopId);
            enriched.put("workshop_name", workshopName(workshopId));
            enriched.put("anomalies", detectAnomalies(row, workshopId, workshopSums, workshopCounts));
            enrichedLines.add(enriched);
        }

        enrichedLines.sort((a, b) -> Double.compare(
                NumberUtils.toDouble(b.get("consumption_kwh")),
                NumberUtils.toDouble(a.get("consumption_kwh"))));

        double total = NumberUtils.toDouble(energy.get("total_consumption_kwh"));
        List<Map<String, Object>> workshopBreakdown = List.of(
                workshopRow(WS_WIRE, "焊丝智能制造车间", wireTotal, total),
                workshopRow(WS_ROD, "焊条智能制造车间", rodTotal, total)
        );

        List<Map<String, Object>> processRanking = buildProcessRanking(total);

        Map<String, Object> demand = new LinkedHashMap<>();
        demand.put("workshop_breakdown", workshopBreakdown);
        demand.put("line_breakdown", enrichedLines);
        demand.put("process_ranking", processRanking);
        demand.put("total_lines", breakdown.size());
        return demand;
    }

    private Map<String, Object> buildCarbon(Map<String, Object> energy, List<OptimizationLineContext> lines) {
        double gridKwh = NumberUtils.toDouble(energy.get("grid_power_kwh"));
        double greenKwh = NumberUtils.toDouble(energy.get("green_power_kwh"));
        double factor = emissionFactor();
        double coeff = greenOffsetCoeff();

        double scope2 = gridKwh * factor;
        double greenOffset = greenKwh * factor * coeff;
        double net = Math.max(0, scope2 - greenOffset);
        double estimatedTons = estimateOutputTons(energy, lines);
        double intensity = estimatedTons > 0 ? net / estimatedTons : 0;

        Map<String, Object> carbon = new LinkedHashMap<>();
        carbon.put("scope1_kg_co2e", 0.0);
        carbon.put("scope2_kg_co2e", NumberUtils.round1(scope2));
        carbon.put("scope3_kg_co2e", 0.0);
        carbon.put("green_power_offset_kg_co2e", NumberUtils.round1(greenOffset));
        carbon.put("net_kg_co2e", NumberUtils.round1(net));
        carbon.put("estimated_output_t", NumberUtils.round1(estimatedTons));
        carbon.put("carbon_intensity_kg_per_t", NumberUtils.round1(intensity));
        carbon.put("annual_carbon_reduction_t", energy.get("annual_carbon_reduction_t"));
        return carbon;
    }

    private List<Map<String, Object>> buildRecommendations(
            Map<String, Object> energy,
            Map<String, Object> carbon,
            Map<String, Object> m05,
            Map<String, Object> headline) {

        List<Map<String, Object>> recs = new ArrayList<>();
        boolean greenWindow = Boolean.TRUE.equals(headline.get("green_window_active"));
        double greenPct = NumberUtils.toDouble(energy.get("green_power_ratio_pct"));
        double gridPct = energy.get("total_consumption_kwh") != null && NumberUtils.toDouble(energy.get("total_consumption_kwh")) > 0
                ? NumberUtils.toDouble(energy.get("grid_power_kwh")) / NumberUtils.toDouble(energy.get("total_consumption_kwh")) * 100
                : 0;

        if (greenWindow) {
            recs.add(rec("high", "当前处于绿电窗口（11:00–14:00）",
                    "优先排入烘干、镀铜、精拉等高耗能工序批次", "cost"));
        } else {
            recs.add(rec("medium", "下一绿电窗口预排",
                    "建议将高耗能批次预排至 11:00–14:00，可再降购电成本约 4–7%", "cost"));
        }
        if (greenPct < 55) {
            recs.add(rec("high", "绿电占比偏低（" + NumberUtils.round1(greenPct) + "%）",
                    "建议提高绿电排产偏移至 70%+，降低 Scope2 碳排", "carbon"));
        }
        if (gridPct > 42) {
            recs.add(rec("medium", "电网购电占比 " + NumberUtils.round1(gridPct) + "%",
                    "结合余热回收与光伏出力高峰，压缩峰段购电", "energy"));
        }
        double intensity = NumberUtils.toDouble(carbon.get("carbon_intensity_kg_per_t"));
        if (intensity > 80) {
            recs.add(rec("medium", "碳排强度偏高（" + NumberUtils.round1(intensity) + " kg/t）",
                    "检查高耗能产线停线空转与维保 dwell 能耗", "carbon"));
        }

        if (m05 != null && m05.get("recommendations") instanceof List<?> m05recs) {
            for (Object item : m05recs) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    m.forEach((k, v) -> r.put(String.valueOf(k), v));
                    r.putIfAbsent("category", "energy");
                    recs.add(r);
                }
            }
        }
        return recs.stream().limit(8).toList();
    }

    private List<Map<String, Object>> buildHourlyGreenCurve() {
        List<Map<String, Object>> curve = new ArrayList<>();
        Map<String, Object> defaults = masterDataService.getSimulationDefaults();
        double base = NumberUtils.toDouble(defaults.getOrDefault("default_green_power_ratio_pct", 60.0));
        List<Map<String, Object>> assets = masterDataService.getEnergyAssets();
        double pvKw = assets.stream().filter(a -> "pv".equals(a.get("asset_type")))
                .mapToDouble(a -> NumberUtils.toDouble(a.get("rated_power_kw"))).sum();
        double windKw = assets.stream().filter(a -> "wind".equals(a.get("asset_type")))
                .mapToDouble(a -> NumberUtils.toDouble(a.get("rated_power_kw"))).sum();
        double total = Math.max(pvKw + windKw, 1.0);

        for (int hour = 0; hour < 24; hour++) {
            double pvCurve = (hour >= 6 && hour <= 18)
                    ? Math.pow(Math.cos((hour - 12.0) / 6.0 * Math.PI / 2.0), 2) : 0.0;
            double dynamic = base * 0.35 + (pvKw / total) * pvCurve * 100.0 + (windKw / total) * 18.0;
            double pct = NumberUtils.round1(Math.min(95, Math.max(18, dynamic)));
            boolean inWindow = hour >= 11 && hour <= 14;
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("hour", hour);
            point.put("label", String.format("%02d:00", hour));
            point.put("green_power_ratio_pct", pct);
            point.put("green_window", inWindow);
            curve.add(point);
        }
        return curve;
    }

    private List<Map<String, Object>> buildProcessRanking(double totalKwh) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> proc : HIGH_ENERGY_PROCESSES) {
            double share = NumberUtils.toDouble(proc.get("share_pct"));
            Map<String, Object> row = new LinkedHashMap<>(proc);
            row.put("consumption_kwh", NumberUtils.round1(totalKwh * share / 100.0));
            row.put("share_pct", NumberUtils.round1(share));
            ranking.add(row);
        }
        ranking.sort((a, b) -> Double.compare(
                NumberUtils.toDouble(b.get("consumption_kwh")),
                NumberUtils.toDouble(a.get("consumption_kwh"))));
        return ranking;
    }

    private List<String> detectAnomalies(
            Map<String, Object> row,
            String workshopId,
            Map<String, Double> workshopSums,
            Map<String, Integer> workshopCounts) {

        List<String> flags = new ArrayList<>();
        double consumption = NumberUtils.toDouble(row.get("consumption_kwh"));
        int count = workshopCounts.getOrDefault(workshopId, 1);
        double avg = workshopSums.getOrDefault(workshopId, consumption) / Math.max(count, 1);
        if (consumption > avg * 1.3) {
            flags.add("high_consumption");
        }
        if (Boolean.TRUE.equals(row.get("dwell_mode"))) {
            flags.add("dwell_active");
        }
        String status = String.valueOf(row.getOrDefault("line_status", "active"));
        if ("maintenance".equals(status) && consumption > avg * 0.5) {
            flags.add("maint_energy");
        }
        if (consumption < 1 && "active".equals(status)) {
            flags.add("low_output");
        }
        return flags;
    }

    private Map<String, Object> workshopRow(String id, String name, double kwh, double total) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("workshop_id", id);
        row.put("workshop_name", name);
        row.put("consumption_kwh", NumberUtils.round1(kwh));
        row.put("share_pct", total > 0 ? NumberUtils.round1(kwh / total * 100) : 0);
        return row;
    }

    private double estimateOutputTons(Map<String, Object> energy, List<OptimizationLineContext> lines) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = energy.get("line_breakdown") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        double tons = 0;
        for (Map<String, Object> row : breakdown) {
            double kwh = NumberUtils.toDouble(row.get("consumption_kwh"));
            double specific = NumberUtils.toDouble(row.get("specific_energy_kwh_per_t"));
            if (specific > 0.01) {
                tons += kwh / specific;
            }
        }
        if (tons > 0) return tons;
        return lines.stream()
                .filter(lc -> "active".equals(lc.line().getStatus()))
                .mapToDouble(lc -> lc.line().getDesignCapacityTPerDay() != null ? lc.line().getDesignCapacityTPerDay() : 8)
                .sum();
    }

    private String resolveWorkshop(String lineId, List<OptimizationLineContext> lines) {
        return lines.stream()
                .filter(lc -> lc.lineId().equals(lineId))
                .map(lc -> lc.line().getWorkshopId())
                .findFirst().orElse(lineId.startsWith("WR") ? WS_ROD : WS_WIRE);
    }

    private String workshopName(String workshopId) {
        return switch (workshopId) {
            case WS_WIRE -> "焊丝智能制造车间";
            case WS_ROD -> "焊条智能制造车间";
            default -> workshopId;
        };
    }

    private String assetTypeLabel(String type) {
        return switch (type) {
            case "pv" -> "光伏";
            case "wind" -> "风电";
            case "grid" -> "电网";
            case "waste_heat" -> "余热回收";
            default -> type;
        };
    }

    private boolean isGreenWindowActive() {
        int hour = Instant.now().atZone(ZoneId.of("Asia/Shanghai")).getHour();
        return hour >= 11 && hour <= 14;
    }

    private double emissionFactor() {
        Map<String, Object> defaults = masterDataService.getSimulationDefaults();
        double v = NumberUtils.toDouble(defaults.get("grid_emission_factor_kg_per_kwh"));
        return v > 0 ? v : DEFAULT_GRID_EMISSION_FACTOR;
    }

    private double greenOffsetCoeff() {
        Map<String, Object> defaults = masterDataService.getSimulationDefaults();
        double v = NumberUtils.toDouble(defaults.get("green_power_offset_coefficient"));
        return v > 0 ? v : DEFAULT_GREEN_OFFSET_COEFF;
    }

    private Map<String, Object> rec(String priority, String title, String detail, String category) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("priority", priority);
        m.put("title", title);
        m.put("detail", detail);
        m.put("category", category);
        return m;
    }

    public Map<String, Object> buildBatchFootprints(
            Map<String, Object> energy,
            List<OptimizationLineContext> lines,
            int limit) {

        double greenPct = NumberUtils.toDouble(energy.get("green_power_ratio_pct")) / 100.0;
        double factor = emissionFactor();
        double coeff = greenOffsetCoeff();

        Map<String, Map<String, Object>> lineEnergy = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = energy.get("line_breakdown") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        for (Map<String, Object> row : breakdown) {
            lineEnergy.put(String.valueOf(row.get("product_line_id")), row);
        }

        List<ProductBatchDoc> batches = productBatchRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductBatchDoc::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(limit, 1))
                .toList();

        List<Map<String, Object>> records = new ArrayList<>();
        double totalNet = 0;
        double totalTons = 0;

        for (ProductBatchDoc batch : batches) {
            String lineId = batch.getProductLineId();
            double qtyKg = batch.getQuantityKg() != null ? batch.getQuantityKg() : 0;
            double tons = qtyKg / 1000.0;
            if (tons <= 0) continue;

            Map<String, Object> lineRow = lineEnergy.getOrDefault(lineId, Map.of());
            double specificPerT = NumberUtils.toDouble(lineRow.get("specific_energy_kwh_per_t"));
            if (specificPerT <= 0.01) specificPerT = 120.0;

            double batchKwh = tons * specificPerT;
            double gridKwh = batchKwh * (1.0 - greenPct);
            double greenKwh = batchKwh * greenPct;
            double scope2 = gridKwh * factor;
            double offset = greenKwh * factor * coeff;
            double net = Math.max(0, scope2 - offset);
            double intensity = tons > 0 ? net / tons : 0;

            totalNet += net;
            totalTons += tons;

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("record_id", "CF-" + batch.getBatchId());
            record.put("batch_id", batch.getBatchId());
            record.put("order_ref", batch.getProductionOrderId());
            record.put("product_line_id", lineId);
            record.put("grade", batch.getGrade());
            record.put("status", batch.getStatus());
            record.put("quantity_kg", NumberUtils.round1(qtyKg));
            record.put("quantity_t", NumberUtils.round1(tons));
            record.put("energy_kwh", NumberUtils.round1(batchKwh));
            record.put("scope2_kg_co2e", NumberUtils.round1(scope2));
            record.put("green_power_offset_kg_co2e", NumberUtils.round1(offset));
            record.put("total_kg_co2e", NumberUtils.round1(net));
            record.put("carbon_intensity_kg_per_t", NumberUtils.round1(intensity));
            record.put("methodology", "batch_allocation_v1");
            record.put("calculated_at", Instant.now());
            records.add(record);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("batch_count", records.size());
        summary.put("total_net_kg_co2e", NumberUtils.round1(totalNet));
        summary.put("total_quantity_t", NumberUtils.round1(totalTons));
        summary.put("avg_intensity_kg_per_t", totalTons > 0 ? NumberUtils.round1(totalNet / totalTons) : 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("computed_at", Instant.now());
        result.put("summary", summary);
        result.put("records", records);
        return result;
    }

    public List<Map<String, Object>> listEnergyScenarios() {
        Object scenarios = masterDataService.getSimulationDefaults().get("scenarios");
        if (!(scenarios instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            String id = String.valueOf(raw.get("id"));
            if (!ENERGY_SCENARIO_IDS.contains(id) && !id.startsWith("plant_green_shift_")) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            raw.forEach((k, v) -> row.put(String.valueOf(k), v));
            row.put("energy_carbon", true);
            row.put("supports_green_shift_pct", id.startsWith("plant_green_shift_"));
            result.add(row);
        }
        return result;
    }

    public String resolveGreenShiftScenarioId(double greenShiftPct) {
        int rounded = (int) (Math.round(greenShiftPct / 10.0) * 10);
        if (rounded == 60) return "plant_green_shift_60";
        if (rounded == 80) return "plant_green_shift_80";
        return "plant_green_shift_80";
    }

    private Map<String, Object> buildLinkageHints() {
        Map<String, Object> links = new LinkedHashMap<>();
        links.put("balance_path", "/optimization/balance");
        links.put("balance_query_template", "?green_shift_pct={pct}&from=energy-carbon");
        links.put("scenarios_path", "/optimization/scenarios");
        links.put("scenarios_query_template", "?scenario={id}&from=energy-carbon&autorun=1");
        links.put("description", "能碳模块与平衡点/场景实验室深链：通过 URL 参数传递绿电偏移与场景 ID");
        return links;
    }
}

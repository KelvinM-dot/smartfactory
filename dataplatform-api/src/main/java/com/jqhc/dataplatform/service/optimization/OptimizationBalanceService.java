package com.jqhc.dataplatform.service.optimization;

import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 三极致最佳平衡点分析：基于工厂 KPI 快照 + 四杠杆启发式网格搜索。
 */
@Service
public class OptimizationBalanceService {

    public Map<String, Object> analyzeBalance(Map<String, Object> snapshot, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> extremes = snapshot.get("three_extremes") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        double baseEff = NumberUtils.toDouble(extremes.get("efficiency_index"));
        double baseCost = NumberUtils.toDouble(extremes.get("cost_index"));
        double baseQual = NumberUtils.toDouble(extremes.get("quality_index"));
        if (baseEff <= 0) baseEff = 75;
        if (baseCost <= 0) baseCost = 72;
        if (baseQual <= 0) baseQual = 78;

        double batchKg = clamp(NumberUtils.toDouble(params.getOrDefault("batch_size_kg", 1200)), 650, 1500);
        double speedPct = clamp(NumberUtils.toDouble(params.getOrDefault("line_speed_pct", 100)), 85, 115);
        double greenPct = clamp(NumberUtils.toDouble(params.getOrDefault("green_shift_pct", 50)), 0, 100);
        double gradePct = clamp(NumberUtils.toDouble(params.getOrDefault("grade_concentration_pct", 60)), 0, 100);
        double wEff = clamp(NumberUtils.toDouble(params.getOrDefault("weight_efficiency", 0.34)), 0.1, 0.8);
        double wCost = clamp(NumberUtils.toDouble(params.getOrDefault("weight_cost", 0.33)), 0.1, 0.8);
        double wQual = clamp(NumberUtils.toDouble(params.getOrDefault("weight_quality", 0.33)), 0.1, 0.8);
        double wSum = wEff + wCost + wQual;
        wEff /= wSum;
        wCost /= wSum;
        wQual /= wSum;

        Map<String, Object> currentLevers = leverMap(batchKg, speedPct, greenPct, gradePct);
        Map<String, Object> currentIndices = projectIndices(baseEff, baseCost, baseQual, batchKg, speedPct, greenPct, gradePct);
        double currentScore = weightedScore(currentIndices, wEff, wCost, wQual);

        BalanceCandidate optimal = null;
        List<Map<String, Object>> pareto = new ArrayList<>();
        double[] batchGrid = {650, 800, 1000, 1200, 1300, 1500};
        double[] speedGrid = {85, 90, 95, 100, 105, 110, 115};
        double[] greenGrid = {0, 25, 50, 75, 100};
        double[] gradeGrid = {0, 33, 66, 100};

        for (double b : batchGrid) {
            for (double s : speedGrid) {
                for (double g : greenGrid) {
                    for (double gr : gradeGrid) {
                        Map<String, Object> idx = projectIndices(baseEff, baseCost, baseQual, b, s, g, gr);
                        double score = weightedScore(idx, wEff, wCost, wQual);
                        BalanceCandidate cand = new BalanceCandidate(b, s, g, gr, idx, score);
                        if (optimal == null || cand.score > optimal.score) {
                            optimal = cand;
                        }
                        if (isParetoCandidate(idx, pareto)) {
                            pareto.add(toBalancePointMap(cand));
                            prunePareto(pareto);
                        }
                    }
                }
            }
        }
        if (pareto.size() > 12) {
            final double fwEff = wEff;
            final double fwCost = wCost;
            final double fwQual = wQual;
            pareto = new ArrayList<>(pareto.stream()
                    .sorted((a, b) -> Double.compare(
                            weightedScoreMap(b, fwEff, fwCost, fwQual),
                            weightedScoreMap(a, fwEff, fwCost, fwQual)))
                    .limit(12)
                    .toList());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("computed_at", Instant.now());
        result.put("baseline_snapshot", snapshot);
        result.put("weights", Map.of(
                "efficiency", round2(wEff),
                "cost", round2(wCost),
                "quality", round2(wQual)
        ));
        result.put("lever_ranges", Map.of(
                "batch_size_kg", Map.of("min", 650, "max", 1500, "default", 1200, "unit", "kg"),
                "line_speed_pct", Map.of("min", 85, "max", 115, "default", 100, "unit", "%"),
                "green_shift_pct", Map.of("min", 0, "max", 100, "default", 50, "unit", "%"),
                "grade_concentration_pct", Map.of("min", 0, "max", 100, "default", 60, "unit", "%")
        ));
        result.put("current", Map.of(
                "levers", currentLevers,
                "indices", currentIndices,
                "weighted_score", NumberUtils.round1(currentScore),
                "triangle_coords", triangleCoords(currentIndices)
        ));
        result.put("optimal", optimal != null ? Map.of(
                "levers", leverMap(optimal.batchKg, optimal.speedPct, optimal.greenPct, optimal.gradePct),
                "indices", optimal.indices,
                "weighted_score", NumberUtils.round1(optimal.score),
                "triangle_coords", triangleCoords(optimal.indices),
                "rationale", buildBalanceRationale(currentLevers, optimal, currentIndices)
        ) : Map.of());
        result.put("score_gap", optimal != null ? NumberUtils.round1(optimal.score - currentScore) : 0);
        result.put("pareto_frontier", pareto);
        result.put("tradeoff_notes", buildTradeoffNotes(baseEff, baseCost, baseQual));
        result.put("sensitivity", List.of(
                Map.of("lever", "line_speed_pct", "efficiency_per_1pct", 0.35, "quality_per_1pct", -0.4, "cost_per_1pct", -0.25),
                Map.of("lever", "batch_size_kg", "efficiency_per_100kg", 0.67, "quality_per_100kg", -0.25, "cost_per_100kg", -0.17),
                Map.of("lever", "green_shift_pct", "efficiency_per_1pct", 0.02, "quality_per_1pct", -0.01, "cost_per_1pct", 0.18),
                Map.of("lever", "grade_concentration_pct", "efficiency_per_1pct", 0.12, "quality_per_1pct", 0.15, "cost_per_1pct", 0.05)
        ));
        return result;
    }

    private record BalanceCandidate(double batchKg, double speedPct, double greenPct, double gradePct,
                                    Map<String, Object> indices, double score) {}

    private Map<String, Object> toBalancePointMap(BalanceCandidate cand) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("levers", leverMap(cand.batchKg, cand.speedPct, cand.greenPct, cand.gradePct));
        m.put("indices", cand.indices);
        m.put("weighted_score", NumberUtils.round1(cand.score));
        m.put("triangle_coords", triangleCoords(cand.indices));
        return m;
    }

    private Map<String, Object> projectIndices(double baseEff, double baseCost, double baseQual,
                                                 double batchKg, double speedPct, double greenPct, double gradePct) {
        double eff = baseEff
                + (batchKg - 1200) / 1200.0 * 8.0
                + (speedPct - 100) * 0.35
                + greenPct * 0.02
                + gradePct * 0.12;
        double cost = baseCost
                - (speedPct - 100) * 0.25
                + greenPct * 0.18
                - (batchKg - 1200) / 1200.0 * 2.0
                + gradePct * 0.05;
        double qual = baseQual
                - (speedPct - 100) * 0.4
                - (batchKg - 1200) / 1200.0 * 3.0
                + gradePct * 0.15
                - greenPct * 0.01;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("efficiency_index", NumberUtils.round1(clamp(eff, 40, 98)));
        m.put("cost_index", NumberUtils.round1(clamp(cost, 40, 98)));
        m.put("quality_index", NumberUtils.round1(clamp(qual, 40, 98)));
        return m;
    }

    private Map<String, Object> leverMap(double batchKg, double speedPct, double greenPct, double gradePct) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("batch_size_kg", OptimizationContextService.round0(batchKg));
        m.put("line_speed_pct", NumberUtils.round1(speedPct));
        m.put("green_shift_pct", NumberUtils.round1(greenPct));
        m.put("grade_concentration_pct", NumberUtils.round1(gradePct));
        return m;
    }

    private Map<String, Object> triangleCoords(Map<String, Object> indices) {
        double e = NumberUtils.toDouble(indices.get("efficiency_index")) / 100.0;
        double c = NumberUtils.toDouble(indices.get("cost_index")) / 100.0;
        double q = NumberUtils.toDouble(indices.get("quality_index")) / 100.0;
        double sum = e + c + q;
        if (sum > 0) {
            e /= sum;
            c /= sum;
            q /= sum;
        } else {
            e = c = q = 1.0 / 3.0;
        }
        Map<String, Object> coords = new LinkedHashMap<>();
        coords.put("efficiency_norm", round2(e));
        coords.put("cost_norm", round2(c));
        coords.put("quality_norm", round2(q));
        return coords;
    }

    private double weightedScore(Map<String, Object> indices, double wEff, double wCost, double wQual) {
        return NumberUtils.toDouble(indices.get("efficiency_index")) * wEff
                + NumberUtils.toDouble(indices.get("cost_index")) * wCost
                + NumberUtils.toDouble(indices.get("quality_index")) * wQual;
    }

    private double weightedScoreMap(Map<String, Object> point, double wEff, double wCost, double wQual) {
        @SuppressWarnings("unchecked")
        Map<String, Object> idx = point.get("indices") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        return weightedScore(idx, wEff, wCost, wQual);
    }

    private boolean isParetoCandidate(Map<String, Object> indices, List<Map<String, Object>> frontier) {
        double e = NumberUtils.toDouble(indices.get("efficiency_index"));
        double c = NumberUtils.toDouble(indices.get("cost_index"));
        double q = NumberUtils.toDouble(indices.get("quality_index"));
        for (Map<String, Object> p : frontier) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pi = p.get("indices") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            double pe = NumberUtils.toDouble(pi.get("efficiency_index"));
            double pc = NumberUtils.toDouble(pi.get("cost_index"));
            double pq = NumberUtils.toDouble(pi.get("quality_index"));
            if (pe >= e && pc >= c && pq >= q && (pe > e || pc > c || pq > q)) {
                return false;
            }
        }
        return true;
    }

    private void prunePareto(List<Map<String, Object>> frontier) {
        frontier.removeIf(p -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> pi = p.get("indices") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            double e = NumberUtils.toDouble(pi.get("efficiency_index"));
            double c = NumberUtils.toDouble(pi.get("cost_index"));
            double q = NumberUtils.toDouble(pi.get("quality_index"));
            for (Map<String, Object> other : frontier) {
                if (other == p) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> oi = other.get("indices") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
                double oe = NumberUtils.toDouble(oi.get("efficiency_index"));
                double oc = NumberUtils.toDouble(oi.get("cost_index"));
                double oq = NumberUtils.toDouble(oi.get("quality_index"));
                if (oe >= e && oc >= c && oq >= q && (oe > e || oc > c || oq > q)) {
                    return true;
                }
            }
            return false;
        });
    }

    private String buildBalanceRationale(Map<String, Object> current, BalanceCandidate optimal,
                                         Map<String, Object> currentIndices) {
        List<String> parts = new ArrayList<>();
        double curBatch = NumberUtils.toDouble(current.get("batch_size_kg"));
        if (Math.abs(optimal.batchKg - curBatch) >= 100) {
            parts.add("批次 " + OptimizationContextService.round0(curBatch) + "→" + OptimizationContextService.round0(optimal.batchKg) + "kg");
        }
        if (Math.abs(optimal.speedPct - NumberUtils.toDouble(current.get("line_speed_pct"))) >= 3) {
            parts.add("线速 " + NumberUtils.round1(NumberUtils.toDouble(current.get("line_speed_pct"))) + "→" + NumberUtils.round1(optimal.speedPct) + "%");
        }
        if (Math.abs(optimal.greenPct - NumberUtils.toDouble(current.get("green_shift_pct"))) >= 10) {
            parts.add("绿电排产偏移 " + NumberUtils.round1(optimal.greenPct) + "%");
        }
        if (Math.abs(optimal.gradePct - NumberUtils.toDouble(current.get("grade_concentration_pct"))) >= 10) {
            parts.add("同规格集中度 " + NumberUtils.round1(optimal.gradePct) + "%");
        }
        if (parts.isEmpty()) {
            return "当前杠杆已接近加权最优，微调即可";
        }
        double effGain = NumberUtils.toDouble(optimal.indices.get("efficiency_index")) - NumberUtils.toDouble(currentIndices.get("efficiency_index"));
        double qualGain = NumberUtils.toDouble(optimal.indices.get("quality_index")) - NumberUtils.toDouble(currentIndices.get("quality_index"));
        return String.join("；", parts) + "。预期效率 " + (effGain >= 0 ? "+" : "") + NumberUtils.round1(effGain)
                + "、质量 " + (qualGain >= 0 ? "+" : "") + NumberUtils.round1(qualGain);
    }

    private List<String> buildTradeoffNotes(double baseEff, double baseCost, double baseQual) {
        Map<String, Object> plusSpeed = projectIndices(baseEff, baseCost, baseQual, 1200, 110, 50, 60);
        Map<String, Object> plusBatch = projectIndices(baseEff, baseCost, baseQual, 1400, 100, 50, 60);
        Map<String, Object> plusGreen = projectIndices(baseEff, baseCost, baseQual, 1200, 100, 90, 60);
        return List.of(
                "拉拔提速 10%：效率 " + fmtDelta(baseEff, plusSpeed.get("efficiency_index"))
                        + "，质量 " + fmtDelta(baseQual, plusSpeed.get("quality_index"))
                        + "，成本 " + fmtDelta(baseCost, plusSpeed.get("cost_index")),
                "批次扩大至 1400kg：效率 " + fmtDelta(baseEff, plusBatch.get("efficiency_index"))
                        + "，质量 " + fmtDelta(baseQual, plusBatch.get("quality_index")),
                "绿电窗口排产 90%：成本 " + fmtDelta(baseCost, plusGreen.get("cost_index"))
                        + "，效率 " + fmtDelta(baseEff, plusGreen.get("efficiency_index"))
        );
    }

    private String fmtDelta(double base, Object projected) {
        double d = NumberUtils.toDouble(projected) - base;
        return (d >= 0 ? "+" : "") + NumberUtils.round1(d);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

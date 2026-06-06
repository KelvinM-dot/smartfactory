package com.jqhc.dataplatform.service.optimization;

import com.jqhc.dataplatform.domain.ProductLineDoc;

public record OptimizationLineContext(
        ProductLineDoc line,
        String lineId,
        boolean telemetry,
        double oeePct,
        double availabilityPct,
        double performancePct,
        double qualityPct,
        long pendingAlarms,
        boolean materialShortage,
        double recipeDeviationScore
) {
    public double utilizationHeadroom() {
        return Math.max(5, 100 - estimatedUtilizationPct());
    }

    public double estimatedUtilizationPct() {
        String status = line.getStatus() != null ? line.getStatus() : "active";
        return switch (status) {
            case "inactive" -> 8;
            case "maintenance" -> 35;
            default -> telemetry ? 68 + oeePct * 0.15 : 62;
        };
    }
}

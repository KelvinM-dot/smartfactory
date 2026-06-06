package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.util.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LineOeeService {

    public record OeeComponents(double availabilityPct, double performancePct, double qualityPct, double oeePct) {}

    private final EquipmentRepository equipmentRepository;
    private final LatestStateRepository latestStateRepository;
    private final ProductBatchRepository productBatchRepository;
    private final QualityGateEventRepository qualityGateEventRepository;
    private final SourceStatusRepository sourceStatusRepository;
    private final FactoryMasterDataService masterDataService;
    private final JqhcProperties properties;

    public LineOeeService(
            EquipmentRepository equipmentRepository,
            LatestStateRepository latestStateRepository,
            ProductBatchRepository productBatchRepository,
            QualityGateEventRepository qualityGateEventRepository,
            SourceStatusRepository sourceStatusRepository,
            FactoryMasterDataService masterDataService,
            JqhcProperties properties) {
        this.equipmentRepository = equipmentRepository;
        this.latestStateRepository = latestStateRepository;
        this.productBatchRepository = productBatchRepository;
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.sourceStatusRepository = sourceStatusRepository;
        this.masterDataService = masterDataService;
        this.properties = properties;
    }

    public OeeComponents computeOeeComponents(String lineId) {
        List<EquipmentDoc> equipment = equipmentRepository.findByProductLineId(lineId);
        long total = equipment.size();
        if (total == 0) {
            double util = NumberUtils.toDouble(masterDataService.getSimulationDefaults().getOrDefault("plant_utilization_pct", 78.0));
            double oee = NumberUtils.round1(util * 0.95);
            return new OeeComponents(util, 95.0, defaultQualityPassRatePct(), oee);
        }
        List<LatestStateDoc> states = latestStateRepository.findByProductLineId(lineId);
        long running = states.stream()
                .filter(s -> "status".equals(s.getFieldId()))
                .filter(s -> "RUNNING".equals(String.valueOf(s.getValue())))
                .count();
        double availabilityPct = NumberUtils.round1(Math.min(1.0, (double) running / total) * 100.0);
        double performancePct = 92.0;
        SourceStatusDoc source = findLatestSource(lineId);
        if (source != null && source.getActivePowerKw() != null && source.getActivePowerKw() > 0) {
            double nominal = total * 12.0;
            performancePct = NumberUtils.round1(Math.min(1.0, source.getActivePowerKw() / Math.max(nominal, 1.0)) * 100.0);
        }
        double qualityPct = computeFirstPassYieldPct(lineId);
        double oeePct = NumberUtils.round1(availabilityPct * performancePct * qualityPct / 10000.0);
        return new OeeComponents(availabilityPct, performancePct, qualityPct, oeePct);
    }

    public double computeOeePct(String lineId) {
        return computeOeeComponents(lineId).oeePct();
    }

    public double computeShiftOutputT(String lineId, ProductLineDoc line) {
        Instant cutoff = Instant.now().minus(4, ChronoUnit.HOURS);
        double fromCompleted = productBatchRepository.findByProductLineIdOrderByStartedAtDesc(lineId).stream()
                .filter(b -> "completed".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getEndedAt() != null && !b.getEndedAt().isBefore(cutoff))
                .mapToDouble(b -> b.getQuantityKg() != null ? b.getQuantityKg() / 1000.0 : 0.0)
                .sum();
        if (fromCompleted > 0) {
            return NumberUtils.round1(fromCompleted);
        }

        Optional<ProductBatchDoc> inProgress = productBatchRepository
                .findFirstByProductLineIdAndStatusOrderByStartedAtDesc(lineId, "in_progress");
        if (inProgress.isPresent() && inProgress.get().getQuantityKg() != null) {
            double progress = 0.35;
            SourceStatusDoc source = findLatestSource(lineId);
            if (source != null && source.getCurrentStep() != null && line.getProcessSteps() != null) {
                int idx = line.getProcessSteps().indexOf(source.getCurrentStep());
                if (idx >= 0) {
                    progress = (idx + 1.0) / Math.max(line.getProcessSteps().size(), 1);
                }
            }
            return NumberUtils.round1(inProgress.get().getQuantityKg() / 1000.0 * progress);
        }

        double dayCap = line.getDesignCapacityTPerDay() != null ? line.getDesignCapacityTPerDay() : 12.0;
        double util = NumberUtils.toDouble(masterDataService.getSimulationDefaults().getOrDefault("plant_utilization_pct", 78.0)) / 100.0;
        return NumberUtils.round1(dayCap * util * 0.5);
    }

    public SourceStatusDoc findLatestSource(String lineId) {
        return sourceStatusRepository.findAll().stream()
                .filter(s -> lineId.equals(s.getProductLineId()))
                .max(Comparator.comparing(SourceStatusDoc::getLastHeartbeatAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    public boolean isSourceConnected(SourceStatusDoc source) {
        if (source == null || source.getLastHeartbeatAt() == null) {
            return false;
        }
        return source.getLastHeartbeatAt()
                .plusSeconds(properties.getHeartbeatTimeoutSec())
                .isAfter(Instant.now());
    }

    public boolean isLineLive(String lineId, SourceStatusDoc source) {
        if (isSourceConnected(source)) {
            return true;
        }
        Instant cutoff = Instant.now().minusSeconds(properties.getHeartbeatTimeoutSec());
        return latestStateRepository.findByProductLineId(lineId).stream()
                .map(LatestStateDoc::getTimestamp)
                .filter(Objects::nonNull)
                .anyMatch(ts -> ts.isAfter(cutoff));
    }

    private double defaultQualityPassRatePct() {
        return NumberUtils.toDouble(masterDataService.getSimulationDefaults()
                .getOrDefault("default_quality_gate_pass_rate_pct", 97.2));
    }

    private double computeFirstPassYieldPct(String lineId) {
        Instant cutoff = Instant.now().minus(properties.getBufferWindowHours(), ChronoUnit.HOURS);
        Set<String> batchIds = productBatchRepository.findByProductLineIdOrderByStartedAtDesc(lineId).stream()
                .map(ProductBatchDoc::getBatchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (batchIds.isEmpty()) {
            return NumberUtils.round1(defaultQualityPassRatePct());
        }
        List<QualityGateEventDoc> gates = qualityGateEventRepository.findAll().stream()
                .filter(g -> g.getBatchId() != null && batchIds.contains(g.getBatchId()))
                .filter(g -> g.getDecidedAt() == null || !g.getDecidedAt().isBefore(cutoff))
                .toList();
        if (gates.isEmpty()) {
            return NumberUtils.round1(defaultQualityPassRatePct());
        }
        long passCount = gates.stream().filter(g -> "pass".equalsIgnoreCase(g.getDecision())).count();
        return NumberUtils.round1(passCount * 100.0 / gates.size());
    }
}

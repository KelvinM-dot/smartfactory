package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComputeService {

    private final ProductLineRepository productLineRepository;
    private final EquipmentRepository equipmentRepository;
    private final LatestStateRepository latestStateRepository;
    private final TelemetryPointRepository telemetryPointRepository;
    private final ProductBatchRepository productBatchRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final MaterialEventRepository materialEventRepository;
    private final LogisticsTaskRepository logisticsTaskRepository;
    private final QualityGateEventRepository qualityGateEventRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final DomainConfigService domainConfigService;
    private final FactoryMasterDataService masterDataService;
    private final JqhcProperties properties;
    private final LineOeeService lineOeeService;
    private final FactoryMetricsService factoryMetricsService;
    private final OrderAnalyticsService orderAnalyticsService;

    public ComputeService(
            ProductLineRepository productLineRepository,
            EquipmentRepository equipmentRepository,
            LatestStateRepository latestStateRepository,
            TelemetryPointRepository telemetryPointRepository,
            ProductBatchRepository productBatchRepository,
            AlarmEventRepository alarmEventRepository,
            MaterialEventRepository materialEventRepository,
            LogisticsTaskRepository logisticsTaskRepository,
            QualityGateEventRepository qualityGateEventRepository,
            ProductionOrderRepository productionOrderRepository,
            DomainConfigService domainConfigService,
            FactoryMasterDataService masterDataService,
            JqhcProperties properties,
            LineOeeService lineOeeService,
            FactoryMetricsService factoryMetricsService,
            OrderAnalyticsService orderAnalyticsService) {
        this.productLineRepository = productLineRepository;
        this.equipmentRepository = equipmentRepository;
        this.latestStateRepository = latestStateRepository;
        this.telemetryPointRepository = telemetryPointRepository;
        this.productBatchRepository = productBatchRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.materialEventRepository = materialEventRepository;
        this.logisticsTaskRepository = logisticsTaskRepository;
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.domainConfigService = domainConfigService;
        this.masterDataService = masterDataService;
        this.properties = properties;
        this.lineOeeService = lineOeeService;
        this.factoryMetricsService = factoryMetricsService;
        this.orderAnalyticsService = orderAnalyticsService;
    }

    public Map<String, Object> getLineOverview(String lineId) {
        ProductLineDoc line = productLineRepository.findById(lineId)
                .orElseThrow(() -> new NoSuchElementException("line not found: " + lineId));

        Optional<ProductBatchDoc> currentBatch = productBatchRepository
                .findFirstByProductLineIdAndStatusOrderByStartedAtDesc(lineId, "in_progress");

        long pendingAlarms = alarmEventRepository.countByProductLineIdAndHandleStatus(lineId, "pending");
        SourceStatusDoc source = lineOeeService.findLatestSource(lineId);
        boolean connected = lineOeeService.isLineLive(lineId, source);

        List<Map<String, Object>> pipeline = new ArrayList<>();
        for (String stepId : line.getProcessSteps()) {
            String aggregateStatus = aggregateStepStatus(lineId, stepId);
            pipeline.add(Map.of(
                    "process_step_id", stepId,
                    "display_name", domainConfigService.getStepDisplayName(stepId),
                    "aggregate_status", aggregateStatus
            ));
        }

        LineOeeService.OeeComponents oee = lineOeeService.computeOeeComponents(lineId);
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("oee_pct", oee.oeePct());
        kpi.put("availability_pct", oee.availabilityPct());
        kpi.put("performance_pct", oee.performancePct());
        kpi.put("quality_pct", oee.qualityPct());
        kpi.put("shift_output_t", lineOeeService.computeShiftOutputT(lineId, line));
        kpi.put("first_pass_yield_pct", oee.qualityPct());
        kpi.put("pending_alarms", pendingAlarms);

        Map<String, Object> dataSource = new LinkedHashMap<>();
        dataSource.put("type", source != null ? source.getSource() : "unknown");
        dataSource.put("instance", source != null ? source.getSourceInstance() : "none");
        dataSource.put("connected", connected);
        dataSource.put("last_heartbeat_at", source != null ? source.getLastHeartbeatAt() : null);
        dataSource.put("scenario_id", source != null ? source.getScenarioId() : null);
        dataSource.put("speed_multiplier", source != null ? source.getSpeedMultiplier() : 1);
        if (source != null) {
            dataSource.put("dwell_mode", source.getDwellMode());
            dataSource.put("dwell_reason", source.getDwellReason());
            dataSource.put("current_step", source.getCurrentStep());
            dataSource.put("active_power_kw", source.getActivePowerKw());
            dataSource.put("running_equipment_count", source.getRunningEquipmentCount());
            dataSource.put("raw_material_low", source.getRawMaterialLow());
        }
        boolean telemetryKpi = Boolean.TRUE.equals(line.getSimulationEnabled()) && connected;
        dataSource.put("kpi_mode", telemetryKpi ? "telemetry" : "estimated");
        if (line.getDetailLevel() != null) {
            dataSource.put("detail_level", line.getDetailLevel());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product_line_id", lineId);
        result.put("computed_at", Instant.now());
        result.put("data_source", dataSource);
        result.put("current_batch", currentBatch.orElse(null));
        result.put("kpi_bar", kpi);
        result.put("process_pipeline", pipeline);
        result.put("buffer_window_hours", properties.getBufferWindowHours());
        return result;
    }

    public Map<String, Object> getTrends(String lineId, List<String> fieldIds, String range, String batchId) {
        Instant end = Instant.now();
        Instant start = end.minus(properties.getBufferWindowHours(), ChronoUnit.HOURS);

        if ("batch".equals(range) && batchId != null) {
            List<TelemetryPointDoc> points = telemetryPointRepository
                    .findByProductBatchAndTimestampBetweenOrderByTimestampAsc(batchId, start, end);
            return buildTrendResponse(lineId, range, start, end, fieldIds, points);
        }

        List<TelemetryPointDoc> points = telemetryPointRepository
                .findByProductLineIdAndFieldIdInAndTimestampBetweenOrderByTimestampAsc(
                        lineId, fieldIds, start, end);
        return buildTrendResponse(lineId, range != null ? range : "buffer", start, end, fieldIds, points);
    }

    public Map<String, Object> getBatchTimeline(String batchId) {
        ProductBatchDoc batch = productBatchRepository.findById(batchId)
                .orElseThrow(() -> new NoSuchElementException("batch not found: " + batchId));
        ProductLineDoc line = productLineRepository.findById(batch.getProductLineId()).orElse(null);
        List<String> steps = line != null ? line.getProcessSteps() : List.of();

        List<Map<String, Object>> nodes = new ArrayList<>();
        Instant windowStart = Instant.now().minus(properties.getBufferWindowHours(), ChronoUnit.HOURS);

        for (String stepId : steps) {
            List<TelemetryPointDoc> points = telemetryPointRepository
                    .findByProductLineIdAndProductBatchAndProcessStepId(
                            batch.getProductLineId(), batchId, stepId);
            points = points.stream()
                    .filter(p -> p.getTimestamp().isAfter(windowStart))
                    .sorted(Comparator.comparing(TelemetryPointDoc::getTimestamp))
                    .toList();

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("process_step_id", stepId);
            node.put("display_name", domainConfigService.getStepDisplayName(stepId));
            if (points.isEmpty()) {
                node.put("status", "pending");
            } else {
                node.put("status", batch.getStatus().equals("in_progress") ? "in_progress" : "completed");
                node.put("started_at", points.get(0).getTimestamp());
                node.put("ended_at", points.get(points.size() - 1).getTimestamp());
                node.put("key_parameters", summarizeParameters(stepId, points));
            }
            nodes.add(node);
        }

        List<Map<String, Object>> gaps = new ArrayList<>();
        if (batch.getParentBatches() == null || batch.getParentBatches().isEmpty()) {
            gaps.add(Map.of(
                    "gap_type", "missing_parent_batch",
                    "message", "上游原料批次未关联（parent_batches 缺失）"
            ));
        }

        List<MaterialEventDoc> materialEvents =
                materialEventRepository.findByMaterialBatchOrderByTimestampAsc(batchId);
        List<Map<String, Object>> materialTimeline = materialEvents.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("event_id", e.getEventId());
            m.put("event_type", e.getEventType());
            m.put("timestamp", e.getTimestamp());
            m.put("process_step_id", e.getProcessStepId());
            m.put("location", e.getLocation());
            m.put("from_location", e.getFromLocation());
            m.put("to_location", e.getToLocation());
            m.put("quantity_kg", e.getQuantityKg());
            m.put("remark", e.getRemark());
            return m;
        }).toList();

        List<QualityGateEventDoc> gateEvents = qualityGateEventRepository.findByBatchIdOrderByDecidedAtDesc(batchId);
        List<Map<String, Object>> qualityGateSummary = gateEvents.stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gate_event_id", g.getGateEventId());
            m.put("gate_type", g.getGateType());
            m.put("decision", g.getDecision());
            m.put("reason_text", g.getReasonText());
            m.put("process_step_id", g.getProcessStepId());
            m.put("decided_at", g.getDecidedAt());
            return m;
        }).toList();

        List<LogisticsTaskDoc> logisticsTasks = logisticsTaskRepository.findAll().stream()
                .filter(t -> batchId.equals(t.getProductBatch()) || batchId.equals(t.getMaterialBatch()))
                .sorted(Comparator.comparing(LogisticsTaskDoc::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<Map<String, Object>> logisticsSummary = logisticsTasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("task_id", t.getTaskId());
            m.put("task_type", t.getTaskType());
            m.put("status", t.getStatus());
            m.put("source_location_id", t.getSourceLocationId());
            m.put("target_location_id", t.getTargetLocationId());
            m.put("agv_id", t.getAgvId());
            m.put("created_at", t.getCreatedAt());
            return m;
        }).toList();

        ProductionOrderDoc order = batch.getProductionOrderId() == null ? null
                : productionOrderRepository.findById(batch.getProductionOrderId()).orElse(null);
        boolean qualityBlocked = gateEvents.stream().anyMatch(g -> g.getDecision() != null && !"pass".equalsIgnoreCase(g.getDecision()));
        boolean logisticsBlocked = logisticsTasks.stream().anyMatch(t -> t.getStatus() == null || !"completed".equalsIgnoreCase(t.getStatus()));
        Map<String, Object> blockerSummary = new LinkedHashMap<>();
        blockerSummary.put("quality_blocked", qualityBlocked);
        blockerSummary.put("logistics_blocked", logisticsBlocked);
        blockerSummary.put("blocking_quality_gates", gateEvents.stream().filter(g -> g.getDecision() != null && !"pass".equalsIgnoreCase(g.getDecision())).count());
        blockerSummary.put("blocking_logistics_tasks", logisticsTasks.stream().filter(t -> t.getStatus() == null || !"completed".equalsIgnoreCase(t.getStatus())).count());
        blockerSummary.put("delivery_blocked", qualityBlocked || logisticsBlocked);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("batch", batch);
        result.put("order", order);
        result.put("computed_at", Instant.now());
        result.put("nodes", nodes);
        result.put("material_events", materialTimeline);
        result.put("quality_gates", qualityGateSummary);
        result.put("logistics_tasks", logisticsSummary);
        result.put("blockers", blockerSummary);
        result.put("trace_gaps", gaps);
        return result;
    }

    public List<Map<String, Object>> getMaterialEvents(String lineId, String batchId, int limit) {
        List<MaterialEventDoc> events;
        if (batchId != null && !batchId.isBlank()) {
            events = materialEventRepository.findByMaterialBatchOrderByTimestampAsc(batchId);
        } else if (lineId != null && !lineId.isBlank()) {
            events = materialEventRepository.findByProductLineIdOrderByTimestampDesc(lineId);
        } else {
            int cap = Math.max(1, Math.min(limit, 200));
            events = materialEventRepository.findAll().stream()
                    .sorted(Comparator.comparing(MaterialEventDoc::getTimestamp,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(cap)
                    .toList();
        }
        return events.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("event_id", e.getEventId());
            m.put("event_type", e.getEventType());
            m.put("timestamp", e.getTimestamp());
            m.put("material_batch", e.getMaterialBatch());
            m.put("product_line_id", e.getProductLineId());
            m.put("process_step_id", e.getProcessStepId());
            m.put("location", e.getLocation());
            m.put("from_location", e.getFromLocation());
            m.put("to_location", e.getToLocation());
            m.put("quantity_kg", e.getQuantityKg());
            m.put("agv_id", e.getAgvId());
            m.put("remark", e.getRemark());
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getLogisticsTasks(String factoryId, String status) {
        List<LogisticsTaskDoc> tasks;
        if (status != null && !status.isBlank()) {
            tasks = logisticsTaskRepository.findByStatusOrderByCreatedAtDesc(status);
            if (factoryId != null && !factoryId.isBlank()) {
                tasks = tasks.stream().filter(t -> factoryId.equals(t.getFactoryId())).toList();
            }
        } else if (factoryId != null && !factoryId.isBlank()) {
            tasks = logisticsTaskRepository.findByFactoryIdOrderByCreatedAtDesc(factoryId);
        } else {
            tasks = logisticsTaskRepository.findAll().stream()
                    .sorted(Comparator.comparing(LogisticsTaskDoc::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("task_id", t.getTaskId());
            m.put("factory_id", t.getFactoryId());
            m.put("workshop_id", t.getWorkshopId());
            m.put("task_type", t.getTaskType());
            m.put("product_batch", t.getProductBatch());
            m.put("material_batch", t.getMaterialBatch());
            m.put("source_location_id", t.getSourceLocationId());
            m.put("target_location_id", t.getTargetLocationId());
            m.put("quantity_kg", t.getQuantityKg());
            m.put("agv_id", t.getAgvId());
            m.put("priority", t.getPriority());
            m.put("status", t.getStatus());
            m.put("created_at", t.getCreatedAt());
            m.put("completed_at", t.getCompletedAt());
            m.put("failure_reason", t.getFailureReason());
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getQualityGateEvents(String factoryId, String batchId, String decision) {
        List<QualityGateEventDoc> events;
        if (batchId != null && !batchId.isBlank()) {
            events = qualityGateEventRepository.findByBatchIdOrderByDecidedAtDesc(batchId);
        } else if (decision != null && !decision.isBlank()) {
            events = qualityGateEventRepository.findByDecisionOrderByDecidedAtDesc(decision);
            if (factoryId != null && !factoryId.isBlank()) {
                events = events.stream().filter(e -> factoryId.equals(e.getFactoryId())).toList();
            }
        } else if (factoryId != null && !factoryId.isBlank()) {
            events = qualityGateEventRepository.findByFactoryIdOrderByDecidedAtDesc(factoryId);
        } else {
            events = qualityGateEventRepository.findAll().stream()
                    .sorted(Comparator.comparing(QualityGateEventDoc::getDecidedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return events.stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gate_event_id", g.getGateEventId());
            m.put("factory_id", g.getFactoryId());
            m.put("workshop_id", g.getWorkshopId());
            m.put("batch_id", g.getBatchId());
            m.put("process_step_id", g.getProcessStepId());
            m.put("gate_type", g.getGateType());
            m.put("decision", g.getDecision());
            m.put("reason_code", g.getReasonCode());
            m.put("reason_text", g.getReasonText());
            m.put("decided_at", g.getDecidedAt());
            return m;
        }).toList();
    }

    public Map<String, Object> getFactoryEnergy(String factoryId) {
        return factoryMetricsService.getFactoryEnergy(factoryId);
    }

    public Map<String, Object> getFactoryKpis(String factoryId) {
        return factoryMetricsService.getFactoryKpis(factoryId);
    }

    public Map<String, Object> getOrderSummary(String factoryId) {
        return orderAnalyticsService.getOrderSummary(factoryId);
    }

    public List<Map<String, Object>> getOrderList(String factoryId, String status, String riskLevel) {
        return orderAnalyticsService.getOrderList(factoryId, status, riskLevel);
    }

    public Map<String, Object> getOrderDetail(String orderId) {
        return orderAnalyticsService.getOrderDetail(orderId);
    }

    public Map<String, Object> getOrderTimeline(String orderId) {
        return orderAnalyticsService.getOrderTimeline(orderId);
    }

    public List<Map<String, Object>> getOrderRiskList(String factoryId) {
        return orderAnalyticsService.getOrderRiskList(factoryId);
    }

    public List<Map<String, Object>> getAlarms(String lineId, String status) {
        List<AlarmEventDoc> alarms;
        if (lineId != null && !lineId.isBlank()) {
            if (status != null && !status.isBlank()) {
                alarms = alarmEventRepository.findByProductLineIdAndHandleStatusOrderByTriggeredAtDesc(lineId, status);
            } else {
                alarms = alarmEventRepository.findByProductLineIdOrderByTriggeredAtDesc(lineId);
            }
        } else {
            alarms = alarmEventRepository.findAll().stream()
                    .filter(a -> status == null || status.isBlank() || status.equals(a.getHandleStatus()))
                    .sorted(Comparator.comparing(AlarmEventDoc::getTriggeredAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return alarms.stream().map(this::toAlarmMap).toList();
    }

    public Map<String, Object> acknowledgeAlarm(String alarmId, String handler, String note) {
        AlarmEventDoc alarm = alarmEventRepository.findById(alarmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "alarm not found"));
        if ("resolved".equals(alarm.getHandleStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "alarm already resolved");
        }
        alarm.setHandleStatus("acknowledged");
        if (handler != null && !handler.isBlank()) {
            alarm.setHandler(handler);
        }
        if (note != null && !note.isBlank()) {
            alarm.setHandleNote(note);
        }
        alarmEventRepository.save(alarm);
        return toAlarmMap(alarm);
    }

    public Map<String, Object> resolveAlarm(String alarmId, String handler, String note) {
        AlarmEventDoc alarm = alarmEventRepository.findById(alarmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "alarm not found"));
        if ("resolved".equals(alarm.getHandleStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "alarm already resolved");
        }
        Instant now = Instant.now();
        alarm.setHandleStatus("resolved");
        alarm.setResolvedAt(now);
        if (alarm.getTriggeredAt() != null) {
            alarm.setDurationSec(Duration.between(alarm.getTriggeredAt(), now).toMillis() / 1000.0);
        }
        if (handler != null && !handler.isBlank()) {
            alarm.setHandler(handler);
        }
        if (note != null && !note.isBlank()) {
            alarm.setHandleNote(note);
        }
        alarmEventRepository.save(alarm);
        return toAlarmMap(alarm);
    }

    private Map<String, Object> toAlarmMap(AlarmEventDoc a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alarm_id", a.getAlarmId());
        m.put("equipment_id", a.getEquipmentId());
        m.put("product_line_id", a.getProductLineId());
        m.put("alarm_code", a.getAlarmCode());
        m.put("alarm_message", a.getAlarmMessage());
        m.put("severity", a.getSeverity());
        m.put("triggered_at", a.getTriggeredAt());
        m.put("resolved_at", a.getResolvedAt());
        m.put("duration_sec", a.getDurationSec());
        m.put("handle_status", a.getHandleStatus());
        m.put("handler", a.getHandler());
        m.put("handle_note", a.getHandleNote());
        m.put("product_batch", a.getProductBatch());
        return m;
    }

    public Map<String, Object> getEquipmentLatest(String equipmentId) {
        List<LatestStateDoc> states = latestStateRepository.findByEquipmentId(equipmentId);
        Map<String, Object> values = new LinkedHashMap<>();
        for (LatestStateDoc s : states) {
            values.put(s.getFieldId(), s.getValue());
        }
        Instant latest = states.stream().map(LatestStateDoc::getTimestamp)
                .max(Instant::compareTo).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipment_id", equipmentId);
        result.put("timestamp", latest);
        result.put("values", values);
        return result;
    }

    public Map<String, Object> getStepDetail(String lineId, String stepId) {
        List<EquipmentDoc> equipment = equipmentRepository.findByProductLineIdAndProcessStepId(lineId, stepId);
        List<Map<String, Object>> cards = new ArrayList<>();
        for (EquipmentDoc eq : equipment) {
            List<LatestStateDoc> states = latestStateRepository.findByEquipmentId(eq.getEquipmentId());
            String status = states.stream()
                    .filter(s -> "status".equals(s.getFieldId()))
                    .map(s -> String.valueOf(s.getValue()))
                    .findFirst().orElse("UNKNOWN");
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("equipment_id", eq.getEquipmentId());
            card.put("name", eq.getName());
            card.put("status", status);
            card.put("latest", states.stream()
                    .filter(s -> !"status".equals(s.getFieldId()))
                    .collect(Collectors.toMap(LatestStateDoc::getFieldId, LatestStateDoc::getValue, (a, b) -> b)));
            cards.add(card);
        }
        return Map.of(
                "process_step_id", stepId,
                "display_name", domainConfigService.getStepDisplayName(stepId),
                "equipment", cards
        );
    }

    private String aggregateStepStatus(String lineId, String stepId) {
        List<EquipmentDoc> equipment = equipmentRepository.findByProductLineIdAndProcessStepId(lineId, stepId);
        boolean anyAlarm = false;
        boolean anyManual = false;
        boolean anyRunning = false;
        boolean anyStopped = false;

        for (EquipmentDoc eq : equipment) {
            String status = latestStateRepository.findByEquipmentId(eq.getEquipmentId()).stream()
                    .filter(s -> "status".equals(s.getFieldId()))
                    .map(s -> String.valueOf(s.getValue()))
                    .findFirst().orElse("UNKNOWN");
            switch (status) {
                case "ALARM" -> anyAlarm = true;
                case "MANUAL" -> anyManual = true;
                case "RUNNING" -> anyRunning = true;
                case "STOPPED" -> anyStopped = true;
                default -> { }
            }
        }
        if (anyAlarm) return "alarm";
        if (anyManual) return "manual";
        if (anyRunning) return "running";
        if (anyStopped) return "stopped";
        return "unknown";
    }

    private Map<String, Object> buildTrendResponse(
            String lineId, String range, Instant start, Instant end,
            List<String> fieldIds, List<TelemetryPointDoc> points) {
        Map<String, List<TelemetryPointDoc>> grouped = points.stream()
                .collect(Collectors.groupingBy(TelemetryPointDoc::getFieldId));

        List<Map<String, Object>> series = new ArrayList<>();
        for (String fieldId : fieldIds) {
            Map<String, Object> fieldDef = domainConfigService.getFieldDef(fieldId);
            List<Map<String, Object>> pts = grouped.getOrDefault(fieldId, List.of()).stream()
                    .map(p -> Map.<String, Object>of("t", p.getTimestamp(), "v", p.getValue(), "quality", p.getQuality()))
                    .toList();
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("field_id", fieldId);
            s.put("display_name", fieldDef != null ? fieldDef.get("display_name") : fieldId);
            s.put("unit", fieldDef != null ? fieldDef.get("unit") : "");
            s.put("spec_limits", domainConfigService.getSpecLimits(fieldId));
            s.put("points", pts);
            series.add(s);
        }

        return Map.of(
                "line_id", lineId,
                "range", range,
                "range_start", start,
                "range_end", end,
                "series", series
        );
    }

    private List<Map<String, Object>> summarizeParameters(String stepId, List<TelemetryPointDoc> points) {
        Map<String, List<Double>> numeric = new HashMap<>();
        for (TelemetryPointDoc p : points) {
            if (p.getValue() instanceof Number n) {
                numeric.computeIfAbsent(p.getFieldId(), k -> new ArrayList<>()).add(n.doubleValue());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : numeric.entrySet()) {
            if (e.getKey().equals("status")) continue;
            List<Double> vals = e.getValue();
            double mean = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double max = vals.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            Map<String, Double> limits = domainConfigService.getSpecLimits(e.getKey());
            double compliance = 100.0;
            if (limits.containsKey("lsl") && limits.containsKey("usl")) {
                long inSpec = vals.stream()
                        .filter(v -> v >= limits.get("lsl") && v <= limits.get("usl"))
                        .count();
                compliance = vals.isEmpty() ? 0 : (inSpec * 100.0 / vals.size());
            }
            result.add(Map.of(
                    "field_id", e.getKey(),
                    "mean", Math.round(mean * 100.0) / 100.0,
                    "max", Math.round(max * 100.0) / 100.0,
                    "spec_compliance_pct", Math.round(compliance * 10.0) / 10.0
            ));
        }
        return result;
    }
}

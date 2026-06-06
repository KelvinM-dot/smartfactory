package com.jqhc.dataplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jqhc.dataplatform.config.JqhcProperties;
import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.dto.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.util.NumberUtils;
import com.jqhc.dataplatform.util.OrderProgressHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class IngestService {

    private final DomainConfigService domainConfigService;
    private final TelemetryPointRepository telemetryPointRepository;
    private final LatestStateRepository latestStateRepository;
    private final ProductBatchRepository productBatchRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final MaterialEventRepository materialEventRepository;
    private final LogisticsTaskRepository logisticsTaskRepository;
    private final QualityGateEventRepository qualityGateEventRepository;
    private final SourceStatusRepository sourceStatusRepository;
    private final JqhcProperties properties;
    private final ObjectMapper objectMapper;
    private final StreamService streamService;

    public IngestService(
            DomainConfigService domainConfigService,
            TelemetryPointRepository telemetryPointRepository,
            LatestStateRepository latestStateRepository,
            ProductBatchRepository productBatchRepository,
            ProductionOrderRepository productionOrderRepository,
            AlarmEventRepository alarmEventRepository,
            MaterialEventRepository materialEventRepository,
            LogisticsTaskRepository logisticsTaskRepository,
            QualityGateEventRepository qualityGateEventRepository,
            SourceStatusRepository sourceStatusRepository,
            JqhcProperties properties,
            ObjectMapper objectMapper,
            StreamService streamService) {
        this.domainConfigService = domainConfigService;
        this.telemetryPointRepository = telemetryPointRepository;
        this.latestStateRepository = latestStateRepository;
        this.productBatchRepository = productBatchRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.materialEventRepository = materialEventRepository;
        this.logisticsTaskRepository = logisticsTaskRepository;
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.sourceStatusRepository = sourceStatusRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.streamService = streamService;
    }

    public IngestResponse ingestTelemetry(IngestEnvelope envelope) {
        List<IngestResponse.IngestError> errors = new ArrayList<>();
        int accepted = 0;
        int rejected = 0;
        String lineId = null;

        if (envelope.getRecords() == null) {
            return response(0, 0, errors);
        }

        for (int i = 0; i < envelope.getRecords().size(); i++) {
            Map<String, Object> record = envelope.getRecords().get(i);
            try {
                String stepId = NumberUtils.str(record.get("process_step_id"));
                String equipmentId = NumberUtils.str(record.get("equipment_id"));
                lineId = NumberUtils.str(record.get("product_line_id"));
                Instant ts = parseInstant(record.get("timestamp"));
                if (ts == null) {
                    errors.add(new IngestResponse.IngestError(i, "SCHEMA_VIOLATION", "missing timestamp"));
                    rejected++;
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> values = (Map<String, Object>) record.get("values");
                if (values == null || values.isEmpty()) {
                    errors.add(new IngestResponse.IngestError(i, "SCHEMA_VIOLATION", "missing values"));
                    rejected++;
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> valueMeta =
                        (Map<String, Map<String, Object>>) record.getOrDefault("value_meta", Map.of());
                String batch = NumberUtils.str(record.get("product_batch"));
                String recordType = NumberUtils.str(record.getOrDefault("record_type", "process_snapshot"));

                boolean anyAccepted = false;
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    String fieldId = entry.getKey();
                    if (!domainConfigService.isFieldAllowed(stepId, fieldId)) {
                        errors.add(new IngestResponse.IngestError(i, "UNKNOWN_FIELD",
                                "field " + fieldId + " not in step " + stepId));
                        continue;
                    }
                    persistPoint(lineId, equipmentId, stepId, fieldId, batch, ts, entry.getValue(),
                            valueMeta.get(fieldId), recordType, envelope.getSource());
                    anyAccepted = true;
                }
                if (anyAccepted) {
                    accepted++;
                } else {
                    rejected++;
                }
            } catch (Exception e) {
                errors.add(new IngestResponse.IngestError(i, "SCHEMA_VIOLATION", e.getMessage()));
                rejected++;
            }
        }

        if (lineId != null) {
            streamService.broadcastOverview(lineId);
        }
        IngestResponse resp = response(accepted, rejected, errors);
        resp.setBufferWatermark(Instant.now().minus(properties.getBufferWindowHours(), ChronoUnit.HOURS));
        return resp;
    }

    private void persistPoint(
            String lineId, String equipmentId, String stepId, String fieldId, String batch,
            Instant ts, Object value, Map<String, Object> meta, String recordType, String source) {
        String quality = meta != null ? NumberUtils.str(meta.get("quality")) : "good";
        String dataSource = meta != null ? NumberUtils.str(meta.getOrDefault("data_source", source)) : source;

        TelemetryPointDoc point = new TelemetryPointDoc();
        point.setId(UUID.randomUUID().toString());
        point.setProductLineId(lineId);
        point.setEquipmentId(equipmentId);
        point.setProcessStepId(stepId);
        point.setFieldId(fieldId);
        point.setProductBatch(batch);
        point.setTimestamp(ts);
        point.setValue(value);
        point.setQuality(quality != null ? quality : "good");
        point.setDataSource(dataSource != null ? dataSource : "unknown");
        point.setRecordType(recordType);
        telemetryPointRepository.save(point);

        LatestStateDoc latest = new LatestStateDoc();
        latest.setId(LatestStateDoc.key(equipmentId, fieldId));
        latest.setProductLineId(lineId);
        latest.setEquipmentId(equipmentId);
        latest.setProcessStepId(stepId);
        latest.setFieldId(fieldId);
        latest.setProductBatch(batch);
        latest.setTimestamp(ts);
        latest.setValue(value);
        latest.setQuality(point.getQuality());
        latest.setDataSource(point.getDataSource());
        latestStateRepository.save(latest);
    }

    public IngestResponse ingestEvents(EventIngestEnvelope envelope) {
        int accepted = 0;
        int rejected = 0;
        String lineId = null;

        if (envelope.getEvents() == null) {
            return response(0, 0, List.of());
        }

        for (Map<String, Object> event : envelope.getEvents()) {
            if (event.containsKey("alarm_id") || event.containsKey("alarm_code")) {
                AlarmEventDoc alarm = objectMapper.convertValue(event, AlarmEventDoc.class);
                if (alarm.getHandleStatus() == null) {
                    alarm.setHandleStatus("pending");
                }
                alarmEventRepository.save(alarm);
                lineId = alarm.getProductLineId();
                accepted++;
            } else if (event.containsKey("task_id") || "LOGISTICS_TASK".equals(NumberUtils.str(event.get("event_type")))) {
                LogisticsTaskDoc task = objectMapper.convertValue(event, LogisticsTaskDoc.class);
                if (task.getTaskId() == null && event.get("task_id") != null) {
                    task.setTaskId(NumberUtils.str(event.get("task_id")));
                }
                if (task.getProductBatch() == null) {
                    task.setProductBatch(NumberUtils.str(event.get("material_batch")));
                }
                if (task.getSourceLocationId() == null) {
                    task.setSourceLocationId(NumberUtils.str(event.get("from_location")));
                }
                if (task.getTargetLocationId() == null) {
                    task.setTargetLocationId(NumberUtils.str(event.get("to_location")));
                }
                if (task.getStatus() == null) {
                    task.setStatus(NumberUtils.str(event.get("task_status")));
                }
                if (task.getCreatedAt() == null) {
                    task.setCreatedAt(parseInstant(event.getOrDefault("timestamp", Instant.now().toString())));
                }
                logisticsTaskRepository.save(task);
                accepted++;
            } else if (event.containsKey("gate_event_id") || "QUALITY_GATE".equals(NumberUtils.str(event.get("event_type")))) {
                QualityGateEventDoc gate = objectMapper.convertValue(event, QualityGateEventDoc.class);
                if (gate.getGateEventId() == null && event.get("gate_event_id") != null) {
                    gate.setGateEventId(NumberUtils.str(event.get("gate_event_id")));
                }
                if (gate.getBatchId() == null) {
                    gate.setBatchId(NumberUtils.str(event.get("material_batch")));
                }
                if (gate.getDecidedAt() == null) {
                    gate.setDecidedAt(parseInstant(event.getOrDefault("timestamp", Instant.now().toString())));
                }
                qualityGateEventRepository.save(gate);
                accepted++;
            } else if (event.containsKey("event_type") && event.containsKey("event_id")) {
                MaterialEventDoc material = objectMapper.convertValue(event, MaterialEventDoc.class);
                if (material.getEventId() == null) {
                    material.setEventId(NumberUtils.str(event.get("event_id")));
                }
                materialEventRepository.save(material);
                lineId = material.getProductLineId();
                accepted++;
            } else {
                rejected++;
            }
        }

        if (lineId != null) {
            streamService.broadcastOverview(lineId);
        }
        return response(accepted, rejected, List.of());
    }

    public IngestResponse ingestBatches(BatchIngestEnvelope envelope) {
        int accepted = 0;
        String lineId = null;

        if (envelope.getBatches() != null) {
            for (Map<String, Object> batchMap : envelope.getBatches()) {
                ProductBatchDoc batch = objectMapper.convertValue(batchMap, ProductBatchDoc.class);
                if (batch.getBatchId() == null && batchMap.get("batch_id") != null) {
                    batch.setBatchId(NumberUtils.str(batchMap.get("batch_id")));
                }
                productBatchRepository.save(batch);
                syncOrderProgress(batch.getProductionOrderId());
                lineId = batch.getProductLineId();
                accepted++;
            }
        }

        if (lineId != null) {
            streamService.broadcastOverview(lineId);
        }
        return response(accepted, 0, List.of());
    }

    public Map<String, Object> heartbeat(HeartbeatRequest req) {
        SourceStatusDoc status = new SourceStatusDoc();
        String lineId = req.getProductLineId();
        status.setId(SourceStatusDoc.key(req.getSourceInstance(), lineId));
        status.setSourceInstance(req.getSourceInstance());
        status.setSource(req.getSource());
        status.setProductLineId(lineId);
        status.setLastHeartbeatAt(req.getTimestamp() != null ? req.getTimestamp() : Instant.now());
        status.setScenarioId(req.getScenarioId());
        status.setSpeedMultiplier(req.getSpeedMultiplier());
        status.setConnected(true);
        Map<String, Object> runtime = req.getRuntime();
        if (runtime != null && !runtime.isEmpty()) {
            status.setDwellMode(NumberUtils.str(runtime.get("dwell_mode")));
            status.setDwellReason(NumberUtils.str(runtime.get("dwell_reason")));
            status.setCurrentStep(NumberUtils.str(runtime.get("current_step")));
            Object power = runtime.get("active_power_kw");
            if (power instanceof Number n) {
                status.setActivePowerKw(n.doubleValue());
            }
            Object running = runtime.get("running_equipment_count");
            if (running instanceof Number n) {
                status.setRunningEquipmentCount(n.intValue());
            }
            Object rawLow = runtime.get("raw_material_low");
            if (rawLow instanceof Boolean b) {
                status.setRawMaterialLow(b);
            } else if (rawLow != null) {
                status.setRawMaterialLow(Boolean.parseBoolean(String.valueOf(rawLow)));
            }
        }
        sourceStatusRepository.save(status);
        streamService.broadcastOverview(lineId);
        return Map.of("ok", true);
    }

    private IngestResponse response(int accepted, int rejected, List<IngestResponse.IngestError> errors) {
        IngestResponse resp = new IngestResponse(accepted, rejected);
        if (!errors.isEmpty()) {
            resp.setErrors(errors);
        }
        return resp;
    }

    private static Instant parseInstant(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(String.valueOf(o));
    }

    private void syncOrderProgress(String productionOrderId) {
        if (productionOrderId == null || productionOrderId.isBlank()) {
            return;
        }
        ProductionOrderDoc order = productionOrderRepository.findById(productionOrderId).orElse(null);
        if (order == null) {
            return;
        }
        List<ProductBatchDoc> batches = productBatchRepository.findByProductionOrderId(productionOrderId);
        Set<String> blockedBatchIds = OrderProgressHelper.blockedBatchIds(qualityGateEventRepository.findAll());
        Map<String, List<LogisticsTaskDoc>> tasksByBatch = OrderProgressHelper.tasksByBatch(logisticsTaskRepository.findAll());

        List<ProductBatchDoc> normalizedBatches = new ArrayList<>();
        for (ProductBatchDoc batch : batches) {
            String current = batch.getStatus();
            boolean qualityBlocked = blockedBatchIds.contains(batch.getBatchId());
            List<LogisticsTaskDoc> batchTasks = tasksByBatch.getOrDefault(batch.getBatchId(), List.of());
            String normalized = OrderProgressHelper.normalizeBatchStatus(current, qualityBlocked, batchTasks);

            if (!Objects.equals(current, normalized)) {
                batch.setStatus(normalized);
                productBatchRepository.save(batch);
            }
            normalizedBatches.add(batch);
        }

        double completedT = normalizedBatches.stream()
                .filter(b -> "completed".equalsIgnoreCase(b.getStatus()))
                .map(ProductBatchDoc::getQuantityKg)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum() / 1000.0;
        double readyToShipT = normalizedBatches.stream()
                .filter(b -> "ready_to_ship".equalsIgnoreCase(b.getStatus()))
                .map(ProductBatchDoc::getQuantityKg)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum() / 1000.0;
        double inProgressT = normalizedBatches.stream()
                .filter(b -> "in_progress".equalsIgnoreCase(b.getStatus()))
                .map(ProductBatchDoc::getQuantityKg)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum() / 1000.0;
        order.setReleasedQuantityT(completedT + readyToShipT + inProgressT);
        double planned = order.getPlannedQuantityT() != null ? order.getPlannedQuantityT() : 0.0;
        order.setStatus(OrderProgressHelper.resolveOrderStatus(
                normalizedBatches, completedT, readyToShipT, inProgressT, planned));
        productionOrderRepository.save(order);
    }
}

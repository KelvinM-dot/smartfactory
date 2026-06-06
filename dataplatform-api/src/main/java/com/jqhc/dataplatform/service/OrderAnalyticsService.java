package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.domain.*;
import com.jqhc.dataplatform.repository.*;
import com.jqhc.dataplatform.util.NumberUtils;
import com.jqhc.dataplatform.util.OrderProgressHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderAnalyticsService {

    private final ProductBatchRepository productBatchRepository;
    private final LogisticsTaskRepository logisticsTaskRepository;
    private final QualityGateEventRepository qualityGateEventRepository;
    private final ProductionOrderRepository productionOrderRepository;

    public OrderAnalyticsService(
            ProductBatchRepository productBatchRepository,
            LogisticsTaskRepository logisticsTaskRepository,
            QualityGateEventRepository qualityGateEventRepository,
            ProductionOrderRepository productionOrderRepository) {
        this.productBatchRepository = productBatchRepository;
        this.logisticsTaskRepository = logisticsTaskRepository;
        this.qualityGateEventRepository = qualityGateEventRepository;
        this.productionOrderRepository = productionOrderRepository;
    }

    public Map<String, Object> getOrderSummary(String factoryId) {
        List<Map<String, Object>> orders = getOrderList(factoryId, null, null);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("factory_id", factoryId != null && !factoryId.isBlank() ? factoryId : "JQHC-PLANT-01");
        summary.put("total_orders", orders.size());
        summary.put("released_orders", orders.stream().filter(o -> "released".equals(o.get("order_status"))).count());
        summary.put("in_progress_orders", orders.stream().filter(o -> "in_progress".equals(o.get("order_status"))).count());
        summary.put("blocked_orders", orders.stream().filter(o -> "blocked".equals(o.get("order_status"))).count());
        summary.put("ready_to_ship_orders", orders.stream().filter(o -> "ready_to_ship".equals(o.get("order_status"))).count());
        summary.put("completed_orders", orders.stream().filter(o -> "completed".equals(o.get("order_status"))).count());
        summary.put("high_risk_orders", orders.stream().filter(o -> {
            Object v = o.get("delivery_risk_level");
            return "high".equals(v) || "critical".equals(v);
        }).count());
        return summary;
    }

    public List<Map<String, Object>> getOrderList(String factoryId, String status, String riskLevel) {
        List<Map<String, Object>> riskList = getOrderRiskList(factoryId);
        return riskList.stream()
                .filter(o -> status == null || status.isBlank() || status.equals(String.valueOf(o.get("order_status"))))
                .filter(o -> riskLevel == null || riskLevel.isBlank() || riskLevel.equals(String.valueOf(o.get("delivery_risk_level"))))
                .sorted(Comparator
                        .comparingInt((Map<String, Object> o) -> riskRank(String.valueOf(o.get("delivery_risk_level"))))
                        .thenComparing(o -> String.valueOf(o.get("due_date")))
                        .thenComparing(o -> String.valueOf(o.get("production_order_id"))))
                .toList();
    }

    public Map<String, Object> getOrderDetail(String orderId) {
        ProductionOrderDoc order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("order not found: " + orderId));
        Map<String, Object> risk = getOrderRiskList(order.getFactoryId()).stream()
                .filter(o -> orderId.equals(o.get("production_order_id")))
                .findFirst()
                .orElseGet(LinkedHashMap::new);
        List<ProductBatchDoc> batches = productBatchRepository.findByProductionOrderId(orderId).stream()
                .sorted(Comparator.comparing(ProductBatchDoc::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<Map<String, Object>> batchList = batches.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("batch_id", b.getBatchId());
            m.put("product_line_id", b.getProductLineId());
            m.put("grade", b.getGrade());
            m.put("recipe_id", b.getRecipeId());
            m.put("status", b.getStatus());
            m.put("quantity_kg", b.getQuantityKg());
            m.put("started_at", b.getStartedAt());
            m.put("ended_at", b.getEndedAt());
            return m;
        }).toList();
        List<Map<String, Object>> qualityGates = qualityGateEventRepository.findAll().stream()
                .filter(g -> batches.stream().anyMatch(b -> Objects.equals(b.getBatchId(), g.getBatchId())))
                .sorted(Comparator.comparing(QualityGateEventDoc::getDecidedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("gate_event_id", g.getGateEventId());
                    m.put("batch_id", g.getBatchId());
                    m.put("process_step_id", g.getProcessStepId());
                    m.put("decision", g.getDecision());
                    m.put("reason_text", g.getReasonText());
                    m.put("decided_at", g.getDecidedAt());
                    return m;
                }).toList();
        List<Map<String, Object>> logisticsTasks = logisticsTaskRepository.findAll().stream()
                .filter(t -> orderId.equals(t.getProductionOrderId()))
                .sorted(Comparator.comparing(LogisticsTaskDoc::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("task_id", t.getTaskId());
                    m.put("task_type", t.getTaskType());
                    m.put("status", t.getStatus());
                    m.put("product_batch", t.getProductBatch());
                    m.put("source_location_id", t.getSourceLocationId());
                    m.put("target_location_id", t.getTargetLocationId());
                    m.put("created_at", t.getCreatedAt());
                    return m;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("risk", risk);
        result.put("batches", batchList);
        result.put("quality_gates", qualityGates);
        result.put("logistics_tasks", logisticsTasks);
        result.put("computed_at", Instant.now());
        return result;
    }

    public Map<String, Object> getOrderTimeline(String orderId) {
        ProductionOrderDoc order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("order not found: " + orderId));
        List<ProductBatchDoc> batches = productBatchRepository.findByProductionOrderId(orderId);
        Set<String> batchIds = batches.stream().map(ProductBatchDoc::getBatchId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (ProductBatchDoc batch : batches) {
            if (batch.getStartedAt() != null) {
                timeline.add(timelineEvent(batch.getStartedAt(), "batch_started", batch.getBatchId(), batch.getStatus(), batch.getProductLineId(), "批次启动"));
            }
            if (batch.getEndedAt() != null) {
                timeline.add(timelineEvent(batch.getEndedAt(), "batch_completed", batch.getBatchId(), batch.getStatus(), batch.getProductLineId(), "批次完工"));
            }
        }
        qualityGateEventRepository.findAll().stream()
                .filter(g -> batchIds.contains(g.getBatchId()))
                .forEach(g -> timeline.add(timelineEvent(
                        g.getDecidedAt(),
                        "quality_gate",
                        g.getBatchId(),
                        g.getDecision(),
                        null,
                        g.getReasonText() != null ? g.getReasonText() : "质量门事件"
                )));
        logisticsTaskRepository.findAll().stream()
                .filter(t -> orderId.equals(t.getProductionOrderId()))
                .forEach(t -> timeline.add(timelineEvent(
                        t.getCreatedAt(),
                        "logistics_task",
                        t.getProductBatch(),
                        t.getStatus(),
                        null,
                        t.getTaskType() != null ? t.getTaskType() : "物流任务"
                )));

        List<Map<String, Object>> sorted = timeline.stream()
                .sorted(Comparator.comparing(m -> (Instant) m.get("timestamp"), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("timeline", sorted);
        result.put("computed_at", Instant.now());
        return result;
    }

    public List<Map<String, Object>> getOrderRiskList(String factoryId) {
        Instant now = Instant.now();
        List<ProductBatchDoc> allBatches = productBatchRepository.findAll();
        List<LogisticsTaskDoc> tasks = logisticsTaskRepository.findAll();
        List<QualityGateEventDoc> gates = qualityGateEventRepository.findAll();

        return productionOrders(factoryId).stream().map(order -> {
            String orderId = order.getProductionOrderId();
            Instant dueDate = order.getDueDate();
            double plannedQty = Optional.ofNullable(order.getPlannedQuantityT()).orElse(0.0);
            double releasedQty = Optional.ofNullable(order.getReleasedQuantityT()).orElse(0.0);

            List<ProductBatchDoc> orderBatches = allBatches.stream()
                    .filter(b -> orderId != null && orderId.equals(b.getProductionOrderId()))
                    .toList();
            Set<String> blockedBatchIds = OrderProgressHelper.blockedBatchIds(gates);
            double completedQty = orderBatches.stream()
                    .filter(b -> "completed".equalsIgnoreCase(b.getStatus()))
                    .filter(b -> !blockedBatchIds.contains(b.getBatchId()))
                    .map(ProductBatchDoc::getQuantityKg)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum() / 1000.0;
            double blockedQty = orderBatches.stream()
                    .filter(b -> blockedBatchIds.contains(b.getBatchId()))
                    .map(ProductBatchDoc::getQuantityKg)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum() / 1000.0;
            double readyToShipQty = orderBatches.stream()
                    .filter(b -> "ready_to_ship".equalsIgnoreCase(b.getStatus()))
                    .map(ProductBatchDoc::getQuantityKg)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum() / 1000.0;
            double inProgressQty = orderBatches.stream()
                    .filter(b -> "in_progress".equalsIgnoreCase(b.getStatus()))
                    .map(ProductBatchDoc::getQuantityKg)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum() / 1000.0;

            List<LogisticsTaskDoc> orderTasks = tasks.stream()
                    .filter(t -> orderId != null && orderId.equals(t.getProductionOrderId()))
                    .toList();
            long pendingTasks = orderTasks.stream()
                    .filter(t -> t.getStatus() == null || !"completed".equalsIgnoreCase(t.getStatus()))
                    .count();
            long blockingLogisticsTasks = orderTasks.stream()
                    .filter(t -> t.getStatus() == null || !"completed".equalsIgnoreCase(t.getStatus()))
                    .filter(OrderProgressHelper::isDispatchTask)
                    .count();
            long blockingGates = gates.stream()
                    .filter(g -> orderBatches.stream().anyMatch(b -> Objects.equals(b.getBatchId(), g.getBatchId())))
                    .filter(g -> g.getDecision() != null && !"pass".equalsIgnoreCase(g.getDecision()))
                    .count();

            int slaDays = Optional.ofNullable(order.getDeliverySlaDays()).orElse(30);
            String orderType = Optional.ofNullable(order.getOrderType()).orElse("regular");
            long daysToDue = dueDate != null ? ChronoUnit.DAYS.between(now, dueDate) : slaDays;
            double progress = plannedQty <= 0 ? 1.0 : completedQty / plannedQty;

            String riskLevel = "low";
            List<String> blockingReasons = new ArrayList<>();
            boolean deliveryBlocked = OrderProgressHelper.deliveryBlocked(orderTasks, blockingGates);
            if (dueDate != null && daysToDue <= 2 && progress < 0.7) {
                riskLevel = "critical";
                blockingReasons.add("交期临近且完工不足");
            } else if ("export".equals(orderType) && daysToDue <= Math.max(5, slaDays / 4) && progress < 0.6) {
                riskLevel = "critical";
                blockingReasons.add("出口订单交期 SLA 逼近");
            } else if (deliveryBlocked || pendingTasks > 3) {
                riskLevel = "high";
            } else if ("custom".equals(orderType) && daysToDue <= slaDays / 3 && progress < 0.4) {
                riskLevel = "high";
                blockingReasons.add("定制单进度落后");
            } else if (completedQty < releasedQty * 0.5) {
                riskLevel = "medium";
            }
            if (blockedQty > 0) {
                blockingReasons.add("存在待放行批次");
            }
            if (pendingTasks > 3) {
                blockingReasons.add("物流任务积压");
            }
            if (blockingGates > 0) {
                blockingReasons.add("存在质量门待处置");
            }
            if (blockingReasons.isEmpty()) {
                blockingReasons.add("暂无明显阻塞");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("production_order_id", orderId);
            result.put("factory_id", factoryId != null && !factoryId.isBlank() ? factoryId : "JQHC-PLANT-01");
            result.put("customer_order_id", order.getCustomerOrderId());
            result.put("order_status", order.getStatus());
            result.put("planned_quantity_t", plannedQty);
            result.put("released_quantity_t", releasedQty);
            result.put("completed_quantity_t", NumberUtils.round1(completedQty));
            result.put("ready_to_ship_quantity_t", NumberUtils.round1(readyToShipQty));
            result.put("in_progress_quantity_t", NumberUtils.round1(inProgressQty));
            result.put("blocked_quantity_t", NumberUtils.round1(blockedQty));
            result.put("progress_pct", NumberUtils.round1(progress * 100.0));
            result.put("batch_status_summary", Map.of(
                    "hold", orderBatches.stream().filter(b -> "hold".equalsIgnoreCase(b.getStatus())).count(),
                    "in_progress", orderBatches.stream().filter(b -> "in_progress".equalsIgnoreCase(b.getStatus())).count(),
                    "ready_to_ship", orderBatches.stream().filter(b -> "ready_to_ship".equalsIgnoreCase(b.getStatus())).count(),
                    "completed", orderBatches.stream().filter(b -> "completed".equalsIgnoreCase(b.getStatus())).count()
            ));
            result.put("due_date", dueDate);
            result.put("delivery_risk_level", riskLevel);
            result.put("delivery_blocked", deliveryBlocked);
            result.put("blocking_logistics_tasks", blockingLogisticsTasks);
            result.put("blocking_quality_gates", blockingGates);
            result.put("estimated_ship_date", dueDate != null ? dueDate.minus(Math.max(0, pendingTasks), ChronoUnit.HOURS) : null);
            result.put("blocking_reasons", blockingReasons);
            result.put("product_id", order.getProductId());
            result.put("grade", order.getGrade());
            result.put("product_category", order.getProductCategory());
            result.put("recipe_id", order.getRecipeId());
            result.put("priority", order.getPriority());
            result.put("assigned_line_ids", order.getAssignedLineIds());
            result.put("order_type", orderType);
            result.put("delivery_sla_days", slaDays);
            result.put("customer_segment", order.getCustomerSegment());
            result.put("is_export", Boolean.TRUE.equals(order.getIsExport()));
            result.put("days_to_due", daysToDue);
            return result;
        }).sorted(Comparator
                .comparingInt((Map<String, Object> m) -> riskRank(String.valueOf(m.get("delivery_risk_level"))))
                .thenComparing(m -> String.valueOf(m.get("due_date")))
                .thenComparing(m -> String.valueOf(m.get("production_order_id"))))
                .toList();
    }

    private static int riskRank(String level) {
        return switch (level) {
            case "critical" -> 0;
            case "high" -> 1;
            case "medium" -> 2;
            case "low" -> 3;
            default -> 4;
        };
    }

    private Map<String, Object> timelineEvent(Instant timestamp, String type, String batchId, String status, String lineId, String message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", timestamp);
        event.put("event_type", type);
        event.put("batch_id", batchId);
        event.put("status", status);
        event.put("product_line_id", lineId);
        event.put("message", message);
        return event;
    }

    private List<ProductionOrderDoc> productionOrders(String factoryId) {
        if (factoryId != null && !factoryId.isBlank()) {
            return productionOrderRepository.findByFactoryIdOrderByDueDateAsc(factoryId);
        }
        return productionOrderRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductionOrderDoc::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

}

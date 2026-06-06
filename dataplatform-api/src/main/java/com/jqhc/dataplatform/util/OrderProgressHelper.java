package com.jqhc.dataplatform.util;

import com.jqhc.dataplatform.domain.LogisticsTaskDoc;
import com.jqhc.dataplatform.domain.ProductBatchDoc;
import com.jqhc.dataplatform.domain.QualityGateEventDoc;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class OrderProgressHelper {

    private OrderProgressHelper() {}

    public static Set<String> blockedBatchIds(List<QualityGateEventDoc> gates) {
        return gates.stream()
                .filter(g -> g.getBatchId() != null)
                .filter(g -> g.getDecision() != null && !"pass".equalsIgnoreCase(g.getDecision()))
                .map(QualityGateEventDoc::getBatchId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static Map<String, List<LogisticsTaskDoc>> tasksByBatch(List<LogisticsTaskDoc> tasks) {
        return tasks.stream()
                .filter(t -> t.getProductBatch() != null)
                .collect(Collectors.groupingBy(LogisticsTaskDoc::getProductBatch));
    }

    public static String normalizeBatchStatus(
            String current,
            boolean qualityBlocked,
            List<LogisticsTaskDoc> batchTasks) {
        long pendingLogistics = batchTasks.stream()
                .filter(t -> t.getStatus() == null || !"completed".equalsIgnoreCase(t.getStatus()))
                .count();
        boolean hasDispatchTask = batchTasks.stream()
                .anyMatch(t -> t.getTaskType() != null
                        && (t.getTaskType().contains("stock")
                        || t.getTaskType().contains("dispatch")
                        || t.getTaskType().contains("transfer")));
        boolean deliveryCompleted = hasDispatchTask && pendingLogistics == 0;

        if (qualityBlocked) {
            return "hold";
        }
        if ("completed".equalsIgnoreCase(current) || "ready_to_ship".equalsIgnoreCase(current)) {
            return deliveryCompleted ? "completed" : "ready_to_ship";
        }
        if ("hold".equalsIgnoreCase(current)) {
            return qualityBlocked ? "hold" : (deliveryCompleted ? "completed" : "ready_to_ship");
        }
        if ("in_progress".equalsIgnoreCase(current) || "released".equalsIgnoreCase(current)) {
            return deliveryCompleted ? "completed" : current;
        }
        if (current == null || current.isBlank()) {
            return "released";
        }
        return current;
    }

    public static String resolveOrderStatus(
            List<ProductBatchDoc> batches,
            double completedT,
            double readyToShipT,
            double inProgressT,
            double planned) {
        boolean hasBlockedBatch = batches.stream().anyMatch(b -> "hold".equalsIgnoreCase(b.getStatus()));
        boolean hasReadyToShipBatch = batches.stream().anyMatch(b -> "ready_to_ship".equalsIgnoreCase(b.getStatus()));
        boolean hasInProgressBatch = batches.stream().anyMatch(b -> "in_progress".equalsIgnoreCase(b.getStatus()));

        if (hasBlockedBatch) {
            return "blocked";
        }
        if (!hasReadyToShipBatch && !hasInProgressBatch && completedT >= planned && planned > 0) {
            return "completed";
        }
        if (readyToShipT > 0 && completedT + readyToShipT >= planned && planned > 0) {
            return "ready_to_ship";
        }
        if (completedT > 0 || readyToShipT > 0 || inProgressT > 0) {
            return "in_progress";
        }
        return "released";
    }

    public static boolean isDispatchTask(LogisticsTaskDoc task) {
        String taskType = task.getTaskType();
        return taskType == null
                || taskType.contains("stock")
                || taskType.contains("dispatch")
                || taskType.contains("transfer");
    }

    public static boolean deliveryBlocked(
            List<LogisticsTaskDoc> orderTasks,
            long blockingGates) {
        long blockingLogisticsTasks = orderTasks.stream()
                .filter(t -> t.getStatus() == null || !"completed".equalsIgnoreCase(t.getStatus()))
                .filter(OrderProgressHelper::isDispatchTask)
                .count();
        return blockingGates > 0 || blockingLogisticsTasks > 0;
    }
}

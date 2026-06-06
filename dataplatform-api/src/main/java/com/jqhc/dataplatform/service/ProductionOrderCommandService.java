package com.jqhc.dataplatform.service;

import com.jqhc.dataplatform.domain.ProductLineDoc;
import com.jqhc.dataplatform.domain.ProductSpecDoc;
import com.jqhc.dataplatform.domain.ProductionOrderDoc;
import com.jqhc.dataplatform.domain.RecipeDoc;
import com.jqhc.dataplatform.repository.ProductLineRepository;
import com.jqhc.dataplatform.repository.ProductSpecRepository;
import com.jqhc.dataplatform.repository.ProductionOrderRepository;
import com.jqhc.dataplatform.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class ProductionOrderCommandService {

    private final ProductSpecRepository productSpecRepository;
    private final RecipeRepository recipeRepository;
    private final ProductLineRepository productLineRepository;
    private final ProductionOrderRepository productionOrderRepository;

    public ProductionOrderCommandService(
            ProductSpecRepository productSpecRepository,
            RecipeRepository recipeRepository,
            ProductLineRepository productLineRepository,
            ProductionOrderRepository productionOrderRepository) {
        this.productSpecRepository = productSpecRepository;
        this.recipeRepository = recipeRepository;
        this.productLineRepository = productLineRepository;
        this.productionOrderRepository = productionOrderRepository;
    }

    public ProductionOrderDoc createOrder(Map<String, Object> payload) {
        String productId = requiredText(payload, "product_id");
        ProductSpecDoc product = productSpecRepository.findById(productId)
                .orElseThrow(() -> badRequest("产品不存在: " + productId));
        String recipeId = requiredText(payload, "recipe_id");
        RecipeDoc recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> badRequest("配方不存在: " + recipeId));
        String productCategory = requiredText(payload, "product_category");
        String grade = requiredText(payload, "grade");
        if (!Objects.equals(productCategory, product.getProductCategory())) {
            throw badRequest("产品类别与产品主数据不匹配");
        }
        if (!Objects.equals(grade, product.getGrade())) {
            throw badRequest("牌号与产品主数据不匹配");
        }
        if (!Objects.equals(productCategory, recipe.getProductCategory())) {
            throw badRequest("产品类别与配方不匹配");
        }
        if (!Objects.equals(grade, recipe.getGrade())) {
            throw badRequest("牌号与配方不匹配");
        }
        Object assignedLines = payload.get("assigned_line_ids");
        if (!(assignedLines instanceof List<?> rawLines) || rawLines.isEmpty()) {
            throw badRequest("必须选择下发产线");
        }
        List<String> lineIds = new ArrayList<>();
        for (Object item : rawLines) {
            if (item != null && !String.valueOf(item).isBlank()) {
                lineIds.add(String.valueOf(item));
            }
        }
        if (lineIds.size() != 1) {
            throw badRequest("演示模式下订单必须直接下发到单条产线");
        }
        String lineId = lineIds.get(0);
        ProductLineDoc line = productLineRepository.findById(lineId)
                .orElseThrow(() -> badRequest("产线不存在: " + lineId));
        if (!Objects.equals(line.getProductCategory(), productCategory)) {
            throw badRequest("产线与产品类别不匹配");
        }
        if (!Objects.equals(recipe.getProductLineId(), lineId)) {
            throw badRequest("配方与下发产线不匹配");
        }
        if (product.getAllowedLineIds() != null && !product.getAllowedLineIds().isEmpty() && !product.getAllowedLineIds().contains(lineId)) {
            throw badRequest("产品与下发产线不匹配");
        }
        if (product.getDefaultRecipeIds() != null && !product.getDefaultRecipeIds().isEmpty() && !product.getDefaultRecipeIds().contains(recipeId)) {
            throw badRequest("配方不在产品允许范围内");
        }
        String priority = payload.get("priority") != null ? String.valueOf(payload.get("priority")) : "normal";
        Set<String> allowedPriorities = new HashSet<>(List.of("low", "normal", "high"));
        if (!allowedPriorities.contains(priority)) {
            throw badRequest("priority 必须是 low / normal / high");
        }
        double plannedQuantityT = payload.get("planned_quantity_t") instanceof Number n ? n.doubleValue() : 0.0;
        if (plannedQuantityT <= 0) {
            throw badRequest("planned_quantity_t 必须大于 0");
        }
        if (plannedQuantityT > 1000) {
            throw badRequest("planned_quantity_t 超出演示系统允许范围");
        }
        Instant dueDate;
        if (payload.get("due_date") != null && !String.valueOf(payload.get("due_date")).isBlank()) {
            dueDate = Instant.parse(String.valueOf(payload.get("due_date")));
            if (!dueDate.isAfter(Instant.now())) {
                throw badRequest("due_date 必须晚于当前时间");
            }
        } else {
            throw badRequest("due_date 为必填");
        }
        ProductionOrderDoc order = new ProductionOrderDoc();
        String orderId = payload.get("production_order_id") != null
                ? String.valueOf(payload.get("production_order_id"))
                : "PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (productionOrderRepository.existsById(orderId)) {
            throw badRequest("订单号已存在: " + orderId);
        }
        order.setProductionOrderId(orderId);
        order.setFactoryId(payload.get("factory_id") != null ? String.valueOf(payload.get("factory_id")) : "JQHC-PLANT-01");
        order.setCustomerOrderId(payload.get("customer_order_id") != null ? String.valueOf(payload.get("customer_order_id")) : null);
        order.setProductId(productId);
        order.setProductCategory(productCategory);
        order.setGrade(grade);
        order.setRecipeId(recipeId);
        order.setPriority(priority);
        order.setPlannedQuantityT(plannedQuantityT);
        order.setReleasedQuantityT(0.0);
        order.setStatus("released");
        order.setRemark(payload.get("remark") != null ? String.valueOf(payload.get("remark")) : "created_from_order_center");
        order.setDueDate(dueDate);
        order.setAssignedLineIds(lineIds);
        return productionOrderRepository.save(order);
    }

    public ProductionOrderDoc patchOrderStatus(String orderId, Map<String, Object> payload) {
        ProductionOrderDoc order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在: " + orderId));
        if (payload.get("status") != null) {
            String status = String.valueOf(payload.get("status"));
            Set<String> allowed = new HashSet<>(List.of("released", "in_progress", "blocked", "ready_to_ship", "completed"));
            if (!allowed.contains(status)) {
                throw badRequest("status 无效: " + status);
            }
            order.setStatus(status);
        }
        if (payload.get("remark") != null) {
            order.setRemark(String.valueOf(payload.get("remark")));
        }
        return productionOrderRepository.save(order);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String requiredText(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw badRequest(field + " 为必填");
        }
        return String.valueOf(value);
    }
}

package com.jqhc.dataplatform.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 master-data 中的 twin_layouts 转为前端 2D/3D 孪生组件所需结构。
 */
@Service
public class TwinLayoutService {

    private final FactoryMasterDataService masterDataService;

    public TwinLayoutService(FactoryMasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAllLayoutsForFrontend() {
        Map<String, Object> raw = masterDataService.getTwinLayouts();
        Map<String, Object> fieldLabels = masterDataService.getTwinFieldLabels();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> lineRaw) {
                result.put(entry.getKey(), toFrontendLayout((Map<String, Object>) lineRaw, fieldLabels));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLayoutForLine(String lineId) {
        Map<String, Object> raw = masterDataService.getTwinLayouts();
        Object lineRaw = raw.get(lineId);
        if (!(lineRaw instanceof Map<?, ?>)) {
            return Map.of();
        }
        return toFrontendLayout((Map<String, Object>) lineRaw, masterDataService.getTwinFieldLabels());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toFrontendLayout(Map<String, Object> lineRaw, Map<String, Object> fieldLabels) {
        Map<String, Object> out = new LinkedHashMap<>();
        Object viewBox = lineRaw.get("view_box");
        if (viewBox == null) {
            viewBox = lineRaw.get("viewBox");
        }
        out.put("viewBox", viewBox);

        Object flowPath = lineRaw.get("flow_path");
        if (flowPath == null) {
            flowPath = lineRaw.get("flowPath");
        }
        out.put("flowPath", flowPath);

        Map<String, Object> nodes = new LinkedHashMap<>();
        Map<String, Object> keyFields = new LinkedHashMap<>();
        Map<String, Object> positions3d = new LinkedHashMap<>();

        Object stepsObj = lineRaw.get("steps");
        if (stepsObj instanceof Map<?, ?> steps) {
            for (Map.Entry<?, ?> stepEntry : steps.entrySet()) {
                String stepId = String.valueOf(stepEntry.getKey());
                if (!(stepEntry.getValue() instanceof Map<?, ?> step)) {
                    continue;
                }
                Map<String, Object> stepMap = (Map<String, Object>) step;
                Object node = stepMap.get("node");
                if (node != null) {
                    nodes.put(stepId, node);
                }
                Object keyField = stepMap.get("key_field_id");
                if (keyField == null) {
                    keyField = stepMap.get("keyFieldId");
                }
                if (keyField != null) {
                    keyFields.put(stepId, keyField);
                }
                Object pos3d = stepMap.get("position_3d");
                if (pos3d == null) {
                    pos3d = stepMap.get("position3d");
                }
                if (pos3d != null) {
                    positions3d.put(stepId, normalizePosition3d((Map<String, Object>) pos3d));
                }
            }
        }

        // 兼容旧式扁平 nodes / keyFields / positions3d
        if (nodes.isEmpty() && lineRaw.get("nodes") instanceof Map<?, ?> legacyNodes) {
            nodes.putAll((Map<String, Object>) legacyNodes);
        }
        if (keyFields.isEmpty() && lineRaw.get("key_fields") instanceof Map<?, ?> legacyKeys) {
            keyFields.putAll((Map<String, Object>) legacyKeys);
        } else if (keyFields.isEmpty() && lineRaw.get("keyFields") instanceof Map<?, ?> legacyKeys) {
            keyFields.putAll((Map<String, Object>) legacyKeys);
        }
        if (positions3d.isEmpty() && lineRaw.get("positions_3d") instanceof Map<?, ?> legacyPos) {
            positions3d.putAll((Map<String, Object>) legacyPos);
        } else if (positions3d.isEmpty() && lineRaw.get("positions3d") instanceof Map<?, ?> legacyPos) {
            positions3d.putAll((Map<String, Object>) legacyPos);
        }

        out.put("nodes", nodes);
        out.put("keyFields", keyFields);
        out.put("positions3d", positions3d);

        Object twin3dReady = lineRaw.get("twin_3d_ready");
        if (twin3dReady == null) {
            twin3dReady = lineRaw.get("twin3dReady");
        }
        if (twin3dReady instanceof Boolean b) {
            out.put("twin3dReady", b);
        }

        Object lineFieldLabels = lineRaw.get("field_labels");
        if (lineFieldLabels instanceof Map<?, ?> map) {
            out.put("fieldLabels", map);
        } else {
            out.put("fieldLabels", fieldLabels);
        }
        return out;
    }

    private Map<String, Object> normalizePosition3d(Map<String, Object> pos) {
        Map<String, Object> out = new LinkedHashMap<>(pos);
        if (!out.containsKey("x") && out.get("position") instanceof Map<?, ?> p) {
            out.put("x", p.get("x"));
            out.put("y", p.get("y"));
            out.put("z", p.get("z"));
        }
        Object variant = out.get("variant");
        if (variant == null) {
            variant = out.get("model_variant");
        }
        if (variant != null) {
            out.put("variant", variant);
        }
        return out;
    }
}

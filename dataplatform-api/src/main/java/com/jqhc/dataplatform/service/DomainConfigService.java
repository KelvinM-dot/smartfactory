package com.jqhc.dataplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jqhc.dataplatform.config.JqhcProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DomainConfigService {

    private static final Logger log = LoggerFactory.getLogger(DomainConfigService.class);

    private final JqhcProperties properties;
    private final ObjectMapper objectMapper;

    private Map<String, Object> config = Map.of();
    private Map<String, String> stepDisplayNames = Map.of();
    private Map<String, Set<String>> stepFields = Map.of();
    private Map<String, Map<String, Object>> fieldDefs = Map.of();

    public DomainConfigService(JqhcProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            Path path = Path.of(properties.getDomainConfigPath()).toAbsolutePath().normalize();
            File file = path.toFile();
            if (!file.exists()) {
                log.warn("Domain config not found at {}, using empty config", path);
                return;
            }
            config = objectMapper.readValue(file, new TypeReference<>() {});
            buildIndexes();
            log.info("Loaded domain config from {} ({} steps, {} fields)",
                    path, stepDisplayNames.size(), fieldDefs.size());
        } catch (Exception e) {
            log.error("Failed to load domain config", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void buildIndexes() {
        Map<String, String> names = new HashMap<>();
        List<Map<String, Object>> steps = (List<Map<String, Object>>) config.getOrDefault("process_steps", List.of());
        for (Map<String, Object> step : steps) {
            names.put(String.valueOf(step.get("step_id")), String.valueOf(step.get("display_name")));
        }
        stepDisplayNames = Collections.unmodifiableMap(names);

        Map<String, Map<String, Object>> fields = new HashMap<>();
        List<Map<String, Object>> fieldList = (List<Map<String, Object>>) config.getOrDefault("fields", List.of());
        for (Map<String, Object> field : fieldList) {
            fields.put(String.valueOf(field.get("field_id")), field);
        }
        fieldDefs = Collections.unmodifiableMap(fields);

        Map<String, Set<String>> bindings = new HashMap<>();
        List<Map<String, Object>> bindingList =
                (List<Map<String, Object>>) config.getOrDefault("step_field_bindings", List.of());
        for (Map<String, Object> binding : bindingList) {
            String stepId = String.valueOf(binding.get("step_id"));
            String fieldId = String.valueOf(binding.get("field_id"));
            bindings.computeIfAbsent(stepId, k -> new HashSet<>()).add(fieldId);
        }
        stepFields = bindings.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getStepDisplayName(String stepId) {
        return stepDisplayNames.getOrDefault(stepId, stepId);
    }

    public boolean isFieldAllowed(String stepId, String fieldId) {
        Set<String> allowed = stepFields.get(stepId);
        if (allowed == null) {
            return false;
        }
        if (allowed.contains(fieldId)) {
            return true;
        }
        return "status".equals(fieldId)
                || "run_mode".equals(fieldId)
                || "alarm_code".equals(fieldId)
                || "power_kw".equals(fieldId);
    }

    public Map<String, Object> getFieldDef(String fieldId) {
        return fieldDefs.get(fieldId);
    }

    @SuppressWarnings("unchecked")
    public List<String> getTemplateStepIds(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> templates =
                (List<Map<String, Object>>) config.getOrDefault("product_templates", List.of());
        for (Map<String, Object> template : templates) {
            if (!templateId.equals(String.valueOf(template.get("template_id")))) {
                continue;
            }
            List<Map<String, Object>> steps =
                    (List<Map<String, Object>>) template.getOrDefault("steps", List.of());
            return steps.stream()
                    .map(s -> String.valueOf(s.get("step_id")))
                    .toList();
        }
        return List.of();
    }

    public Map<String, Double> getSpecLimits(String fieldId) {
        Map<String, Object> def = fieldDefs.get(fieldId);
        if (def == null || def.get("spec_limits") == null) {
            return Map.of();
        }
        Map<String, Object> raw = (Map<String, Object>) def.get("spec_limits");
        Map<String, Double> limits = new HashMap<>();
        raw.forEach((k, v) -> {
            if (v instanceof Number n) {
                limits.put(k, n.doubleValue());
            }
        });
        return limits;
    }
}

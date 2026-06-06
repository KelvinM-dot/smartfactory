package com.jqhc.dataplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jqhc.dataplatform.config.JqhcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** 智优模块 ↔ line-simulator 控制面代理（:3002），不侵入 Ingest 链路。 */
@Service
public class SimulatorProxyService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorProxyService.class);

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public SimulatorProxyService(JqhcProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        String base = properties.getSimulatorUrl();
        if (base == null || base.isBlank()) {
            base = "http://127.0.0.1:3002";
        }
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public Map<String, Object> getStatus() {
        return get("/sim/status");
    }

    public Map<String, Object> applyScenario(String scenarioId) {
        return applyScenario(scenarioId, null);
    }

    public Map<String, Object> applyScenario(String scenarioId, Double greenShiftPct) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scenario_id", scenarioId);
        if (greenShiftPct != null) {
            body.put("green_shift_pct", greenShiftPct);
        }
        return post("/sim/scenario", body);
    }

    public boolean isAvailable() {
        try {
            Map<String, Object> health = get("/health");
            return "ok".equals(String.valueOf(health.get("status")));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> get(String path) {
        try {
            HttpURLConnection conn = open(path, "GET", null);
            return readJson(conn);
        } catch (Exception e) {
            log.warn("Simulator GET {} failed: {}", path, e.getMessage());
            return errorMap(e.getMessage());
        }
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpURLConnection conn = open(path, "POST", json);
            return readJson(conn);
        } catch (Exception e) {
            log.warn("Simulator POST {} failed: {}", path, e.getMessage());
            return errorMap(e.getMessage());
        }
    }

    private HttpURLConnection open(String path, String method, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + path).toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Accept", "application/json");
        if (jsonBody != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }
        }
        return conn;
    }

    private Map<String, Object> readJson(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            Map<String, Object> err = errorMap("HTTP " + code + " empty body");
            err.put("http_status", code);
            return err;
        }
        Map<String, Object> parsed = objectMapper.readValue(stream, new TypeReference<LinkedHashMap<String, Object>>() {});
        if (code >= 400) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false);
            err.put("http_status", code);
            err.put("error", parsed);
            return err;
        }
        return parsed;
    }

    private Map<String, Object> errorMap(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("ok", false);
        err.put("error", message);
        err.put("simulator_available", false);
        return err;
    }
}

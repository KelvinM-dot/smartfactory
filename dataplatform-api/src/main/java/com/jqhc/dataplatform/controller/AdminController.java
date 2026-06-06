package com.jqhc.dataplatform.controller;

import com.jqhc.dataplatform.service.SeedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POC 运维：由模拟器触发「清库 + 主数据重灌」。
 */
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final SeedService seedService;

    public AdminController(SeedService seedService) {
        this.seedService = seedService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("ok", true, "reseed_endpoint", "POST /v1/admin/reseed", "storage", "sqlite");
    }

    @PostMapping("/reseed")
    public Map<String, Object> reseed() {
        Map<String, Object> summary = seedService.reseedFromMasterData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("action", "reseed_from_master_data");
        result.put("source", "jqhc-factory-master-data.json");
        result.put("summary", summary);
        return result;
    }
}

package com.jqhc.dataplatform.controller;

import com.jqhc.dataplatform.dto.*;
import com.jqhc.dataplatform.service.IngestService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/telemetry")
    public IngestResponse telemetry(@RequestBody IngestEnvelope envelope) {
        return ingestService.ingestTelemetry(envelope);
    }

    @PostMapping("/events")
    public IngestResponse events(@RequestBody EventIngestEnvelope envelope) {
        return ingestService.ingestEvents(envelope);
    }

    @PostMapping("/batches")
    public IngestResponse batches(@RequestBody BatchIngestEnvelope envelope) {
        return ingestService.ingestBatches(envelope);
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody HeartbeatRequest request) {
        return ingestService.heartbeat(request);
    }
}

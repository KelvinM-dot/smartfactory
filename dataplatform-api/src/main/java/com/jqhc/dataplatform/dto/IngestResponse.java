package com.jqhc.dataplatform.dto;

import java.time.Instant;
import java.util.List;

public class IngestResponse {

    private int accepted;
    private int rejected;
    private List<IngestError> errors;
    private Instant bufferWatermark;

    public IngestResponse(int accepted, int rejected) {
        this.accepted = accepted;
        this.rejected = rejected;
    }

    public int getAccepted() { return accepted; }
    public void setAccepted(int accepted) { this.accepted = accepted; }
    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }
    public List<IngestError> getErrors() { return errors; }
    public void setErrors(List<IngestError> errors) { this.errors = errors; }
    public Instant getBufferWatermark() { return bufferWatermark; }
    public void setBufferWatermark(Instant bufferWatermark) { this.bufferWatermark = bufferWatermark; }

    public record IngestError(int index, String code, String message) {}
}

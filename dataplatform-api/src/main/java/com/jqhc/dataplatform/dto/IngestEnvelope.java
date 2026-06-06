package com.jqhc.dataplatform.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class IngestEnvelope {

    private String source;
    private String sourceInstance;
    private Instant sentAt;
    private List<Map<String, Object>> records;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceInstance() { return sourceInstance; }
    public void setSourceInstance(String sourceInstance) { this.sourceInstance = sourceInstance; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }
}

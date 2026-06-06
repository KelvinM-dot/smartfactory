package com.jqhc.dataplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class StreamService {

    private final ComputeService computeService;
    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> lineSessions = new ConcurrentHashMap<>();

    public StreamService(ComputeService computeService, ObjectMapper objectMapper) {
        this.computeService = computeService;
        this.objectMapper = objectMapper;
    }

    public void subscribe(String lineId, WebSocketSession session) {
        lineSessions.computeIfAbsent(lineId, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unsubscribe(String lineId, WebSocketSession session) {
        Set<WebSocketSession> sessions = lineSessions.get(lineId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public void broadcastOverview(String lineId) {
        Set<WebSocketSession> sessions = lineSessions.get(lineId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> overview = computeService.getLineOverview(lineId);
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("type", "overview_patch");
            envelope.put("payload", overview);
            String json = objectMapper.writeValueAsString(envelope);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception ignored) {
            // best-effort push
        }
    }
}

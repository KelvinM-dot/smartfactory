package com.jqhc.dataplatform.websocket;

import com.jqhc.dataplatform.service.StreamService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

@Component
public class LineStreamHandler extends TextWebSocketHandler {

    private final StreamService streamService;

    public LineStreamHandler(StreamService streamService) {
        this.streamService = streamService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String lineId = extractLineId(session.getUri());
        if (lineId != null) {
            streamService.subscribe(lineId, session);
            session.sendMessage(new TextMessage("{\"type\":\"connected\",\"line_id\":\"" + lineId + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String lineId = extractLineId(session.getUri());
        if (lineId != null) {
            streamService.unsubscribe(lineId, session);
        }
    }

    private String extractLineId(URI uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        // /v1/stream/lines/{lineId}
        String[] parts = path.split("/");
        if (parts.length >= 5) {
            return parts[parts.length - 1];
        }
        return null;
    }
}

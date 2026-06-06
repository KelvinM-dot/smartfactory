package com.jqhc.dataplatform.config;

import com.jqhc.dataplatform.websocket.LineStreamHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LineStreamHandler lineStreamHandler;

    public WebSocketConfig(LineStreamHandler lineStreamHandler) {
        this.lineStreamHandler = lineStreamHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lineStreamHandler, "/v1/stream/lines/{lineId}")
                .setAllowedOrigins("*");
    }
}

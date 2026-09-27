package com.liveinterviewer.sessionservice.config;

import com.liveinterviewer.sessionservice.security.JwtHandshakeInterceptor;
import com.liveinterviewer.sessionservice.websocket.InterviewWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final InterviewWebSocketHandler interviewWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewWebSocketHandler, "/ws/interview")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*"); // tighten this to your actual frontend origin before deploying
    }
}

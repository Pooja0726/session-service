package com.liveinterviewer.sessionservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

// Connection URL shape: wss://host/ws/interview?token=<jwt>&sessionId=<id>
// Both are checked once, before the connection upgrades to a WebSocket.
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                    @NonNull ServerHttpResponse response,
                                    @NonNull WebSocketHandler wsHandler,
                                    @NonNull Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        String token = extractParam(query, "token");
        String sessionId = extractParam(query, "sessionId");

        if (token == null || !jwtUtil.isTokenValid(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (sessionId == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        attributes.put("email", jwtUtil.extractEmail(token));
        attributes.put("sessionId", Long.parseLong(sessionId));
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                                @NonNull ServerHttpResponse response,
                                @NonNull WebSocketHandler wsHandler,
                                Exception exception) {
    }

    private String extractParam(String rawQuery, String key) {
        if (rawQuery == null) return null;

        for (String param : rawQuery.split("&")) {
            if (param.startsWith(key + "=")) {
                return param.substring(key.length() + 1);
            }
        }
        return null;
    }
}

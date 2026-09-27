package com.liveinterviewer.sessionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/heygen")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class HeyGenController {

    @Value("${heygen.api.key}")
    private String heyGenApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String HEYGEN_BASE = "https://api.heygen.com";

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", heyGenApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        return headers;
    }

    /**
     * Step 0: Get a temporary access token for the streaming API.
     */
    @PostMapping("/token")
    public ResponseEntity<?> createToken() {
        try {
            HttpEntity<String> request = new HttpEntity<>("{}", buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.create_token",
                    request, Map.class
            );
            log.info("HeyGen create_token response status: {}", response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error creating HeyGen token", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 1: Create a new streaming session. Returns session_id, ice_servers2, sdp offer, etc.
     */
    @PostMapping("/new")
    public ResponseEntity<?> createSession(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("quality", "high");
            // Use a known public avatar
            payload.put("avatar_name", body != null && body.containsKey("avatar_name")
                    ? body.get("avatar_name") : "Anna_public_3_20240108");
            payload.put("voice", Map.of("voice_id", "en-US-AriaNeural"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.new",
                    request, Map.class
            );
            log.info("HeyGen streaming.new response status: {}", response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error creating streaming session", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 2: Start the session by sending the SDP answer from the client.
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.start",
                    request, Map.class
            );
            log.info("HeyGen streaming.start response status: {}", response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error starting streaming session", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 3: Send ICE candidate to HeyGen.
     */
    @PostMapping("/ice")
    public ResponseEntity<?> sendIce(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.ice",
                    request, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error sending ICE candidate", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Make the avatar speak.
     */
    @PostMapping("/task")
    public ResponseEntity<?> sendTask(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.task",
                    request, Map.class
            );
            log.info("HeyGen streaming.task response status: {}", response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error sending task", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send interrupt to stop speaking.
     */
    @PostMapping("/interrupt")
    public ResponseEntity<?> interrupt(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.interrupt",
                    request, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error interrupting", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Stop the streaming session.
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopSession(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    HEYGEN_BASE + "/v1/streaming.stop",
                    request, Map.class
            );
            log.info("HeyGen streaming.stop response status: {}", response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error stopping streaming session", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

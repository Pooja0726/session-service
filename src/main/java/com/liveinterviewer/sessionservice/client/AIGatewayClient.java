package com.liveinterviewer.sessionservice.client;

import com.liveinterviewer.sessionservice.entity.InterviewSession;
import com.liveinterviewer.sessionservice.entity.InterviewStage;
import com.liveinterviewer.sessionservice.entity.InterviewTurn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// NOTE: this calls AI-Gateway service via a direct URL, NOT via Eureka's
// lb:// scheme. We deliberately excluded spring-cloud-starter-loadbalancer
// from this service earlier (it crashed on this Boot/Cloud version combo),
// so service-to-service calls use fixed local URLs for now. Fine for local
// dev with one instance of each service; if you deploy multiple instances
// of ai-gateway-service later, this needs a working load balancer instead.
@Component
public class AIGatewayClient {

    @Value("${ai-gateway.url:http://localhost:8085}")
    private String aiGatewayUrl;

    private final RestClient restClient = RestClient.create();

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateNextTurn(InterviewSession session, List<InterviewTurn> turns,
                                                 String latestCandidateMessage, String latestCode) {
        Map<String, Object> payload = buildContextPayload(session, turns, latestCandidateMessage, latestCode);

        return restClient.post()
                .uri(aiGatewayUrl + "/api/ai/generate-turn")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateEvaluation(InterviewSession session, List<InterviewTurn> turns) {
        Map<String, Object> payload = buildContextPayload(session, turns, null, null);

        return restClient.post()
                .uri(aiGatewayUrl + "/api/ai/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    private Map<String, Object> buildContextPayload(InterviewSession session, List<InterviewTurn> turns,
                                                      String latestCandidateMessage, String latestCode) {
        List<Map<String, Object>> turnDtos = new ArrayList<>();
        for (InterviewTurn turn : turns) {
            turnDtos.add(Map.of(
                    "stage", turn.getStage().name(),
                    "question", nullToEmpty(turn.getQuestion()),
                    "candidateAnswer", nullToEmpty(turn.getCandidateAnswer()),
                    "codeSubmitted", nullToEmpty(turn.getCodeSubmitted())
            ));
        }

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("currentStage", session.getCurrentStage().name());
        payload.put("interviewerStyle", session.getInterviewerStyle() != null ? session.getInterviewerStyle().name() : null);
        payload.put("role", session.getRole());
        payload.put("company", session.getCompany());
        payload.put("resumeText", session.getResumeText());
        payload.put("jobDescriptionText", session.getJobDescriptionText());
        payload.put("turns", turnDtos);
        payload.put("latestCandidateMessage", latestCandidateMessage);
        payload.put("latestCodeSubmitted", latestCode);
        return payload;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

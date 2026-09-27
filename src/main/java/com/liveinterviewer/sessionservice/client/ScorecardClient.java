package com.liveinterviewer.sessionservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// Same direct-URL approach as AIGatewayClient — see the note there.
@Component
public class ScorecardClient {

    @Value("${scorecard-service.url:http://localhost:8084}")
    private String scorecardServiceUrl;

    private final RestClient restClient = RestClient.create();

    public void createScorecard(Long sessionId, String candidateEmail, Map<String, Object> evaluation) {
        Integer codingScore = toScore100(evaluation.get("coding"));
        Integer communicationScore = toScore100(evaluation.get("communication"));

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("candidateEmail", candidateEmail);
        payload.put("codingScore", codingScore);
        payload.put("communicationScore", communicationScore);
        payload.put("overallScore", evaluation.get("overallScore"));
        payload.put("technicalKnowledge", evaluation.get("technicalKnowledge"));
        payload.put("problemSolving", evaluation.get("problemSolving"));
        payload.put("resumeKnowledge", evaluation.get("resumeKnowledge"));
        payload.put("jdAlignment", evaluation.get("jdAlignment"));
        payload.put("strengths", evaluation.get("strengths"));
        payload.put("weaknesses", evaluation.get("weaknesses"));
        payload.put("topicsToImprove", evaluation.get("topicsToImprove"));
        payload.put("overallFeedback", buildFeedbackText(evaluation));

        restClient.post()
                .uri(scorecardServiceUrl + "/api/scorecards")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    // Evaluation scores come back on a 0-10 scale from the AI; Scorecard
    // service stores 0-100, matching the sample report in the requirements doc.
    private Integer toScore100(Object rawScore) {
        if (rawScore == null) return 0;
        double value = ((Number) rawScore).doubleValue();
        return (int) Math.round(value * 10);
    }

    private String buildFeedbackText(Map<String, Object> evaluation) {
        Object recommendation = evaluation.get("finalRecommendation");
        return recommendation != null ? recommendation.toString() : "";
    }
}

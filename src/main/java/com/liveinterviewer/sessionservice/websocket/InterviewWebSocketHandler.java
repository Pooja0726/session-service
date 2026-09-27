package com.liveinterviewer.sessionservice.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveinterviewer.sessionservice.client.AIGatewayClient;
import com.liveinterviewer.sessionservice.client.ScorecardClient;
import com.liveinterviewer.sessionservice.entity.InterviewSession;
import com.liveinterviewer.sessionservice.entity.InterviewStage;
import com.liveinterviewer.sessionservice.entity.InterviewTurn;
import com.liveinterviewer.sessionservice.entity.SessionStatus;
import com.liveinterviewer.sessionservice.repository.InterviewSessionRepository;
import com.liveinterviewer.sessionservice.repository.InterviewTurnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// This is where the interview actually happens turn by turn. Flow per message:
//   1. Candidate's answer arrives -> fill in the open turn's answer/code
//   2. Send the full context (session + all turns) to AI-Gateway
//   3. AI-Gateway returns the next message + whether the stage should advance
//   4. If EVALUATION was just reached: get the structured report, post it to
//      Scorecard service, mark the session COMPLETED, send a summary
//   5. Otherwise: save a new turn with the AI's next question, send it down
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewTurnRepository turnRepository;
    private final AIGatewayClient aiGatewayClient;
    private final ScorecardClient scorecardClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long sessionId = (Long) session.getAttributes().get("sessionId");
        String email = (String) session.getAttributes().get("email");
        log.info("WebSocket connected: {} (interview session {})", email, sessionId);

        InterviewSession interviewSession = sessionRepository.findById(sessionId).orElse(null);
        if (interviewSession == null || !interviewSession.getCandidateEmail().equals(email)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        List<InterviewTurn> turns = turnRepository.findBySessionIdOrderByTurnOrderAsc(sessionId);

        if (turns.isEmpty()) {
            // First-ever connection for this session: kick off the introduction.
            if (interviewSession.getStatus() == SessionStatus.CREATED) {
                interviewSession.setStatus(SessionStatus.IN_PROGRESS);
                interviewSession.setStartedAt(Instant.now());
                sessionRepository.save(interviewSession);
            }

            try {
                Map<String, Object> result = aiGatewayClient.generateNextTurn(interviewSession, turns, null, null);
                sendAiMessageAndPersist(session, interviewSession, turns, result);
            } catch (IOException e) {
                // Client disconnected while we were waiting for the AI response — data is persisted, reconnect will pick it up
                log.warn("Client disconnected during initial AI call for session {} — will recover on reconnect", sessionId);
            } catch (Exception e) {
                log.error("Error generating first turn for session {}", sessionId, e);
                sendToClient(session, Map.of("type", "error", "message", "Failed to start interview — please try again."));
            }
        } else {
            // Reconnect: resend whatever question is still awaiting an answer.
            InterviewTurn last = turns.get(turns.size() - 1);
            if (last.getCandidateAnswer() == null) {
                sendToClient(session, Map.of("type", "question", "message", last.getQuestion(), "stage", last.getStage().name()));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long sessionId = (Long) session.getAttributes().get("sessionId");

        InterviewSession interviewSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        Map<String, Object> incoming = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) incoming.get("type");
        String candidateMessage = (String) incoming.get("message");
        String candidateCode = (String) incoming.get("code");

        List<InterviewTurn> turns = turnRepository.findBySessionIdOrderByTurnOrderAsc(sessionId);

        if ("end_interview".equals(type)) {
            finishInterview(session, interviewSession, turns);
            return;
        }

        if (!turns.isEmpty()) {
            InterviewTurn openTurn = turns.get(turns.size() - 1);
            if (openTurn.getCandidateAnswer() == null) {
                openTurn.setCandidateAnswer(candidateMessage);
                openTurn.setCodeSubmitted(candidateCode);
                turnRepository.save(openTurn);
            }
        }

        try {
            Map<String, Object> result = aiGatewayClient.generateNextTurn(interviewSession, turns, candidateMessage, candidateCode);
            sendAiMessageAndPersist(session, interviewSession, turns, result);
        } catch (IOException e) {
            log.warn("Client disconnected during AI response for session {} — data saved, will recover on reconnect", sessionId);
        } catch (Exception e) {
            log.error("Error generating turn for session {}", sessionId, e);
            sendToClient(session, Map.of("type", "error", "message", "Something went wrong — please try again."));
        }
    }

    private void sendAiMessageAndPersist(WebSocketSession wsSession, InterviewSession interviewSession,
                                          List<InterviewTurn> priorTurns, Map<String, Object> result) throws Exception {
        String aiMessage = (String) result.get("message");
        String nextStageName = (String) result.get("nextStage");
        InterviewStage nextStage = InterviewStage.valueOf(nextStageName);

        boolean stageChanged = nextStage != interviewSession.getCurrentStage();
        interviewSession.setCurrentStage(nextStage);

        if (nextStage == InterviewStage.EVALUATION && stageChanged) {
            finishInterview(wsSession, interviewSession, priorTurns);
            return;
        }

        sessionRepository.save(interviewSession);

        int turnOrder = priorTurns.size() + 1;
        InterviewTurn newTurn = InterviewTurn.builder()
                .sessionId(interviewSession.getId())
                .stage(nextStage)
                .question(aiMessage)
                .turnOrder(turnOrder)
                .build();
        turnRepository.save(newTurn);

        // Data is persisted — even if this send fails, the client can reconnect and get the question
        sendToClient(wsSession, Map.of("type", "question", "message", aiMessage, "stage", nextStage.name()));
    }

    private void finishInterview(WebSocketSession wsSession, InterviewSession interviewSession,
                                  List<InterviewTurn> turns) throws Exception {
        Map<String, Object> evaluation = aiGatewayClient.generateEvaluation(interviewSession, turns);

        try {
            scorecardClient.createScorecard(interviewSession.getId(), interviewSession.getCandidateEmail(), evaluation);
        } catch (Exception ex) {
            log.error("Failed to persist scorecard for session {}", interviewSession.getId(), ex);
        }

        interviewSession.setStatus(SessionStatus.COMPLETED);
        interviewSession.setCurrentStage(InterviewStage.COMPLETED);
        interviewSession.setEndedAt(Instant.now());
        sessionRepository.save(interviewSession);

        sendToClient(wsSession, Map.of("type", "evaluation", "evaluation", evaluation));
        if (wsSession.isOpen()) {
            wsSession.close(CloseStatus.NORMAL);
        }
    }

    /**
     * Safe send — checks session.isOpen() first to avoid IOException when the client
     * has already disconnected (e.g. during a long AI gateway call).
     */
    private void sendToClient(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (!session.isOpen()) {
            log.debug("Skipping send — WebSocket session already closed");
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String email = (String) session.getAttributes().get("email");
        Long sessionId = (Long) session.getAttributes().get("sessionId");
        log.info("WebSocket closed: {} (interview session {}, status {})", email, sessionId, status);
    }
}

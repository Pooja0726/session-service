package com.liveinterviewer.sessionservice.controller;

import com.liveinterviewer.sessionservice.dto.*;
import com.liveinterviewer.sessionservice.entity.InterviewSession;
import com.liveinterviewer.sessionservice.entity.InterviewTurn;
import com.liveinterviewer.sessionservice.entity.SessionStatus;
import com.liveinterviewer.sessionservice.repository.InterviewSessionRepository;
import com.liveinterviewer.sessionservice.repository.InterviewTurnRepository;
import com.liveinterviewer.sessionservice.service.FileTextExtractor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewTurnRepository turnRepository;
    private final FileTextExtractor fileTextExtractor;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(Authentication authentication,
                                                          @Valid @RequestBody CreateSessionRequest request) {
        String email = authentication.getName();

        InterviewSession session = InterviewSession.builder()
                .candidateEmail(email)
                .interviewerStyle(request.getInterviewerStyle())
                .status(SessionStatus.CREATED)
                .build();

        InterviewSession saved = sessionRepository.save(session);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/me")
    public ResponseEntity<List<SessionResponse>> getMySessions(Authentication authentication) {
        String email = authentication.getName();

        List<SessionResponse> sessions = sessionRepository
                .findByCandidateEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(Authentication authentication, @PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        return ResponseEntity.ok(toResponse(session));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<SessionResponse> startSession(Authentication authentication, @PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());

        return ResponseEntity.ok(toResponse(sessionRepository.save(session)));
    }

    @PatchMapping("/{id}/end")
    public ResponseEntity<SessionResponse> endSession(Authentication authentication, @PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(Instant.now());

        return ResponseEntity.ok(toResponse(sessionRepository.save(session)));
    }

    // Called before the interview starts: candidate picks role + (optionally) company.
    @PutMapping("/{id}/setup")
    public ResponseEntity<SessionResponse> setupSession(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody SetupSessionRequest request) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        session.setRole(request.getRole());
        session.setCompany(request.getCompany());

        return ResponseEntity.ok(toResponse(sessionRepository.save(session)));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SessionResponse> uploadResume(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @RequestParam("file") MultipartFile file) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        String text = fileTextExtractor.extractText(file);
        session.setResumeText(text);

        return ResponseEntity.ok(toResponse(sessionRepository.save(session)));
    }

    @PostMapping("/{id}/job-description")
    public ResponseEntity<SessionResponse> uploadJobDescription(Authentication authentication,
                                                                 @PathVariable Long id,
                                                                 @RequestParam("file") MultipartFile file) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        String text = fileTextExtractor.extractText(file);
        session.setJobDescriptionText(text);

        return ResponseEntity.ok(toResponse(sessionRepository.save(session)));
    }

    // The full picture, assembled for the AI-Gateway. This is called by the
    // WebSocket handler's server-side code, not directly by the frontend.
    @GetMapping("/{id}/context")
    public ResponseEntity<InterviewContextResponse> getContext(Authentication authentication, @PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        List<InterviewTurnResponse> turns = turnRepository.findBySessionIdOrderByTurnOrderAsc(id)
                .stream()
                .map(this::toTurnResponse)
                .toList();

        InterviewContextResponse context = InterviewContextResponse.builder()
                .sessionId(session.getId())
                .interviewerStyle(session.getInterviewerStyle())
                .currentStage(session.getCurrentStage())
                .role(session.getRole())
                .company(session.getCompany())
                .resumeText(session.getResumeText())
                .jobDescriptionText(session.getJobDescriptionText())
                .turns(turns)
                .build();

        return ResponseEntity.ok(context);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(Authentication authentication, @PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        // Delete turns first (FK constraint), then the session itself
        turnRepository.deleteBySessionId(id);
        sessionRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/turns")
    public ResponseEntity<List<InterviewTurnResponse>> getTurns(Authentication authentication, @PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        ensureOwnership(session, authentication.getName());

        List<InterviewTurnResponse> turns = turnRepository.findBySessionIdOrderByTurnOrderAsc(id)
                .stream()
                .map(this::toTurnResponse)
                .toList();

        return ResponseEntity.ok(turns);
    }

    private InterviewTurnResponse toTurnResponse(InterviewTurn turn) {
        return InterviewTurnResponse.builder()
                .id(turn.getId())
                .stage(turn.getStage())
                .question(turn.getQuestion())
                .candidateAnswer(turn.getCandidateAnswer())
                .codeSubmitted(turn.getCodeSubmitted())
                .turnOrder(turn.getTurnOrder())
                .createdAt(turn.getCreatedAt())
                .build();
    }

    // Prevents one candidate from reading or modifying another candidate's session
    // just by guessing/incrementing the numeric ID in the URL.
    private void ensureOwnership(InterviewSession session, String requesterEmail) {
        if (!session.getCandidateEmail().equals(requesterEmail)) {
            throw new IllegalStateException("Session not found");
        }
    }

    private SessionResponse toResponse(InterviewSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .candidateEmail(session.getCandidateEmail())
                .interviewerStyle(session.getInterviewerStyle())
                .status(session.getStatus())
                .currentStage(session.getCurrentStage())
                .role(session.getRole())
                .company(session.getCompany())
                .resumeUploaded(session.getResumeText() != null && !session.getResumeText().isBlank())
                .jobDescriptionUploaded(session.getJobDescriptionText() != null && !session.getJobDescriptionText().isBlank())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .build();
    }
}

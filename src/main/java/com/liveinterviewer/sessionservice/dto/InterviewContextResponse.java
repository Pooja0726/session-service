package com.liveinterviewer.sessionservice.dto;

import com.liveinterviewer.sessionservice.entity.InterviewStage;
import com.liveinterviewer.sessionservice.entity.InterviewerStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// This is the payload the Session service's WebSocket handler assembles
// and sends to AI-Gateway on every turn. AI-Gateway is stateless, so it
// needs this ENTIRE picture every single call — nothing is remembered
// on the AI-Gateway side between requests.
@Getter
@Builder
@AllArgsConstructor
public class InterviewContextResponse {

    private Long sessionId;
    private InterviewerStyle interviewerStyle;
    private InterviewStage currentStage;
    private String role;
    private String company;
    private String resumeText;
    private String jobDescriptionText;
    private List<InterviewTurnResponse> turns; // full history, oldest first
}

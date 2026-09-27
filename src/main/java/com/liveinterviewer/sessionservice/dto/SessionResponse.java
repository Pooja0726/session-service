package com.liveinterviewer.sessionservice.dto;

import com.liveinterviewer.sessionservice.entity.InterviewStage;
import com.liveinterviewer.sessionservice.entity.InterviewerStyle;
import com.liveinterviewer.sessionservice.entity.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class SessionResponse {

    private Long id;
    private String candidateEmail;
    private InterviewerStyle interviewerStyle;
    private SessionStatus status;
    private InterviewStage currentStage;
    private String role;
    private String company;
    private boolean resumeUploaded;
    private boolean jobDescriptionUploaded;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
}

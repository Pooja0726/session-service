package com.liveinterviewer.sessionservice.dto;

import com.liveinterviewer.sessionservice.entity.InterviewStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class InterviewTurnResponse {

    private Long id;
    private InterviewStage stage;
    private String question;
    private String candidateAnswer;
    private String codeSubmitted;
    private Integer turnOrder;
    private Instant createdAt;
}

package com.liveinterviewer.sessionservice.dto;

import com.liveinterviewer.sessionservice.entity.InterviewerStyle;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionRequest {

    @NotNull(message = "Interviewer style is required")
    private InterviewerStyle interviewerStyle;
}

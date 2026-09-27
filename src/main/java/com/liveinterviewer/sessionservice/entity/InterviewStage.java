package com.liveinterviewer.sessionservice.entity;

// The interview MUST progress through these in order — never jump around.
// This mirrors the required flow: Intro -> Coding -> Resume -> JD -> Company -> Evaluation.
public enum InterviewStage {
    INTRODUCTION,
    CODING,
    TECHNICAL,
    RESUME,
    JOB_DESCRIPTION,
    COMPANY_SPECIFIC,
    EVALUATION,
    COMPLETED
}

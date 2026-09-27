package com.liveinterviewer.sessionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links back to the candidate's identity in the Auth service — same
    // pattern as User service, no cross-database foreign key.
    @Column(nullable = false)
    private String candidateEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewerStyle interviewerStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStage currentStage;

    private String role;        // e.g. "Backend Developer"

    private String company;     // e.g. "Amazon" — optional

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resumeText;  // extracted plain text, populated via file upload endpoint

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String jobDescriptionText; // extracted plain text

    private Instant startedAt;

    private Instant endedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = SessionStatus.CREATED;
        }
        if (this.currentStage == null) {
            this.currentStage = InterviewStage.INTRODUCTION;
        }
    }
}

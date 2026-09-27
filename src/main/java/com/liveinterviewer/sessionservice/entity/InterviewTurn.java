package com.liveinterviewer.sessionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// One row per back-and-forth exchange. This IS the interview's memory —
// everything the AI needs to "remember" gets reconstructed by reading all
// turns for a session, in order, and replaying them as conversation
// history in the next Groq call.
@Entity
@Table(name = "interview_turns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStage stage;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question; // the interviewer's message (question, follow-up, or remark)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String candidateAnswer; // null until the candidate responds

    @Lob
    @Column(columnDefinition = "TEXT")
    private String codeSubmitted; // populated only during the coding stage, if applicable

    @Column(nullable = false)
    private Integer turnOrder; // explicit ordering, since createdAt alone can collide under load

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}

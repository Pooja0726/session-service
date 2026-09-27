package com.liveinterviewer.sessionservice.repository;

import com.liveinterviewer.sessionservice.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByCandidateEmailOrderByCreatedAtDesc(String candidateEmail);
}

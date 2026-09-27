package com.liveinterviewer.sessionservice.repository;

import com.liveinterviewer.sessionservice.entity.InterviewTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, Long> {

    List<InterviewTurn> findBySessionIdOrderByTurnOrderAsc(Long sessionId);

    int countBySessionId(Long sessionId);

    @Transactional
    void deleteBySessionId(Long sessionId);
}

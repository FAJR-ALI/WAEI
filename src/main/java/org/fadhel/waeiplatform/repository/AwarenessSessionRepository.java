package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.model.AwarenessSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwarenessSessionRepository extends JpaRepository<AwarenessSession, Long> {

    AwarenessSession findAwarenessSessionById(Long id);

    List<AwarenessSession> findAwarenessSessionsByAwarenessService(AwarenessService awarenessService);

    @Query("select avg(s.score) from AwarenessSession s where s.awarenessService.id = ?1")
    Double getAverageScore(Long awarenessServiceId);
}

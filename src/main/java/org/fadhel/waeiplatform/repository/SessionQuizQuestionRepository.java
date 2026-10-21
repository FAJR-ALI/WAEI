package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.AwarenessSession;
import org.fadhel.waeiplatform.model.SessionQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionQuizQuestionRepository extends JpaRepository<SessionQuizQuestion, Long> {

    List<SessionQuizQuestion> findSessionQuizQuestionsByAwarenessSessionOrderByIdAsc(AwarenessSession awarenessSession);
}

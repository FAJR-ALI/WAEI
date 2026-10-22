package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.AwarenessCalendarEvent;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwarenessCalendarEventRepository extends JpaRepository<AwarenessCalendarEvent, Long> {

    AwarenessCalendarEvent findAwarenessCalendarEventById(Long id);

    AwarenessCalendarEvent findAwarenessCalendarEventByTitle(String title);

    AwarenessCalendarEvent findAwarenessCalendarEventBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByTitle(String title);

    List<AwarenessCalendarEvent> findAwarenessCalendarEventsByIsPublished(Boolean isPublished);

    List<AwarenessCalendarEvent> findAwarenessCalendarEventsByCategory(String category);

    List<AwarenessCalendarEvent> findAwarenessCalendarEventsByAwarenessService(AwarenessService awarenessService);
}

package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.model.EmergencyGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyGuideRepository extends JpaRepository<EmergencyGuide, Long> {

    EmergencyGuide findEmergencyGuideById(Long id);

    EmergencyGuide findEmergencyGuideByTitle(String title);

    List<EmergencyGuide> findEmergencyGuidesByIsPublishedOrderByTitleAsc(Boolean isPublished);

    List<EmergencyGuide> findEmergencyGuidesByIsPublishedAndCategoryOrderByTitleAsc(Boolean isPublished, String category);

    List<EmergencyGuide> findEmergencyGuidesByAwarenessService(AwarenessService awarenessService);
}

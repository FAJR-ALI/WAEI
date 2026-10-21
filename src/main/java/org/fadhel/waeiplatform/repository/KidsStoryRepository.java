package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.KidsStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KidsStoryRepository extends JpaRepository<KidsStory, Long> {

    KidsStory findKidsStoryById(Long id);

    List<KidsStory> findKidsStoriesByStatus(String status);

    List<KidsStory> findKidsStoriesByStatusAndAgeGroup(String status, String ageGroup);

    List<KidsStory> findKidsStoriesByStatusAndTopic(String status, String topic);
}

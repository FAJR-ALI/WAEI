package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.KidsStory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.junit.jupiter.api.Assertions.*;

// A temporary H2 database is used; each test is rolled back automatically.
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:kids-story-repository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KidsStoryRepositoryTest {
    @Autowired
    private KidsStoryRepository repository;

    @Test
    void findStoryByIdReturnsSavedStory() {
        KidsStory story = saveStory("Safe road", "APPROVED", "6-8", "Road safety");

        KidsStory result = repository.findKidsStoryById(story.getId());

        assertNotNull(result);
        assertEquals("Safe road", result.getTitle());
    }

    @Test
    void findMissingStoryReturnsNull() {
        assertNull(repository.findKidsStoryById(-1L));
    }

    @Test
    void findByStatusExcludesPendingStories() {
        KidsStory approved = saveStory("Safe road", "APPROVED", "6-8", "Road safety");
        saveStory("Waiting for review", "PENDING", "6-8", "Road safety");

        var result = repository.findKidsStoriesByStatus("APPROVED");

        assertEquals(1, result.size());
        assertEquals(approved.getId(), result.get(0).getId());
    }

    @Test
    void findByAgeExcludesOtherAgesAndPendingStories() {
        KidsStory matching = saveStory("Safe road", "APPROVED", "6-8", "Road safety");
        saveStory("Younger children", "APPROVED", "3-5", "Road safety");
        saveStory("Waiting for review", "PENDING", "6-8", "Road safety");

        var result = repository.findKidsStoriesByStatusAndAgeGroup("APPROVED", "6-8");

        assertEquals(1, result.size());
        assertEquals(matching.getId(), result.get(0).getId());
    }

    @Test
    void findByTopicExcludesOtherTopicsAndPendingStories() {
        KidsStory matching = saveStory("Safe road", "APPROVED", "6-8", "Road safety");
        saveStory("Safe home", "APPROVED", "6-8", "Home safety");
        saveStory("Waiting for review", "PENDING", "6-8", "Road safety");

        var result = repository.findKidsStoriesByStatusAndTopic("APPROVED", "Road safety");

        assertEquals(1, result.size());
        assertEquals(matching.getId(), result.get(0).getId());
    }

    private KidsStory saveStory(String title, String status, String ageGroup, String topic) {
        KidsStory story = new KidsStory();
        story.setTitle(title);
        story.setStatus(status);
        story.setAgeGroup(ageGroup);
        story.setTopic(topic);
        story.setContent("Ask an adult to help.");
        return repository.saveAndFlush(story);
    }
}

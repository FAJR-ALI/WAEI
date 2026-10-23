package org.fadhel.waeiplatform.config;

import org.fadhel.waeiplatform.model.KidsStory;
import org.fadhel.waeiplatform.repository.KidsStoryRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KidsStoryDataSeederTest {

    @Test
    void seedsThreeShortPublishedStoriesPerAgeWithoutDuplicatingOrChangingExistingStories() {
        KidsStoryRepository repository = mock(KidsStoryRepository.class);
        List<KidsStory> stored = new ArrayList<>();
        KidsStory existing = new KidsStory();
        existing.setAgeGroup("6-8");
        existing.setTitle("Existing team story");
        existing.setContent("Original content");
        existing.setStatus("PENDING");
        stored.add(existing);
        when(repository.findAll()).thenAnswer(invocation -> new ArrayList<>(stored));
        when(repository.save(any(KidsStory.class))).thenAnswer(invocation -> {
            KidsStory story = invocation.getArgument(0);
            stored.add(story);
            return story;
        });

        KidsStoryDataSeeder seeder = new KidsStoryDataSeeder(repository);
        seeder.run();
        assertEquals(10, stored.size());
        for (String age : List.of("3-5", "6-8", "9-12")) {
            assertEquals(3, stored.stream().filter(s -> s != existing && age.equals(s.getAgeGroup())).count());
        }
        for (KidsStory story : stored) {
            if (story == existing) continue;
            assertEquals(2, story.getContent().lines().count());
            assertTrue(story.getContent().length() <= 150);
            assertEquals("APPROVED", story.getStatus());
            assertNotNull(story.getPublishedAt());
        }

        stored.get(1).setStatus("REJECTED");
        seeder.run();
        assertEquals(10, stored.size());
        assertEquals("REJECTED", stored.get(1).getStatus());
        assertEquals("Original content", existing.getContent());
        assertEquals("PENDING", existing.getStatus());
        verify(repository, times(9)).save(any(KidsStory.class));
    }
}

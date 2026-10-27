package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.model.KidsStory;
import org.fadhel.waeiplatform.repository.KidsStoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KidsStoryServiceTest {
    @Mock
    private KidsStoryRepository repository;
    @Mock
    private GeminiService geminiService;
    @InjectMocks
    private KidsStoryService service;

    private KidsStory story;

    @BeforeEach
    void setup() {
        story = new KidsStory();
        story.setId(1L);
        story.setTitle("Safe road");
        story.setTopic("Road safety");
        story.setAgeGroup("6-8");
        story.setContent("Ask an adult.");
        story.setStatus("APPROVED");
    }

    @Test
    void getPublishedStoriesRequestsApprovedStoriesOnly() {
        when(repository.findKidsStoriesByStatus("APPROVED")).thenReturn(List.of(story));

        var result = service.getPublishedStories();

        assertEquals(1, result.size());
        assertEquals("Safe road", result.get(0).getTitle());
        verify(repository).findKidsStoriesByStatus("APPROVED");
    }

    @Test
    void getStoriesByAgeUsesAgeAndApprovedStatus() {
        when(repository.findKidsStoriesByStatusAndAgeGroup("APPROVED", "6-8")).thenReturn(List.of(story));

        var result = service.getPublishedStoriesByAge("6-8");

        assertEquals(1, result.size());
        assertEquals("6-8", result.get(0).getAgeGroup());
        verify(repository).findKidsStoriesByStatusAndAgeGroup("APPROVED", "6-8");
    }

    @Test
    void invalidAgeIsRejectedBeforeAccessingRepository() {
        assertThrows(ApiException.class, () -> service.getPublishedStoriesByAge("1-2"));

        verifyNoInteractions(repository);
    }

    @Test
    void approvedStoryCanBeRead() {
        when(repository.findKidsStoryById(1L)).thenReturn(story);

        var result = service.getPublishedStoryById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Ask an adult.", result.getContent());
    }

    @Test
    void pendingStoryCannotBeRead() {
        story.setStatus("PENDING");
        when(repository.findKidsStoryById(1L)).thenReturn(story);

        assertThrows(ApiException.class, () -> service.getPublishedStoryById(1L));
    }
}

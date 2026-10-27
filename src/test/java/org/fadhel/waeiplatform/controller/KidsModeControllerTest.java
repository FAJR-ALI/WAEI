package org.fadhel.waeiplatform.controller;

import org.fadhel.waeiplatform.dto.out.KidsStoryPublicResponse;
import org.fadhel.waeiplatform.service.KidsStoryNarrationService;
import org.fadhel.waeiplatform.service.KidsStoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KidsModeControllerTest {
    @Mock
    private KidsStoryService storyService;
    @Mock
    private KidsStoryNarrationService narrationService;
    @InjectMocks
    private KidsModeController controller;

    private MockMvc mockMvc;
    private KidsStoryPublicResponse story;

    @BeforeEach
    void setup() {
        // Test controller routes without starting the application or calling AI.
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        story = new KidsStoryPublicResponse(1L, "Safe road", "Road safety", "6-8", "Ask an adult.", "Stay safe.");
    }

    @Test
    void getStoriesReturnsStoryList() throws Exception {
        when(storyService.getPublishedStories()).thenReturn(List.of(story));

        mockMvc.perform(get("/api/v1/kids/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Safe road"));
    }

    @Test
    void getStoryByIdReturnsRequestedStory() throws Exception {
        when(storyService.getPublishedStoryById(1L)).thenReturn(story);

        mockMvc.perform(get("/api/v1/kids/stories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getStoriesByAgeReturnsMatchingStories() throws Exception {
        when(storyService.getPublishedStoriesByAge("6-8")).thenReturn(List.of(story));

        mockMvc.perform(get("/api/v1/kids/stories/age/6-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageGroup").value("6-8"));
    }

    @Test
    void getStoriesByTopicReturnsMatchingStories() throws Exception {
        when(storyService.getPublishedStoriesByTopic("Road safety")).thenReturn(List.of(story));

        mockMvc.perform(get("/api/v1/kids/stories/topic/Road safety"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("Road safety"));
    }

    @Test
    void narrateStoryReturnsAudioWithoutCaching() throws Exception {
        byte[] audio = {1, 2, 3};
        when(narrationService.narrate(1L)).thenReturn(audio);

        mockMvc.perform(get("/api/v1/kids/stories/1/narration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(audio));
    }
}

package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.KidsStoryPublicResponse;
import org.fadhel.waeiplatform.service.GeminiSpeechService;
import org.fadhel.waeiplatform.service.KidsStoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KidsStoryNarrationIntegrationTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private KidsStoryService stories;
    @MockitoBean private GeminiSpeechService speech;

    @Test
    void anonymousVisitorCanListenToPublishedStoryEvenWithStaleToken() throws Exception {
        when(stories.getPublishedStoryById(71L)).thenReturn(KidsStoryPublicResponse.builder()
                .id(71L).title("Title").content("Story").ageGroup("3-5").build());
        byte[] wav = new byte[]{82, 73, 70, 70};
        when(speech.narrate(anyString(), anyString())).thenReturn(wav);
        mvc.perform(get("/api/v1/kids/stories/71/narration").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/wav"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(wav));
    }

    @Test
    void unpublishedStoryNeverReachesSpeechProviderAndProtectedApiRemainsProtected() throws Exception {
        when(stories.getPublishedStoryById(72L)).thenThrow(new ApiException("Story not found"));
        mvc.perform(get("/api/v1/kids/stories/72/narration"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(speech);
        mvc.perform(get("/api/v1/account/get"))
                .andExpect(status().isUnauthorized());
    }
}

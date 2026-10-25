package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.dto.ai.AiStoryResult;
import org.fadhel.waeiplatform.service.KidsStoryBuilderService;
import org.fadhel.waeiplatform.service.KidsVoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class KidsInteractionIntegrationTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private KidsStoryBuilderService builder;
    @MockitoBean private KidsVoiceService voice;

    @Test
    void anonymousChildCanOpenBuilderGenerateAndListen() throws Exception {
        mvc.perform(get("/kids/stories/create").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"builderForm\"")));
        when(builder.generate("6-8", "saad", "home", "help", "ar"))
                .thenReturn(new AiStoryResult("سعد", "طلب سعد المساعدة.", "اطلب مساعدة شخص كبير."));
        mvc.perform(post("/api/v1/kids/stories/create").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer expired-token")
                        .content("{\"ageGroup\":\"6-8\",\"character\":\"saad\",\"place\":\"home\",\"topic\":\"help\",\"lang\":\"ar\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("title").value("سعد"));
        when(voice.speak("مرحبا", "ar", "6-8")).thenReturn(new byte[]{1, 2});
        mvc.perform(post("/api/v1/kids/voice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"مرحبا\",\"lang\":\"ar\",\"ageGroup\":\"6-8\"}"))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/wav"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void invalidInputIsBlockedAndOtherWriteEndpointsRemainProtected() throws Exception {
        mvc.perform(post("/api/v1/kids/stories/create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ageGroup\":\"6-8\",\"character\":\"custom input\",\"place\":\"home\",\"topic\":\"help\",\"lang\":\"ar\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/kids/voice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\",\"lang\":\"ar\",\"ageGroup\":\"6-8\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(builder, voice);
        mvc.perform(post("/api/v1/kids/other")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/kids-story/generate")).andExpect(status().isUnauthorized());
    }
}

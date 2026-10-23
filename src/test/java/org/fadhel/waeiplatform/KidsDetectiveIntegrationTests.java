package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.service.KidsDetectiveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class KidsDetectiveIntegrationTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private KidsDetectiveService detective;

    @Test
    void pageAndFeedbackArePublicButProtectedApisRemainProtected() throws Exception {
        mvc.perform(get("/kids/detective").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-item=\"wire\"")))
                .andExpect(content().string(containsString("data-item=\"book\"")));
        when(detective.check("cup", "en")).thenReturn(new KidsDetectiveService.Feedback(true, "Tell an adult."));
        mvc.perform(get("/api/v1/kids/detective/check/cup?lang=en"))
                .andExpect(status().isOk()).andExpect(jsonPath("danger").value(true))
                .andExpect(jsonPath("message").value("Tell an adult."));
        mvc.perform(get("/api/v1/account/get")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/kids/detective/check/cup")).andExpect(status().isUnauthorized());
    }

    @Test
    void serviceRejectsUnrecognizedInputBeforeCallingGemini() {
        KidsDetectiveService service = new KidsDetectiveService();
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.check("arbitrary prompt", "ar"));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.check("book", "unsupported-language"));
    }
}

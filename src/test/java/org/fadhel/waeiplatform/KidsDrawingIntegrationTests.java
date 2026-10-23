package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.service.KidsDrawingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class KidsDrawingIntegrationTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private KidsDrawingService drawingService;

    @Test
    void drawingPageAndOnlyItsReviewEndpointArePublic() throws Exception {
        mvc.perform(get("/kids/drawing").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"drawingCanvas\"")));
        when(drawingService.review(any(), eq("road"), eq("ar"), eq(true)))
                .thenReturn(new KidsDrawingService.DrawingFeedback("أحسنت!", true));
        mvc.perform(multipart("/api/v1/kids/drawing/review")
                        .file(new MockMultipartFile("drawing", "drawing.png", "image/png", new byte[]{1}))
                        .param("topic", "road").param("lang", "ar").param("consent", "true")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("message").value("أحسنت!"));
        mvc.perform(post("/api/v1/kids/drawing/other")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/account/get")).andExpect(status().isUnauthorized());
    }
}

package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.InternalAnnouncementResponse;
import org.fadhel.waeiplatform.service.InternalAnnouncementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalAnnouncementEndpointIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternalAnnouncementService internalAnnouncementService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiary-assistant/internal-announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingRequestId\":10,\"lang\":\"ar\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdminCannotUseInternalAnnouncementEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiary-assistant/internal-announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingRequestId\":10,\"lang\":\"ar\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void rejectsMissingTrainingRequestId() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiary-assistant/internal-announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lang\":\"ar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trainingRequestId").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void orgAdminCanGenerateInternalAnnouncement() throws Exception {
        when(internalAnnouncementService.generate(any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/beneficiary-assistant/internal-announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingRequestId\":10,\"lang\":\"ar\",\"channel\":\"notice\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.subject").value("تدريب الإسعافات القادمة"))
                .andExpect(jsonPath("$.whenLine").isNotEmpty())
                .andExpect(jsonPath("$.checklist.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void aiFailureReturnsAiMessage() throws Exception {
        when(internalAnnouncementService.generate(any()))
                .thenThrow(new ApiException("AI announcement is unavailable, please try again"));

        mockMvc.perform(post("/api/v1/beneficiary-assistant/internal-announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingRequestId\":10,\"lang\":\"ar\",\"channel\":\"notice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("AI announcement is unavailable, please try again"));
    }

    private InternalAnnouncementResponse response() {
        return InternalAnnouncementResponse.builder()
                .trainingRequestId(10L)
                .language("ar")
                .channel("notice")
                .generatedAt(LocalDateTime.now())
                .programTitle("First Aid")
                .providerOrganizationName("Safety Center")
                .scheduledStart(LocalDateTime.now().plusDays(2))
                .scheduledEnd(LocalDateTime.now().plusDays(4))
                .whenLine("الموعد: غدًا")
                .deliveryMode("ONLINE")
                .participantCount(20)
                .onlineJoinUrl("https://zoom.us/j/123")
                .aiGenerated(true)
                .subject("تدريب الإسعافات القادمة")
                .message("سيبدأ تدريب الإسعافات الأولية للموظفين في الموعد المحدد.")
                .checklist(List.of("احضر في الموعد", "احتفظ برابط الانضمام"))
                .reminder("تأكد من الحضور قبل الموعد.")
                .build();
    }
}
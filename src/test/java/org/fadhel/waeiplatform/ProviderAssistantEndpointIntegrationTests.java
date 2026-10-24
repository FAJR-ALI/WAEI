package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.ProviderWeeklySummaryResponse;
import org.fadhel.waeiplatform.service.ProviderWeeklySummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderAssistantEndpointIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderWeeklySummaryService providerWeeklySummaryService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/provider-assistant/weekly-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdminCannotUseProviderAssistant() throws Exception {
        mockMvc.perform(get("/api/v1/provider-assistant/weekly-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void orgAdminCanGetWeeklySummary() throws Exception {
        when(providerWeeklySummaryService.getWeeklySummary("ar", 7)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/provider-assistant/weekly-summary?lang=ar&days=7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.headline").value("أسبوع جيد"))
                .andExpect(jsonPath("$.metrics.pendingRequestCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void invalidDaysReturnBadRequestMessage() throws Exception {
        when(providerWeeklySummaryService.getWeeklySummary("ar", 0))
                .thenThrow(new ApiException("Weekly summary period must be between 1 and 30 days"));

        mockMvc.perform(get("/api/v1/provider-assistant/weekly-summary?lang=ar&days=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Weekly summary period must be between 1 and 30 days"));
    }

    private ProviderWeeklySummaryResponse summary() {
        return ProviderWeeklySummaryResponse.builder()
                .language("ar")
                .days(7)
                .generatedAt(LocalDateTime.now())
                .from(LocalDateTime.now().minusDays(7))
                .to(LocalDateTime.now().plusDays(7))
                .headline("أسبوع جيد")
                .bullets(List.of("لديك طلبات بانتظار الرد", "هناك جلسة قادمة"))
                .nextAction("راجع الطلبات الجديدة")
                .aiGenerated(true)
                .metrics(ProviderWeeklySummaryResponse.Metrics.builder()
                        .programCount(3)
                        .activeProgramCount(2)
                        .newRequestCount(1)
                        .pendingRequestCount(2)
                        .awaitingPaymentCount(0)
                        .upcomingSessionCount(1)
                        .inProgressSessionCount(0)
                        .completedSessionCount(1)
                        .cancelledSessionCount(0)
                        .onlineMeetingMissingCount(0)
                        .paidProviderRevenue(new BigDecimal("500.00"))
                        .build())
                .build();
    }
}
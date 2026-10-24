package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.AdminWeeklySummaryResponse;
import org.fadhel.waeiplatform.service.AdminWeeklySummaryService;
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
class AdminAssistantEndpointIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminWeeklySummaryService adminWeeklySummaryService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin-assistant/weekly-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void orgAdminCannotUseAdminAssistant() throws Exception {
        mockMvc.perform(get("/api/v1/admin-assistant/weekly-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdminCanGetAdminWeeklySummary() throws Exception {
        when(adminWeeklySummaryService.getWeeklySummary("ar", 7)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/admin-assistant/weekly-summary?lang=ar&days=7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.headline").value("المنصة نشطة"))
                .andExpect(jsonPath("$.metrics.pendingOrganizationReviewCount").value(3));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void invalidDaysReturnBadRequestMessage() throws Exception {
        when(adminWeeklySummaryService.getWeeklySummary("ar", 0))
                .thenThrow(new ApiException("Weekly summary period must be between 1 and 30 days"));

        mockMvc.perform(get("/api/v1/admin-assistant/weekly-summary?lang=ar&days=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Weekly summary period must be between 1 and 30 days"));
    }

    private AdminWeeklySummaryResponse summary() {
        return AdminWeeklySummaryResponse.builder()
                .language("ar")
                .days(7)
                .generatedAt(LocalDateTime.now())
                .from(LocalDateTime.now().minusDays(7))
                .to(LocalDateTime.now().plusDays(7))
                .headline("المنصة نشطة")
                .bullets(List.of("طلبات جهات بانتظار المراجعة", "تدريبات مجدولة قادمة"))
                .nextAction("راجع طلبات الجهات")
                .aiGenerated(true)
                .metrics(AdminWeeklySummaryResponse.Metrics.builder()
                        .organizationCount(12)
                        .pendingOrganizationReviewCount(3)
                        .newOrganizationApplicationCount(2)
                        .verifiedOrganizationCount(9)
                        .newVerifiedOrganizationCount(1)
                        .rejectedOrganizationCount(0)
                        .programCount(7)
                        .activeProgramCount(6)
                        .inactiveProgramCount(1)
                        .programsMissingDurationCount(2)
                        .requestCount(14)
                        .newRequestCount(4)
                        .pendingProviderResponseCount(3)
                        .awaitingPaymentCount(2)
                        .activeTrainingCount(5)
                        .upcomingTrainingCount(6)
                        .completedTrainingCount(2)
                        .cancelledTrainingCount(1)
                        .onlineTrainingMissingJoinLinkCount(1)
                        .paidPaymentCount(3)
                        .paidAmount(new BigDecimal("1500.00"))
                        .platformCommissionAmount(new BigDecimal("150.00"))
                        .pendingPaymentCount(1)
                        .pendingPaymentAmount(new BigDecimal("300.00"))
                        .refundedAmount(new BigDecimal("0.00"))
                        .build())
                .build();
    }
}
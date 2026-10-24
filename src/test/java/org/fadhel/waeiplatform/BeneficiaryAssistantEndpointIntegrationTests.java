package org.fadhel.waeiplatform;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.BeneficiaryWeeklySummaryResponse;
import org.fadhel.waeiplatform.service.BeneficiaryWeeklySummaryService;
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
class BeneficiaryAssistantEndpointIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BeneficiaryWeeklySummaryService beneficiaryWeeklySummaryService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiary-assistant/weekly-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdminCannotUseBeneficiaryAssistant() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiary-assistant/weekly-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void orgAdminCanGetBeneficiaryWeeklySummary() throws Exception {
        when(beneficiaryWeeklySummaryService.getWeeklySummary("ar", 7)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/beneficiary-assistant/weekly-summary?lang=ar&days=7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.headline").value("أسبوع جيد"))
                .andExpect(jsonPath("$.metrics.pendingProviderResponseCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ORG_ADMIN")
    void invalidDaysReturnBadRequestMessage() throws Exception {
        when(beneficiaryWeeklySummaryService.getWeeklySummary("ar", 0))
                .thenThrow(new ApiException("Weekly summary period must be between 1 and 30 days"));

        mockMvc.perform(get("/api/v1/beneficiary-assistant/weekly-summary?lang=ar&days=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Weekly summary period must be between 1 and 30 days"));
    }

    private BeneficiaryWeeklySummaryResponse summary() {
        return BeneficiaryWeeklySummaryResponse.builder()
                .language("ar")
                .days(7)
                .generatedAt(LocalDateTime.now())
                .from(LocalDateTime.now().minusDays(7))
                .to(LocalDateTime.now().plusDays(7))
                .headline("أسبوع جيد")
                .bullets(List.of("طلبات بانتظار موافقة المقدم", "تدريبات مجدولة قادمة"))
                .nextAction("تابع الطلبات الجديدة")
                .aiGenerated(true)
                .metrics(BeneficiaryWeeklySummaryResponse.Metrics.builder()
                        .requestCount(5)
                        .newRequestCount(1)
                        .pendingProviderResponseCount(2)
                        .awaitingPaymentCount(1)
                        .activeTrainingCount(2)
                        .upcomingTrainingCount(3)
                        .inProgressTrainingCount(1)
                        .completedTrainingCount(1)
                        .cancelledTrainingCount(0)
                        .onlineTrainingMissingJoinLinkCount(0)
                        .paidTrainingAmount(new BigDecimal("900.00"))
                        .build())
                .build();
    }
}
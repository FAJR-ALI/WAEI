package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiProviderWeeklySummary;
import org.fadhel.waeiplatform.dto.out.AdminWeeklySummaryResponse;
import org.fadhel.waeiplatform.dto.out.OrganizationDTOOut;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.TrainingProgramResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminWeeklySummaryServiceTest {

    private CurrentUser currentUser;
    private OrganizationService organizationService;
    private TrainingProgramService trainingProgramService;
    private TrainingRequestService trainingRequestService;
    private PaymentService paymentService;
    private GeminiService geminiService;

    @Test
    void validatesSupportedPeriodAndFallsBackToDeterministicSummaryWhenGeminiFails() {
        AdminWeeklySummaryService service = service(true);

        ApiException invalidDays = assertThrows(ApiException.class, () -> service.getWeeklySummary("ar", 0));
        assertEquals("Weekly summary period must be between 1 and 30 days", invalidDays.getMessage());

        LocalDateTime now = LocalDateTime.now();
        OrganizationDTOOut pending = new OrganizationDTOOut();
        pending.setVerificationStatus(OrganizationVerificationStatus.PENDING);
        pending.setSubmittedAt(now);
        OrganizationDTOOut verified = new OrganizationDTOOut();
        verified.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);
        verified.setSubmittedAt(now.minusDays(10));
        verified.setVerifiedAt(now);

        when(organizationService.getAllOrganizations()).thenReturn(List.of(pending, verified));
        when(trainingProgramService.getAllPrograms()).thenReturn(List.of(
                TrainingProgramResponse.builder().id(1L).isActive(true).build(),
                TrainingProgramResponse.builder().id(2L).isActive(false).hoursPerDay(BigDecimal.ONE).maxDays(3).build()
        ));
        when(trainingRequestService.getAllRequests()).thenReturn(List.of(
                TrainingRequestResponse.builder()
                        .id(10L)
                        .status("PENDING")
                        .requestedAt(now)
                        .build(),
                TrainingRequestResponse.builder()
                        .id(11L)
                        .status("CONFIRMED")
                        .requestedAt(now.minusDays(2))
                        .scheduledStart(now.plusDays(2))
                        .trainingProgramDeliveryMode("ONLINE")
                        .meetingStatus("CREATED")
                        .meetingJoinUrl("")
                        .build()
        ));
        when(paymentService.getAllPayments()).thenReturn(List.of(
                PaymentResponse.builder()
                        .id(100L)
                        .status("PAID")
                        .amount(new BigDecimal("100.00"))
                        .commissionAmount(new BigDecimal("10.00"))
                        .paidAt(now)
                        .build(),
                PaymentResponse.builder()
                        .id(101L)
                        .status("PENDING")
                        .amount(new BigDecimal("50.00"))
                        .build()
        ));
        when(geminiService.summarizeAdminWeek(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        AdminWeeklySummaryResponse response = service.getWeeklySummary("ar", 7);

        assertFalse(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals(2, response.getMetrics().getOrganizationCount());
        assertEquals(1, response.getMetrics().getPendingOrganizationReviewCount());
        assertEquals(1, response.getMetrics().getNewOrganizationApplicationCount());
        assertEquals(1, response.getMetrics().getNewVerifiedOrganizationCount());
        assertEquals(2, response.getMetrics().getProgramCount());
        assertEquals(1, response.getMetrics().getActiveProgramCount());
        assertEquals(1, response.getMetrics().getProgramsMissingDurationCount());
        assertEquals(1, response.getMetrics().getPendingProviderResponseCount());
        assertEquals(1, response.getMetrics().getUpcomingTrainingCount());
        assertEquals(1, response.getMetrics().getOnlineTrainingMissingJoinLinkCount());
        assertEquals(1, response.getMetrics().getPaidPaymentCount());
        assertEquals(1, response.getMetrics().getPendingPaymentCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getMetrics().getPaidAmount()));
        assertEquals(0, new BigDecimal("10.00").compareTo(response.getMetrics().getPlatformCommissionAmount()));
        assertTrue(response.getHeadline().contains("طلبات جهات"));
        assertTrue(response.getBullets().stream().anyMatch(line -> line.contains("تدريبات مجدولة قادمة")));
    }

    @Test
    void usesGeneratedSummaryWhenGeminiReturnsValidJson() {
        AdminWeeklySummaryService service = service(true);
        when(organizationService.getAllOrganizations()).thenReturn(List.of());
        when(trainingProgramService.getAllPrograms()).thenReturn(List.of());
        when(trainingRequestService.getAllRequests()).thenReturn(List.of());
        when(paymentService.getAllPayments()).thenReturn(List.of());
        when(geminiService.summarizeAdminWeek(anyString(), anyString()))
                .thenReturn(new AiProviderWeeklySummary(
                        "المنصة مستقرة",
                        List.of("لا توجد طلبات عاجلة", "تابع مؤشرات النمو"),
                        "تابع نشاط المنصة"));

        AdminWeeklySummaryResponse response = service.getWeeklySummary("ar", 7);

        assertTrue(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals("المنصة مستقرة", response.getHeadline());
        assertEquals(2, response.getBullets().size());
        assertEquals("تابع نشاط المنصة", response.getNextAction());
    }

    @Test
    void blocksNonPlatformAdmins() {
        AdminWeeklySummaryService service = service(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.getWeeklySummary("ar", 7));
        assertEquals(403, error.getStatusCode().value());
        assertEquals("Only the platform administrator can use the weekly summary assistant", error.getReason());
    }

    private AdminWeeklySummaryService service(boolean platformAdmin) {
        currentUser = mock(CurrentUser.class);
        organizationService = mock(OrganizationService.class);
        trainingProgramService = mock(TrainingProgramService.class);
        trainingRequestService = mock(TrainingRequestService.class);
        paymentService = mock(PaymentService.class);
        geminiService = mock(GeminiService.class);

        when(currentUser.isPlatformAdmin()).thenReturn(platformAdmin);

        return new AdminWeeklySummaryService(
                currentUser,
                organizationService,
                trainingProgramService,
                trainingRequestService,
                paymentService,
                geminiService
        );
    }
}
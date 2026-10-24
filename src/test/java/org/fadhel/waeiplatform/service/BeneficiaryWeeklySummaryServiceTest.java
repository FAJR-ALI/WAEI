package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiProviderWeeklySummary;
import org.fadhel.waeiplatform.dto.out.BeneficiaryWeeklySummaryResponse;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationType;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BeneficiaryWeeklySummaryServiceTest {

    @Test
    void validatesSupportedPeriodAndFallsBackToDeterministicSummaryWhenGeminiFails() {
        BeneficiaryWeeklySummaryService service = service(OrganizationType.BENEFICIARY);

        ApiException invalidDays = assertThrows(ApiException.class, () -> service.getWeeklySummary("ar", 31));
        assertEquals("Weekly summary period must be between 1 and 30 days", invalidDays.getMessage());

        LocalDateTime now = LocalDateTime.now();
        when(trainingRequestService.getRequestsByOrganization(1L)).thenReturn(List.of(
                TrainingRequestResponse.builder()
                        .id(1L)
                        .status("PENDING")
                        .requestedAt(now)
                        .build(),
                TrainingRequestResponse.builder()
                        .id(2L)
                        .status("CONFIRMED")
                        .requestedAt(now.minusDays(2))
                        .scheduledStart(now.plusDays(2))
                        .trainingProgramDeliveryMode("ONLINE")
                        .meetingStatus("CREATED")
                        .meetingJoinUrl("")
                        .build(),
                TrainingRequestResponse.builder()
                        .id(3L)
                        .status("COMPLETED")
                        .requestedAt(now.minusDays(10))
                        .completedAt(now)
                        .payments(Set.of(PaymentResponse.builder()
                                .status("PAID")
                                .paidAt(now)
                                .amount(new BigDecimal("200.00"))
                                .build()))
                        .build()
        ));
        when(geminiService.summarizeBeneficiaryWeek(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        BeneficiaryWeeklySummaryResponse response = service.getWeeklySummary("ar", 7);

        assertFalse(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals(3, response.getMetrics().getRequestCount());
        assertEquals(1, response.getMetrics().getPendingProviderResponseCount());
        assertEquals(1, response.getMetrics().getUpcomingTrainingCount());
        assertEquals(1, response.getMetrics().getActiveTrainingCount());
        assertEquals(1, response.getMetrics().getOnlineTrainingMissingJoinLinkCount());
        assertEquals(1, response.getMetrics().getCompletedTrainingCount());
        assertEquals(0, new BigDecimal("200.00").compareTo(response.getMetrics().getPaidTrainingAmount()));
        assertTrue(response.getHeadline().contains("طلب"));
        assertTrue(response.getBullets().stream().anyMatch(line -> line.contains("تدريبات مجدولة قادمة")));
    }

    @Test
    void usesGeneratedSummaryWhenGeminiReturnsValidJson() {
        BeneficiaryWeeklySummaryService service = service(OrganizationType.BENEFICIARY);
        when(trainingRequestService.getRequestsByOrganization(1L)).thenReturn(List.of());
        when(geminiService.summarizeBeneficiaryWeek(anyString(), anyString()))
                .thenReturn(new AiProviderWeeklySummary(
                        "أسبوع هادئ",
                        List.of("لا توجد طلبات عاجلة", "تابع البرامج المتاحة"),
                        "راقب الطلبات القادمة"));

        BeneficiaryWeeklySummaryResponse response = service.getWeeklySummary("ar", 7);

        assertTrue(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals("أسبوع هادئ", response.getHeadline());
        assertEquals(2, response.getBullets().size());
        assertEquals("راقب الطلبات القادمة", response.getNextAction());
    }

    @Test
    void blocksProviderOnlyOrganizations() {
        BeneficiaryWeeklySummaryService service = service(OrganizationType.PROVIDER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.getWeeklySummary("ar", 7));
        assertEquals(403, error.getStatusCode().value());
        assertEquals("Only the beneficiary organization can use the weekly summary assistant", error.getReason());
    }

    private CurrentUser currentUser;
    private AccessControl accessControl;
    private OrganizationRepository organizationRepository;
    private TrainingRequestService trainingRequestService;
    private GeminiService geminiService;

    private BeneficiaryWeeklySummaryService service(OrganizationType type) {
        currentUser = mock(CurrentUser.class);
        accessControl = mock(AccessControl.class);
        organizationRepository = mock(OrganizationRepository.class);
        trainingRequestService = mock(TrainingRequestService.class);
        geminiService = mock(GeminiService.class);

        Organization organization = new Organization();
        organization.setId(1L);
        organization.setType(type);
        organization.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);

        when(currentUser.getOrganizationId()).thenReturn(1L);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(accessControl.isBeneficiaryOrganization(any(Organization.class))).thenReturn(type != OrganizationType.PROVIDER);

        return new BeneficiaryWeeklySummaryService(
                currentUser,
                accessControl,
                organizationRepository,
                trainingRequestService,
                geminiService
        );
    }
}
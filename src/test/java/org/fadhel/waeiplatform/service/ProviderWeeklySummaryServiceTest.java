package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiProviderWeeklySummary;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.ProviderWeeklySummaryResponse;
import org.fadhel.waeiplatform.dto.out.TrainingProgramResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderWeeklySummaryServiceTest {

    @Test
    void validatesSupportedPeriodAndFallsBackToDeterministicSummaryWhenGeminiFails() {
        ProviderWeeklySummaryService service = service(OrganizationType.PROVIDER);

        ApiException invalidDays = assertThrows(ApiException.class, () -> service.getWeeklySummary("ar", 0));
        assertEquals("Weekly summary period must be between 1 and 30 days", invalidDays.getMessage());

        LocalDateTime now = LocalDateTime.now();
        when(trainingProgramService.getMyPrograms()).thenReturn(List.of(
                TrainingProgramResponse.builder().id(1L).isActive(true).build(),
                TrainingProgramResponse.builder().id(2L).isActive(false).build()
        ));
        when(trainingRequestService.getMyProviderRequests()).thenReturn(List.of(
                TrainingRequestResponse.builder()
                        .id(1L)
                        .status("PENDING")
                        .requestedAt(now)
                        .build(),
                TrainingRequestResponse.builder()
                        .id(2L)
                        .status("COMPLETED")
                        .requestedAt(now.minusDays(10))
                        .completedAt(now)
                        .payments(java.util.Set.of(PaymentResponse.builder()
                                .status("PAID")
                                .paidAt(now)
                                .providerAmount(new BigDecimal("100.00"))
                                .build()))
                        .build(),
                TrainingRequestResponse.builder()
                        .id(3L)
                        .status("CONFIRMED")
                        .requestedAt(now.minusDays(3))
                        .scheduledStart(now.plusDays(2))
                        .trainingProgramDeliveryMode("ONLINE")
                        .meetingStatus("CREATED")
                        .meetingJoinUrl("")
                        .build()
        ));
        when(geminiService.summarizeProviderWeek(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        ProviderWeeklySummaryResponse response = service.getWeeklySummary("ar", 7);

        assertFalse(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals(2, response.getMetrics().getProgramCount());
        assertEquals(1, response.getMetrics().getActiveProgramCount());
        assertEquals(1, response.getMetrics().getPendingRequestCount());
        assertEquals(1, response.getMetrics().getCompletedSessionCount());
        assertEquals(1, response.getMetrics().getUpcomingSessionCount());
        assertEquals(1, response.getMetrics().getOnlineMeetingMissingCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getMetrics().getPaidProviderRevenue()));
        assertTrue(response.getHeadline().contains("طلب"));
        assertTrue(response.getBullets().stream().anyMatch(line -> line.contains("تدريبات مجدولة قادمة")));
    }

    @Test
    void usesGeneratedSummaryWhenGeminiReturnsValidJson() {
        ProviderWeeklySummaryService service = service(OrganizationType.PROVIDER);
        when(trainingProgramService.getMyPrograms()).thenReturn(List.of());
        when(trainingRequestService.getMyProviderRequests()).thenReturn(List.of());
        when(geminiService.summarizeProviderWeek(anyString(), anyString()))
                .thenReturn(new AiProviderWeeklySummary(
                        "أسبوع بهدوء",
                        List.of("لا توجد طلبات عاجلة", "راجع برامجك النشطة"),
                        "راقب الطلبات الجديدة"));

        ProviderWeeklySummaryResponse response = service.getWeeklySummary("ar", 7);

        assertTrue(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals("أسبوع بهدوء", response.getHeadline());
        assertEquals(2, response.getBullets().size());
        assertEquals("راقب الطلبات الجديدة", response.getNextAction());
    }

    @Test
    void blocksBeneficiaryOrganizations() {
        ProviderWeeklySummaryService service = service(OrganizationType.BENEFICIARY);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.getWeeklySummary("ar", 7));
        assertEquals(403, error.getStatusCode().value());
        assertEquals("Only the training provider can use the weekly summary assistant", error.getReason());
    }

    private CurrentUser currentUser;
    private AccessControl accessControl;
    private OrganizationRepository organizationRepository;
    private TrainingProgramService trainingProgramService;
    private TrainingRequestService trainingRequestService;
    private GeminiService geminiService;

    private ProviderWeeklySummaryService service(OrganizationType type) {
        currentUser = mock(CurrentUser.class);
        accessControl = mock(AccessControl.class);
        organizationRepository = mock(OrganizationRepository.class);
        trainingProgramService = mock(TrainingProgramService.class);
        trainingRequestService = mock(TrainingRequestService.class);
        geminiService = mock(GeminiService.class);

        Organization organization = new Organization();
        organization.setId(1L);
        organization.setType(type);
        organization.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);

        when(currentUser.getOrganizationId()).thenReturn(1L);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(accessControl.isProviderOrganization(any(Organization.class))).thenReturn(type != OrganizationType.BENEFICIARY);

        return new ProviderWeeklySummaryService(
                currentUser,
                accessControl,
                organizationRepository,
                trainingProgramService,
                trainingRequestService,
                geminiService
        );
    }
}
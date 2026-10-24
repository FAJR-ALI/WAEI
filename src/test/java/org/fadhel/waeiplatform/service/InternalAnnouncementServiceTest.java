package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiInternalAnnouncement;
import org.fadhel.waeiplatform.dto.in.InternalAnnouncementRequest;
import org.fadhel.waeiplatform.dto.out.InternalAnnouncementResponse;
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

class InternalAnnouncementServiceTest {

    @Test
    void pureAiSuccessReturnsAiGeneratedAnnouncement() {
        InternalAnnouncementService service = service(OrganizationType.BENEFICIARY);
        when(trainingRequestService.getRequestsByOrganization(1L)).thenReturn(List.of(upcomingRequest()));
        when(geminiService.generateInternalAnnouncement(anyString(), anyString())).thenReturn(new AiInternalAnnouncement(
                "تدريب الإسعافات القادمة",
                "سيبدأ تدريب الإسعافات الأولية للموظفين في الموعد المحدد.",
                List.of("احضر في الموعد", "احتفظ برابط الانضمام"),
                "تأكد من الحضور قبل الموعد."
        ));

        InternalAnnouncementResponse response = service.generate(request(10L));

        assertTrue(Boolean.TRUE.equals(response.getAiGenerated()));
        assertEquals("تدريب الإسعافات القادمة", response.getSubject());
        assertEquals(10L, response.getTrainingRequestId());
        assertEquals(2, response.getChecklist().size());
        assertFalse(response.getWhenLine() == null || response.getWhenLine().isBlank());
        assertTrue(response.getWhenLine().contains(String.valueOf(LocalDateTime.now().plusDays(2).getYear())));
    }

    @Test
    void pureAiDoesNotFallbackWhenGeminiFails() {
        InternalAnnouncementService service = service(OrganizationType.BENEFICIARY);
        when(trainingRequestService.getRequestsByOrganization(1L)).thenReturn(List.of(upcomingRequest()));
        when(geminiService.generateInternalAnnouncement(anyString(), anyString()))
                .thenThrow(new ApiException("AI announcement is unavailable, please try again"));

        ApiException error = assertThrows(ApiException.class, () -> service.generate(request(10L)));
        assertEquals("AI announcement is unavailable, please try again", error.getMessage());
    }

    @Test
    void rejectsRequestThatIsNotUpcoming() {
        InternalAnnouncementService service = service(OrganizationType.BENEFICIARY);
        TrainingRequestResponse pending = upcomingRequest();
        pending.setStatus("PENDING");
        when(trainingRequestService.getRequestsByOrganization(1L)).thenReturn(List.of(pending));

        ApiException error = assertThrows(ApiException.class, () -> service.generate(request(10L)));
        assertEquals("This training request is not ready for an internal announcement", error.getMessage());
    }

    @Test
    void blocksPlatformAdminAndProviderOnlyOrganizations() {
        InternalAnnouncementService providerService = service(OrganizationType.PROVIDER);
        ResponseStatusException providerError = assertThrows(ResponseStatusException.class,
                () -> providerService.generate(request(10L)));
        assertEquals(403, providerError.getStatusCode().value());
        assertEquals("Only the beneficiary organization can use the announcement assistant", providerError.getReason());
    }

    private CurrentUser currentUser;
    private AccessControl accessControl;
    private OrganizationRepository organizationRepository;
    private TrainingRequestService trainingRequestService;
    private GeminiService geminiService;

    private InternalAnnouncementService service(OrganizationType type) {
        currentUser = mock(CurrentUser.class);
        accessControl = mock(AccessControl.class);
        organizationRepository = mock(OrganizationRepository.class);
        trainingRequestService = mock(TrainingRequestService.class);
        geminiService = mock(GeminiService.class);

        Organization organization = new Organization();
        organization.setId(1L);
        organization.setName("Hospital");
        organization.setSector("HEALTHCARE");
        organization.setType(type);
        organization.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);

        when(currentUser.isPlatformAdmin()).thenReturn(false);
        when(currentUser.getOrganizationId()).thenReturn(1L);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(accessControl.isBeneficiaryOrganization(any(Organization.class))).thenReturn(type != OrganizationType.PROVIDER);

        return new InternalAnnouncementService(
                currentUser,
                accessControl,
                organizationRepository,
                trainingRequestService,
                geminiService
        );
    }

    private InternalAnnouncementRequest request(Long id) {
        InternalAnnouncementRequest request = new InternalAnnouncementRequest();
        request.setTrainingRequestId(id);
        request.setLang("ar");
        request.setChannel("notice");
        return request;
    }

    private TrainingRequestResponse upcomingRequest() {
        return TrainingRequestResponse.builder()
                .id(10L)
                .status("CONFIRMED")
                .participantCount(20)
                .daysCount(2)
                .hoursPerDay(new BigDecimal("3.0"))
                .scheduledStart(LocalDateTime.now().plusDays(2))
                .scheduledEnd(LocalDateTime.now().plusDays(4))
                .trainingProgramTitle("First Aid")
                .trainingProgramDeliveryMode("ONLINE")
                .providerOrganizationName("Safety Center")
                .beneficiaryOrganizationId(1L)
                .beneficiaryOrganizationName("Hospital")
                .meetingJoinUrl("https://zoom.us/j/123")
                .build();
    }
}
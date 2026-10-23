package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.repository.PaymentRepository;
import org.fadhel.waeiplatform.repository.TrainingProgramRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingRequestLifecycleTest {

    @Mock
    private TrainingRequestRepository trainingRequestRepository;

    @Mock
    private TrainingProgramRepository trainingProgramRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ZoomMeetingService zoomMeetingService;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private AccessControl accessControl;

    @InjectMocks
    private TrainingRequestService trainingRequestService;

    @Test
    void confirmedTrainingStartsAtScheduledStart() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        whenStartCandidates(List.of(request));
        whenCompletionCandidates(List.of());

        int changed = trainingRequestService.advanceScheduledTrainingRequests();

        assertEquals(1, changed);
        assertEquals(TrainingRequestStatus.IN_PROGRESS, request.getStatus());
        assertNull(request.getCompletedAt());
        verify(trainingRequestRepository).saveAll(any());
    }

    @Test
    void confirmedTrainingWithPastEndIsCompletedImmediately() {
        LocalDateTime scheduledEnd = LocalDateTime.now().minusMinutes(1);
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED,
                LocalDateTime.now().minusHours(2), scheduledEnd);
        whenStartCandidates(List.of(request));
        whenCompletionCandidates(List.of());

        int changed = trainingRequestService.advanceScheduledTrainingRequests();

        assertEquals(1, changed);
        assertEquals(TrainingRequestStatus.COMPLETED, request.getStatus());
        assertEquals(scheduledEnd, request.getCompletedAt());
    }

    @Test
    void inProgressTrainingCompletesAtScheduledEnd() {
        LocalDateTime scheduledEnd = LocalDateTime.now().minusMinutes(1);
        TrainingRequest request = request(TrainingRequestStatus.IN_PROGRESS,
                LocalDateTime.now().minusHours(2), scheduledEnd);
        whenStartCandidates(List.of());
        whenCompletionCandidates(List.of(request));

        int changed = trainingRequestService.advanceScheduledTrainingRequests();

        assertEquals(1, changed);
        assertEquals(TrainingRequestStatus.COMPLETED, request.getStatus());
        assertEquals(scheduledEnd, request.getCompletedAt());
    }

    @Test
    void futureTrainingIsNotChanged() {
        whenStartCandidates(List.of());
        whenCompletionCandidates(List.of());

        int changed = trainingRequestService.advanceScheduledTrainingRequests();

        assertEquals(0, changed);
        verify(trainingRequestRepository, never()).saveAll(any());
    }

    private void whenStartCandidates(List<TrainingRequest> requests) {
        when(trainingRequestRepository.findByStatusAndScheduledStartLessThanEqual(
                eq(TrainingRequestStatus.CONFIRMED), any(LocalDateTime.class))).thenReturn(requests);
    }

    private void whenCompletionCandidates(List<TrainingRequest> requests) {
        when(trainingRequestRepository.findByStatusAndScheduledEndLessThanEqual(
                eq(TrainingRequestStatus.IN_PROGRESS), any(LocalDateTime.class))).thenReturn(requests);
    }

    private TrainingRequest request(TrainingRequestStatus status,
                                    LocalDateTime scheduledStart, LocalDateTime scheduledEnd) {
        TrainingRequest request = new TrainingRequest();
        request.setId(1L);
        request.setStatus(status);
        request.setScheduledStart(scheduledStart);
        request.setScheduledEnd(scheduledEnd);
        return request;
    }
}

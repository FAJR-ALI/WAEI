package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.integration.ZoomClient;
import org.fadhel.waeiplatform.model.OnlineMeetingStatus;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.PaymentRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoomMeetingServiceTest {

    @Mock
    private ZoomClient zoomClient;

    @Mock
    private TrainingRequestRepository trainingRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccessControl accessControl;

    @InjectMocks
    private ZoomMeetingService zoomMeetingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(zoomMeetingService, "batchSize", 20);
        ReflectionTestUtils.setField(zoomMeetingService, "retryCooldownMs", 300000L);
        ReflectionTestUtils.setField(zoomMeetingService, "timezone", "Asia/Riyadh");
    }

    @Test
    void createsMeetingForPaidOnlineRequest() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "ONLINE", 1, "2.5");
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.existsByTrainingRequest_IdAndStatusIn(eq(1L), any(Collection.class)))
                .thenReturn(true);
        when(zoomClient.isConfigured()).thenReturn(true);
        when(zoomClient.createMeeting(any())).thenReturn(
                new ZoomClient.ZoomMeeting("123456789", "https://zoom.test/j/123", "https://zoom.test/s/123", "secret"));

        assertTrue(zoomMeetingService.createForRequest(1L));

        assertEquals("123456789", request.getZoomMeetingId());
        assertEquals("https://zoom.test/j/123", request.getZoomJoinUrl());
        assertEquals("secret", request.getZoomPassword());
        assertEquals(OnlineMeetingStatus.CREATED, request.getMeetingStatus());
        assertEquals(1, request.getZoomAttemptCount());
        verify(trainingRequestRepository).save(request);

        ArgumentCaptor<ZoomClient.MeetingSpec> spec = ArgumentCaptor.forClass(ZoomClient.MeetingSpec.class);
        verify(zoomClient).createMeeting(spec.capture());
        assertEquals(2, spec.getValue().type());
        assertEquals(150, spec.getValue().durationMinutes());
        assertNull(spec.getValue().recurringOccurrences());
    }

    @Test
    void createsDailyRecurringMeetingForShortMultiDayProgram() {
        TrainingRequest request = request(TrainingRequestStatus.IN_PROGRESS, "ONLINE", 3, "2");
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.existsByTrainingRequest_IdAndStatusIn(eq(1L), any(Collection.class)))
                .thenReturn(true);
        when(zoomClient.isConfigured()).thenReturn(true);
        when(zoomClient.createMeeting(any())).thenReturn(
                new ZoomClient.ZoomMeeting("123456789", "https://zoom.test/j/123", null, null));

        assertTrue(zoomMeetingService.createForRequest(1L));

        ArgumentCaptor<ZoomClient.MeetingSpec> spec = ArgumentCaptor.forClass(ZoomClient.MeetingSpec.class);
        verify(zoomClient).createMeeting(spec.capture());
        assertEquals(8, spec.getValue().type());
        assertEquals(3, spec.getValue().recurringOccurrences());
    }

    @Test
    void usesNoFixedTimeMeetingWhenProgramExceedsZoomRecurrenceLimit() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "ONLINE", 51, "1");
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.existsByTrainingRequest_IdAndStatusIn(eq(1L), any(Collection.class)))
                .thenReturn(true);
        when(zoomClient.isConfigured()).thenReturn(true);
        when(zoomClient.createMeeting(any())).thenReturn(
                new ZoomClient.ZoomMeeting("123456789", "https://zoom.test/j/123", null, null));

        assertTrue(zoomMeetingService.createForRequest(1L));

        ArgumentCaptor<ZoomClient.MeetingSpec> spec = ArgumentCaptor.forClass(ZoomClient.MeetingSpec.class);
        verify(zoomClient).createMeeting(spec.capture());
        assertEquals(3, spec.getValue().type());
        assertNull(spec.getValue().startTime());
        assertNull(spec.getValue().recurringOccurrences());
    }

    @Test
    void doesNotCreateMeetingForInPersonProgram() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "IN_PERSON", 1, "2");
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertFalse(zoomMeetingService.createForRequest(1L));

        verify(zoomClient, never()).createMeeting(any());
        verify(trainingRequestRepository, never()).save(any(TrainingRequest.class));
    }

    @Test
    void doesNotCreateMeetingWithoutPaidPayment() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "ONLINE", 1, "2");
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.existsByTrainingRequest_IdAndStatusIn(eq(1L), any(Collection.class)))
                .thenReturn(false);

        assertFalse(zoomMeetingService.createForRequest(1L));

        verify(zoomClient, never()).createMeeting(any());
    }

    @Test
    void recordsFailureWithoutRemovingTheRequest() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "ONLINE", 1, "2");
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.existsByTrainingRequest_IdAndStatusIn(eq(1L), any(Collection.class)))
                .thenReturn(true);
        when(zoomClient.isConfigured()).thenReturn(true);
        when(zoomClient.createMeeting(any())).thenThrow(new ApiException("Zoom is unavailable"));

        assertFalse(zoomMeetingService.createForRequest(1L));

        assertEquals(OnlineMeetingStatus.FAILED, request.getMeetingStatus());
        assertEquals("Zoom is unavailable", request.getZoomLastError());
        verify(trainingRequestRepository).save(request);
    }

    @Test
    void deletingMeetingClearsTheJoinLink() {
        TrainingRequest request = request(TrainingRequestStatus.CANCELLED, "ONLINE", 1, "2");
        request.setZoomMeetingId("123456789");
        request.setZoomJoinUrl("https://zoom.test/j/123");
        request.setMeetingStatus(OnlineMeetingStatus.CREATED);
        when(zoomClient.isConfigured()).thenReturn(true);

        zoomMeetingService.deleteForRequest(request);

        verify(zoomClient).deleteMeeting("123456789");
        assertNull(request.getZoomJoinUrl());
        assertEquals(OnlineMeetingStatus.DELETED, request.getMeetingStatus());
        verify(trainingRequestRepository).save(request);
    }

    @Test
    void providerGetsAFreshHostLink() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "ONLINE", 1, "2");
        request.setZoomMeetingId("123456789");
        request.setZoomJoinUrl("https://zoom.test/j/123");
        request.setMeetingStatus(OnlineMeetingStatus.CREATED);
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(zoomClient.isConfigured()).thenReturn(true);
        when(zoomClient.fetchHostStartUrl("123456789")).thenReturn("https://zoom.test/s/fresh");

        assertEquals("https://zoom.test/s/fresh", zoomMeetingService.getFreshHostStartUrl(1L));
        verify(accessControl).requireProviderOwner(request);
    }

    @Test
    void hostLinkIsRejectedWhenCallerCannotManageTheRequest() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED, "ONLINE", 1, "2");
        request.setZoomMeetingId("123456789");
        request.setZoomJoinUrl("https://zoom.test/j/123");
        request.setMeetingStatus(OnlineMeetingStatus.CREATED);
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        org.mockito.Mockito.doThrow(new ApiException("Only the training provider can perform this action"))
                .when(accessControl).requireProviderOwner(request);

        assertThrows(ApiException.class, () -> zoomMeetingService.getFreshHostStartUrl(1L));
        verify(zoomClient, never()).fetchHostStartUrl(any());
    }

    private TrainingRequest request(TrainingRequestStatus status, String deliveryMode, int days, String hours) {
        TrainingProgram program = new TrainingProgram();
        program.setId(10L);
        program.setTitle("Online safety");
        program.setDeliveryMode(deliveryMode);

        TrainingRequest request = new TrainingRequest();
        request.setId(1L);
        request.setStatus(status);
        request.setTrainingProgram(program);
        request.setScheduledStart(LocalDateTime.now().plusDays(1));
        request.setScheduledEnd(LocalDateTime.now().plusDays(days).plusHours(2));
        request.setDaysCount(days);
        request.setHoursPerDay(new BigDecimal(hours));
        return request;
    }
}

package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.CancelTrainingRequest;
import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.PaymentStatus;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingRequestCancellationTest {

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
    void cancelAcceptedRequestWithoutPaymentCancelsPendingAttempts() {
        TrainingRequest request = request(TrainingRequestStatus.ACCEPTED);
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.findFirstByTrainingRequest_IdAndStatusOrderByCreatedAtDesc(
                1L, PaymentStatus.PAID)).thenReturn(Optional.empty());

        CancelTrainingRequest cancellation = new CancelTrainingRequest();
        cancellation.setReason("No longer needed");

        trainingRequestService.cancelRequest(1L, cancellation);

        assertEquals(TrainingRequestStatus.CANCELLED, request.getStatus());
        assertEquals("No longer needed", request.getCancellationReason());
        assertNotNull(request.getCancelledAt());
        verify(accessControl).requireBeneficiaryOwner(request);
        verify(paymentService).cancelPendingPayments(request);
        verify(paymentService, never()).refundForCancellation(any(Payment.class));
        verify(trainingRequestRepository).save(request);
        verify(zoomMeetingService).deleteForRequest(request);
    }

    @Test
    void cancelPaymentPendingRequestRefundsPaidPayment() {
        TrainingRequest request = request(TrainingRequestStatus.PAYMENT_PENDING);
        Payment paidPayment = new Payment();
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.findFirstByTrainingRequest_IdAndStatusOrderByCreatedAtDesc(
                1L, PaymentStatus.PAID)).thenReturn(Optional.of(paidPayment));

        trainingRequestService.cancelRequest(1L, null);

        assertEquals(TrainingRequestStatus.CANCELLED, request.getStatus());
        assertEquals("Cancelled by the beneficiary", request.getCancellationReason());
        verify(paymentService).refundForCancellation(paidPayment);
        verify(paymentService).cancelPendingPayments(request);
        verify(trainingRequestRepository).save(request);
        verify(zoomMeetingService).deleteForRequest(request);
    }

    @Test
    void cancelConfirmedRequestRefundsPaidPayment() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED);
        Payment paidPayment = new Payment();
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.findFirstByTrainingRequest_IdAndStatusOrderByCreatedAtDesc(
                1L, PaymentStatus.PAID)).thenReturn(Optional.of(paidPayment));

        trainingRequestService.cancelRequest(1L, null);

        assertEquals(TrainingRequestStatus.CANCELLED, request.getStatus());
        assertNotNull(request.getCancelledAt());
        verify(paymentService).refundForCancellation(paidPayment);
        verify(paymentService).cancelPendingPayments(request);
        verify(trainingRequestRepository).save(request);
        verify(zoomMeetingService).deleteForRequest(request);
    }

    @Test
    void confirmedRequestWithoutPaidPaymentCannotBeCancelled() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED);
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(paymentRepository.findFirstByTrainingRequest_IdAndStatusOrderByCreatedAtDesc(
                1L, PaymentStatus.PAID)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> trainingRequestService.cancelRequest(1L, null));

        assertEquals(TrainingRequestStatus.CONFIRMED, request.getStatus());
        verifyNoInteractions(paymentService);
        verify(trainingRequestRepository, never()).save(any(TrainingRequest.class));
    }

    @Test
    void requestCannotBeCancelledAfterTrainingStarts() {
        TrainingRequest request = request(TrainingRequestStatus.IN_PROGRESS);
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(ApiException.class, () -> trainingRequestService.cancelRequest(1L, null));

        assertEquals(TrainingRequestStatus.IN_PROGRESS, request.getStatus());
        verifyNoInteractions(paymentService);
        verify(trainingRequestRepository, never()).save(any(TrainingRequest.class));
    }

    @Test
    void confirmedRequestCannotBeCancelledAfterScheduledStart() {
        TrainingRequest request = request(TrainingRequestStatus.CONFIRMED);
        request.setScheduledStart(LocalDateTime.now().minusMinutes(1));
        when(trainingRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(ApiException.class, () -> trainingRequestService.cancelRequest(1L, null));

        assertEquals(TrainingRequestStatus.CONFIRMED, request.getStatus());
        verifyNoInteractions(paymentService);
        verify(trainingRequestRepository, never()).save(any(TrainingRequest.class));
    }

    private TrainingRequest request(TrainingRequestStatus status) {
        TrainingRequest request = new TrainingRequest();
        request.setId(1L);
        request.setStatus(status);
        request.setRequestedAt(LocalDateTime.now().minusHours(1));
        return request;
    }
}

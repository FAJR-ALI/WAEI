package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.PaymentRequest;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.integration.MoyasarClient;
import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.PaymentRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final List<PaymentStatus> BLOCKING_PAYMENT_STATUSES =
            List.of(PaymentStatus.PENDING, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final TrainingRequestRepository trainingRequestRepository;
    private final MoyasarClient moyasarClient;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;

    @Value("${payment.commission-rate-percent:10}")
    private BigDecimal commissionRatePercent;

    @Value("${moyasar.webhook.secret:}")
    private String webhookSecret;

    public PaymentResponse toResponse(Payment payment) {
        Long trainingRequestId = payment.getTrainingRequest() != null
                ? payment.getTrainingRequest().getId()
                : null;

        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getMoyasarPaymentId(),
                payment.getMoyasarInvoiceId(),
                payment.getCheckoutUrl(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getPaidAt(),
                payment.getRefundedAmount(),
                payment.getRefundedAt(),
                trainingRequestId,
                payment.getCommissionRatePercent(),
                payment.getCommissionAmount(),
                payment.getProviderAmount());
    }

    public List<PaymentResponse> getAllPayments() {
        if (currentUser.isPlatformAdmin()) {
            return paymentRepository.findAll().stream().map(this::toResponse).toList();
        }

        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            return List.of();
        }

        return paymentRepository.findAccessibleByOrganization(organizationId).stream()
                .map(this::toResponse).toList();
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = findPayment(id);
        accessControl.requireCanView(payment);
        return toResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByTrainingRequest(Long trainingRequestId) {
        TrainingRequest request = trainingRequestRepository.findById(trainingRequestId)
                .orElseThrow(() -> new ApiException("Training request not found"));

        accessControl.requireCanView(request);

        return paymentRepository.findPaymentsByTrainingRequest_Id(trainingRequestId).stream()
                .map(this::toResponse).toList();
    }

    public List<PaymentResponse> getPaymentsByStatus(String status) {
        PaymentStatus parsedStatus;
        try {
            parsedStatus = PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid status");
        }

        if (currentUser.isPlatformAdmin()) {
            return paymentRepository.findPaymentsByStatus(parsedStatus).stream().map(this::toResponse).toList();
        }

        return getAllPayments().stream()
                .filter(payment -> parsedStatus.name().equals(payment.getStatus()))
                .toList();
    }

    public PaymentResponse getPaymentByMoyasarId(String moyasarPaymentId) {
        Payment payment = paymentRepository.findPaymentByMoyasarPaymentId(moyasarPaymentId);
        if (payment == null) {
            throw new ApiException("Payment not found");
        }
        accessControl.requireCanView(payment);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse addPayment(PaymentRequest request) {
        TrainingRequest trainingRequest = trainingRequestRepository.findById(request.getTrainingRequestId())
                .orElseThrow(() -> new ApiException("Training request not found"));

        accessControl.requireBeneficiaryOwner(trainingRequest);

        if (trainingRequest.getStatus() != TrainingRequestStatus.ACCEPTED) {
            throw new ApiException("A payment can only be started for an accepted training request");
        }

        boolean blockingPaymentExists = paymentRepository
                .existsByTrainingRequest_IdAndStatusIn(trainingRequest.getId(), BLOCKING_PAYMENT_STATUSES);

        if (blockingPaymentExists) {
            throw new ApiException("A payment is already pending or paid for this training request");
        }

        if (!moyasarClient.isConfigured()) {
            throw new ApiException("Moyasar is not configured. Set MOYASAR_SECRET_KEY to a test or live secret key.");
        }

        BigDecimal rate = commissionRatePercent.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = trainingRequest.getProgramPrice();
        BigDecimal commissionAmount = amount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal providerAmount = amount.subtract(commissionAmount);

        String description = "Training request #" + trainingRequest.getId()
                + (trainingRequest.getTrainingProgram() != null
                ? " - " + trainingRequest.getTrainingProgram().getTitle()
                : "");

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setCurrency("SAR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCommissionRatePercent(rate);
        payment.setCommissionAmount(commissionAmount);
        payment.setProviderAmount(providerAmount);
        payment.setTrainingRequest(trainingRequest);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        MoyasarClient.MoyasarInvoice invoice = moyasarClient.createInvoice(
                amount, description, trainingRequest.getId(), payment.getId());

        payment.setMoyasarInvoiceId(invoice.id());
        payment.setCheckoutUrl(invoice.url());
        paymentRepository.save(payment);

        transitionRequest(trainingRequest, TrainingRequestStatus.PAYMENT_PENDING);

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(Long id) {
        Payment payment = findPayment(id);
        TrainingRequest trainingRequest = payment.getTrainingRequest();

        if (!currentUser.isPlatformAdmin()) {
            if (trainingRequest == null) {
                throw new ApiException("This payment is not linked to a training request");
            }
            accessControl.requireBeneficiaryOwner(trainingRequest);
        }

        if (trainingRequest != null
                && (trainingRequest.getStatus() == TrainingRequestStatus.IN_PROGRESS
                || trainingRequest.getStatus() == TrainingRequestStatus.COMPLETED)) {
            throw new ApiException("A payment cannot be refunded after the training has started");
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException("Only a paid payment can be refunded");
        }

        refundForCancellation(payment);

        if (trainingRequest != null && trainingRequest.getStatus() != TrainingRequestStatus.COMPLETED
                && trainingRequest.getStatus().canTransitionTo(TrainingRequestStatus.CANCELLED)) {
            trainingRequest.setStatus(TrainingRequestStatus.CANCELLED);
            trainingRequest.setCancelledAt(LocalDateTime.now());
            trainingRequest.setCancellationReason("Payment refunded");
            trainingRequestRepository.save(trainingRequest);
        }

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse syncPayment(Long id) {
        Payment payment = findPayment(id);
        accessControl.requireCanView(payment);

        if (!moyasarClient.isConfigured()) {
            throw new ApiException("Moyasar is not configured");
        }
        if (payment.getMoyasarPaymentId() == null) {
            throw new ApiException("This payment does not have a Moyasar payment id yet");
        }

        MoyasarClient.MoyasarPayment remote = moyasarClient.fetchPayment(payment.getMoyasarPaymentId());
        applyRemotePayment(payment, remote);

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse syncByInvoice(String invoiceId) {
        Payment payment = paymentRepository.findPaymentByMoyasarInvoiceId(invoiceId);
        if (payment == null) {
            throw new ApiException("Payment not found for this Moyasar invoice");
        }

        accessControl.requireCanView(payment);

        if (!moyasarClient.isConfigured()) {
            throw new ApiException("Moyasar is not configured");
        }

        MoyasarClient.MoyasarInvoice invoice = moyasarClient.fetchInvoice(invoiceId);
        MoyasarClient.MoyasarPayment remote = null;
        if (invoice.payments() != null && !invoice.payments().isEmpty()) {
            remote = invoice.payments().get(invoice.payments().size() - 1);
        }

        if (remote == null) {
            throw new ApiException("No Moyasar payment attempt exists for this invoice yet");
        }

        applyRemotePayment(payment, remote);

        return toResponse(payment);
    }

    /**
     * Used by the public payment return page. It never exposes other payments and only reflects
     * the status that Moyasar reports for the stored invoice.
     */
    @Transactional
    public PaymentResponse syncForCallback(Long paymentId) {
        Payment payment = findPayment(paymentId);

        if (!moyasarClient.isConfigured() || payment.getMoyasarInvoiceId() == null) {
            return toResponse(payment);
        }

        MoyasarClient.MoyasarInvoice invoice = moyasarClient.fetchInvoice(payment.getMoyasarInvoiceId());
        if (invoice.payments() != null && !invoice.payments().isEmpty()) {
            MoyasarClient.MoyasarPayment remote = invoice.payments().get(invoice.payments().size() - 1);
            applyRemotePayment(payment, remote);
        }

        return toResponse(payment);
    }

    @Transactional
    public void handleWebhook(MoyasarClient.MoyasarWebhook payload) {
        verifyWebhookSecret(payload);

        MoyasarClient.MoyasarPayment data = payload.data();
        if (data == null || data.id() == null) {
            throw new ApiException("Invalid Moyasar webhook payload");
        }

        Payment payment = null;
        if (data.invoiceId() != null) {
            payment = paymentRepository.findPaymentByMoyasarInvoiceId(data.invoiceId());
        }
        if (payment == null) {
            payment = paymentRepository.findPaymentByMoyasarPaymentId(data.id());
        }
        if (payment == null) {
            log.warn("Moyasar webhook {} for unknown payment {}", payload.type(), data.id());
            throw new ApiException("Payment not found for this Moyasar webhook");
        }

        applyRemotePayment(payment, data);
    }

    @Transactional
    public void refundForCancellation(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException("Only a paid payment can be refunded");
        }

        if (moyasarClient.isConfigured() && payment.getMoyasarPaymentId() != null) {
            MoyasarClient.MoyasarPayment refunded =
                    moyasarClient.refundPayment(payment.getMoyasarPaymentId());

            payment.setMoyasarPaymentId(refunded.id() != null ? refunded.id() : payment.getMoyasarPaymentId());
            payment.setRefundedAmount(refunded.refundedAmount().compareTo(BigDecimal.ZERO) > 0
                    ? refunded.refundedAmount()
                    : payment.getAmount());
            payment.setRefundedAt(refunded.refundedAtAsDateTime() != null
                    ? refunded.refundedAtAsDateTime()
                    : LocalDateTime.now());
        } else {
            payment.setRefundedAmount(payment.getAmount());
            payment.setRefundedAt(LocalDateTime.now());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
    }

    @Transactional
    public void cancelPendingPayments(TrainingRequest trainingRequest) {
        for (Payment payment : paymentRepository.findPaymentsByTrainingRequest_Id(trainingRequest.getId())) {
            if (payment.getStatus() != PaymentStatus.PENDING) {
                continue;
            }

            if (moyasarClient.isConfigured() && payment.getMoyasarInvoiceId() != null) {
                try {
                    moyasarClient.cancelInvoice(payment.getMoyasarInvoiceId());
                } catch (ApiException e) {
                    log.warn("Could not cancel Moyasar invoice {}: {}",
                            payment.getMoyasarInvoiceId(), e.getMessage());
                }
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        }
    }

    @Transactional
    public void deletePayment(Long id) {
        currentUser.requirePlatformAdmin();

        Payment payment = findPayment(id);
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ApiException("Paid or refunded payments cannot be deleted");
        }

        paymentRepository.delete(payment);
    }

    private void applyRemotePayment(Payment payment, MoyasarClient.MoyasarPayment remote) {
        if (remote.amount() != null) {
            int expectedAmount = MoyasarClient.toHalalas(payment.getAmount());
            if (remote.amount() != expectedAmount) {
                throw new ApiException("The Moyasar payment amount does not match the expected amount");
            }
        }

        if (remote.currency() != null && !remote.currency().equalsIgnoreCase(payment.getCurrency())) {
            throw new ApiException("The Moyasar payment currency does not match");
        }

        PaymentStatus target = mapMoyasarStatus(remote.status());
        if (target == null) {
            return;
        }

        if (remote.id() != null) {
            payment.setMoyasarPaymentId(remote.id());
        }

        if (payment.getStatus() == target) {
            paymentRepository.save(payment);
            return;
        }

        if (!payment.getStatus().canTransitionTo(target)) {
            log.warn("Ignoring out-of-order Moyasar status {} for payment {} currently {}",
                    remote.status(), payment.getId(), payment.getStatus());
            return;
        }

        payment.setStatus(target);

        if (target == PaymentStatus.PAID && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        if (target == PaymentStatus.REFUNDED) {
            payment.setRefundedAmount(remote.refundedAmount().compareTo(BigDecimal.ZERO) > 0
                    ? remote.refundedAmount()
                    : payment.getAmount());
            payment.setRefundedAt(remote.refundedAtAsDateTime() != null
                    ? remote.refundedAtAsDateTime()
                    : LocalDateTime.now());
        }

        paymentRepository.save(payment);
        updateRequestStatus(payment, target);
    }

    private void updateRequestStatus(Payment payment, PaymentStatus paymentStatus) {
        TrainingRequest request = payment.getTrainingRequest();
        if (request == null) {
            return;
        }

        switch (paymentStatus) {
            case PAID -> {
                if (request.getStatus() == TrainingRequestStatus.PAYMENT_PENDING) {
                    transitionRequest(request, TrainingRequestStatus.PAID);
                } else if (request.getStatus() == TrainingRequestStatus.ACCEPTED) {
                    transitionRequest(request, TrainingRequestStatus.PAYMENT_PENDING);
                    transitionRequest(request, TrainingRequestStatus.PAID);
                }
            }
            case FAILED, CANCELLED -> {
                if (request.getStatus() == TrainingRequestStatus.PAYMENT_PENDING) {
                    transitionRequest(request, TrainingRequestStatus.ACCEPTED);
                }
            }
            case REFUNDED -> {
                if (request.getStatus() != TrainingRequestStatus.IN_PROGRESS
                        && request.getStatus() != TrainingRequestStatus.COMPLETED
                        && request.getStatus() != TrainingRequestStatus.CANCELLED
                        && request.getStatus().canTransitionTo(TrainingRequestStatus.CANCELLED)) {
                    request.setStatus(TrainingRequestStatus.CANCELLED);
                    request.setCancelledAt(LocalDateTime.now());
                    request.setCancellationReason("Payment refunded");
                    trainingRequestRepository.save(request);
                }
            }
            default -> {
            }
        }
    }

    private void transitionRequest(TrainingRequest request, TrainingRequestStatus next) {
        if (!request.getStatus().canTransitionTo(next)) {
            throw new ApiException("Cannot change training request status from "
                    + request.getStatus() + " to " + next);
        }
        request.setStatus(next);
        trainingRequestRepository.save(request);
    }

    private PaymentStatus mapMoyasarStatus(String moyasarStatus) {
        if (moyasarStatus == null) {
            return null;
        }

        return switch (moyasarStatus.toLowerCase()) {
            case "paid", "captured" -> PaymentStatus.PAID;
            case "failed" -> PaymentStatus.FAILED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "voided" -> PaymentStatus.CANCELLED;
            case "initiated", "authorized", "verified" -> PaymentStatus.PENDING;
            default -> null;
        };
    }

    private void verifyWebhookSecret(MoyasarClient.MoyasarWebhook payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Moyasar webhook secret is not configured");
        }

        String provided = payload.secretToken();
        if (provided == null || !MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Moyasar webhook secret");
        }
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Payment not found"));
    }
}

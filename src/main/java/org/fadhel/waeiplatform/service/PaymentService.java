package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.PaymentRequest;
import org.fadhel.waeiplatform.dto.in.PaymentUpdateRequest;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.repository.PaymentRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PAID", "FAILED", "CANCELLED");
    private static final Set<String> BLOCKED_REQUEST_STATUSES = Set.of("REJECTED", "CANCELLED", "PAID");

    private final PaymentRepository paymentRepository;
    private final TrainingRequestRepository trainingRequestRepository;

    @Value("${payment.commission-rate-percent:10}")
    private BigDecimal commissionRatePercent;

    private PaymentResponse convertToResponse(Payment payment) {
        Long trainingRequestId = payment.getTrainingRequest() != null
                ? payment.getTrainingRequest().getId()
                : null;

        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getMoyasarPaymentId(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                trainingRequestId,
                payment.getCommissionRatePercent(),
                payment.getCommissionAmount(),
                payment.getProviderAmount());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream().map(this::convertToResponse).toList();
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Payment not found"));
        return convertToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByTrainingRequest(Long trainingRequestId) {
        if (!trainingRequestRepository.existsById(trainingRequestId)) {
            throw new ApiException("Training request not found");
        }
        return paymentRepository.findPaymentsByTrainingRequest_Id(trainingRequestId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<PaymentResponse> getPaymentsByStatus(String status) {
        validateStatus(status);
        return paymentRepository.findPaymentsByStatus(status).stream().map(this::convertToResponse).toList();
    }

    public PaymentResponse getPaymentByMoyasarId(String moyasarPaymentId) {
        Payment payment = paymentRepository.findPaymentByMoyasarPaymentId(moyasarPaymentId);
        if (payment == null) {
            throw new ApiException("Payment not found");
        }
        return convertToResponse(payment);
    }

    @Transactional
    public void addPayment(PaymentRequest request) {
        TrainingRequest trainingRequest = trainingRequestRepository.findById(request.getTrainingRequestId())
                .orElseThrow(() -> new ApiException("Training request not found"));

        if (BLOCKED_REQUEST_STATUSES.contains(trainingRequest.getStatus())) {
            throw new ApiException("A payment cannot be created for a training request with status "
                    + trainingRequest.getStatus());
        }

        BigDecimal rate = commissionRatePercent.setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = trainingRequest.getProgramPrice();
        BigDecimal commissionAmount = amount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal providerAmount = amount.subtract(commissionAmount);

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setCurrency("SAR");
        payment.setStatus("PENDING");
        payment.setCommissionRatePercent(rate);
        payment.setCommissionAmount(commissionAmount);
        payment.setProviderAmount(providerAmount);
        payment.setTrainingRequest(trainingRequest);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        trainingRequest.setStatus("PAYMENT_PENDING");
        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void updatePayment(Long id, PaymentUpdateRequest request) {
        validateStatus(request.getStatus());

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Payment not found"));

        TrainingRequest trainingRequest = payment.getTrainingRequest();

        payment.setStatus(request.getStatus());
        payment.setMoyasarPaymentId(request.getMoyasarPaymentId());

        if ("PAID".equals(request.getStatus())) {
            if (payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
            if (trainingRequest != null) {
                trainingRequest.setStatus("PAID");
                trainingRequestRepository.save(trainingRequest);
            }
        } else {
            payment.setPaidAt(null);
            if ("PENDING".equals(request.getStatus()) && trainingRequest != null) {
                trainingRequest.setStatus("PAYMENT_PENDING");
                trainingRequestRepository.save(trainingRequest);
            }
        }

        paymentRepository.save(payment);
    }

    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Payment not found"));
        paymentRepository.delete(payment);
    }

    private void validateStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new ApiException("Invalid status");
        }
    }
}

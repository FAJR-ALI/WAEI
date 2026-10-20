package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.TrainingRequestRequest;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.repository.TrainingProgramRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingRequestService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING", "ACCEPTED", "REJECTED", "PAYMENT_PENDING",
            "PAID", "IN_PROGRESS", "COMPLETED", "CANCELLED");

    private final TrainingRequestRepository trainingRequestRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;

    private TrainingRequestResponse convertToResponse(TrainingRequest request) {
        Set<PaymentResponse> payments = new LinkedHashSet<>();
        if (request.getPayments() != null) {
            for (Payment payment : request.getPayments()) {
                payments.add(convertPaymentToResponse(payment, request.getId()));
            }
        }

        TrainingProgram program = request.getTrainingProgram();
        Organization organization = request.getOrganization();
        Account account = request.getAccount();

        return new TrainingRequestResponse(
                request.getId(),
                request.getParticipantCount(),
                request.getProgramPrice(),
                request.getNotes(),
                request.getStatus(),
                request.getRequestedAt(),
                program != null ? program.getId() : null,
                program != null ? program.getTitle() : null,
                organization != null ? organization.getId() : null,
                organization != null ? organization.getName() : null,
                account != null ? account.getId() : null,
                account != null ? account.getFullName() : null,
                payments);
    }

    private PaymentResponse convertPaymentToResponse(Payment payment, Long trainingRequestId) {
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

    public List<TrainingRequestResponse> getAllRequests() {
        return trainingRequestRepository.findAll().stream().map(this::convertToResponse).toList();
    }

    public TrainingRequestResponse getRequestById(Long id) {
        TrainingRequest request = trainingRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training request not found"));
        return convertToResponse(request);
    }

    public List<TrainingRequestResponse> getRequestsByOrganization(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException("Organization not found");
        }
        return trainingRequestRepository.findTrainingRequestsByOrganization_Id(organizationId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getRequestsByAccount(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ApiException("Account not found");
        }
        return trainingRequestRepository.findTrainingRequestsByAccount_Id(accountId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getRequestsByProgram(Long trainingProgramId) {
        if (!trainingProgramRepository.existsById(trainingProgramId)) {
            throw new ApiException("Training program not found");
        }
        return trainingRequestRepository.findTrainingRequestsByTrainingProgram_Id(trainingProgramId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getRequestsByStatus(String status) {
        validateStatus(status);
        return trainingRequestRepository.findTrainingRequestsByStatus(status).stream()
                .map(this::convertToResponse).toList();
    }

    @Transactional
    public void addRequest(TrainingRequestRequest request) {
        TrainingProgram program = trainingProgramRepository.findById(request.getTrainingProgramId())
                .orElseThrow(() -> new ApiException("Training program not found"));
        if (!Boolean.TRUE.equals(program.getIsActive())) {
            throw new ApiException("This training program is not active right now");
        }

        Organization organization = organizationRepository.findById(request.getBeneficiaryOrganizationId())
                .orElseThrow(() -> new ApiException("Organization not found"));
        Account account = accountRepository.findById(request.getCreatedByAccountId())
                .orElseThrow(() -> new ApiException("Account not found"));

        TrainingRequest trainingRequest = new TrainingRequest();
        applyFields(trainingRequest, request, program, organization, account);
        trainingRequest.setProgramPrice(program.getPrice());
        trainingRequest.setStatus("PENDING");
        trainingRequest.setRequestedAt(LocalDateTime.now());

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void updateRequest(Long id, TrainingRequestRequest request) {
        TrainingRequest trainingRequest = trainingRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training request not found"));

        TrainingProgram program = trainingProgramRepository.findById(request.getTrainingProgramId())
                .orElseThrow(() -> new ApiException("Training program not found"));
        Organization organization = organizationRepository.findById(request.getBeneficiaryOrganizationId())
                .orElseThrow(() -> new ApiException("Organization not found"));
        Account account = accountRepository.findById(request.getCreatedByAccountId())
                .orElseThrow(() -> new ApiException("Account not found"));

        applyFields(trainingRequest, request, program, organization, account);
        trainingRequest.setProgramPrice(program.getPrice());

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        validateStatus(status);

        TrainingRequest trainingRequest = trainingRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training request not found"));

        trainingRequest.setStatus(status);

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void deleteRequest(Long id) {
        TrainingRequest trainingRequest = trainingRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training request not found"));

        trainingRequestRepository.delete(trainingRequest);
    }

    private void applyFields(TrainingRequest trainingRequest, TrainingRequestRequest request,
                             TrainingProgram program, Organization organization, Account account) {
        trainingRequest.setTrainingProgram(program);
        trainingRequest.setOrganization(organization);
        trainingRequest.setAccount(account);
        trainingRequest.setParticipantCount(request.getParticipantCount());
        trainingRequest.setNotes(request.getNotes());
    }

    private void validateStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new ApiException("Invalid status");
        }
    }
}

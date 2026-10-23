package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.CancelTrainingRequest;
import org.fadhel.waeiplatform.dto.in.TrainingRequestRequest;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.repository.PaymentRepository;
import org.fadhel.waeiplatform.repository.TrainingProgramRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingRequestService {

    private static final List<TrainingRequestStatus> RELEASED_STATUSES =
            List.of(TrainingRequestStatus.REJECTED, TrainingRequestStatus.CANCELLED);

    private final TrainingRequestRepository trainingRequestRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ZoomMeetingService zoomMeetingService;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;

    private TrainingRequestResponse convertToResponse(TrainingRequest request) {
        Set<PaymentResponse> payments = new LinkedHashSet<>();
        if (request.getPayments() != null) {
            for (Payment payment : request.getPayments()) {
                payments.add(paymentService.toResponse(payment));
            }
        }

        TrainingProgram program = request.getTrainingProgram();
        Organization beneficiary = request.getOrganization();
        Organization provider = program != null ? program.getOrganization() : null;
        Account account = request.getAccount();

        return new TrainingRequestResponse(
                request.getId(),
                request.getParticipantCount(),
                request.getProgramPrice(),
                request.getDaysCount() != null ? request.getDaysCount() : 1,
                request.getDailyPrice() != null ? request.getDailyPrice() : request.getProgramPrice(),
                request.getHoursPerDay(),
                request.getNotes(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getRequestedAt(),
                request.getScheduledStart(),
                request.getScheduledEnd(),
                request.getAcceptedAt(),
                request.getCompletedAt(),
                request.getCancelledAt(),
                request.getCancellationReason(),
                program != null ? program.getId() : null,
                program != null ? program.getTitle() : null,
                provider != null ? provider.getId() : null,
                provider != null ? provider.getName() : null,
                beneficiary != null ? beneficiary.getId() : null,
                beneficiary != null ? beneficiary.getName() : null,
                account != null ? account.getId() : null,
                account != null ? account.getFullName() : null,
                payments,
                program != null ? program.getDeliveryMode() : null,
                request.getMeetingStatus() != null ? request.getMeetingStatus().name() : null,
                request.getZoomJoinUrl());
    }

    public List<TrainingRequestResponse> getAllRequests() {
        if (currentUser.isPlatformAdmin()) {
            return trainingRequestRepository.findAll().stream().map(this::convertToResponse).toList();
        }
        return getAccessibleRequests().stream().map(this::convertToResponse).toList();
    }

    public TrainingRequestResponse getRequestById(Long id) {
        TrainingRequest request = findRequest(id);
        accessControl.requireCanView(request);
        return convertToResponse(request);
    }

    public List<TrainingRequestResponse> getRequestsByOrganization(Long organizationId) {
        currentUser.requireOrganization(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException("Organization not found");
        }

        return trainingRequestRepository.findTrainingRequestsByOrganization_Id(organizationId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getRequestsByAccount(Long accountId) {
        if (!currentUser.isPlatformAdmin() && !accountId.equals(currentUser.getAccountId())) {
            throw new ApiException("You can only view your own training requests");
        }

        if (!accountRepository.existsById(accountId)) {
            throw new ApiException("Account not found");
        }

        return trainingRequestRepository.findTrainingRequestsByAccount_Id(accountId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getRequestsByProgram(Long trainingProgramId) {
        TrainingProgram program = trainingProgramRepository.findById(trainingProgramId)
                .orElseThrow(() -> new ApiException("Training program not found"));

        if (!currentUser.isPlatformAdmin()
                && (program.getOrganization() == null
                || !currentUser.belongsToOrganization(program.getOrganization().getId()))) {
            throw new ApiException("Only the provider organization can view the requests for this program");
        }

        return trainingRequestRepository.findTrainingRequestsByTrainingProgram_Id(trainingProgramId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getMyProviderRequests() {
        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            return List.of();
        }
        return trainingRequestRepository.findTrainingRequestsByTrainingProgram_Organization_Id(organizationId)
                .stream().map(this::convertToResponse).toList();
    }

    public List<TrainingRequestResponse> getRequestsByStatus(String status) {
        TrainingRequestStatus parsedStatus;
        try {
            parsedStatus = TrainingRequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid status");
        }

        if (currentUser.isPlatformAdmin()) {
            return trainingRequestRepository.findTrainingRequestsByStatus(parsedStatus).stream()
                    .map(this::convertToResponse).toList();
        }

        return getAccessibleRequests().stream()
                .filter(request -> request.getStatus() == parsedStatus)
                .map(this::convertToResponse).toList();
    }

    @Transactional
    public void addRequest(TrainingRequestRequest request) {
        Organization beneficiaryOrganization =
                resolveBeneficiaryOrganization(request.getBeneficiaryOrganizationId());
        Account account = resolveAccount(request.getCreatedByAccountId());

        TrainingProgram program = lockProgram(request.getTrainingProgramId());
        if (!Boolean.TRUE.equals(program.getIsActive())) {
            throw new ApiException("This training program is not active right now");
        }
        if (!accessControl.isProviderOrganization(program.getOrganization())) {
            throw new ApiException("This training program does not belong to a training provider");
        }
        accessControl.requireVerifiedOrganization(program.getOrganization());

        LocalDateTime scheduledEnd = validateSchedule(request, program);
        assertSlotAvailable(program.getId(), request.getScheduledStart(), scheduledEnd, -1L);

        TrainingRequest trainingRequest = new TrainingRequest();
        applyFields(trainingRequest, request, program, beneficiaryOrganization, account);
        trainingRequest.setStatus(TrainingRequestStatus.PENDING);
        trainingRequest.setRequestedAt(LocalDateTime.now());

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void updateRequest(Long id, TrainingRequestRequest request) {
        TrainingRequest trainingRequest = findRequest(id);
        accessControl.requireBeneficiaryOwner(trainingRequest);

        if (trainingRequest.getStatus() != TrainingRequestStatus.PENDING) {
            throw new ApiException("A training request can only be edited while it is pending");
        }

        TrainingProgram program = lockProgram(request.getTrainingProgramId());
        if (!Boolean.TRUE.equals(program.getIsActive())) {
            throw new ApiException("This training program is not active right now");
        }

        LocalDateTime scheduledEnd = validateSchedule(request, program);
        assertSlotAvailable(program.getId(), request.getScheduledStart(), scheduledEnd, id);

        applyFields(trainingRequest, request, program, trainingRequest.getOrganization(), trainingRequest.getAccount());

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void acceptRequest(Long id) {
        TrainingRequest trainingRequest = findRequest(id);
        accessControl.requireProviderOwner(trainingRequest);

        if (trainingRequest.getStatus() != TrainingRequestStatus.PENDING) {
            throw new ApiException("Only a pending request can be accepted");
        }

        if (trainingRequest.getScheduledStart() == null || trainingRequest.getScheduledEnd() == null) {
            throw new ApiException("This request has no training schedule. Ask the beneficiary to update it first.");
        }

        TrainingProgram program = lockProgram(trainingRequest.getTrainingProgram().getId());
        assertSlotAvailable(program.getId(), trainingRequest.getScheduledStart(),
                trainingRequest.getScheduledEnd(), trainingRequest.getId());

        transition(trainingRequest, TrainingRequestStatus.ACCEPTED);
        trainingRequest.setAcceptedAt(LocalDateTime.now());

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public void rejectRequest(Long id, CancelTrainingRequest request) {
        TrainingRequest trainingRequest = findRequest(id);
        accessControl.requireProviderOwner(trainingRequest);

        if (trainingRequest.getStatus() != TrainingRequestStatus.PENDING) {
            throw new ApiException("Only a pending request can be rejected");
        }

        transition(trainingRequest, TrainingRequestStatus.REJECTED);
        trainingRequest.setCancelledAt(LocalDateTime.now());
        trainingRequest.setCancellationReason(
                request != null && request.getReason() != null ? request.getReason() : "Rejected by the provider");

        trainingRequestRepository.save(trainingRequest);
    }

    @Transactional
    public int advanceScheduledTrainingRequests() {
        LocalDateTime now = LocalDateTime.now();
        Set<TrainingRequest> candidates = new LinkedHashSet<>(
                trainingRequestRepository.findByStatusAndScheduledStartLessThanEqual(
                        TrainingRequestStatus.CONFIRMED, now));
        candidates.addAll(trainingRequestRepository.findByStatusAndScheduledEndLessThanEqual(
                TrainingRequestStatus.IN_PROGRESS, now));

        int changed = 0;
        for (TrainingRequest trainingRequest : candidates) {
            boolean updated = false;
            if (trainingRequest.getStatus() == TrainingRequestStatus.CONFIRMED
                    && trainingRequest.getScheduledStart() != null
                    && !trainingRequest.getScheduledStart().isAfter(now)) {
                transition(trainingRequest, TrainingRequestStatus.IN_PROGRESS);
                updated = true;
            }
            if (trainingRequest.getStatus() == TrainingRequestStatus.IN_PROGRESS
                    && trainingRequest.getScheduledEnd() != null
                    && !trainingRequest.getScheduledEnd().isAfter(now)) {
                transition(trainingRequest, TrainingRequestStatus.COMPLETED);
                trainingRequest.setCompletedAt(trainingRequest.getScheduledEnd());
                updated = true;
            }
            if (updated) {
                changed++;
            }
        }

        if (changed > 0) {
            trainingRequestRepository.saveAll(candidates);
        }
        return changed;
    }

    @Transactional
    public void cancelRequest(Long id, CancelTrainingRequest request) {
        TrainingRequest trainingRequest = findRequest(id);

        if (!currentUser.isPlatformAdmin()) {
            accessControl.requireBeneficiaryOwner(trainingRequest);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean startedByTime = trainingRequest.getScheduledStart() != null
                && !trainingRequest.getScheduledStart().isAfter(now);
        if (trainingRequest.getStatus() == TrainingRequestStatus.IN_PROGRESS
                || trainingRequest.getStatus() == TrainingRequestStatus.COMPLETED
                || (trainingRequest.getStatus() == TrainingRequestStatus.CONFIRMED && startedByTime)) {
            throw new ApiException("A training request cannot be cancelled or refunded after the training has started");
        }

        if (!trainingRequest.getStatus().canTransitionTo(TrainingRequestStatus.CANCELLED)) {
            throw new ApiException("A " + trainingRequest.getStatus() + " request cannot be cancelled");
        }

        Payment paidPayment = paymentRepository
                .findFirstByTrainingRequest_IdAndStatusOrderByCreatedAtDesc(
                        trainingRequest.getId(), PaymentStatus.PAID)
                .orElse(null);

        if (paidPayment != null) {
            paymentService.refundForCancellation(paidPayment);
        } else if (trainingRequest.getStatus() == TrainingRequestStatus.CONFIRMED) {
            throw new ApiException("The paid payment for this request was not found");
        }

        paymentService.cancelPendingPayments(trainingRequest);

        transition(trainingRequest, TrainingRequestStatus.CANCELLED);
        trainingRequest.setCancelledAt(LocalDateTime.now());
        trainingRequest.setCancellationReason(
                request != null && request.getReason() != null ? request.getReason() : "Cancelled by the beneficiary");

        trainingRequestRepository.save(trainingRequest);
        zoomMeetingService.deleteForRequest(trainingRequest);
    }

    @Transactional
    public void deleteRequest(Long id) {
        TrainingRequest trainingRequest = findRequest(id);

        if (!currentUser.isPlatformAdmin()) {
            accessControl.requireBeneficiaryOwner(trainingRequest);
        }

        if (trainingRequest.getStatus() != TrainingRequestStatus.PENDING
                && trainingRequest.getStatus() != TrainingRequestStatus.REJECTED) {
            throw new ApiException("Only pending or rejected requests can be deleted");
        }

        trainingRequestRepository.delete(trainingRequest);
    }

    private TrainingRequest findRequest(Long id) {
        return trainingRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training request not found"));
    }

    private TrainingProgram lockProgram(Long programId) {
        return trainingProgramRepository.findByIdForUpdate(programId)
                .orElseThrow(() -> new ApiException("Training program not found"));
    }

    private List<TrainingRequest> getAccessibleRequests() {
        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            return List.of();
        }
        return trainingRequestRepository.findAccessibleByOrganization(organizationId);
    }

    private Organization resolveBeneficiaryOrganization(Long requestedOrganizationId) {
        Long organizationId = requestedOrganizationId;

        if (!currentUser.isPlatformAdmin()) {
            organizationId = currentUser.getOrganizationId();
        }

        if (organizationId == null) {
            throw new ApiException("A beneficiary organization is required");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found"));

        if (!accessControl.isBeneficiaryOrganization(organization)) {
            throw new ApiException("Training requests can only be created by a beneficiary organization");
        }

        accessControl.requireVerifiedOrganization(organization);

        return organization;
    }

    private Account resolveAccount(Long requestedAccountId) {
        Long accountId = requestedAccountId;

        if (!currentUser.isPlatformAdmin() || accountId == null) {
            accountId = currentUser.getAccountId();
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException("Account not found"));
    }

    private void applyFields(TrainingRequest trainingRequest, TrainingRequestRequest request,
                             TrainingProgram program, Organization organization, Account account) {
        int daysCount = request.getDaysCount();
        LocalDateTime scheduledEnd = TrainingRequestPricing.end(
                request.getScheduledStart(), daysCount, program.getHoursPerDay());

        trainingRequest.setTrainingProgram(program);
        trainingRequest.setOrganization(organization);
        trainingRequest.setAccount(account);
        trainingRequest.setParticipantCount(request.getParticipantCount());
        trainingRequest.setDaysCount(daysCount);
        trainingRequest.setDailyPrice(program.getPrice());
        trainingRequest.setHoursPerDay(program.getHoursPerDay());
        trainingRequest.setProgramPrice(TrainingRequestPricing.total(program.getPrice(), daysCount));
        trainingRequest.setScheduledStart(request.getScheduledStart());
        trainingRequest.setScheduledEnd(scheduledEnd);
        trainingRequest.setNotes(request.getNotes());
    }

    private LocalDateTime validateSchedule(TrainingRequestRequest request, TrainingProgram program) {
        LocalDateTime start = request.getScheduledStart();
        Integer daysCount = request.getDaysCount();

        if (start == null) {
            throw new ApiException("The training date and time are required");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ApiException("The training start time must be in the future");
        }
        if (daysCount == null || daysCount < 1) {
            throw new ApiException("The number of training days must be at least 1");
        }
        if (program.getHoursPerDay() == null || program.getHoursPerDay().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("This training program does not have a fixed daily duration yet");
        }
        if (program.getMaxDays() == null) {
            throw new ApiException("This training program does not have a maximum duration yet");
        }
        if (daysCount > program.getMaxDays()) {
            throw new ApiException("The selected number of training days exceeds the program maximum");
        }

        LocalDateTime end = TrainingRequestPricing.end(start, daysCount, program.getHoursPerDay());
        if (!end.isAfter(start)) {
            throw new ApiException("The training end time must be after the start time");
        }
        return end;
    }

    private void assertSlotAvailable(Long programId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        long conflicts = trainingRequestRepository.countOverlapping(
                programId, start, end, excludeId, RELEASED_STATUSES);

        if (conflicts > 0) {
            throw new ApiException("The selected date and time is already booked for this program");
        }
    }

    private void transition(TrainingRequest trainingRequest, TrainingRequestStatus next) {
        if (!trainingRequest.getStatus().canTransitionTo(next)) {
            throw new ApiException("Cannot change status from " + trainingRequest.getStatus() + " to " + next);
        }
        trainingRequest.setStatus(next);
    }
}

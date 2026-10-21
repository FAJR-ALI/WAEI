package org.fadhel.waeiplatform.security;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationType;
import org.fadhel.waeiplatform.model.Payment;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessControl {

    private final CurrentUser currentUser;

    public boolean canView(TrainingRequest request) {
        if (currentUser.isPlatformAdmin()) {
            return true;
        }
        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            return false;
        }
        return organizationId.equals(beneficiaryOrganizationId(request))
                || organizationId.equals(providerOrganizationId(request));
    }

    public void requireCanView(TrainingRequest request) {
        if (!canView(request)) {
            throw new ApiException("You do not have access to this training request");
        }
    }

    public void requireBeneficiaryOwner(TrainingRequest request) {
        if (currentUser.isPlatformAdmin()) {
            return;
        }
        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null || !organizationId.equals(beneficiaryOrganizationId(request))) {
            throw new ApiException("Only the beneficiary organization can perform this action");
        }
    }

    public void requireProviderOwner(TrainingRequest request) {
        if (currentUser.isPlatformAdmin()) {
            return;
        }
        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null || !organizationId.equals(providerOrganizationId(request))) {
            throw new ApiException("Only the training provider can perform this action");
        }
    }

    public boolean canView(Payment payment) {
        return payment.getTrainingRequest() != null && canView(payment.getTrainingRequest());
    }

    public void requireCanView(Payment payment) {
        if (!canView(payment)) {
            throw new ApiException("You do not have access to this payment");
        }
    }

    public boolean canManageProgram(TrainingProgram program) {
        if (currentUser.isPlatformAdmin()) {
            return true;
        }
        Long organizationId = currentUser.getOrganizationId();
        return organizationId != null
                && program.getOrganization() != null
                && organizationId.equals(program.getOrganization().getId());
    }

    public void requireCanManageProgram(TrainingProgram program) {
        if (!canManageProgram(program)) {
            throw new ApiException("Only the provider organization that owns this program can manage it");
        }
    }

    public boolean isBeneficiaryOrganization(Organization organization) {
        return organization != null
                && (organization.getType() == OrganizationType.BENEFICIARY || organization.getType() == OrganizationType.BOTH);
    }

    public boolean isProviderOrganization(Organization organization) {
        return organization != null
                && (organization.getType() == OrganizationType.PROVIDER || organization.getType() == OrganizationType.BOTH);
    }

    private Long beneficiaryOrganizationId(TrainingRequest request) {
        return request.getOrganization() != null ? request.getOrganization().getId() : null;
    }

    private Long providerOrganizationId(TrainingRequest request) {
        return request.getTrainingProgram() != null && request.getTrainingProgram().getOrganization() != null
                ? request.getTrainingProgram().getOrganization().getId()
                : null;
    }
}

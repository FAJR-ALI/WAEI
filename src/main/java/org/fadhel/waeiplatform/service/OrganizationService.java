package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.OrganizationDTOIn;
import org.fadhel.waeiplatform.dto.in.OrganizationRegistrationRequest;
import org.fadhel.waeiplatform.dto.in.OrganizationResubmitRequest;
import org.fadhel.waeiplatform.dto.out.OrganizationDTOOut;
import org.fadhel.waeiplatform.dto.out.OrganizationRegistrationDTOOut;
import org.fadhel.waeiplatform.dto.out.OrganizationRegistrationResponse;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationType;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentStorageService documentStorageService;
    private final CurrentUser currentUser;

    private OrganizationDTOOut convertToDTO(Organization organization) {
        OrganizationDTOOut dto = new OrganizationDTOOut();

        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setType(organization.getType());
        dto.setSector(organization.getSector());
        dto.setEmail(organization.getEmail());
        dto.setPhoneNumber(organization.getPhoneNumber());
        dto.setVerificationStatus(organization.getVerificationStatus());
        dto.setRejectionReason(organization.getRejectionReason());
        dto.setCreatedAt(organization.getCreatedAt());
        dto.setSubmittedAt(organization.getSubmittedAt());
        dto.setVerifiedAt(organization.getVerifiedAt());
        dto.setDocumentReference(organization.getDocumentReference());
        dto.setDocumentOriginalName(organization.getDocumentOriginalName());
        dto.setDocumentContentType(organization.getDocumentContentType());
        dto.setDocumentSize(organization.getDocumentSize());
        dto.setDocumentAvailable(organization.getDocumentStoredName() != null);
        dto.setDocumentDownloadUrl(documentDownloadUrl(organization));

        return dto;
    }

    private OrganizationRegistrationDTOOut convertToRegistrationDTO(Organization organization) {
        Account adminAccount = findOrganizationAdmin(organization.getId());

        OrganizationRegistrationDTOOut dto = new OrganizationRegistrationDTOOut();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setType(organization.getType());
        dto.setSector(organization.getSector());
        dto.setEmail(organization.getEmail());
        dto.setPhoneNumber(organization.getPhoneNumber());
        dto.setDocumentReference(organization.getDocumentReference());
        dto.setVerificationStatus(organization.getVerificationStatus());
        dto.setRejectionReason(organization.getRejectionReason());
        dto.setCreatedAt(organization.getCreatedAt());
        dto.setSubmittedAt(organization.getSubmittedAt());
        dto.setVerifiedAt(organization.getVerifiedAt());

        dto.setDocumentAvailable(organization.getDocumentStoredName() != null);
        dto.setDocumentOriginalName(organization.getDocumentOriginalName());
        dto.setDocumentContentType(organization.getDocumentContentType());
        dto.setDocumentSize(organization.getDocumentSize());
        dto.setDocumentDownloadUrl(documentDownloadUrl(organization));

        if (adminAccount != null) {
            dto.setAdminFullName(adminAccount.getFullName());
            dto.setAdminEmail(adminAccount.getEmail());
            dto.setAdminAccountStatus(adminAccount.getStatus());
        }

        return dto;
    }

    public List<OrganizationDTOOut> getAllOrganizations() {
        return organizationRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    public OrganizationDTOOut getOrganizationById(Long id) {
        return convertToDTO(findOrganization(id));
    }

    public List<OrganizationRegistrationDTOOut> getRegistrations(OrganizationVerificationStatus status) {
        OrganizationVerificationStatus requestedStatus =
                status != null ? status : OrganizationVerificationStatus.PENDING;
        return organizationRepository.findByVerificationStatusOrderBySubmittedAtAsc(requestedStatus)
                .stream()
                .map(this::convertToRegistrationDTO)
                .toList();
    }

    @Transactional
    public OrganizationRegistrationResponse registerOrganization(OrganizationRegistrationRequest request) {
        validateRegistrationType(request.getType());

        if (organizationRepository.findOrganizationByEmail(request.getEmail()) != null) {
            throw new ApiException("Organization email already exists");
        }
        if (accountRepository.findAccountByEmail(request.getAdminEmail()) != null) {
            throw new ApiException("Organization administrator email already exists");
        }

        DocumentStorageService.StoredDocument storedDocument =
                documentStorageService.store(request.getDocument());
        deleteOnRollback(storedDocument.storedName());

        Organization organization = new Organization();
        applyRegistrationFields(organization, request.getName(), request.getType(), request.getSector(),
                request.getEmail(), request.getPhoneNumber(), request.getDocumentReference());
        organization.setVerificationStatus(OrganizationVerificationStatus.PENDING);
        organization.setSubmittedAt(LocalDateTime.now());
        organization.setDocumentOriginalName(storedDocument.originalName());
        organization.setDocumentStoredName(storedDocument.storedName());
        organization.setDocumentContentType(storedDocument.contentType());
        organization.setDocumentSize(storedDocument.size());
        organizationRepository.save(organization);

        Account account = new Account();
        account.setFullName(request.getAdminFullName());
        account.setEmail(request.getAdminEmail());
        account.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        account.setRole("ORG_ADMIN");
        account.setStatus("PENDING");
        account.setOrganization(organization);
        accountRepository.save(account);

        return new OrganizationRegistrationResponse(
                organization.getId(),
                organization.getName(),
                organization.getVerificationStatus(),
                "Registration submitted successfully. The platform administrator will review your document.");
    }

    @Transactional
    public OrganizationRegistrationResponse resubmitRegistration(Long id, OrganizationResubmitRequest request) {
        Organization organization = findOrganization(id);

        if (organization.getVerificationStatus() != OrganizationVerificationStatus.REJECTED) {
            throw new ApiException("Only rejected registrations can be resubmitted");
        }

        Account adminAccount = accountRepository.findAccountByEmail(request.getAdminEmail());
        if (adminAccount == null
                || adminAccount.getOrganization() == null
                || !organization.getId().equals(adminAccount.getOrganization().getId())
                || !"ORG_ADMIN".equals(adminAccount.getRole())
                || !passwordEncoder.matches(request.getPassword(), adminAccount.getPassword())) {
            throw new ApiException("Invalid organization administrator credentials");
        }

        validateRegistrationType(request.getType());

        Organization existingOrganization = organizationRepository.findOrganizationByEmail(request.getEmail());
        if (existingOrganization != null && !existingOrganization.getId().equals(id)) {
            throw new ApiException("Organization email already exists");
        }

        DocumentStorageService.StoredDocument newDocument = null;
        if (request.getDocument() != null && !request.getDocument().isEmpty()) {
            newDocument = documentStorageService.store(request.getDocument());
            deleteOnRollback(newDocument.storedName());
        }

        String previousDocument = organization.getDocumentStoredName();

        applyRegistrationFields(organization, request.getName(), request.getType(), request.getSector(),
                request.getEmail(), request.getPhoneNumber(), request.getDocumentReference());
        organization.setVerificationStatus(OrganizationVerificationStatus.PENDING);
        organization.setRejectionReason(null);
        organization.setSubmittedAt(LocalDateTime.now());

        if (newDocument != null) {
            organization.setDocumentOriginalName(newDocument.originalName());
            organization.setDocumentStoredName(newDocument.storedName());
            organization.setDocumentContentType(newDocument.contentType());
            organization.setDocumentSize(newDocument.size());

            if (previousDocument != null && !previousDocument.equals(newDocument.storedName())) {
                deleteAfterCommit(previousDocument);
            }
        }

        organizationRepository.save(organization);

        return new OrganizationRegistrationResponse(
                organization.getId(),
                organization.getName(),
                organization.getVerificationStatus(),
                "Registration updated and resubmitted. The platform administrator will review it again.");
    }

    @Transactional
    public OrganizationRegistrationResponse verifyOrganization(Long id) {
        Organization organization = findOrganization(id);

        if (organization.getVerificationStatus() == OrganizationVerificationStatus.VERIFIED) {
            throw new ApiException("Organization is already verified");
        }

        organization.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);
        organization.setRejectionReason(null);
        organization.setVerifiedAt(LocalDateTime.now());
        organization.setVerifiedByAccountId(currentUser.getAccountId());

        List<Account> accounts = accountRepository.findAccountsByOrganization_Id(id);
        accounts.stream()
                .filter(account -> "ORG_ADMIN".equals(account.getRole()))
                .filter(account -> "PENDING".equals(account.getStatus()))
                .forEach(account -> account.setStatus("ACTIVE"));

        organizationRepository.save(organization);

        return new OrganizationRegistrationResponse(
                organization.getId(),
                organization.getName(),
                organization.getVerificationStatus(),
                "Organization verified and its administrator account activated.");
    }

    @Transactional
    public OrganizationRegistrationResponse rejectOrganization(Long id, String reason) {
        Organization organization = findOrganization(id);

        if (organization.getVerificationStatus() != OrganizationVerificationStatus.PENDING) {
            throw new ApiException("Only pending registrations can be rejected");
        }

        organization.setVerificationStatus(OrganizationVerificationStatus.REJECTED);
        organization.setRejectionReason(reason);
        organization.setVerifiedAt(null);
        organization.setVerifiedByAccountId(null);

        List<Account> accounts = accountRepository.findAccountsByOrganization_Id(id);
        accounts.stream()
                .filter(account -> "ORG_ADMIN".equals(account.getRole()))
                .filter(account -> "ACTIVE".equals(account.getStatus()))
                .forEach(account -> account.setStatus("SUSPENDED"));

        organizationRepository.save(organization);

        return new OrganizationRegistrationResponse(
                organization.getId(),
                organization.getName(),
                organization.getVerificationStatus(),
                "Organization registration rejected.");
    }

    public DocumentDownload getOrganizationDocument(Long id) {
        Organization organization = findOrganization(id);

        if (organization.getDocumentStoredName() == null || organization.getDocumentStoredName().isBlank()) {
            throw new ApiException("No document was uploaded for this organization");
        }

        Resource resource = documentStorageService.load(organization.getDocumentStoredName());
        String originalName = organization.getDocumentOriginalName() != null
                ? organization.getDocumentOriginalName()
                : "organization-document";
        String contentType = organization.getDocumentContentType() != null
                ? organization.getDocumentContentType()
                : "application/octet-stream";

        return new DocumentDownload(resource, originalName, contentType);
    }

    @Transactional
    public void updateOrganization(Long id, OrganizationDTOIn dto) {
        Organization organization = findOrganization(id);

        Organization existingOrganization = organizationRepository.findOrganizationByEmail(dto.getEmail());
        if (existingOrganization != null && !existingOrganization.getId().equals(id)) {
            throw new ApiException("Organization email already exists");
        }

        organization.setName(dto.getName());
        organization.setType(dto.getType());
        organization.setSector(dto.getSector());
        organization.setEmail(dto.getEmail());
        organization.setPhoneNumber(dto.getPhoneNumber());
        organization.setDocumentReference(dto.getDocumentReference());

        organizationRepository.save(organization);
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Organization organization = findOrganization(id);
        String storedDocument = organization.getDocumentStoredName();

        accountRepository.deleteAll(accountRepository.findAccountsByOrganization_Id(id));
        organizationRepository.delete(organization);

        if (storedDocument != null) {
            deleteAfterCommit(storedDocument);
        }
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Organization not found"));
    }

    private Account findOrganizationAdmin(Long organizationId) {
        return accountRepository.findAccountsByOrganization_Id(organizationId).stream()
                .filter(account -> "ORG_ADMIN".equals(account.getRole()))
                .findFirst()
                .orElse(null);
    }

    private void validateRegistrationType(OrganizationType type) {
        if (type != OrganizationType.BENEFICIARY && type != OrganizationType.PROVIDER) {
            throw new ApiException("Registration type must be BENEFICIARY or PROVIDER");
        }
    }

    private void applyRegistrationFields(Organization organization, String name, OrganizationType type,
                                         String sector, String email, String phoneNumber,
                                         String documentReference) {
        organization.setName(name);
        organization.setType(type);
        organization.setSector(sector);
        organization.setEmail(email);
        organization.setPhoneNumber(phoneNumber);
        organization.setDocumentReference(documentReference);
    }

    private String documentDownloadUrl(Organization organization) {
        if (organization.getId() == null || organization.getDocumentStoredName() == null) {
            return null;
        }
        return "/api/v1/organization/" + organization.getId() + "/document";
    }

    private void deleteOnRollback(String storedName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    documentStorageService.deleteQuietly(storedName);
                }
            }
        });
    }

    private void deleteAfterCommit(String storedName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            documentStorageService.deleteQuietly(storedName);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    documentStorageService.deleteQuietly(storedName);
                }
            }
        });
    }

    public record DocumentDownload(Resource resource, String originalName, String contentType) {
    }
}

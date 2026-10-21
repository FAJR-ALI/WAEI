package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.AccountDTOIn;
import org.fadhel.waeiplatform.dto.in.AccountStatusRequest;
import org.fadhel.waeiplatform.dto.in.AccountUpdateDTOIn;
import org.fadhel.waeiplatform.dto.out.AccountDTOOut;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    private AccountDTOOut convertToDTO(Account account) {
        Organization organization = account.getOrganization();

        AccountDTOOut dto = new AccountDTOOut();
        dto.setId(account.getId());
        dto.setFullName(account.getFullName());
        dto.setEmail(account.getEmail());
        dto.setRole(account.getRole());
        dto.setStatus(account.getStatus());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setOrganizationId(organization != null ? organization.getId() : null);
        dto.setOrganizationName(organization != null ? organization.getName() : null);
        dto.setOrganizationType(organization != null ? organization.getType().name() : null);
        return dto;
    }

    public List<AccountDTOOut> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    public List<AccountDTOOut> getAccountsByOrganization(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException("Organization not found");
        }
        return accountRepository.findAccountsByOrganization_Id(organizationId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public void addAccount(AccountDTOIn dto) {
        Account existingAccount = accountRepository.findAccountByEmail(dto.getEmail());
        if (existingAccount != null) {
            throw new ApiException("Email already exists");
        }

        Organization organization = null;
        if ("ORG_ADMIN".equals(dto.getRole())) {
            if (dto.getOrganizationId() == null) {
                throw new ApiException("An organization is required for an organization administrator");
            }
            organization = organizationRepository.findById(dto.getOrganizationId())
                    .orElseThrow(() -> new ApiException("Organization not found"));
        }

        Account account = new Account();
        account.setFullName(dto.getFullName());
        account.setEmail(dto.getEmail());
        account.setPassword(passwordEncoder.encode(dto.getPassword()));
        account.setRole(dto.getRole());
        account.setStatus("PENDING");
        account.setOrganization(organization);

        accountRepository.save(account);
    }

    @Transactional
    public void updateAccount(Long id, AccountUpdateDTOIn dto) {
        Account oldAccount = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException("Account not found"));
        Account existingAccount = accountRepository.findAccountByEmail(dto.getEmail());

        if (existingAccount != null && !existingAccount.getId().equals(id)) {
            throw new ApiException("Email already exists");
        }

        oldAccount.setFullName(dto.getFullName());
        oldAccount.setEmail(dto.getEmail());

        accountRepository.save(oldAccount);
    }

    @Transactional
    public void changeStatus(Long id, AccountStatusRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException("Account not found"));

        account.setStatus(request.getStatus());
        accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException("Account not found"));

        accountRepository.delete(account);
    }
}

package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.AccountDTOIn;
import org.fadhel.waeiplatform.dto.in.AccountUpdateDTOIn;
import org.fadhel.waeiplatform.dto.out.AccountDTOOut;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    // CONVERT ENTITY TO DTO OUT
    private AccountDTOOut convertToDTO(Account account) {

        AccountDTOOut dto = new AccountDTOOut();

        dto.setId(account.getId());
        dto.setFullName(account.getFullName());
        dto.setEmail(account.getEmail());
        dto.setRole(account.getRole());
        dto.setStatus(account.getStatus());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }

    // GET ALL
    public List<AccountDTOOut> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    // ADD
    public void addAccount(AccountDTOIn dto) {

        Account existingAccount = accountRepository.findAccountByEmail(dto.getEmail());
        if (existingAccount != null) {
            throw new ApiException("Email already exists");
        }

        Account account = new Account();

        account.setFullName(dto.getFullName());
        account.setEmail(dto.getEmail());
        account.setPassword(
                passwordEncoder.encode(dto.getPassword()));
        account.setRole(dto.getRole());
        account.setStatus("PENDING");

        accountRepository.save(account);
    }

    // UPDATE
    public void updateAccount(Long id, AccountUpdateDTOIn dto) {

        Account oldAccount = accountRepository.findById(id).orElseThrow(() -> new ApiException("Account not found"));
        Account existingAccount = accountRepository.findAccountByEmail(dto.getEmail());

        if (existingAccount != null && !existingAccount.getId().equals(id)) {
            throw new ApiException("Email already exists");
        }

        oldAccount.setFullName(dto.getFullName());
        oldAccount.setEmail(dto.getEmail());

        accountRepository.save(oldAccount);
    }

    // DELETE
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new ApiException("Account not found"));
        accountRepository.delete(account);
    }
}

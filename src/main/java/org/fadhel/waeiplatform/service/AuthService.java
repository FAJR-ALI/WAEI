package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.LoginRequest;
import org.fadhel.waeiplatform.dto.out.AuthResponse;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.fadhel.waeiplatform.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findAccountByEmail(request.getEmail());

        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new ApiException("Invalid email or password");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("This account is not active yet. Please contact the platform administrator.");
        }

        return toResponse(account, jwtService.generateToken(account));
    }

    public AuthResponse me() {
        Account account = accountRepository.findById(currentUser.getAccountId())
                .orElseThrow(() -> new ApiException("Account not found"));
        return toResponse(account, null);
    }

    private AuthResponse toResponse(Account account, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType(token != null ? "Bearer" : null)
                .accountId(account.getId())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .role(account.getRole())
                .status(account.getStatus())
                .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
                .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
                .organizationType(account.getOrganization() != null ? account.getOrganization().getType().name() : null)
                .build();
    }
}

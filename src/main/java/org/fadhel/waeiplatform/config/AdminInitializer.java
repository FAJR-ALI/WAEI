package org.fadhel.waeiplatform.config;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@waei.sa}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.admin.full-name:Platform Administrator}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        if (accountRepository.findAccountByEmail(adminEmail) != null) {
            return;
        }

        Account account = new Account();
        account.setFullName(adminFullName);
        account.setEmail(adminEmail);
        account.setPassword(passwordEncoder.encode(adminPassword));
        account.setRole("PLATFORM_ADMIN");
        account.setStatus("ACTIVE");
        accountRepository.save(account);
    }
}

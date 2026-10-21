package org.fadhel.waeiplatform.config;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrganizationStatusInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Organization> organizations = organizationRepository.findByVerificationStatusIsNull();
        if (organizations.isEmpty()) {
            return;
        }

        organizations.forEach(organization ->
                organization.setVerificationStatus(OrganizationVerificationStatus.PENDING));
        organizationRepository.saveAll(organizations);
    }
}

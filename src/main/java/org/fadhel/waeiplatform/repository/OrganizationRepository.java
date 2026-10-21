package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Organization findOrganizationByEmail(String email);

    List<Organization> findByVerificationStatusOrderBySubmittedAtAsc(OrganizationVerificationStatus verificationStatus);

    List<Organization> findByVerificationStatusIsNull();

}

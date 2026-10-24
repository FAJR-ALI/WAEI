package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Explicit FOR UPDATE works with MariaDB as well as MySQL (without an OF alias clause).
    @org.springframework.data.jpa.repository.Query(
            value = "select * from organization where id = :id for update", nativeQuery = true)
    java.util.Optional<Organization> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    Organization findOrganizationByEmail(String email);

    List<Organization> findByVerificationStatusOrderBySubmittedAtAsc(OrganizationVerificationStatus verificationStatus);

    List<Organization> findByVerificationStatusIsNull();

}

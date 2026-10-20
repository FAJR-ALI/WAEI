package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {

    Organization findOrganizationByEmail(String email);

}

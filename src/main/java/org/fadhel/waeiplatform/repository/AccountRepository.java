package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findAccountByEmail(String email);

    List<Account> findAccountsByOrganization_Id(Long organizationId);
}

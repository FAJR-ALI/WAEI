package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Account findAccountByEmail(String email);
}

package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Account;

@Repository
public interface AccountRepo extends JpaRepository<Account, Long> {
    boolean existsByAuthenticator(String accountAuth);
    Optional<Account> findByAuthenticator(String accountAuth);
    Optional<Account> findByCustomer_Cin(String cin);
    Optional<Account> findByRib(String rib);
    boolean existsByRib(String rib);
    boolean existsByAccountNumber(String accountNumber);
}

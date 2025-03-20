package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.A2ATransfer;

@Repository
public interface A2ATransferRepo extends JpaRepository<A2ATransfer, Long>{
    List<A2ATransfer> findByAccountDebitRib(String rib);
    List<A2ATransfer> findByAccountCreditRib(String rib);
    List<A2ATransfer> findAllByOrderByDateTransactionDesc();
    List<A2ATransfer> findByAccountDebitRibOrAccountCreditRibAndDateTransactionAfter(
        String accountRib, String accountRib2, LocalDateTime dateTransaction, Sort sort
    );
}

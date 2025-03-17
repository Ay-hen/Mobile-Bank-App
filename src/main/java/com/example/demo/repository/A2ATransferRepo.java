package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.A2ATransfer;

@Repository
public interface A2ATransferRepo extends JpaRepository<A2ATransfer, String>{
    List<A2ATransfer> findByAccountDebitRib(String rib);
    List<A2ATransfer> findByAccountCreditRib(String rib);
    List<A2ATransfer> findAllByOrderByDateTransactionDesc();
    
}

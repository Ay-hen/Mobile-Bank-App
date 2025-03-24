package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.enums.TransactionStatus;
import com.example.demo.model.Account;
import com.example.demo.model.QRCode;

public interface QRCodeRepo extends JpaRepository<QRCode, Long> {
    List<QRCode> findBySenderOrReceiver(Account sender, Account receiver);
    List<QRCode> findAll(Sort sort);
    List<QRCode> findByExpirationDateBeforeAndTransactionStatus(
        LocalDateTime expirationDate, 
        TransactionStatus status
    );
}
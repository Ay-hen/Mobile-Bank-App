package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.demo.enums.QRCodeStatus;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;
import com.example.demo.model.A2ATransfer;
import com.example.demo.model.Account;
import com.example.demo.model.ActivityTracking;
import com.example.demo.model.Customer;
import com.example.demo.model.QRCode;
import com.example.demo.repository.A2ATransferRepo;
import com.example.demo.repository.AccountRepo;
import com.example.demo.repository.ActivityTrackingRepo;
import com.example.demo.repository.QRCodeRepo;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TransactionService {
    @Autowired
    private A2ATransferRepo a2aTransferRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private ActivityTrackingRepo activityTrackingRepo;

    @Autowired
    private HttpServletRequest request; 
    
    @Autowired
    private QRCodeRepo qrCodeRepo;

    public boolean existsByRib(String rib) {
        return accountRepo.existsByRib(rib);
    }

    public String getRib(String authenticator) {
        return accountRepo.findByAuthenticator(authenticator)
                .orElseThrow(() -> new RuntimeException("Account not found"))
                .getRib();
    }

    public void sendAmountMoney(String ribSender, String ribReceiver, BigDecimal amount, Customer user) {
        var sender = accountRepo.findByRib(ribSender)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        var receiver = accountRepo.findByRib(ribReceiver)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        sender.setAmount(sender.getAmount().subtract(amount));
        receiver.setAmount(receiver.getAmount().add(amount));

        accountRepo.save(sender);
        accountRepo.save(receiver);

        A2ATransfer a2aTransfer = A2ATransfer.builder()
                .amount(amount)
                .accountDebit(sender)
                .accountCredit(receiver)
                .transactionType(TransactionType.TRANSFER)
                .build();

        a2aTransferRepo.save(a2aTransfer);

        logActivity(user, "TRANSFER", "Sent " + amount + " to " + ribReceiver);
        logActivity(user, "TRANSFER", "Received " + amount + " from " + ribSender);
    }

    public void depositAmountMoney(String rib, BigDecimal amount, Customer user) {
        var account = accountRepo.findByRib(rib)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setAmount(account.getAmount().add(amount));
        accountRepo.save(account);

        A2ATransfer a2aTransfer = A2ATransfer.builder()
                .amount(amount)
                .accountDebit(account)
                .accountCreditRib(rib)
                .transactionType(TransactionType.DEPOSIT)
                .build();

        a2aTransferRepo.save(a2aTransfer);

        logActivity(user, "DEPOSIT", "Deposited " + amount);
    }

    public List<A2ATransfer> getAllSenderTransactions(String rib) {
        return a2aTransferRepo.findByAccountDebitRib(rib);
    }

    public List<A2ATransfer> getAllReceiverTransactions(String rib) {
        return a2aTransferRepo.findByAccountCreditRib(rib);
    }

    public List<A2ATransfer> getTransactionsHistoryNewest() {
        return a2aTransferRepo.findAll(Sort.by(Sort.Direction.DESC, "dateTransaction"));
    }

    public List<A2ATransfer> getTransactionsHistoryOldest() {
        return a2aTransferRepo.findAll(Sort.by(Sort.Direction.ASC, "dateTransaction"));
    }

    private void logActivity(Customer user, String operationType, String description) {
        ActivityTracking activity = ActivityTracking.builder()
                .user(user)
                .operationType(operationType)
                .operationDescription(description)
                .userIp(request.getRemoteAddr()) 
                .userAgent(request.getHeader("User-Agent")) 
                .operationDate(LocalDateTime.now())
                .build();

        activityTrackingRepo.save(activity);
    }

    public QRCode createQRTransaction(String terminalId, String ribSender, String ribReceiver, BigDecimal amount) {
        Account receiver = accountRepo.findByRib(ribReceiver).orElse(null);
        Account sender = accountRepo.findByRib(ribSender).orElse(null);

        if ( receiver == null) {
            throw new RuntimeException("Receiver account not found");
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Create a new QR code transaction
        QRCode qrCode = QRCode.builder()
                .terminalId(terminalId)
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .expirationDate(LocalDateTime.now().plusMinutes(1)) 
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(TransactionType.QR_PAYMENT)
                .qrStatus(QRCodeStatus.ACTIVE)
                .dateTransaction(LocalDateTime.now())
                .build();

        // Save the QR code transaction
        return qrCodeRepo.save(qrCode);
    }

    public void processQRTransaction(Long qrId) {
        // Find the QR code transaction
        QRCode qrCode = qrCodeRepo.findById(qrId)
                .orElseThrow(() -> new RuntimeException("QR code not found"));

        // Check if the QR code is expired
        if (qrCode.getExpirationDate().isBefore(LocalDateTime.now())) {
            qrCode.setQrStatus(QRCodeStatus.EXPIRED);
            qrCodeRepo.save(qrCode);
            throw new RuntimeException("QR code has expired");
        }

        // Check if the QR code is already processed
        if (qrCode.getTransactionStatus() == TransactionStatus.COMPLETED) {
            throw new RuntimeException("QR code transaction already completed");
        }

        // Check if the sender has sufficient balance
        if (qrCode.getSender().getAmount().compareTo(qrCode.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in sender's account");
        }

        // Transfer the amount from sender to receiver
        qrCode.getSender().setAmount(qrCode.getSender().getAmount().subtract(qrCode.getAmount()));
        qrCode.getReceiver().setAmount(qrCode.getReceiver().getAmount().add(qrCode.getAmount()));

        // Update the transaction status
        qrCode.setTransactionStatus(TransactionStatus.COMPLETED);
        qrCode.setQrStatus(QRCodeStatus.INACTIVE);

        // Save the updated accounts and QR code transaction
        accountRepo.save(qrCode.getSender());
        accountRepo.save(qrCode.getReceiver());
        qrCodeRepo.save(qrCode);

        // Log the activity
        logActivity(qrCode.getSender().getCustomer(), "QR_PAYMENT", "Processed QR payment of " + qrCode.getAmount() + " to " + qrCode.getReceiver().getCustomer().getUsername());
    }

    public List<QRCode> getQRTransactionHistory(String rib) {
        // Find the account by RIB
        Account account = accountRepo.findByRib(rib)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    
        // Retrieve all QR transactions where the account is the sender or receiver
        return qrCodeRepo.findBySenderOrReceiver(account, account);
    }

    public List<QRCode> getAllQRTransactionsNewest() {
        return qrCodeRepo.findAll(Sort.by(Sort.Direction.DESC, "dateTransaction"));
    }

    public List<QRCode> getAllQRTransactionsOldest() {
        return qrCodeRepo.findAll(Sort.by(Sort.Direction.ASC, "dateTransaction"));
    }

    
}

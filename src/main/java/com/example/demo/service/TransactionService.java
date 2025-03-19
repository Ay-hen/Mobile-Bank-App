package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.example.demo.enums.CheckStatus;
import com.example.demo.enums.QRCodeStatus;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;
import com.example.demo.model.A2ATransfer;
import com.example.demo.model.Account;
import com.example.demo.model.ActivityTracking;
import com.example.demo.model.Card;
import com.example.demo.model.Check;
import com.example.demo.model.Customer;
import com.example.demo.model.QRCode;
import com.example.demo.repository.A2ATransferRepo;
import com.example.demo.repository.AccountRepo;
import com.example.demo.repository.ActivityTrackingRepo;
import com.example.demo.repository.CardRepo;
import com.example.demo.repository.CheckRepo;
import com.example.demo.repository.CustomerRepo;
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

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private CheckRepo checkRepo;

    @Autowired
    private CardRepo cardRepo;


    public boolean existsByRib(String rib) {
        return accountRepo.existsByRib(rib);
    }

    public String getRib(String authenticator) {
        return accountRepo.findByAuthenticator(authenticator)
                .orElseThrow(() -> new RuntimeException("Account not found"))
                .getRib();
    }

    public void sendAmountMoney(String ribSender, String ribReceiver, BigDecimal amount) {
        var sender = accountRepo.findByRib(ribSender)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        var receiver = accountRepo.findByRib(ribReceiver)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        if (sender.getAmount().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in sender's account");
        }

        sender.setAmount(sender.getAmount().subtract(amount));
        receiver.setAmount(receiver.getAmount().add(amount));
    
        accountRepo.save(sender);
        accountRepo.save(receiver);

        A2ATransfer a2aTransfer = A2ATransfer.builder()
                .amount(amount)
                .accountDebit(sender)
                .accountCredit(receiver)
                .accountCreditRib(ribReceiver)
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.COMPLETED)
                .dateTransaction(LocalDateTime.now())
                .build();
    
        a2aTransferRepo.save(a2aTransfer);
        var user = sender.getCustomer();

        logActivity(user, "TRANSFER", "Sent " + amount + " from " + sender.getCustomer().getUsername() + " to " + receiver.getCustomer().getUsername());
    }

    public void depositAmountMoney(String rib, BigDecimal amount) {
        var account = accountRepo.findByRib(rib)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setAmount(account.getAmount().add(amount));
        accountRepo.save(account);

        var customer = account.getCustomer();

        A2ATransfer a2aTransfer = A2ATransfer.builder()
                .amount(amount)
                .accountDebit(null) 
                .accountCredit(account)
                .accountCreditRib(rib)
                .transactionType(TransactionType.DEPOSIT)
                .transactionStatus(TransactionStatus.COMPLETED)
                .dateTransaction(LocalDateTime.now())
                .build();

        a2aTransferRepo.save(a2aTransfer);
    
        logActivity(customer, "DEPOSIT", "Deposited " + amount + " to " + rib);
    }

    public List<A2ATransfer> getAllSenderTransactions(String rib) {
        return a2aTransferRepo.findByAccountDebitRib(rib);
    }

    public List<A2ATransfer> getAllReceiverTransactions(String rib) {
        return a2aTransferRepo.findByAccountCreditRib(rib);
    }

    public List<Map<String, Object>> getTransactionsHistoryNewest() {
        List<A2ATransfer> transactions = a2aTransferRepo.findAll(Sort.by(Sort.Direction.DESC, "dateTransaction"));
        List<Map<String, Object>> result = new ArrayList<>();
    
        for (A2ATransfer transaction : transactions) {
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("id", transaction.getId());
    
            // Handle Account Debit
            Map<String, String> accountDebit = new HashMap<>();
            if (transaction.getAccountDebit() != null) {
                accountDebit.put("accountId", String.valueOf(transaction.getAccountDebit().getAccountId()));
                accountDebit.put("customerName", 
                    transaction.getAccountDebit().getCustomer() != null 
                        ? transaction.getAccountDebit().getCustomer().getName() 
                        : "N/A"
                );
            } else {
                accountDebit.put("accountId", "N/A");
                accountDebit.put("customerName", "N/A");
            }
            transactionData.put("accountDebit", accountDebit);
    
            // Handle Account Credit
            Map<String, String> accountCredit = new HashMap<>();
            if (transaction.getAccountCredit() != null) {
                accountCredit.put("accountId", String.valueOf(transaction.getAccountCredit().getAccountId()));
                accountCredit.put("customerName", 
                    transaction.getAccountCredit().getCustomer() != null 
                        ? transaction.getAccountCredit().getCustomer().getName() 
                        : "N/A"
                );
            } else {
                accountCredit.put("accountId", "N/A");
                accountCredit.put("customerName", "N/A");
            }
            transactionData.put("accountCredit", accountCredit);
    
            transactionData.put("dateTransaction", transaction.getDateTransaction());
            transactionData.put("transactionType", transaction.getTransactionType());
            transactionData.put("transactionStatus", transaction.getTransactionStatus());
            transactionData.put("amount", transaction.getAmount());
    
            result.add(transactionData);
        }
    
        return result;
    }

    public List<Map<String, Object>> getTransactionsHistoryOldest() {
        List<A2ATransfer> transactions = a2aTransferRepo.findAll(Sort.by(Sort.Direction.ASC, "dateTransaction"));
        List<Map<String, Object>> result = new ArrayList<>();
    
        for (A2ATransfer transaction : transactions) {
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("id", transaction.getId());
    
            // Handle Account Debit
            Map<String, String> accountDebit = new HashMap<>();
            if (transaction.getAccountDebit() != null) {
                accountDebit.put("accountId", String.valueOf(transaction.getAccountDebit().getAccountId()));
                accountDebit.put("customerName", 
                    transaction.getAccountDebit().getCustomer() != null 
                        ? transaction.getAccountDebit().getCustomer().getName() 
                        : "N/A"
                );
            } else {
                accountDebit.put("accountId", "N/A");
                accountDebit.put("customerName", "N/A");
            }
            transactionData.put("accountDebit", accountDebit);
    
            // Handle Account Credit
            Map<String, String> accountCredit = new HashMap<>();
            if (transaction.getAccountCredit() != null) {
                accountCredit.put("accountId", String.valueOf(transaction.getAccountCredit().getAccountId()));
                accountCredit.put("customerName", 
                    transaction.getAccountCredit().getCustomer() != null 
                        ? transaction.getAccountCredit().getCustomer().getName() 
                        : "N/A"
                );
            } else {
                accountCredit.put("accountId", "N/A");
                accountCredit.put("customerName", "N/A");
            }
            transactionData.put("accountCredit", accountCredit);
    
            transactionData.put("dateTransaction", transaction.getDateTransaction());
            transactionData.put("transactionType", transaction.getTransactionType());
            transactionData.put("transactionStatus", transaction.getTransactionStatus());
            transactionData.put("amount", transaction.getAmount());
    
            result.add(transactionData);
        }
    
        return result;
    }
    

    public QRCode createQRTransaction(String terminalId, String ribSender, String ribReceiver, BigDecimal amount) {
        Account receiver = accountRepo.findByRib(ribReceiver).orElse(null);
        Account sender = accountRepo.findByRib(ribSender).orElse(null);

        if ( receiver == null) {
            throw new RuntimeException("Receiver account not found");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

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

        return qrCodeRepo.save(qrCode);
    }

    public void processQRTransaction(Long qrId) {
        
        QRCode qrCode = qrCodeRepo.findById(qrId)
                .orElseThrow(() -> new RuntimeException("QR code not found"));

        if (qrCode.getExpirationDate().isBefore(LocalDateTime.now())) {
            qrCode.setQrStatus(QRCodeStatus.EXPIRED);
            qrCodeRepo.save(qrCode);
            throw new RuntimeException("QR code has expired");
        }

        if (qrCode.getTransactionStatus() == TransactionStatus.COMPLETED) {
            throw new RuntimeException("QR code transaction already completed");
        }

        if (qrCode.getSender().getAmount().compareTo(qrCode.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in sender's account");
        }

        qrCode.getSender().setAmount(qrCode.getSender().getAmount().subtract(qrCode.getAmount()));
        qrCode.getReceiver().setAmount(qrCode.getReceiver().getAmount().add(qrCode.getAmount()));

        qrCode.setTransactionStatus(TransactionStatus.COMPLETED);
        qrCode.setQrStatus(QRCodeStatus.INACTIVE);

        accountRepo.save(qrCode.getSender());
        accountRepo.save(qrCode.getReceiver());
        qrCodeRepo.save(qrCode);

        logActivity(qrCode.getSender().getCustomer(), "QR_PAYMENT", "Processed QR payment of " + qrCode.getAmount() + " to " + qrCode.getReceiver().getCustomer().getUsername());
    }

    public List<QRCode> getQRTransactionHistory(String rib) {

        Account account = accountRepo.findByRib(rib)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    
        return qrCodeRepo.findBySenderOrReceiver(account, account);
    }

    public List<QRCode> getAllQRTransactionsNewest() {
        return qrCodeRepo.findAll(Sort.by(Sort.Direction.DESC, "dateTransaction"));
    }

    public List<QRCode> getAllQRTransactionsOldest() {
        return qrCodeRepo.findAll(Sort.by(Sort.Direction.ASC, "dateTransaction"));
    }


    //Check Management
    public Check createCheck(String username, BigDecimal amount, LocalDate expirationDate) {
        Customer customer = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Check amount must be greater than zero");
        }

        if (expirationDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Expiration date must be in the future");
        }

        Check check = Check.builder()
                .checkReference(generateCheckReference())
                .checkAmount(amount)
                .checkStatus(CheckStatus.PENDING)
                .checkCreationDate(LocalDate.now())
                .checkExpirationDate(expirationDate)
                .customer(customer)
                .build();

        return checkRepo.save(check);
    }

    private String generateCheckReference() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomChars = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CHK-" + timestamp + "-" + randomChars;
    }

    public void processCheck(Long checkId) {
        Check check = checkRepo.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Check not found"));

        if (check.getCheckStatus() == CheckStatus.COMPLETED) {
            throw new RuntimeException("Check already processed");
        }

        if (check.getCheckExpirationDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Check has expired");
        }

        if (check.getCustomer().getAccount().getAmount().compareTo(check.getCheckAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in customer's account");
        }

        check.getCustomer().getAccount().setAmount(check.getCustomer().getAccount().getAmount().subtract(check.getCheckAmount()));
        check.setCheckStatus(CheckStatus.COMPLETED);

        accountRepo.save(check.getCustomer().getAccount());
        checkRepo.save(check);

        logActivity(check.getCustomer(), "CHECK", "Processed check of " + check.getCheckAmount());
    }

    public List<Check> getCheckHistory(String username) {
        Customer customer = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return checkRepo.findByCustomer(customer);
    }

    public List<Check> getAllChecksNewest() {
        return checkRepo.findAll(Sort.by(Sort.Direction.DESC, "checkCreationDate"));
    }

    public List<Check> getAllChecksOldest() {
        return checkRepo.findAll(Sort.by(Sort.Direction.ASC, "checkCreationDate"));
    }

    public void cancelCheck(Long checkId) {
        Check check = checkRepo.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Check not found"));

        if (check.getCheckStatus() == CheckStatus.COMPLETED) {
            throw new RuntimeException("Check already processed");
        }

        check.setCheckStatus(CheckStatus.CANCELLED);
        checkRepo.save(check);
    } 


    //Card Management
    public void activatiblityCard(Long cardId) {
        var card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.isActivated()) {
            card.setActivated(false);
            logActivity(card.getAccount().getCustomer(), "CARD", "Deactivated card " + card.getCardNumber());
        }else{
            card.setActivated(true);
            logActivity(card.getAccount().getCustomer(), "CARD", "Activated card " );
        }
        cardRepo.save(card);
    }

    public void changePINCard(Long cardId, String newPIN) {
        var card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setPin(newPIN);
        cardRepo.save(card);

        logActivity(card.getAccount().getCustomer(), "CARD", "Changed PIN for card ");
    }

    public void changeTransactionLimit(Long cardId, int newLimit) {
        var card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setLimitTransaction(newLimit);
        cardRepo.save(card);

        logActivity(card.getAccount().getCustomer(), "CARD", "Changed transaction limit for card ");
    }

    public List<Card> getCardkHistory(String username) {
        Account account = accountRepo.findByAuthenticator(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return cardRepo.findByAccount(account);
    }

    public Customer getCustomerByUsername(String username) {
        return customerRepo.findByUsernameCustomer(username).get();
    }

    /* ****************************** LogActivity **************************** */
    private void logActivity(Customer user, String operationType, String description) {
        ActivityTracking activity = ActivityTracking.builder()
                .user(user)
                .operationType(operationType)
                .userName(user.getUsername())
                .userFullName(user.getName())
                .operationDescription(description)
                .userIp(request.getRemoteAddr()) 
                .userAgent(request.getHeader("User-Agent")) 
                .operationDate(LocalDateTime.now())
                .build();

        activityTrackingRepo.save(activity);
    }
}

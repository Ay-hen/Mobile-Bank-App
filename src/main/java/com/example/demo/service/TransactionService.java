package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.format.TextStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;
import com.example.demo.model.A2ATransfer;
import com.example.demo.model.Account;
import com.example.demo.model.ActivityTracking;
import com.example.demo.model.Customer;
import com.example.demo.repository.A2ATransferRepo;
import com.example.demo.repository.AccountRepo;
import com.example.demo.repository.ActivityTrackingRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

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
    private NotificationService notificationService;

    public boolean existsByRib(String rib) {
        return accountRepo.existsByRib(rib);
    }

    public String getRib(String authenticator) {
        return accountRepo.findByAuthenticator(authenticator)
                .orElseThrow(() -> new RuntimeException("Account not found"))
                .getRib();
    }

    @Transactional
    public void transferMoney(String ribSender, String ribReceiver, BigDecimal amount) {
        Account sender = null;
        Account receiver = null;
        Customer user = null;

        try {
            sender = accountRepo.findByRib(ribSender)
                    .orElseThrow(() -> new RuntimeException("Sender account not found"));
            receiver = accountRepo.findByRib(ribReceiver)
                    .orElseThrow(() -> new RuntimeException("Receiver account not found"));
            user = sender.getCustomer();

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
                    .accountDebitRib(sender.getRib())
                    .accountCreditRib(ribReceiver)
                    .transactionType(TransactionType.TRANSFER)
                    .transactionStatus(TransactionStatus.COMPLETED)
                    .dateTransaction(LocalDateTime.now())
                    .build();

            a2aTransferRepo.save(a2aTransfer);

            notificationService.sendNotification(
                    "TRANSACTION",
                    "TRANSACTION SUCCESS",
                    "Transfer of " + amount + " from " + sender.getCustomer().getUsername() + 
                    " to " + receiver.getCustomer().getUsername() + " completed successfully",
                    "Sendt by API",
                    List.of(sender.getCustomer(), receiver.getCustomer())
            );

            logActivity(user, "TRANSFER", "Sent " + amount + " from " + 
                sender.getCustomer().getUsername() + " to " + receiver.getCustomer().getUsername());

        } catch (Exception e) {
            String errorMessage = "Transfer failed: " + e.getMessage();
            List<Customer> recipients = new ArrayList<>();
            if (sender != null && sender.getCustomer() != null) {
                recipients.add(sender.getCustomer());
            }
            if (receiver != null && receiver.getCustomer() != null) {
                recipients.add(receiver.getCustomer());
            }

            notificationService.sendNotification(
                    "TRANSACTION",
                    "TRANSACTION FAILED",
                    errorMessage,
                    "Sendt by API",
                    recipients
            );

            if (user != null) {
                logActivity(user, "TRANSFER_ERROR", errorMessage);
            }

            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void depositAmountMoney(String rib, BigDecimal amount) {
        Account account = null;
        Customer customer = null;
        
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Deposit amount must be greater than zero");
            }

            account = accountRepo.findByRib(rib)
                    .orElseThrow(() -> new RuntimeException("Account with RIB " + rib + " not found"));
            customer = account.getCustomer();

            account.setAmount(account.getAmount().add(amount));
            accountRepo.save(account);

            A2ATransfer a2aTransfer = A2ATransfer.builder()
                    .amount(amount)
                    .accountDebit(null) 
                    .accountCredit(account)
                    .accountCreditRib(rib)
                    .accountDebitRib("N/A")
                    .transactionType(TransactionType.DEPOSIT)
                    .transactionStatus(TransactionStatus.COMPLETED)
                    .dateTransaction(LocalDateTime.now())
                    .build();

            a2aTransferRepo.save(a2aTransfer);

            notificationService.sendNotification(
                    "TRANSACTION",
                    "DEPOSIT SUCCESS",
                    "Deposit of " + amount + " to account " + rib + " was successful",
                    "Sendt by API",
                    List.of(customer)
            );

            logActivity(customer, "DEPOSIT", "Deposited " + amount + " to " + rib);

        } catch (Exception e) {
            String errorMessage = "Deposit failed: " + e.getMessage();
            List<Customer> recipients = new ArrayList<>();
            if (customer != null) {
                recipients.add(customer);
            }

            notificationService.sendNotification(
                    "TRANSACTION",
                    "DEPOSIT FAILED",
                    errorMessage,
                    "Sendt by API",
                    recipients
            );

            if (customer != null) {
                logActivity(customer, "DEPOSIT_ERROR", errorMessage);
            }

            throw new RuntimeException("Deposit operation failed: " + e.getMessage(), e);
        }
    }

    public List<A2ATransfer> getAllSenderTransactions(String rib) {
        return a2aTransferRepo.findByAccountDebitRib(rib);
    }

    public List<A2ATransfer> getAllReceiverTransactions(String rib) {
        return a2aTransferRepo.findByAccountCreditRib(rib);
    }

    @Transactional
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

    @Transactional
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

    public List<Map<String, Object>> getTransactionsVisualization(Account account) {
        // Get the date 6 months ago
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        // Fetch transactions for the last 6 months, sorted by latest
        List<A2ATransfer> transactions = a2aTransferRepo.findByAccountDebitRibOrAccountCreditRibAndDateTransactionAfter(
            account.getRib(), account.getRib(), sixMonthsAgo, Sort.by(Sort.Direction.DESC, "dateTransaction")
        );

        Map<String, List<A2ATransfer>> transactionsByMonth = new LinkedHashMap<>();

        // Initialize the map with all months from the last 6 months
        LocalDate currentDate = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            String month = currentDate.minusMonths(i).getMonth()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .toUpperCase();
            transactionsByMonth.put(month, new ArrayList<>());
        }

        // Group transactions by month
        transactions.forEach(tx -> {
            String month = tx.getDateTransaction().getMonth()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .toUpperCase();
            transactionsByMonth.get(month).add(tx);
        });

        // Prepare result list
        List<Map<String, Object>> visualizationData = new ArrayList<>();
        for (Map.Entry<String, List<A2ATransfer>> entry : transactionsByMonth.entrySet()) {
            String month = entry.getKey();
            List<A2ATransfer> monthlyTransactions = entry.getValue();

            // Calculate total debit (outgoing) and credit (incoming) for this month
            BigDecimal totalDebit = monthlyTransactions.stream()
                .filter(tx -> tx.getAccountDebit() != null && tx.getAccountDebit().equals(account))
                .map(A2ATransfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = monthlyTransactions.stream()
                .filter(tx -> tx.getAccountCredit() != null && tx.getAccountCredit().equals(account))
                .map(A2ATransfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Get transaction IDs (for reference)
            List<Long> transactionIds = monthlyTransactions.stream()
                .map(A2ATransfer::getId)
                .collect(Collectors.toList());

            // Add to visualization data
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("totalDebit", totalDebit); //spend
            monthData.put("totalCredit", totalCredit); //gain   
            monthData.put("transactionCount", monthlyTransactions.size()); 
            monthData.put("transactionIds", transactionIds); 

            visualizationData.add(monthData);
        }

        return visualizationData;
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

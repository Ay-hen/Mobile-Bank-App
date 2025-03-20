package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.A2ATransfer;
import com.example.demo.model.Account;
import com.example.demo.service.TransactionService;
import com.example.demo.repository.AccountRepo;

@RequestMapping("/transaction")
@RestController
@CrossOrigin(origins = "*")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepo accountRepo;

    @PostMapping("/send-money")
    public ResponseEntity<String> sendMoney(
            @RequestParam String ribSender,
            @RequestParam String ribReceiver,
            @RequestParam BigDecimal amount
            ) {

        transactionService.sendAmountMoney(ribSender, ribReceiver, amount);
        return ResponseEntity.ok("Transfer successful.");
    }

    //Admins only can deposit money to any account
    @PostMapping("/deposit")
    public ResponseEntity<String> depositMoney(
            @RequestBody DepositRequest request
            ) {

        transactionService.depositAmountMoney(request.getRib(), request.getAmount());
        return ResponseEntity.ok("Deposit successful.");
    }

    @GetMapping("/history/newest")
    public ResponseEntity<?> getTransactionHistoryNewest() {
        return ResponseEntity.ok(transactionService.getTransactionsHistoryNewest());
    }

    @GetMapping("/history/{rib}")
    public ResponseEntity<Map<String, List<A2ATransfer>>> getUserTransactions(@PathVariable String rib) {
        Map<String, List<A2ATransfer>> transactions = new HashMap<>();
        transactions.put("sent", transactionService.getAllSenderTransactions(rib));
        transactions.put("received", transactionService.getAllReceiverTransactions(rib));
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/visualization/{authenticator}")
    public ResponseEntity<?> getTransactionVisualization(@PathVariable String authenticator) {

        Account account = accountRepo.findByAuthenticator(authenticator)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + authenticator));

        // Call the service method to get visualization data
        List<Map<String, Object>> visualizationData = transactionService.getTransactionsVisualization(account);

        // Return the visualization data
        return ResponseEntity.ok(visualizationData);
    }
}

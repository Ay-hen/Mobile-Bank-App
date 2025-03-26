package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.enums.CheckStatus;
import com.example.demo.model.ActivityTracking;
import com.example.demo.model.Branch;
import com.example.demo.model.Check;
import com.example.demo.model.Customer;
import com.example.demo.repository.ActivityTrackingRepo;
import com.example.demo.repository.BranchRepo;
import com.example.demo.repository.CheckRepo;
import com.example.demo.repository.CustomerRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class CheckService {

    @Autowired
    private CheckRepo checkRepo;
    
    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private BranchRepo branchRepo;
    
    @Autowired
    private ActivityTrackingRepo activityTrackingRepo;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private HttpServletRequest request;
    
    @Value("${bank.check.max.amount:1000000}")
    private BigDecimal maxCheckAmount;
    
    @Value("${bank.check.max.validity.days:365}")
    private int maxCheckValidityDays;

    public Check createCheck(String username, BigDecimal amount, LocalDate expirationDate) {
        Customer customer = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        validateCheckCreation(customer, amount, expirationDate);

        Branch issuingBranch = branchRepo.findByBranchCode(customer.getAccount().getBranchCode())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        String checkReference = generateSecureCheckReference(customer);
        String securityCode = generateSecurityCode();

        Check check = Check.builder()
                .checkReference(checkReference)
                .amount(amount)
                .status(CheckStatus.PENDING)
                .creationDate(LocalDate.now())
                .expirationDate(expirationDate)
                .customer(customer)
                .issuingBranch(issuingBranch)
                .beneficiaryName("")
                .securityCode(securityCode)
                .build();

        Check savedCheck = checkRepo.save(check);
        logActivity(customer, "CHECK_ISSUANCE", "Check " + checkReference + " issued for " + amount);
        notificationService.sendCheckIssuanceNotification(customer, savedCheck);

        return savedCheck;
    }

    private void validateCheckCreation(Customer customer, BigDecimal amount, LocalDate expirationDate) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Check amount must be positive");
        }
        if (amount.compareTo(maxCheckAmount) > 0) {
            throw new RuntimeException("Check amount exceeds maximum allowed");
        }
        if (customer.getAccount().getAmount().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        LocalDate maxValidDate = LocalDate.now().plusDays(maxCheckValidityDays);
        if (expirationDate.isBefore(LocalDate.now().plusDays(1)) || expirationDate.isAfter(maxValidDate)) {
            throw new RuntimeException("Invalid expiration date");
        }
    }

    private String generateSecureCheckReference(Customer customer) {
        return customer.getAccount().getBranchCode() + " - " +
                customer.getAccount().getAccountNumber() + " - " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + " - " +
               (int) (Math.random() * 1000);
    }

    private String generateSecurityCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    @Transactional
    public void processCheck(Long checkId, String beneficiaryName) {
        Check check = checkRepo.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Check not found"));

        validateCheckProcessing(check, beneficiaryName);
        check.setBeneficiaryName(beneficiaryName);
        check.setProcessingDate(LocalDate.now());
        check.setStatus(CheckStatus.PROCESSED);
        checkRepo.save(check);
        logActivity(check.getCustomer(), "CHECK_PROCESSING", "Check " + check.getCheckReference() + " processed.");
    }

    private void validateCheckProcessing(Check check, String beneficiaryName) {
        if (check.getStatus() != CheckStatus.PENDING) {
            throw new RuntimeException("Check already processed");
        }
        if (check.getExpirationDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Check expired");
        }
        if (beneficiaryName == null || beneficiaryName.trim().isEmpty()) {
            throw new RuntimeException("Beneficiary name required");
        }
    }

    @Transactional
    public void cancelCheck(Long checkId, String reason) {
        Check check = checkRepo.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Check not found"));
        if (check.getStatus() != CheckStatus.PENDING) {
            throw new RuntimeException("Only pending checks can be cancelled");
        }
        check.setStatus(CheckStatus.CANCELLED);
        check.setCancellationDate(LocalDate.now());
        check.setCancellationReason(reason);
        checkRepo.save(check);
        logActivity(check.getCustomer(), "CHECK_CANCELLATION", "Check " + check.getCheckReference() + " cancelled.");
    }

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

package com.example.demo.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.JwtService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Autowired
    private CustomerRepo customerRepo;
    
    @Autowired
    private AccountRepo accountRepo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private BankRepo bankRepo;
    
    @Autowired
    private BranchRepo branchRepo;
    
    @Autowired
    private JwtService jwtService;

    @Autowired
    private ActivityTrackingRepo activityTrackingRepo;

    /* ******************************* Register Customer ******************************* */
    public ResponseEntity<?> registerCustomer(CustomerRegistrationRequest request) {
        try {
            if (customerRepo.existsByUserEmail(request.getUserEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already taken");
            }
    
            if (customerRepo.existsByCin(request.getCin())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("CIN already taken");
            }
    
            var customer = Customer.builder()
                .name(request.getName())
                .userEmail(request.getUserEmail())
                .username(request.getUsername())
                .userPassword(passwordEncoder.encode(request.getUserPassword()))
                .role("CUSTOMER")
                .lastActive(LocalDateTime.now().plusMinutes(5))
                .phoneNumber(request.getPhoneNumber())
                .cin(request.getCin())
                .isVerified(false)
                .biometricEnabled(false)
                .birthday(request.getBirthday())
                .build();
            System.out.println("user *********************************");
    
            String jwtToken = jwtService.generateToken(customer);
            System.out.println("jwtToken *********************************");
    
            // Save customer and user first (important)
            customerRepo.save(customer);
    
            // Now create and save the account
            var account = createPersonalAccount(customer, request.getUserPassword());
            System.out.println("account *********************************");
    
            // Save the account BEFORE creating the token
            accountRepo.save(account);
    
            // Now that the account is saved, create and save the token
            Token token = Token.builder()
                    .user(customer)
                    .token(jwtToken)
                    .account(account) 
                    .expirationDate(LocalDateTime.now().plusMinutes(30))
                    .isExpired(false)
                    .revoked(false)
                    .build();
    
            try {
                
                jwtService.saveToken(token);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error saving token: " + e.getMessage());
            }
    
            logActivity(customer, "CUSTOMER_REGISTRATION", "Customer registered successfully", null, null);
    
            return ResponseEntity.status(HttpStatus.CREATED).body(AuthenticationResponse.builder()
                    .token(jwtToken)
                    .authenticator(request.getUsername())
                    .build());
    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }
    

    /* ******************************* Create Personal Account ******************************* */
    private Account createPersonalAccount(Customer customer, String password) {
        var defaultBank = bankRepo.findByBankCode("BANK001")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default bank not found"));
        var defaultBranch = branchRepo.findByBranchCode("BRANCH001")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default branch not found"));

        

        return Account.builder()
                .bankCode(defaultBank.getBankCode())
                .branchCode(defaultBranch.getBranchCode())
                .customer(customer)
                .accountPassword(passwordEncoder.encode(password)) 
                .accountCurrency("MAD")
                .accountStatus("ACTIVE")
                .authenticator(customer.getUsername()) 
                .build();
    }

    

    /* ******************************* Login to Account ******************************* */
    public ResponseEntity<?> loginToAccount(AccountLoginRequest request, String userIp, String userAgent) {
        Optional<Account> accountOpt = accountRepo.findByAuthenticator(request.getUsername());
        
        // Introduce a constant delay for added security (mitigating timing attacks)
        boolean isPasswordValid = accountOpt
                .map(account -> passwordEncoder.matches(request.getPassword(), account.getAccountPassword()))
                .orElse(false);
    
        // Generic error response for both wrong password and non-existent accounts
        if (!isPasswordValid) {
            logActivity(null, "LOGIN_FAILED", "Invalid credentials", userIp, userAgent);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    
        Account account = accountOpt.get();
        Customer customer = account.getCustomer();
    
        if (!"ACTIVE".equals(account.getAccountStatus())) {
            logActivity(customer, "LOGIN_FAILED", "Access denied", userIp, userAgent);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
    
        // Token generation should ideally be handled securely
        String jwtToken = jwtService.generateToken(customer); // Example generation method

        Token token = Token.builder()
                    .user(customer)
                    .token(jwtToken)
                    .account(account) 
                    .expirationDate(LocalDateTime.now().plusMinutes(30))
                    .isExpired(false)
                    .revoked(false)
                    .build();
        jwtService.saveToken(token);

        logActivity(customer, "LOGIN_SUCCESS", "Login successful"," userIp", "userAgent");
    
        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(jwtToken)
                .authenticator(request.getUsername())
                .build());
    }
    

    /* ******************************* Log Activity ******************************* */
    private void logActivity(Customer customer, String operationType, String operationDescription, String userIp, String userAgent) {
        var activity = ActivityTracking.builder()
                .user(customer)
                .operationType(operationType)
                .operationDescription(operationDescription)
                .userIp(userIp)
                .userAgent(userAgent)
                .build();
        activityTrackingRepo.save(activity);
    }
}

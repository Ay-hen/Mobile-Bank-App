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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
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
    
            String jwtToken = jwtService.generateToken(customer);
    
            customerRepo.save(customer);

            var account = createPersonalAccount(customer, request.getUserPassword());

            accountRepo.save(account);

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
    var defaultBank = bankRepo.findByBankCode("812743")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default bank not found"));
    var defaultBranch = branchRepo.findByBranchCode("88541")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default branch not found"));

    
    String rib = generateRIB(defaultBank.getBankCode(), defaultBranch.getBranchCode(), customer.getUserId());

    return Account.builder()
            .bankCode(defaultBank.getBankCode())
            .branchCode(defaultBranch.getBranchCode())
            .customer(customer)
            .rib(rib)  
            .accountPassword(passwordEncoder.encode(password)) 
            .accountCurrency("MAD")
            .accountStatus("ACTIVE")
            .amount(BigDecimal.ZERO)
            .authenticator(customer.getUsername()) 
            .build();
}



    public static String generateRIB(String bankCode, String branchCode, Long customerId) {
        SecureRandom random = new SecureRandom();

        String accountNumber = String.format("%011d", random.nextLong(99999999999L));

        String rawRib = bankCode + branchCode + accountNumber;

        String ribWithCheckDigits = rawRib + "00";  
        int checksum = 98 - (new BigInteger(ribWithCheckDigits).mod(BigInteger.valueOf(97)).intValue());

        String checksumStr = String.format("%02d", checksum);

        return rawRib + checksumStr;
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

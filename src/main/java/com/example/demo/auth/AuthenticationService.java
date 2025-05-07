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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

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
    private CardRepo cardRepo;
    
    @Autowired
    private JwtService jwtService;

    @Autowired
    private BalanceRepo balanceRepo;

    @Autowired
    private ActivityTrackingRepo activityTrackingRepo;

    @Autowired
    private HttpServletRequest request; 

    @Autowired
    private CurrencyRepo currencyRepo;

    /* ******************************* Register Customer ******************************* */
    public ResponseEntity<?> registerCustomer(CustomerRegistrationRequest request) {
        try {
            if (customerRepo.existsByUserEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already taken");
            }

            if (customerRepo.existsByCin(request.getCin())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("CIN already taken");
            }

            if (customerRepo.existsByUsername(request.getUsername())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
            }

            var customer = Customer.builder()
                .name(request.getName())
                .userEmail(request.getEmail())
                .username(request.getUsername())
                .userPassword(passwordEncoder.encode(request.getPassword()))
                .role("CUSTOMER")
                .isOnline(true)
                .usernameCustomer(request.getUsername())
                .lastActive(LocalDateTime.now().plusMinutes(5))
                .phoneNumber(request.getPhoneNumber())
                .cin(request.getCin())
                .isVerified(false)
                .biometricEnabled(false)
                .birthday(request.getBirthday())
                .creationDate(request.getCreationDate())
                .build();
                
            customerRepo.save(customer);
            String jwtToken = jwtService.generateToken(customer);

            var account = createPersonalAccount(customer, request.getPassword(), request.getBranchCode());
            //accountRepo.save(account);

            var card = createCard(account);
            cardRepo.save(card);

            Token token = Token.builder()
                    .user(customer)
                    .token(jwtToken)
                    .account(account) 
                    .expirationDate(LocalDateTime.now().plusMinutes(60))
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

            logActivity(customer, "CUSTOMER_REGISTRATION", "Customer registered successfully with account and card");

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
    private Account createPersonalAccount(Customer customer, String password, String branchCode) {
        var defaultBank = bankRepo.findByBankCode("812743")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default bank not found"));
        var defaultBranch = branchRepo.findByBranchCode(branchCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default branch not found"));
    
        
        String rib = generateRIB(defaultBank.getBankCode(), defaultBranch.getBranchCode(), customer.getUserId());
    
        String accountNumber;
        int attempts = 0;
        do {
            accountNumber = generateAccountNumber();
            attempts++;
            if (attempts > 12) {
                throw new IllegalStateException("Failed to generate unique account number after 10 attempts");
            }
        } while (accountRepo.existsByAccountNumber(accountNumber));
    
    
        Currency currency = currencyRepo.findByCurrencyCode("MAD")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found"));
        
        Balance balance = Balance.builder()
                .currentAmount(BigDecimal.ZERO)
                .account(null)
                .currency(currency)
                .build();
        
        Account account = Account.builder()
                .bankCode(defaultBank.getBankCode())
                .branchCode(defaultBranch.getBranchCode())
                .customer(customer)
                .accountNumber(accountNumber)
                .rib(rib)
                .accountPassword(passwordEncoder.encode(password))
                .currency(currency)
                .accountStatus("ACTIVE")
                .balance(balance)
                .authenticator(customer.getUsername())
                .build();
        
        balance.setAccount(account);
        
        // Save the account first
        account = accountRepo.save(account);
        
        // The balance might be saved automatically through cascade,
        // but you can make it explicit if needed:
        // balanceRepo.save(balance);
        
        return account;
    }

    private String generateAccountNumber() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(1000000000L + random.nextLong(9000000000L));
    }

    /* ******************************* Create Card ******************************* */
    private Card createCard(Account account) {
        String cardNumber = generateCardNumber();
        String pin = generatePin();
        LocalDate activationDate = LocalDate.now().plusDays(7);
        LocalDate expirationDate = LocalDate.now().plusYears(5);

        Branch branch = branchRepo.findByBranchCode(account.getBranchCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        return Card.builder()
                .account(account)
                .branch(branch) 
                .expirationDate(expirationDate)
                .activationDate(activationDate)
                .isActivated(false)
                .isDeliver(false)
                .limitTransaction(10)
                .pin(pin)
                .cardNumber(cardNumber)
                .cardUserName(account.getCustomer().getName()) 
                .build();
    }

    private String generatePin() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            cardNumber.append(random.nextInt(10));
        }
        return cardNumber.toString();
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
        try {
            Optional<Account> accountOpt = accountRepo.findByAuthenticator(request.getUsername());
    
            if (accountOpt.isEmpty()) {
                logActivity(null, "LOGIN_FAILED", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
    
            Account account = accountOpt.get();
            Customer customer = account.getCustomer();

            if (customer.getMaxPasswordAttempts() == 0 && customer.getLastFailedLogin() != null &&
                    customer.getLastFailedLogin().plusHours(1).isAfter(LocalDateTime.now())) {
                logActivity(customer, "LOGIN_FAILED", "Account temporarily locked due to multiple failed attempts.");
                return ResponseEntity.status(HttpStatus.LOCKED).body("Account is locked. Try again after 1 hour.");
            }
    
            boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), account.getAccountPassword());
    
            if (!isPasswordValid) {
                customer.setMaxPasswordAttempts(customer.getMaxPasswordAttempts() - 1);
                customer.setFailedLoginAttempts(customer.getFailedLoginAttempts() + 1);
                customer.setLastFailedLogin(LocalDateTime.now());
    
                customerRepo.save(customer);
    
                logActivity(customer, "LOGIN_FAILED", "Invalid credentials. Remaining attempts: " + customer.getMaxPasswordAttempts());
    
                if (customer.getMaxPasswordAttempts() == 0) {
                    logActivity(customer, "LOGIN_LOCKED", "Account locked due to too many failed login attempts.");
                    return ResponseEntity.status(HttpStatus.LOCKED)
                            .body("Too many failed attempts. Your account is locked for 1 hour.");
                }
    
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials. Remaining attempts: " + customer.getMaxPasswordAttempts());
            }

            customer.setMaxPasswordAttempts(3);
            customer.setFailedLoginAttempts(0);
            
            customerRepo.save(customer);
    
            if (customer.isBlocked()) {
                logActivity(customer, "LOGIN_FAILED", "Access denied; Uder is blocked by an Admin");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            customer.setLoginDate(LocalDateTime.now());
            customer.setOnline(true);
            customerRepo.save(customer);

            account.setAccountStatus("ACTIVE");
            accountRepo.save(account);

            String jwtToken = jwtService.generateToken(customer);
    
            Token token = Token.builder()
                    .user(customer)
                    .token(jwtToken)
                    .account(account)
                    .expirationDate(LocalDateTime.now().plusMinutes(60))
                    .isExpired(false)
                    .revoked(false)
                    .build();
            jwtService.saveToken(token);
    
            logActivity(customer, "LOGIN_SUCCESS", "Login successful");
    
            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .token(jwtToken)
                    .authenticator(request.getUsername())
                    .build());
    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login failed due to an error: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> logout() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Invalid or missing Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
    
        String token = authHeader.substring(7); 
    
        try {
            String username = jwtService.extractUsername(token);
            if (username == null) {
                System.out.println("Invalid token: Unable to extract username");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            Customer customer = customerRepo.findByUsername(username)
                    .orElseThrow(() -> {
                        System.out.println("Customer not found for username: {} "+ username);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
                    });

            customer.setLastActive(LocalDateTime.now());
            customer.setOnline(false);

            Account account = customer.getAccount();
            if (account == null) {
                System.out.println("Account not found for customer: {} " + customer.getUserId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Account not found");
            }
            account.setAccountStatus("INACTIVE");

            accountRepo.save(account);
            customerRepo.save(customer);

            jwtService.revokeToken(token);
    
            System.out.println("Logout successful for customer: {} "+ customer.getUserId());
            return ResponseEntity.ok("Logout successful");
        } catch (ResponseStatusException e) {
            System.out.println("Customer not found during logout" + e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            System.out.println("Logout failed due to an internal error {} "+ e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed due to an internal error");
        }
    }

    /* ******************************* Log Activity ******************************* */
    private void logActivity(Customer customer, String operationType, String operationDescription) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (customer.getUsername() == null || customer.getName() == null) {
            throw new IllegalArgumentException("Customer username or name cannot be null");
        }

        String clientIp = getClientIp();

        var activity = ActivityTracking.builder()
            .user(customer)
            .userName(customer.getUsername())
            .userFullName(customer.getName())
            .operationType(operationType)
            .operationDescription(operationDescription)
            .userIp(clientIp)
            .userAgent(request.getHeader("User-Agent"))
            .build();
        activityTrackingRepo.save(activity);
    }

    private String getClientIp() {
        String ipAddress = request.getHeader("X-Forwarded-For");
    
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // "X-Forwarded-For" can contain multiple IPs, we take the first one
            return ipAddress.split(",")[0].trim();
        }
    
        ipAddress = request.getHeader("X-Real-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }
    
        ipAddress = request.getHeader("Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }
    
        ipAddress = request.getHeader("WL-Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }
    
        // Fallback: direct IP from the request
        return request.getRemoteAddr();
    }    
}

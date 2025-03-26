package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.model.Account;
import com.example.demo.model.ActivityTracking;
import com.example.demo.model.Card;
import com.example.demo.model.Customer;
import com.example.demo.repository.AccountRepo;
import com.example.demo.repository.ActivityTrackingRepo;
import com.example.demo.repository.CardRepo;
import com.example.demo.repository.CustomerRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class CardService {

    @Autowired
    private CardRepo cardRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ActivityTrackingRepo activityTrackingRepo;

    @Value("${bank.card.transaction.limit.max:50}")
    private int maxTransactionLimit;

    /**
     * Activates or deactivates a card based on its current state.
     **/
    @Transactional
    public void toggleCardActivation(Long cardId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        boolean newStatus = !card.isActivated();
        card.setActivated(newStatus);
        cardRepo.save(card);

        String statusMessage = newStatus ? "Activated" : "Deactivated";
        logActivity(card.getAccount().getCustomer(), "CARD", statusMessage + " card " + card.getCardNumber());

        notificationService.sendNotification(
                "CARD",
                "STATUS UPDATED",
                statusMessage + " your card ending in " + maskCardNumber(card.getCardNumber()),
                "CardService",
                List.of(card.getAccount().getCustomer())
        );
    }

    /**
     * Securely changes a card's PIN.
     */
    @Transactional
    public void changePIN(Long cardId, String newPIN) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Encrypt the PIN before saving
        String encryptedPin = encryptPIN(newPIN);
        card.setPin(encryptedPin);
        cardRepo.save(card);

        notificationService.sendNotification(
                "CARD",
                "PIN CHANGED",
                "PIN changed successfully for your card ending in " + maskCardNumber(card.getCardNumber()),
                "CardService",
                List.of(card.getAccount().getCustomer())
        );

        logActivity(card.getAccount().getCustomer(), "CARD", "Changed PIN for card");
    }

    /**
     * Updates the transaction limit for a card.
     */
    @Transactional
    public void updateTransactionLimit(Long cardId, int newLimit) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (newLimit < 1 || newLimit > maxTransactionLimit) {
            throw new RuntimeException("Invalid transaction limit. Must be between 1 and " + maxTransactionLimit);
        }

        card.setLimitTransaction(newLimit);
        cardRepo.save(card);

        logActivity(card.getAccount().getCustomer(), "CARD", "Updated transaction limit to " + newLimit);
    }

    /**
     * Fetches card history for a customer.
     */
    @Transactional
    public List<Card> getCardHistory(String username) {
        Account account = accountRepo.findByAuthenticator(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return cardRepo.findByAccount(account);
    }

    /**
     * Marks a card as delivered.
     */
    @Transactional
    public void markCardAsDelivered(Long cardId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.isDeliver()) {
            throw new RuntimeException("Card is already marked as delivered.");
        }

        card.setDeliver(true);
        cardRepo.save(card);

        notificationService.sendNotification(
                "CARD",
                "DELIVERY UPDATE",
                "Your card has been marked as delivered.",
                "CardService",
                List.of(card.getAccount().getCustomer())
        );

        logActivity(card.getAccount().getCustomer(), "CARD", "Marked card as delivered.");
    }

    /**
     * Retrieves customer details by username.
     */
    @Transactional
    public Customer getCustomerByUsername(String username) {
        return customerRepo.findByUsernameCustomer(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }



    /**
     * Fetches a card and ensures it is activated if the activation date has passed.
     **/
    @Transactional
    public Card getCardDetails(Long cardId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Check if activation date has passed and update activation status
        if (!card.isActivated() && LocalDate.now().isAfter(card.getActivationDate())) {
            card.setActivated(true);
            cardRepo.save(card);

            logActivity(card.getAccount().getCustomer(), "CARD", "Card auto-activated after activation date.");

            // Notify the user
            notificationService.sendNotification(
                    "CARD",
                    "CARD ACTIVATED",
                    "Your card ending in " + maskCardNumber(card.getCardNumber()) + " is now activated.",
                    "CardService",
                    List.of(card.getAccount().getCustomer())
            );
        }

        return card;
    }

    /**
     * Runs a batch job to activate all eligible cards.
     * This can be scheduled to run daily using @Scheduled.
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Runs every day at 2 AM
    public void activateEligibleCards() {
        List<Card> pendingCards = cardRepo.findByIsActivatedFalseAndActivationDateBefore(LocalDate.now());

        for (Card card : pendingCards) {
            card.setActivated(true);
            cardRepo.save(card);

            logActivity(card.getAccount().getCustomer(), "CARD", "Card auto-activated after waiting period.");

            notificationService.sendNotification(
                    "CARD",
                    "CARD ACTIVATED",
                    "Your card ending in " + maskCardNumber(card.getCardNumber()) + " is now activated.",
                    "CardService",
                    List.of(card.getAccount().getCustomer())
            );
        }
    }


    /**
     * Logs user activities for audits.
     */
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

    /**
     * Encrypts the PIN for security.
     */
    private String encryptPIN(String pin) {
        // Replace with real encryption (e.g., BCrypt)
        return Base64.getEncoder().encodeToString(pin.getBytes());
    }

    /**
     * Masks a card number for security.
     */
    private String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

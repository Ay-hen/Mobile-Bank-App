package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CardDto;
import com.example.demo.dto.CardHistoryDto;
import com.example.demo.service.CardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CardController {

    private final CardService cardService;

    @PutMapping("/activate/{cardId}")
    public ResponseEntity<?> toggleCardActivation(@PathVariable Long cardId) {
        try {
            cardService.toggleCardActivation(cardId);
            return ResponseEntity.ok("Card activation status updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/change-pin/{cardId}")
    public ResponseEntity<?> changePin(@PathVariable Long cardId, @RequestParam String newPin) {
        try {
            cardService.changePIN(cardId, newPin);
            return ResponseEntity.ok("PIN changed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/limit/{cardId}")
    public ResponseEntity<?> updateTransactionLimit(@PathVariable Long cardId, @RequestParam int newLimit) {
        try {
            cardService.updateTransactionLimit(cardId, newLimit);
            return ResponseEntity.ok("Transaction limit updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/history/{username}")
    public ResponseEntity<?> getCardHistory(@PathVariable String username) {
        try {
            List<CardHistoryDto> cards = cardService.getCardHistory(username);
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/deliver/{cardId}")
    public ResponseEntity<?> markCardAsDelivered(@PathVariable Long cardId) {
        try {
            cardService.markCardAsDelivered(cardId);
            return ResponseEntity.ok("Card marked as delivered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<?> getCardDetails(@PathVariable Long cardId) {
        try {
            CardDto card = cardService.getCardDetails(cardId);
            return ResponseEntity.ok(card);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

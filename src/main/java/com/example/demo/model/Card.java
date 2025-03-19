package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "card_management")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "check_expiration_date", columnDefinition = "TIMESTAMP")
    private LocalDate expirationDate;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "is_deliver", columnDefinition = "boolean DEFAULT 'false'")
    private boolean isDeliver;

    @Column(name = "limit_transaction", nullable = false, columnDefinition = "INT DEFAULT 10")
    private int limitTransaction;

    @Column(name = "PIN", nullable = false)
    private String pin;

    @Column(name = "is_activated", columnDefinition = "boolean DEFAULT 'false'")
    private boolean isActivated;

    @Column(name = "card_number", nullable = false, columnDefinition = "VARCHAR(50)")
    private String cardNumber;

    @Column(name = "card_user_name", nullable = false, columnDefinition = "VARCHAR(50)")
    private String cardUserName;
}
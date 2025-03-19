package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.demo.enums.CheckStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "check_management")
public class Check {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Long checkId;

    @Column(name = "check_reference", length = 50, unique = true)
    private String checkReference;

    @Column(name = "check_amount", nullable = false)
    private BigDecimal checkAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", length = 50, nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private CheckStatus checkStatus;

    @Column(name = "check_creation_date", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDate checkCreationDate;

    @Column(name = "check_expiration_date", columnDefinition = "TIMESTAMP")
    private LocalDate checkExpirationDate;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private Customer customer;
}

package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.enums.QRCodeStatus;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;

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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "qrcode_management")
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_id")
    private Long qrId;

    @Column(name = "terminal_id", length = 100)
    private String terminalId;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private Account sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private Account receiver;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "expiration_date", columnDefinition = "TIMESTAMP DEFAULT (NOW() + INTERVAL '1 minutes')")
    private LocalDateTime expirationDate;

    @Column(name = "transaction_status", length = 50, nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private TransactionStatus transactionStatus;

    @Column(name = "date_transaction", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateTransaction;

    @Column(name = "transaction_type", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'QR_PAYMENT'")
    private TransactionType transactionType;

    @Column(name = "qr_status", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private QRCodeStatus qrStatus;
}

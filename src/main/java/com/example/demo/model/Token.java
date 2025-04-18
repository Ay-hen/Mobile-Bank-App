package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "jwt_token")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jwt_id")
    private int id; 

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; 
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account; 

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token; 

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate; 

    @Column(name = "is_expired", nullable = false)
    private boolean isExpired; 

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate; 

    @Column(name = "revoked", nullable = false)
    private boolean revoked; 

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
        this.isExpired = false; 
        this.revoked = false;
    }
}
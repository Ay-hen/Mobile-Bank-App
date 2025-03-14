package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true) 
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "customer_management")
@PrimaryKeyJoinColumn(name = "user_id")
public class Customer extends User {


    @Column(name = "phone_number", nullable = false, length = 25)
    private String phoneNumber;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "biometric_enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean biometricEnabled;

    @Column(name = "security_question", length = 5)
    private String securityQuestion;

    @Column(name = "answer", length = 50)
    private String answer; 

    @Column(name = "cin", nullable = false, unique = true, length = 20)
    private String cin; 

    @Column(name = "is_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isVerified; 

    @Column(name = "birthday")
    private LocalDateTime birthday; 

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts; 

    

    protected Customer(CustomerBuilder<?, ?> b) {
        super(b);  
        this.phoneNumber = b.phoneNumber;
        this.deviceId = b.deviceId;
        this.biometricEnabled = b.biometricEnabled;
        this.securityQuestion = b.securityQuestion;
        this.answer = b.answer;
        this.cin = b.cin;
        this.isVerified = b.isVerified;
        this.birthday = b.birthday;
    }
}
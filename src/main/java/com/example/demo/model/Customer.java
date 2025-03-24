package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"account"})
@SuperBuilder
@AllArgsConstructor
@Entity
@Table(name = "customer_management")
@PrimaryKeyJoinColumn(name = "user_id")
public class Customer extends User {
    
    @Column(name = "customer_auth", unique = true) 
    private String usernameCustomer;

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
    private LocalDate birthday; 

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Account account; 

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "customer_notifications",
        joinColumns = @JoinColumn(name = "customer_id"),
        inverseJoinColumns = @JoinColumn(name = "notification_id")
    )
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();
    
    public Customer() {
        super();  
        this.notifications = new ArrayList<>(); 
    }

    protected Customer(CustomerBuilder<?, ?> b) {
        super(b);  
        this.usernameCustomer = b.usernameCustomer;
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
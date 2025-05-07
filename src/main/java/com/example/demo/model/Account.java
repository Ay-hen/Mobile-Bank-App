package com.example.demo.model;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(exclude = {"customer"}) 
@Table(name = "account_profile")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Balance balance;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "rib", nullable = false, unique = true)
    private String rib;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Customer customer;

    @Column(name = "account_password", nullable = false)
    private String accountPassword;  

    @Column(name = "account_authenticator")
    private String authenticator;    

    @Column(name = "account_currency", nullable = false, length = 6)
    private String accountCurrency;

    @Column(name = "account_status", length = 10)
    private String accountStatus;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Token> token;

    @OneToMany(mappedBy = "accountDebit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<A2ATransfer> a2aTransferDebit;

    @OneToMany(mappedBy = "accountCredit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<A2ATransfer> a2aTransferCredit;

    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;
}
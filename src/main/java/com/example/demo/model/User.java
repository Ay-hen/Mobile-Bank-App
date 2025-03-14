package com.example.demo.model;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "user_management")
@Inheritance(strategy = InheritanceType.JOINED)
public class User  implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId; 

    @Column(name = "user_name", nullable = false, length = 100)
    private String name;

    @Column(name = "user_auth", nullable = false, length = 50, unique = true)
    private String username;


    @Column(name = "user_email", nullable = false, unique = true, length = 255)
    private String userEmail; 

    @Column(name = "user_password", nullable = false, columnDefinition = "TEXT")
    private String userPassword;
    
    @Column(name = "role_name", nullable = false, columnDefinition = "TEXT")
    private String role;


    @Column(name = "last_active")
    private LocalDateTime lastActive; 

    @Column(name = "login_date")
    private LocalDateTime loginDate; 

    @Column(name = "is_online", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isOnline; 
    @Column(name = "user_creation_date", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime userCreationDate; 

    @Column(name = "is_blocked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isBlocked; 

    @Column(name = "login_first_time", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean loginFirstTime; 

    @Column(name = "max_password_attempts", nullable = false, columnDefinition = "INT DEFAULT 3")
    private int maxPasswordAttempts; 

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int failedLoginAttempts; 

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Token> token;

    @PrePersist
    protected void onCreate() {
        this.isOnline = false;     
        this.isBlocked = false;   
        this.loginFirstTime = true; 
        this.maxPasswordAttempts = 3; 
        this.failedLoginAttempts = 0;
        this.userCreationDate = LocalDateTime.now();
    }

    protected User(UserBuilder<?, ?> b) {
        this.userId = b.userId;
        this.name = b.name;
        this.userEmail = b.userEmail;
        this.userPassword = b.userPassword;
        this.role = b.role;
        this.lastActive = b.lastActive;
        this.loginDate = b.loginDate;
        this.isOnline = b.isOnline;
        this.userCreationDate = b.userCreationDate;
        this.isBlocked = b.isBlocked;
        this.loginFirstTime = b.loginFirstTime;
        this.maxPasswordAttempts = b.maxPasswordAttempts;
        this.failedLoginAttempts = b.failedLoginAttempts;
        this.username = b.username;
    }

    

    @Override
    public String getPassword() {
        return this.userPassword;
    }

    

    @Override
    public String getUsername() {
        return this.username; 
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.getRole()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

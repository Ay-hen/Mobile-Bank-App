package com.example.demo.auth;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String userName;
    private String email;
    private String password;
    private String role;
    private String authenticator;
    private String phoneNumber;
    private Long deviceId;
    private boolean biometricEnabled;
    private String securityQuestion;
    private String answer;
    private String cin;
    private boolean isVerified;
    private LocalDateTime birthday;
    private LocalDateTime sessionLastActive;
    private LocalDateTime lastActive;
    private LocalDateTime loginDate;
    private boolean isOnline;
    private LocalDateTime userCreationDate;
}

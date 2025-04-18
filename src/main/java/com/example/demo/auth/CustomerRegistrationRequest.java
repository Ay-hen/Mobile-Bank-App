package com.example.demo.auth;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegistrationRequest {
    private String name;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private String cin;
    private LocalDate birthday;
    private String branchCode;
    private LocalDateTime creationDate;
}
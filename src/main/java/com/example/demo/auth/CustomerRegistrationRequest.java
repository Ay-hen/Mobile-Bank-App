package com.example.demo.auth;

import java.time.LocalDate;

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
    private String userEmail;
    private String userPassword;
    private String phoneNumber;
    private String cin;
    private LocalDate birthday;
}
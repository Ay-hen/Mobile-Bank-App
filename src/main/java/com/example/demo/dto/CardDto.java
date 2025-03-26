package com.example.demo.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardDto {
    private Long id;
    private String cardNumber;
    private String branchName;
    private boolean isDeliver;
    private boolean isActivated;
    private String pin;
    private LocalDate expirationDate;
}

package com.example.demo.requests;

import java.math.BigDecimal;

import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeRequest {

    //private String terminalId = "MERCHANT-001";

    private String senderRib;

    private String receiverRib;

    private BigDecimal amount;


}

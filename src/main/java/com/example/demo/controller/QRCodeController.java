package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.requests.QRCodeRequest;
import com.example.demo.service.QRCodeService;

@RestController
@RequestMapping("/api/qrcodes")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    @PostMapping
    public ResponseEntity<Map<String, String>> generateQRCode(
            @RequestBody QRCodeRequest request) {
        
        String qrImage = qrCodeService.generateQRCode(
            "MERCHANT-001",
            request.getSenderRib(),
            request.getReceiverRib(),
            request.getAmount()
        );
        
        return ResponseEntity.ok(Map.of(
            "qrImage", qrImage,
            "message", "QR code generated successfully"
        ));
    }
}

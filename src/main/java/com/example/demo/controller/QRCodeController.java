package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.QRCode;
import com.example.demo.requests.QRCodeRequest;
import com.example.demo.service.QRCodeResponse;
import com.example.demo.service.QRCodeService;

@RestController
@RequestMapping("/api/qrcodes")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    @PostMapping
    public ResponseEntity<Map<String, ?>> generateQRCode(@RequestBody QRCodeRequest request) {
        QRCodeResponse response = qrCodeService.generateQRCode(
            "MERCHANT-001",
            request.getSenderRib(),
            request.getReceiverRib(),
            request.getAmount()
        );

        return ResponseEntity.ok(Map.of(
            "qrImage", response.getQrImage(),
            "id", response.getQrCodeId(),
            "message", "QR code generated successfully"
        ));
    }

    @PostMapping("/confirm/{qrId}")
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable Long qrId) {
        QRCode confirmedQR = qrCodeService.confirmPayment(qrId);

        return ResponseEntity.ok(Map.of(
            "transactionStatus", confirmedQR.getTransactionStatus().toString(),
            "message", "Payment confirmed successfully"
        ));
    }

    @GetMapping("/history/{rib}")
    public ResponseEntity<List<QRCode>> getQRTransactionHistory(@PathVariable String rib) {
        List<QRCode> history = qrCodeService.getQRTransactionHistory(rib);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/transactions/newest")
    public ResponseEntity<List<QRCode>> getAllQRTransactionsNewest() {
        List<QRCode> transactions = qrCodeService.getAllQRTransactionsNewest();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/oldest")
    public ResponseEntity<List<QRCode>> getAllQRTransactionsOldest() {
        List<QRCode> transactions = qrCodeService.getAllQRTransactionsOldest();
        return ResponseEntity.ok(transactions);
    }
}

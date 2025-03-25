package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.QRCode;
import com.example.demo.service.QRCodeResponse;
import com.example.demo.service.QRCodeService;

@RestController
@RequestMapping("/api/qrcodes")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    /**
     * Generates a new receive QR code for the specified account
     * 
     * @param receiverRib The RIB of the receiving account
     * @return QR code response with ID and image
     */
    @PostMapping("/generate")
    public ResponseEntity<QRCodeResponse> generateQRCode(@RequestParam String receiverRib) {
        QRCodeResponse response = qrCodeService.generateReceiveQRCode(receiverRib);
        return ResponseEntity.ok(response);
    }

    /**
     * Initializes a payment using a QR code
     * 
     * @param qrId The ID of the QR code
     * @param senderRib The RIB of the sending account
     * @param amount The amount to transfer
     * @return The initialized QR transaction
     */
    @PostMapping("/initialize-payment")
    public ResponseEntity<String> initializePayment(
            @RequestParam Long qrId,
            @RequestParam String senderRib,
            @RequestParam BigDecimal amount) {
        qrCodeService.initializePayment(qrId, senderRib, amount);
        return ResponseEntity.ok("Opperation is done");
    }

    /**
     * Confirms and completes a QR code payment
     * 
     * @param qrId The ID of the QR code transaction
     * @return The completed transaction
     */
    @PostMapping("/confirm-payment")
    public ResponseEntity<String> confirmPayment(@RequestParam Long qrId) {
        qrCodeService.confirmPayment(qrId);
        return ResponseEntity.ok("Success");
    }

    /**
     * Gets QR transaction history for an account
     * 
     * @param rib The account RIB to get history for
     * @return List of QR transactions
     */
    @GetMapping("/history")
    public ResponseEntity<List<QRCode>> getTransactionHistory(@RequestParam String rib) {
        List<QRCode> history = qrCodeService.getQRTransactionHistory(rib);
        return ResponseEntity.ok(history);
    }

    /**
     * Gets all QR transactions, sorted newest first
     * 
     * @return List of all QR transactions
     */
    @GetMapping("/all/newest")
    public ResponseEntity<List<QRCode>> getAllTransactionsNewest() {
        List<QRCode> transactions = qrCodeService.getAllQRTransactionsNewest();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Gets all QR transactions, sorted oldest first
     * 
     * @return List of all QR transactions
     */
    @GetMapping("/all/oldest")
    public ResponseEntity<List<QRCode>> getAllTransactionsOldest() {
        List<QRCode> transactions = qrCodeService.getAllQRTransactionsOldest();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Manually triggers expiration of pending QR codes
     * 
     * @return Confirmation message
     */
    @PostMapping("/expire-pending")
    public ResponseEntity<String> expirePendingQRCodes() {
        qrCodeService.expirePendingQRCodes();
        return ResponseEntity.ok("Pending QR codes expiration process completed");
    }
}

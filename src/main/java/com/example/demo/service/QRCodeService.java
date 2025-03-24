package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import com.example.demo.enums.QRCodeStatus;
import com.example.demo.enums.TransactionStatus;
import com.example.demo.enums.TransactionType;
import com.example.demo.exception.QRCodeGenerationException;
import com.example.demo.model.Account;
import com.example.demo.model.QRCode;
import com.example.demo.repository.AccountRepo;
import com.example.demo.repository.QRCodeRepo;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


@Service
@Transactional
public class QRCodeService {

    @Autowired
    private QRCodeRepo qrCodeRepo;
    
    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private NotificationService notificationService;
    
    @Value("${qr.code.width:200}")
    private int qrCodeWidth;
    
    @Value("${qr.code.height:200}")
    private int qrCodeHeight;
    
    @Value("${qr.code.expiration.minutes:5}")
    private int expirationMinutes;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> expirationTask;



    /**
     * Generates a QR code for payment transactions
     * 
     * @param terminalId Terminal identifier
     * @param senderRib Sender account RIB
     * @param receiverRib Receiver account RIB
     * @param amount Transaction amount
     * @return Base64 encoded QR code image
     */
    public String generateQRCode(String terminalId, String senderRib, String receiverRib, BigDecimal amount) {
        try {
            // 1. Validate input parameters
            if (terminalId == null || terminalId.isBlank()) {
                throw new IllegalArgumentException("Terminal ID cannot be empty");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be a positive value");
            }

            // 2. Validate accounts
            Account sender = accountRepo.findByRib(senderRib)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found with RIB: " + senderRib));
            
            Account receiver = accountRepo.findByRib(receiverRib)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found with RIB: " + receiverRib));

            // 3. Create QR code data
            String qrData = buildQRData(terminalId, senderRib, receiverRib, amount);
            
            // 4. Generate QR code image
            byte[] qrImageBytes = generateQRImage(qrData, qrCodeWidth, qrCodeHeight);
            String base64Image = Base64.getEncoder().encodeToString(qrImageBytes);

            // 5. Create and persist QR code record
            QRCode qrCode = QRCode.builder()
                .terminalId(terminalId)
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .expirationDate(LocalDateTime.now().plusMinutes(expirationMinutes))
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(TransactionType.QR_PAYMENT)
                .qrStatus(QRCodeStatus.ACTIVE)
                .dateTransaction(LocalDateTime.now())
                .build();

            qrCodeRepo.save(qrCode);

            startExpirationTask();

            return base64Image;
            
        } catch (IllegalArgumentException e) {
            throw new QRCodeGenerationException("Validation error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new QRCodeGenerationException("Unexpected error generating QR code", e);
        }
    }

    private String buildQRData(String terminalId, String senderRib, String receiverRib, BigDecimal amount) {
        return String.format(
            "QRPAY  |  TERMINAL : %s  |  SENDER : %s  |  RECEIVER : %s  |  AMOUNT : %s  |  CURRENCY : MAD",
            terminalId,
            senderRib,
            receiverRib,
            amount.toString()
        );
    }

    private byte[] generateQRImage(String data, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        
        return pngOutputStream.toByteArray();
    }

    public QRCode confirmPayment(Long qrId) {
        QRCode qrCode = qrCodeRepo.findById(qrId)
            .orElseThrow(() -> new RuntimeException("QR transaction not found"));
    
        // Validate payment conditions
        if (qrCode.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("QR code has expired");
        }
    
        if (qrCode.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction already processed");
        }
    
        // Check sender balance
        if (qrCode.getSender().getAmount().compareTo(qrCode.getAmount()) < 0) {
            qrCode.setTransactionStatus(TransactionStatus.FAILED);
            qrCode.setQrStatus(QRCodeStatus.INACTIVE);
            qrCodeRepo.save(qrCode);
            throw new RuntimeException("Insufficient funds in sender account");
        }
    
        // Process payment
        qrCode.getSender().setAmount(
            qrCode.getSender().getAmount().subtract(qrCode.getAmount())
        );
        qrCode.getReceiver().setAmount(
            qrCode.getReceiver().getAmount().add(qrCode.getAmount())
        );
    
        // Update status
        qrCode.setTransactionStatus(TransactionStatus.COMPLETED);
        qrCode.setQrStatus(QRCodeStatus.INACTIVE);
        qrCode.setDateTransaction(LocalDateTime.now());
    
        // Save changes
        accountRepo.save(qrCode.getSender());
        accountRepo.save(qrCode.getReceiver());
        return qrCodeRepo.save(qrCode);
    }

    /*@Scheduled(fixedRate = 60000) // Runs every minute
    public void expirePendingQRCodes() {
        LocalDateTime now = LocalDateTime.now();
        List<QRCode> expiredCodes = qrCodeRepo.findByExpirationDateBeforeAndTransactionStatus(
            now, TransactionStatus.PENDING
        );

        expiredCodes.forEach(qrCode -> {
            qrCode.setTransactionStatus(TransactionStatus.FAILED);
            qrCode.setQrStatus(QRCodeStatus.EXPIRED);
            qrCodeRepo.save(qrCode);
            
            // Optional: Notify users
            notificationService.sendPaymentExpiredNotification(
                qrCode.getSender(), 
                qrCode.getReceiver(), 
                qrCode.getAmount()
            );
        });
    }*/

    public List<QRCode> getQRTransactionHistory(String rib) {

        Account account = accountRepo.findByRib(rib)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    
        return qrCodeRepo.findBySenderOrReceiver(account, account);
    }

    public List<QRCode> getAllQRTransactionsNewest() {
        return qrCodeRepo.findAll(Sort.by(Sort.Direction.DESC, "dateTransaction"));
    }

    public List<QRCode> getAllQRTransactionsOldest() {
        return qrCodeRepo.findAll(Sort.by(Sort.Direction.ASC, "dateTransaction"));
    }

    private void startExpirationTask() {
        if (expirationTask == null || expirationTask.isCancelled()) {
            expirationTask = taskScheduler.schedule(this::expirePendingQRCodes, 
                Instant.now().plus(Duration.ofMinutes(expirationMinutes))
            );
        }
    }

    private void stopExpirationTask() {
        if (expirationTask != null && !expirationTask.isCancelled()) {
            expirationTask.cancel(false);
        }
    }

    public void expirePendingQRCodes() {
        LocalDateTime now = LocalDateTime.now();
        List<QRCode> expiredCodes = qrCodeRepo.findByExpirationDateBeforeAndTransactionStatus(now, TransactionStatus.PENDING);

        if (!expiredCodes.isEmpty()) {
            expiredCodes.forEach(qrCode -> {
                qrCode.setTransactionStatus(TransactionStatus.FAILED);
                qrCode.setQrStatus(QRCodeStatus.EXPIRED);
                qrCodeRepo.save(qrCode);

                // Notify users
                notificationService.sendPaymentExpiredNotification(qrCode.getSender(), qrCode.getReceiver(), qrCode.getAmount());
            });

            // Stop task after expiration
            stopExpirationTask();
        }
    }
}
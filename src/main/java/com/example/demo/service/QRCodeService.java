package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
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
    
    @Value("${qr.code.expiration.minutes:15}")
    private int expirationMinutes;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> expirationTask;

    /**
     * Generates a QR code for receiving payments
     * 
     * @param receiverRib Receiver's account RIB
     * @return QR code response with unique identifier and base64 encoded image
     */
    public QRCodeResponse generateReceiveQRCode(String receiverRib) {
        try {
            // 1. Validate receiver account
            Account receiver = accountRepo.findByRib(receiverRib)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found with RIB: " + receiverRib));
    
            QRCode qrCode = QRCode.builder()
                .receiver(receiver)
                .sender(null)
                .expirationDate(LocalDateTime.now().plusMinutes(expirationMinutes))
                .dateTransaction(LocalDateTime.now())
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(TransactionType.QR_PAYMENT)
                .qrStatus(QRCodeStatus.ACTIVE)
                .amount(BigDecimal.ZERO)
                .dateTransaction(LocalDateTime.now())
                .build();
    
            qrCode = qrCodeRepo.save(qrCode);

            String qrData = buildReceiverQRData(receiver, qrCode.getQrId());

            byte[] qrImageBytes = generateQRImage(qrData, qrCodeWidth, qrCodeHeight);
            String base64Image = Base64.getEncoder().encodeToString(qrImageBytes);

            startExpirationTask();
    
            return new QRCodeResponse(qrCode.getQrId(), base64Image);
            
        } catch (Exception e) {
            throw new QRCodeGenerationException("Error generating receive QR code", e);
        }
    }

    /**
     * Initiates a payment by scanning a receive QR code
     * 
     * @param qrId Unique identifier of the receive QR code
     * @param senderRib Sender's account RIB
     * @param amount Amount to be transferred
     * @return Confirmed QR code transaction
     */
    public QRCode initializePayment(Long qrId, String senderRib, BigDecimal amount) {
        // 1. Validate QR code
        QRCode qrCode = qrCodeRepo.findById(qrId)
            .orElseThrow(() -> new RuntimeException("QR code not found"));
    
        // 2. Validate QR code status
        if (qrCode.getExpirationDate().isBefore(LocalDateTime.now())) {
            qrCode.setTransactionStatus(TransactionStatus.FAILED);
            qrCode.setQrStatus(QRCodeStatus.EXPIRED);
            qrCodeRepo.save(qrCode);
            throw new RuntimeException("QR code has expired");
        }
    
        if (qrCode.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("QR code is already in use");
        }
    
        // 3. Validate sender account
        Account sender = accountRepo.findByRib(senderRib)
            .orElseThrow(() -> new RuntimeException("Sender account not found"));
    
        // 4. Validate payment amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
    
        // 5. Update QR code with sender and amount details
        qrCode.setSender(sender);
        qrCode.setAmount(amount);
        qrCode.setTransactionStatus(TransactionStatus.INITIALIZED);
        qrCode.setTerminalId(UUID.randomUUID().toString()); // Generate unique terminal ID
    
        return qrCodeRepo.save(qrCode);
    }

    /**
     * Confirms and completes the payment
     * 
     * @param qrId Unique identifier of the QR code transaction
     * @return Completed QR code transaction
     */
    public QRCode confirmPayment(Long qrId) {
        QRCode qrCode = qrCodeRepo.findById(qrId)
            .orElseThrow(() -> new RuntimeException("QR transaction not found"));
    
        // 1. Validate transaction status
        if (qrCode.getTransactionStatus() != TransactionStatus.INITIALIZED) {
            throw new RuntimeException("Transaction cannot be confirmed");
        }
    
        // 2. Check sender balance
        if (qrCode.getSender().getAmount().compareTo(qrCode.getAmount()) < 0) {
            qrCode.setTransactionStatus(TransactionStatus.FAILED);
            qrCode.setQrStatus(QRCodeStatus.INACTIVE);
            qrCodeRepo.save(qrCode);
            throw new RuntimeException("Insufficient funds");
        }
    
        // 3. Process payment
        Account sender = qrCode.getSender();
        Account receiver = qrCode.getReceiver();
        
        sender.setAmount(sender.getAmount().subtract(qrCode.getAmount()));
        receiver.setAmount(receiver.getAmount().add(qrCode.getAmount()));
    
        // 4. Update transaction status
        qrCode.setTransactionStatus(TransactionStatus.COMPLETED);
        qrCode.setQrStatus(QRCodeStatus.INACTIVE);
        qrCode.setDateTransaction(LocalDateTime.now());
    
        // 5. Save changes
        accountRepo.save(sender);
        accountRepo.save(receiver);
        return qrCodeRepo.save(qrCode);
    }

    /**
     * Builds QR code data for the receiver
     * 
     * @param receiver Receiver's account
     * @return Formatted QR code data string
     */
    private String buildReceiverQRData(Account receiver, Long id) {
        return String.format(
            "QR ID : %s  | RECEIVER : %s  |   RIB : %s   |  NAME : %s",
            id,
            receiver.getRib(),
            receiver.getRib(),
            receiver.getCustomer().getName()
        );
    }

    /**
     * Generates QR code image
     * 
     * @param data QR code data
     * @param width Image width
     * @param height Image height
     * @return QR code image bytes
     */
    private byte[] generateQRImage(String data, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        
        return pngOutputStream.toByteArray();
    }
    
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
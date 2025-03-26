package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.NotificationDto;
import com.example.demo.model.Account;
import com.example.demo.model.Check;
import com.example.demo.model.Customer;
import com.example.demo.model.Notification;
import com.example.demo.repository.CustomerRepo;
import com.example.demo.repository.NotificationRepo;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepo notificationRepo;
    @Autowired
    private CustomerRepo customerRepo;
    
    public void sendNotification(String type, String title,String message, String senderModule, List<Customer> customers) {
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .senderModule(senderModule)
                .isRead(false)
                .createdDate(LocalDateTime.now())
                .build();

        notificationRepo.save(notification);

        for (Customer customer : customers) {
            customer.getNotifications().add(notification); 
            customerRepo.save(customer);
        }

        notificationRepo.save(notification);
        customerRepo.saveAll(customers);
    }


    public List<NotificationDto> getNotificationsForCustomer(String username) {
        Customer customer = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        return customer.getNotifications().stream()
                .map(notification -> NotificationDto.builder()
                        .id(notification.getId())
                        .date(notification.getCreatedDate())
                        .type(notification.getType())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .isRead(notification.isRead())
                        .build())
                .collect(Collectors.toList());
    }

    public List<Notification> getUnreadNotifications(Customer customer) {
        return notificationRepo.findByCustomersAndIsReadFalse(customer);
    }


    public boolean markAsRead(Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId).orElse(null);
        if (notification == null) {
            return false;
        }
        notification.setRead(true);
        notificationRepo.save(notification);
        return true;
    }


    public void sendPaymentExpiredNotification(Account sender, Account receiver, BigDecimal amount) {
        String title = "Payment Expired";
        String message = String.format("Your payment of %s MAD to %s has expired. No funds were transferred.", amount.toString(), receiver.getRib());
        sendNotification("payment", title, message, "Bank", List.of(sender.getCustomer()));
    }

    public void markAllAsRead() {
        List<Notification> notifications = notificationRepo.findByIsReadFalse();
        for (Notification notification : notifications) {
            notification.setRead(true);
            notificationRepo.save(notification);
        }
    }


    public void sendCheckIssuanceNotification(Customer customer, Check savedCheck) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendCheckIssuanceNotification'");
    }


    
}

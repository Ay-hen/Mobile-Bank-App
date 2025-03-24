package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                .build();

        notificationRepo.save(notification);

        for (Customer customer : customers) {
            customer.getNotifications().add(notification); 
            customerRepo.save(customer);
        }

        notificationRepo.save(notification);
        customerRepo.saveAll(customers);
    }


    public List<Notification> getNotificationsForCustomer(String username) {
        Customer customer = customerRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return customer.getNotifications();
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
}

package com.example.demo.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Account;
import com.example.demo.model.Customer;
import com.example.demo.model.Notification;
import com.example.demo.service.NotificationService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import java.util.List;

@Component
public class DatabaseChangeListener {

    private static NotificationService notificationService;

    @Autowired
    public void setNotificationService(NotificationService service) {
        notificationService = service; // Static injection
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    @Transactional
    public void onDatabaseChange(Object entity) {
        if (entity instanceof Notification) {
            return; // Skip notifications to avoid infinite loops
        }

        String entityName = entity.getClass().getSimpleName();
        String message = entityName + " has been updated.";

        List<Customer> customers = fetchRelevantCustomers(entity);

        if (notificationService != null) {
            notificationService.sendNotification("UPDATE", "Updated", message, entityName, customers);
        } else {
            System.err.println("NotificationService is not injected!");
        }
    }

    private List<Customer> fetchRelevantCustomers(Object entity) {
        if (entity instanceof Customer) {
            return List.of((Customer) entity);
        } else if (entity instanceof Account) {
            Account account = (Account) entity;
            return List.of(account.getCustomer());
        }
        return List.of();
    }
}

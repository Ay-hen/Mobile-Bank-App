package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Notification;
import com.example.demo.model.Customer;

public interface NotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByCustomers_UserId(Long customerId);
    List<Notification> findByCustomersAndIsReadFalse(Customer customer);
    List<Notification> findByIsReadFalse();
}
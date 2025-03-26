package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.NotificationDto;
import com.example.demo.service.NotificationService;

import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/notification")
@CrossOrigin(origins = "*")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/get-notification/{username}")
    public List<NotificationDto> getNotification(@PathVariable String username) {
        return notificationService.getNotificationsForCustomer(username);
    }

    @GetMapping("/mark-as-read/{id}")
    public void markAsRead(Long id) {
        notificationService.markAsRead(id);
    }

    @GetMapping("/mark-all-as-read")
    public void markAllAsRead() {
        notificationService.markAllAsRead();
    }
}

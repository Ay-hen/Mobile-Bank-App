package com.example.demo.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;
    
    @Column(name = "notification_type", nullable = false, length = 50)
    private String type;

    @Column(name = "notification_message", nullable = false, length = 255)
    private String message;

    @Column(name = "is_read", columnDefinition = "boolean default false")
    private boolean isRead;

    @Column(name = "sender_module", nullable = false)
    private String senderModule;

    @ManyToMany(mappedBy = "notifications")
    private List<Customer> customers;
}

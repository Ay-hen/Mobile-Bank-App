package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "notification_management")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "notification_title", nullable = false, length = 50)
    private String title;
    
    @Column(name = "notification_type", nullable = false, length = 50)
    private String type;

    @Column(name = "notification_message", nullable = false, length = 255)
    private String message;

    @Column(name = "is_read", columnDefinition = "boolean default false")
    private boolean isRead;

    @Column(name = "sender_module", nullable = false)
    private String senderModule;

    @ManyToMany(mappedBy = "notifications")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @Column(name = "created_date", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdDate;
}

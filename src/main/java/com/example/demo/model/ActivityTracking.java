package com.example.demo.model;



import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activity_tracking")
public class ActivityTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  

    @Column(name = "username", nullable = false, length = 100)
    private String userName; 

    @Column(name = "user_full_name", nullable = false, length = 255)
    private String userFullName;

    @Column(name = "operation_type", nullable = false, length = 100)
    private String operationType; 

    @Column(name = "operation_date", nullable = false)
    private LocalDateTime operationDate;

    @Column(name = "user_ip", length = 45)
    private String userIp;

    @Column(name = "operation_description", columnDefinition = "TEXT")
    private String operationDescription;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        this.operationDate = LocalDateTime.now();
    }
}

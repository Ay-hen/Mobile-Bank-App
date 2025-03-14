package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.ActivityTracking;

@Repository
public interface ActivityTrackingRepo extends JpaRepository<ActivityTracking, Long> {
    
}

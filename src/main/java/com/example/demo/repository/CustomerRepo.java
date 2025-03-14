package com.example.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Customer;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {
    boolean existsByUserEmail(String userEmail);
    boolean existsByCin(String cin);
    boolean existsByUsername(String username);
}

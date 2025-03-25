package com.example.demo.repository;

import com.example.demo.model.Check;
import com.example.demo.model.Customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckRepo extends JpaRepository<Check, Long> {
    List<Check> findByCustomer(Customer customer);
    Optional<Check> findById(Long id);
}

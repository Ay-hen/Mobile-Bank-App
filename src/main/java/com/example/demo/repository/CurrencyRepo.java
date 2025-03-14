package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Currency;

@Repository
public interface CurrencyRepo extends JpaRepository<Currency, Long> {
    Optional<Currency> findByCurrencyCode(String currencyCode); 
}

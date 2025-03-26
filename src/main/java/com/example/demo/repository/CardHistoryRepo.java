package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Account;
import com.example.demo.model.CardHistory;

@Repository
public interface CardHistoryRepo extends JpaRepository<CardHistory, Long> {

    List<CardHistory> findByCardAccount(Account account);
    
}

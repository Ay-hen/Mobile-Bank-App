package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Account;
import com.example.demo.model.Card;

@Repository
public interface CardRepo extends JpaRepository<Card,Long> {
    List<Card> findByAccount(Account account);
}

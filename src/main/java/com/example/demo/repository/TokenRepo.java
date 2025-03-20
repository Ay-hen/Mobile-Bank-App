package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Token;

@Repository
public interface TokenRepo extends JpaRepository<Token, Integer> {
    public Optional<Token> findById(Long token);
    public Optional<Token> findByToken(String token);
    public void deleteByToken(String token);
}
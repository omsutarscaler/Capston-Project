package com.example.UserAuthService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.UserAuthService.models.Token;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {

    Optional<Token> findTokenByValue(String value);

    @Modifying
    @Query("update Token t set t.isActive = false where t.user.id = :userId and t.isActive = true")
    void revokeByUserId(long userId);
}

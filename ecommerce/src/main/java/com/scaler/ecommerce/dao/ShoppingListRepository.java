package com.scaler.ecommerce.dao;

import java.util.Optional;

import com.scaler.ecommerce.model.ShoppingList;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShoppingList s where s.userId = :id")
    Optional<ShoppingList> lockById(long id);
}

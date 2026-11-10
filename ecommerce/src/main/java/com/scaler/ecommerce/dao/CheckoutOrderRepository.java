package com.scaler.ecommerce.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scaler.ecommerce.model.CheckoutOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CheckoutOrderRepository extends JpaRepository<CheckoutOrder, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CheckoutOrder o where o.id = :id")
    Optional<CheckoutOrder> lockById(UUID id);

    @Query("select o.id from CheckoutOrder o where o.nextCheckAt <= :now order by o.nextCheckAt, o.id")
    List<UUID> findDueChecks(Instant now, Pageable page);
}

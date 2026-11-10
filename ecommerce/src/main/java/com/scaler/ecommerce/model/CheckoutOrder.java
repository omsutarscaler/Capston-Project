package com.scaler.ecommerce.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CheckoutOrder {
    @Id
    private UUID id;
    @Version
    private Long version;
    private long userId;
    private long productId;
    private String email;
    private String title;
    private long amountMinor;
    private String currency;
    private Instant createdAt;
    private String paymentId;
    @Column(length = 2048)
    private String checkoutUrl;
    private boolean paid;
    private boolean expired;
    private Instant receiptSentAt;
    private Instant nextCheckAt;
}

package com.scaler.ecommerce.model;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "user_id"}))
@Getter
@Setter
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id")
    private long productId;
    @Column(name = "user_id")
    private long userId;
    private int rating;
    @Column(length = 1000)
    private String comment;
    private Instant updatedAt;
}

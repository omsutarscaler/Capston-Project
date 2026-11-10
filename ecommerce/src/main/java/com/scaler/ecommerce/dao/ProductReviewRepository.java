package com.scaler.ecommerce.dao;

import java.util.Optional;
import com.scaler.ecommerce.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Page<ProductReview> findByProductId(long productId, Pageable page);
    Optional<ProductReview> findByProductIdAndUserId(long productId, long userId);
}

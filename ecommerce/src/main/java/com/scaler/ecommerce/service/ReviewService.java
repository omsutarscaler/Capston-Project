package com.scaler.ecommerce.service;

import java.time.Instant;
import com.scaler.ecommerce.dao.ProductRepository;
import com.scaler.ecommerce.dao.ProductReviewRepository;
import com.scaler.ecommerce.exceptions.ProductNotFoundException;
import com.scaler.ecommerce.model.ProductReview;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ProductReviewRepository reviews;
    private final ProductRepository products;

    public ReviewService(ProductReviewRepository reviews, ProductRepository products) {
        this.reviews = reviews;
        this.products = products;
    }

    public PagedModel<Review> list(long productId, int page) {
        return new PagedModel<>(reviews.findByProductId(productId,
                PageRequest.of(page, 10, Sort.by("updatedAt", "id").descending())).map(Review::from));
    }

    @Transactional
    public Review save(long productId, long userId, int rating, String comment) {
        products.lockById(productId).orElseThrow(() -> new ProductNotFoundException("Product " + productId + " does not exist"));
        var review = reviews.findByProductIdAndUserId(productId, userId).orElseGet(ProductReview::new);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment(comment);
        review.setUpdatedAt(Instant.now());
        return Review.from(reviews.save(review));
    }

    public record Review(int rating, String comment, Instant updatedAt) {
        static Review from(ProductReview review) {
            return new Review(review.getRating(), review.getComment(), review.getUpdatedAt());
        }
    }
}

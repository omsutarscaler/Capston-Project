package com.scaler.ecommerce.controller;

import com.scaler.ecommerce.service.ReviewService;
import com.scaler.ecommerce.service.ReviewService.Review;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/{id}")
    public PagedModel<Review> list(@PathVariable long id,
                                  @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return reviews.list(id, page);
    }

    @PutMapping("/{id}")
    public Review save(@PathVariable long id, @Valid @RequestBody Rating request,
                       @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        return reviews.save(id, CheckoutController.userId(user), request.rating(), request.comment());
    }

    public record Rating(@NotNull @Min(1) @Max(5) Integer rating, @NotBlank @Size(max = 1000) String comment) {}
}

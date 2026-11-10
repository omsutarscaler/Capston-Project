package com.scaler.ecommerce.controller;

import com.scaler.ecommerce.service.ShoppingService;
import com.scaler.ecommerce.service.ShoppingService.Contents;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shopping")
public class ShoppingController {
    private final ShoppingService shopping;

    public ShoppingController(ShoppingService shopping) {
        this.shopping = shopping;
    }

    @GetMapping
    public Contents get(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        return shopping.contents(CheckoutController.userId(user));
    }

    @PutMapping("/cart/{id}")
    public Contents cart(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user, @PathVariable long id,
                         @Valid @RequestBody Quantity request) {
        return shopping.setQuantity(CheckoutController.userId(user), id, request.quantity());
    }

    @PutMapping("/wishlist/{id}")
    public Contents wishlist(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal user, @PathVariable long id,
                             @Valid @RequestBody Saved request) {
        return shopping.setSaved(CheckoutController.userId(user), id, request.saved());
    }

    public record Quantity(@NotNull @Min(0) @Max(ShoppingService.MAX_CART_ITEMS) Integer quantity) {}
    public record Saved(@NotNull Boolean saved) {}
}

package com.scaler.ecommerce.controller;

import java.util.UUID;

import com.scaler.ecommerce.config.OpenApiConfig;
import com.scaler.ecommerce.service.CheckoutService;
import com.scaler.ecommerce.service.CheckoutService.Checkout;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/checkout")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CheckoutController {
    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping
    public Checkout start(@Valid @RequestBody Request request,
                          @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        return checkout.start(request.orderId(), request.productId(), userId(user), user.getAttribute("email"));
    }

    @PostMapping("/{id}/confirm")
    public Checkout confirm(@PathVariable UUID id, @AuthenticationPrincipal OAuth2AuthenticatedPrincipal user) {
        return checkout.confirm(id, userId(user));
    }

    private static long userId(OAuth2AuthenticatedPrincipal user) {
        Long id = user.getAttribute("user_id");
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User Auth Service must supply account details");
        }
        return id;
    }

    public record Request(@NotNull UUID orderId, @NotNull @Positive Long productId) {}
}

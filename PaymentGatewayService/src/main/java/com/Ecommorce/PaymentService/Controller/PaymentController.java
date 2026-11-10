package com.Ecommorce.PaymentService.Controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.Ecommorce.PaymentService.Dtos.PaymentResponse;
import com.Ecommorce.PaymentService.Dtos.RequestPaymentDto;
import com.Ecommorce.PaymentService.Service.paymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final paymentService service;
    private final String serviceKey;

    public PaymentController(paymentService service, @Value("${payment.service-key}") String serviceKey) {
        this.service = service;
        this.serviceKey = serviceKey;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestHeader(value = "X-Service-Key", defaultValue = "") String key,
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[a-zA-Z0-9._-]{1,128}") String idempotencyKey,
            @Valid @RequestBody RequestPaymentDto request) {
        authorize(key);
        var payment = service.createPayment(request, idempotencyKey);
        return ResponseEntity.created(URI.create("/payment/" + payment.id())).body(payment);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@RequestHeader(value = "X-Service-Key", defaultValue = "") String key,
            @PathVariable @Pattern(regexp = "cs_[a-zA-Z0-9_]{1,200}") String id) {
        authorize(key);
        return service.getPayment(id);
    }

    private void authorize(String key) {
        if (serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment service access is not configured");
        }
        if (!MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment service key");
        }
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(@RequestHeader(value = "X-Service-Key", defaultValue = "") String key,
            @PathVariable @Pattern(regexp = "cs_[a-zA-Z0-9_]{1,200}") String id) {
        authorize(key);
        return service.cancelPayment(id);
    }
}

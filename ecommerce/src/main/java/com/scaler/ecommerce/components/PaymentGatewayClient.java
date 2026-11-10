package com.scaler.ecommerce.components;

import java.time.Duration;
import java.util.UUID;

import com.scaler.ecommerce.model.CheckoutOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentGatewayClient {
    // Keep the Stripe metadata and idempotency namespace stable across the service rename.
    public static final String SOURCE = "capston";
    private final RestTemplate http;
    private final String storefrontUrl;
    private final String serviceKey;

    public PaymentGatewayClient(RestTemplateBuilder builder,
                                @Value("${payment.gateway-url}") String gatewayUrl,
                                @Value("${payment.service-key}") String serviceKey,
                                @Value("${checkout.storefront-url}") String storefrontUrl) {
        http = builder.rootUri(gatewayUrl).connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(20)).build();
        this.storefrontUrl = storefrontUrl;
        this.serviceKey = serviceKey;
    }

    public Payment create(CheckoutOrder order) {
        HttpHeaders headers = headers();
        headers.set("Idempotency-Key", order.getId().toString());
        var body = new PaymentRequest(SOURCE, order.getId().toString(), order.getAmountMinor(),
                order.getCurrency(), order.getTitle(), returnUrl(order.getId(), "success"),
                returnUrl(order.getId(), "cancel"));
        return exchange("/payment", HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    public Payment get(String id) {
        return exchange("/payment/" + id, HttpMethod.GET, new HttpEntity<>(headers()));
    }

    private HttpHeaders headers() {
        if (serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment gateway access is not configured");
        }
        var headers = new HttpHeaders();
        headers.set("X-Service-Key", serviceKey);
        return headers;
    }

    private String returnUrl(UUID id, String result) {
        return storefrontUrl + "?order=" + id + "&result=" + result;
    }

    private Payment exchange(String path, HttpMethod method, HttpEntity<?> request) {
        try {
            Payment payment = http.exchange(path, method, request, Payment.class).getBody();
            if (payment != null && payment.id() != null && payment.id().matches("cs_[a-zA-Z0-9_]{1,200}")) {
                return payment;
            }
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway request failed");
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway returned an invalid response");
    }

    private record PaymentRequest(String source, String reference, long amountMinor, String currency,
                                  String description, String successUrl, String cancelUrl) {}

    public record Payment(String id, String source, String reference, Long amountMinor, String currency,
                          String checkoutUrl, String checkoutStatus, String paymentStatus) {}
}

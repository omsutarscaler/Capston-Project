package com.Ecommorce.PaymentService.Dtos;

import java.net.URI;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequestPaymentDto(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String source,
        @NotBlank @Size(max = 200) String reference,
        @NotNull @Positive @Max(99999999) Long amountMinor,
        @NotBlank @Pattern(regexp = "[a-zA-Z]{3}") String currency,
        @NotBlank @Size(max = 200) String description,
        @NotNull URI successUrl,
        @NotNull URI cancelUrl) {

    @AssertTrue(message = "Return URLs must use HTTPS or loopback HTTP and must have a host")
    public boolean isReturnUrlsValid() {
        return isAllowedUrl(successUrl) && isAllowedUrl(cancelUrl);
    }

    private static boolean isAllowedUrl(URI url) {
        if (url == null || url.getHost() == null || url.getUserInfo() != null) {
            return false;
        }
        String host = url.getHost();
        return "https".equalsIgnoreCase(url.getScheme()) || ("http".equalsIgnoreCase(url.getScheme())
                && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host)));
    }
}

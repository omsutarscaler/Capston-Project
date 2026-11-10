package com.Ecommorce.PaymentService.Dtos;

public record PaymentResponse(String id, String source, String reference, Long amountMinor, String currency,
                              String checkoutUrl, String checkoutStatus, String paymentStatus) {}

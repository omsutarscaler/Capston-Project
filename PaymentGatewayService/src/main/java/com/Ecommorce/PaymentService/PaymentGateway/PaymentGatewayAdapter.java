package com.Ecommorce.PaymentService.PaymentGateway;

import com.Ecommorce.PaymentService.Dtos.PaymentResponse;
import com.Ecommorce.PaymentService.Dtos.RequestPaymentDto;

public interface PaymentGatewayAdapter {
    PaymentResponse createCheckout(RequestPaymentDto request, String idempotencyKey);

    PaymentResponse getCheckout(String id);
}

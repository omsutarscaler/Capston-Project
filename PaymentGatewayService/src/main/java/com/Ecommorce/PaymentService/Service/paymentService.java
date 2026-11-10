package com.Ecommorce.PaymentService.Service;

import com.Ecommorce.PaymentService.Dtos.PaymentResponse;
import com.Ecommorce.PaymentService.Dtos.RequestPaymentDto;

public interface paymentService {
    PaymentResponse createPayment(RequestPaymentDto request, String idempotencyKey);

    PaymentResponse getPayment(String id);

    PaymentResponse cancelPayment(String id);
}

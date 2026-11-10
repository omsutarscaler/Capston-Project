package com.Ecommorce.PaymentService.Service;

import com.Ecommorce.PaymentService.Dtos.PaymentResponse;
import com.Ecommorce.PaymentService.Dtos.RequestPaymentDto;
import com.Ecommorce.PaymentService.PaymentGateway.PaymentGatewayAdapter;
import org.springframework.stereotype.Service;

@Service
public class paymentServiceImpl implements paymentService {
    private final PaymentGatewayAdapter gateway;

    public paymentServiceImpl(PaymentGatewayAdapter gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentResponse createPayment(RequestPaymentDto request, String idempotencyKey) {
        return gateway.createCheckout(request, idempotencyKey);
    }

    @Override
    public PaymentResponse getPayment(String id) {
        return gateway.getCheckout(id);
    }
}

package com.Ecommorce.PaymentService.PaymentGateway;

import java.util.Locale;
import java.util.Map;

import com.Ecommorce.PaymentService.Dtos.PaymentResponse;
import com.Ecommorce.PaymentService.Dtos.RequestPaymentDto;
import com.stripe.exception.IdempotencyException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripeAdapter implements PaymentGatewayAdapter {
    private final String apiKey;

    public StripeAdapter(@Value("${payment.stripe.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public PaymentResponse createCheckout(RequestPaymentDto request, String idempotencyKey) {
        var options = options().setIdempotencyKey(request.source() + ":" + idempotencyKey).build();
        var price = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(request.currency().toLowerCase(Locale.ROOT))
                .setUnitAmount(request.amountMinor())
                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(request.description()).build())
                .build();
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(request.reference())
                .putMetadata("source", request.source())
                .setSuccessUrl(request.successUrl().toASCIIString())
                .setCancelUrl(request.cancelUrl().toASCIIString())
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(price).build())
                .build();
        try {
            return response(Session.create(params, options));
        } catch (IdempotencyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used with another request");
        } catch (InvalidRequestException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Stripe rejected the checkout details");
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe could not create the checkout");
        }
    }

    @Override
    public PaymentResponse getCheckout(String id) {
        try {
            Session session = Session.retrieve(id, options().build());
            if (session.getMetadata() == null || !session.getMetadata().containsKey("source")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found");
            }
            return response(session);
        } catch (InvalidRequestException e) {
            if (Integer.valueOf(404).equals(e.getStatusCode())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe could not retrieve the checkout");
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe could not retrieve the checkout");
        }
    }

    private RequestOptions.RequestOptionsBuilder options() {
        if (!apiKey.startsWith("sk_test_") && !apiKey.startsWith("rk_test_")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A Stripe sandbox key is required");
        }
        return RequestOptions.builder().setApiKey(apiKey).setConnectTimeout(5000).setReadTimeout(10000);
    }

    private static PaymentResponse response(Session session) {
        Map<String, String> metadata = session.getMetadata();
        return new PaymentResponse(session.getId(), metadata.get("source"), session.getClientReferenceId(),
                session.getAmountTotal(), session.getCurrency(), session.getUrl(), session.getStatus(),
                session.getPaymentStatus());
    }
}

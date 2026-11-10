# Payment Gateway Service

Run with Java 17 and `./mvnw spring-boot:run` (`mvnw.cmd spring-boot:run` on Windows). The service listens on `127.0.0.1:8082`. Set `STRIPE_API_KEY` to a Stripe sandbox secret for testing and `PAYMENT_SERVICE_KEY` to a shared secret for trusted backend callers. Keep both keys on the server.

Send `POST /payment` with `X-Service-Key`, `Idempotency-Key`, and a JSON body.

```json
{
  "source": "capston",
  "reference": "order-123",
  "amountMinor": 49900,
  "currency": "INR",
  "description": "Order 123",
  "successUrl": "https://example.com/checkout/success",
  "cancelUrl": "https://example.com/checkout/cancel"
}
```

Send the amount in the currency's minor units. Save the returned `id` and redirect the customer to `checkoutUrl`. Reuse the same source, idempotency key, and request body when retrying an uncertain request. Stripe can remove idempotency keys after they are at least 24 hours old.

Use `GET /payment/{id}` with `X-Service-Key` to check `paymentStatus`. Confirm that the reference, source, amount, and currency match your order and that the status is `paid` before fulfillment. The return URL alone is insufficient. The service uses polling for status and shares access across trusted callers that hold the key.

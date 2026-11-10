package com.Ecommorce.PaymentService;

import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"payment.service-key=local-test-key", "payment.stripe.api-key=sk_test_fake"})
@AutoConfigureMockMvc
class CheckoutApiTests {
    @Autowired
    private MockMvc mvc;

    @Test
    void createsCheckoutWithCallerReferenceAndReadsProviderStatus() throws Exception {
        var session = new Session();
        session.setId("cs_test_example");
        session.setMetadata(Map.of("source", "orders"));
        session.setClientReferenceId("order-1");
        session.setAmountTotal(499L);
        session.setCurrency("usd");
        session.setUrl("https://checkout.stripe.com/example");
        session.setStatus("open");
        session.setPaymentStatus("unpaid");

        var request = post("/payment").header("Idempotency-Key", "retry-1")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"source":"orders","reference":"order-1","amountMinor":499,"currency":"USD",
                         "description":"Order 1","successUrl":"https://example.com/success",
                         "cancelUrl":"https://example.com/cancel"}
                        """);
        try (var stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.create(
                    argThat((SessionCreateParams params) -> params.getClientReferenceId().equals("order-1")
                            && params.getLineItems().get(0).getPriceData().getUnitAmount() == 499L
                            && params.getLineItems().get(0).getPriceData().getCurrency().equals("usd")),
                    argThat((RequestOptions options) -> "orders:retry-1".equals(options.getIdempotencyKey()))))
                    .thenReturn(session);
            stripe.when(() -> Session.retrieve(org.mockito.ArgumentMatchers.eq("cs_test_example"),
                    org.mockito.ArgumentMatchers.any(RequestOptions.class))).thenReturn(session);

            mvc.perform(request).andExpect(status().isUnauthorized());
            stripe.verifyNoInteractions();
            mvc.perform(request.header("X-Service-Key", "local-test-key"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/payment/cs_test_example"))
                    .andExpect(jsonPath("$.paymentStatus").value("unpaid"));
            session.setPaymentStatus("paid");
            mvc.perform(get("/payment/cs_test_example").header("X-Service-Key", "local-test-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("paid"));
        }
    }
}

package com.scaler.ecommerce;

import java.util.UUID;

import com.scaler.ecommerce.components.PaymentGatewayClient;
import com.scaler.ecommerce.components.PaymentGatewayClient.Payment;
import com.scaler.ecommerce.service.ProductService;
import com.scaler.ecommerce.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class EcommerceApplicationTests {

	@Autowired
	private ProductService products;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MockRestServiceServer auth;

	@MockitoBean
	private PaymentGatewayClient gateway;

	@MockitoBean
	private JavaMailSender mail;

	@Test
	void verifiesPaymentAndEmailsAccountOnce() throws Exception {
		var product = products.createProduct("Checkout product", "", "", 99.50, "Checkout");
		UUID orderId = UUID.randomUUID();
		String paymentId = "cs_test_checkout";
		String checkoutUrl = "https://checkout.stripe.com/c/pay/cs_test_checkout";
		String buyer = """
				{"id":11,"value":"buyer","expiresAt":"2099-01-01T00:00:00Z","active":true,
				 "userId":101,"email":"buyer@example.test"}
				""";
		auth.expect(ExpectedCount.times(4), requestTo("/user/validate-token"))
				.andRespond(withSuccess(buyer, MediaType.APPLICATION_JSON));
		auth.expect(requestTo("/user/validate-token"))
				.andRespond(withSuccess("""
						{"id":12,"value":"other","expiresAt":"2099-01-01T00:00:00Z","active":true,
						 "userId":102,"email":"other@example.test"}
						""", MediaType.APPLICATION_JSON));
		when(gateway.create(argThat(order -> order.getAmountMinor() == 9950 && order.getId().equals(orderId))))
				.thenReturn(new Payment(paymentId, "capston", orderId.toString(), 9950L, "inr", checkoutUrl, "open", "unpaid"));
		when(gateway.get(paymentId))
				.thenReturn(new Payment(paymentId, "capston", orderId.toString(), 9950L, "inr", checkoutUrl, "open", "unpaid"))
				.thenReturn(new Payment(paymentId, "capston", orderId.toString(), 9950L, "inr", null, "complete", "paid"));

		mvc.perform(post("/checkout").header("Authorization", "Bearer buyer").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"orderId":"%s","productId":%d,"amountMinor":1}
								""".formatted(orderId, product.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkoutUrl").value(checkoutUrl));
		mvc.perform(post("/checkout/{id}/confirm", orderId).header("Authorization", "Bearer buyer"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.paid").value(false));
		verifyNoInteractions(mail);
		mvc.perform(post("/checkout/{id}/confirm", orderId).header("Authorization", "Bearer buyer"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.receiptSent").value(true));
		mvc.perform(post("/checkout/{id}/confirm", orderId).header("Authorization", "Bearer buyer"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.receiptSent").value(true));
		verify(mail).send(argThat((SimpleMailMessage receipt) ->
				"buyer@example.test".equals(receipt.getTo()[0]) && receipt.getText().contains("INR 99.50")));
		mvc.perform(post("/checkout/{id}/confirm", orderId).header("Authorization", "Bearer other"))
				.andExpect(status().isNotFound());
		auth.verify();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void createsTwoProductsWithTheSameCategory() {
		Product first = products.createProduct("First", "", "", 20, "Books");
		Product second = products.createProduct("Second", "", "", 10, "Books");

		assertEquals(first.getCategory().getId(), second.getCategory().getId());
		assertEquals(second.getId(), products.findByProductTitle("Second", 0, 2).getContent().get(0).getId());
	}

	@Test
	void forwardsLoginAndRequiresValidBearerTokensForProductWrites() throws Exception {
		String token = "header.payload.signature";
		String tokenResponse = """
				{"id":1,"value":"header.payload.signature","expiresAt":"2099-01-01T00:00:00Z","active":true}
				""";
		auth.expect(requestTo("/user/login"))
				.andExpect(MockRestRequestMatchers.content()
						.json("{\"email\":\"buyer@example.test\",\"password\":\"test-password\"}"))
				.andRespond(withSuccess(tokenResponse, MediaType.APPLICATION_JSON));
		auth.expect(requestTo("/user/validate-token"))
				.andExpect(MockRestRequestMatchers.content()
						.json("{\"token\":\"header.payload.signature\"}"))
				.andRespond(withSuccess(tokenResponse, MediaType.APPLICATION_JSON));
		auth.expect(requestTo("/user/validate-token"))
				.andRespond(withUnauthorizedRequest());
		auth.expect(requestTo("/user/validate-token"))
				.andRespond(withServerError());

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"buyer@example.test\",\"password\":\"test-password\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value(token));
		mvc.perform(post("/products")).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"));
		mvc.perform(post("/products").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Secure product\",\"categoryName\":\"Test\",\"price\":9.99}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.category.name").value("Test"));
		mvc.perform(put("/products").header("Authorization", "Bearer invalid"))
				.andExpect(status().isUnauthorized());
		mvc.perform(put("/products").header("Authorization", "Bearer " + token))
				.andExpect(status().isServiceUnavailable());
		auth.verify();
	}

	@Test
	void returnsBoundedPagesAndProblemDetails() throws Exception {
		products.createProduct("Paged", "", "", 1, "Pagination");
		products.createProduct("Paged", "", "", 1, "Pagination");
		mvc.perform(get("/products/title").param("title", "Paged").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.hasNext").value(true));
		mvc.perform(get("/products").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("size"));
		mvc.perform(get("/products/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}
}

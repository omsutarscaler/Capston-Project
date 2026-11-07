package com.scaler.ecommorce.capston;

import com.scaler.ecommorce.capston.service.ProductService;
import com.scaler.ecommorce.capston.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class CapstonApplicationTests {

	@Autowired
	private ProductService products;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MockRestServiceServer auth;

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

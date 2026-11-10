package com.example.UserAuthService;

import com.example.UserAuthService.services.JwtTokens;
import com.example.UserAuthService.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:user-auth-test;MODE=MySQL;NON_KEYWORDS=USER,VALUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"auth.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
class UserAuthServiceApplicationTests {
	@Autowired
	private UserService users;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private JwtTokens jwtTokens;

	@Test
	void contextLoads() {
	}

	@Test
	void issuesSignedTokenAndRejectsTamperingAndLogout() throws Exception {
		users.signup("Test User", "jwt@example.test", "test-password");
		var login = mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"jwt@example.test","password":"test-password"}
								"""))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.user").doesNotExist())
				.andReturn();
		String token = mapper.readTree(login.getResponse().getContentAsString()).get("value").asText();
		assertEquals(3, token.split("\\.").length);
		assertTrue(jwtTokens.isValid(token));
		String payload = "{\"token\":\"" + token + "\"}";
		mvc.perform(post("/user/validate-token").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isOk());
		int signatureStart = token.lastIndexOf('.') + 1;
		char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
		String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
		assertFalse(jwtTokens.isValid(tampered));
		mvc.perform(post("/user/validate-token").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + tampered + "\"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/user/logout").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isOk());
		mvc.perform(post("/user/validate-token").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isUnauthorized());
	}
}

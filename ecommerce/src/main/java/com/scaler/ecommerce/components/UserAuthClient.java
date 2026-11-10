package com.scaler.ecommerce.components;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.scaler.ecommerce.dtos.LoginRequest;
import com.scaler.ecommerce.dtos.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserAuthClient implements OpaqueTokenIntrospector {
    private final RestTemplate http;

    public UserAuthClient(RestTemplateBuilder builder, @Value("${userservice.base-url}") String baseUrl) {
        this.http = builder.rootUri(baseUrl)
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        AuthToken token;
        try {
            token = http.postForObject("/user/login",
                    Map.of("email", request.email(), "password", request.password()), AuthToken.class);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User Auth Service is unavailable");
        }
        if (token == null || !token.usable()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User Auth Service returned an invalid token");
        }
        return new LoginResponse(token.value(), "Bearer", token.expiresAt());
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String value) {
        AuthToken token;
        try {
            token = http.postForObject("/user/validate-token", Map.of("token", value), AuthToken.class);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            throw new BadOpaqueTokenException("Invalid or expired access token");
        } catch (RestClientException e) {
            throw new OAuth2IntrospectionException("User Auth Service is unavailable");
        }
        if (token == null || !token.usable() || !value.equals(token.value())) {
            throw new BadOpaqueTokenException("Invalid or expired access token");
        }
        if (token.userId() != null && token.userId() > 0 && token.email() != null && !token.email().isBlank()) {
            return new DefaultOAuth2AuthenticatedPrincipal(token.userId().toString(),
                    Map.of("exp", token.expiresAt(), "user_id", token.userId(), "email", token.email()), List.of());
        }
        return new DefaultOAuth2AuthenticatedPrincipal(Integer.toString(token.id()),
                Map.of("exp", token.expiresAt()), List.of());
    }

    private record AuthToken(int id, String value, Instant expiresAt, boolean active, Long userId, String email) {
        boolean usable() {
            return id > 0 && value != null && !value.isBlank() && active
                    && expiresAt != null && expiresAt.isAfter(Instant.now());
        }
    }
}

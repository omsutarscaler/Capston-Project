package com.scaler.ecommorce.capston.dtos;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {
    @Override
    public String toString() {
        return "LoginResponse";
    }
}

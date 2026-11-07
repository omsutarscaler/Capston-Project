package com.scaler.ecommorce.capston.controller;

import com.scaler.ecommorce.capston.components.UserAuthClient;
import com.scaler.ecommorce.capston.dtos.LoginRequest;
import com.scaler.ecommorce.capston.dtos.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final UserAuthClient userAuth;

    public AuthController(UserAuthClient userAuth) {
        this.userAuth = userAuth;
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in and get a JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(userAuth.login(request));
    }
}

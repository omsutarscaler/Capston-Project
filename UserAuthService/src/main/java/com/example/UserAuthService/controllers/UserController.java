package com.example.UserAuthService.controllers;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.UserAuthService.dtos.LoginRequestDto;
import com.example.UserAuthService.dtos.LogoutRequestDto;
import com.example.UserAuthService.dtos.SignupRequestDto;
import com.example.UserAuthService.dtos.TokenResponseDto;
import com.example.UserAuthService.dtos.ValidateTokenRequestDto;
import com.example.UserAuthService.exceptions.ExpiredTokenException;
import com.example.UserAuthService.exceptions.InvalidTokenException;
import com.example.UserAuthService.exceptions.PasswordMissMatchException;
import com.example.UserAuthService.exceptions.UserNotFoundException;
import com.example.UserAuthService.models.Token;
import com.example.UserAuthService.models.User;
import com.example.UserAuthService.services.UserService;

@RestController
@RequestMapping("/user")
@OpenAPIDefinition(info = @Info(title = "User Auth Service API", version = "1",
        description = "Sign up, then log in to get a JWT. Pass the login response's value as token "
                + "in the JSON body for token validation or logout."))
@Tag(name = "Authentication")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Register a user")
    @ApiResponse(responseCode = "201", description = "User registered", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Registration rejected")
    public ResponseEntity<User> signUp(@RequestBody SignupRequestDto requestDto) {
        try {
            // TODO add basic validations
            User user = userService.signup(requestDto.getName(), requestDto.getEmail(), requestDto.getPassword());
            return new ResponseEntity<>(user, HttpStatusCode.valueOf(201));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in and get a JWT")
    @ApiResponse(responseCode = "200", description = "JWT issued", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Email or password missing")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<TokenResponseDto> login(@RequestBody LoginRequestDto requestDto) {
        if (requestDto.getEmail() == null || requestDto.getPassword() == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Token token = this.userService.login(requestDto.getEmail(), requestDto.getPassword());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(toResponse(token));
        } catch (UserNotFoundException | PasswordMissMatchException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a token")
    @ApiResponse(responseCode = "200", description = "Logged out")
    @ApiResponse(responseCode = "400", description = "Invalid token")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequestDto requestDto) {
        try {
            // TODO some basic validations
            this.userService.logout(requestDto.getToken());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/validate-token")
    @Operation(summary = "Validate a token")
    @ApiResponse(responseCode = "200", description = "Token is active", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid token")
    @ApiResponse(responseCode = "401", description = "Token expired or revoked")
    public ResponseEntity<TokenResponseDto> validateToken(@RequestBody ValidateTokenRequestDto requestDto) {
        try {
            // TODO basic validations
            Token token = this.userService.validateToken(requestDto.getToken());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(toResponse(token));
        } catch (ExpiredTokenException ete) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(401));
        } catch (InvalidTokenException ite) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

    }

    private TokenResponseDto toResponse(Token token) {
        return new TokenResponseDto(token.getId(), token.getValue(), token.getExpiresAt(), token.isActive());
    }
}

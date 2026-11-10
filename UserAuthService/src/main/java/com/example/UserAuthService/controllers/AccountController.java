package com.example.UserAuthService.controllers;

import com.example.UserAuthService.services.AccountService;
import com.example.UserAuthService.services.AccountService.Profile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/profile")
    public Profile profile(@RequestHeader(value = "Authorization", required = false) String bearer) {
        return accounts.profile(bearer);
    }

    @PutMapping("/profile")
    public Profile update(@RequestHeader(value = "Authorization", required = false) String bearer,
                          @Valid @RequestBody UpdateProfile request) {
        return accounts.update(bearer, request.name());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgot(@Valid @RequestBody Forgot request) {
        accounts.requestReset(request.email().strip());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody Reset request) {
        accounts.reset(request.token(), request.password());
    }

    public record UpdateProfile(@NotBlank @Size(max = 100) String name) {}
    public record Forgot(@NotBlank @Email @Size(max = 254) String email) {}
    public record Reset(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
                        @NotBlank @Size(min = 8, max = 72) String password) {}
}

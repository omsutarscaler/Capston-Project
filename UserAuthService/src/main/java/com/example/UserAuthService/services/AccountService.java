package com.example.UserAuthService.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.example.UserAuthService.exceptions.ExpiredTokenException;
import com.example.UserAuthService.exceptions.InvalidTokenException;
import com.example.UserAuthService.models.User;
import com.example.UserAuthService.repositories.TokenRepository;
import com.example.UserAuthService.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final UserService authentication;
    private final UserRepository users;
    private final TokenRepository tokens;
    private final BCryptPasswordEncoder passwords;
    private final JavaMailSender mail;
    private final TransactionTemplate transaction;
    private final String resetUrl;
    private final String sender;

    public AccountService(UserService authentication, UserRepository users, TokenRepository tokens,
                          BCryptPasswordEncoder passwords, JavaMailSender mail, TransactionTemplate transaction,
                          @Value("${account.storefront-url}") String storefront,
                          @Value("${account.email-from}") String sender) {
        this.authentication = authentication;
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.mail = mail;
        this.transaction = transaction;
        this.resetUrl = storefront.replaceFirst("/$", "") + "/#reset=";
        this.sender = sender;
    }

    @Transactional(readOnly = true)
    public Profile profile(String authorization) {
        return Profile.from(currentUser(authorization));
    }

    @Transactional
    public Profile update(String authorization, String name) {
        var user = currentUser(authorization);
        user.setName(name.strip());
        return Profile.from(user);
    }

    public void requestReset(String email) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String recipient = transaction.execute(status -> users.lockByEmail(email).map(user -> {
            Instant now = Instant.now();
            if (user.getResetExpiresAt() != null && user.getResetExpiresAt().isAfter(now.plusSeconds(840))) {
                return null;
            }
            user.setResetTokenHash(hash(token));
            user.setResetExpiresAt(now.plusSeconds(900));
            return user.getEmail();
        }).orElse(null));
        if (recipient == null) return;
        var message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject("Reset your Ecommerce password");
        message.setText("This link expires in 15 minutes.\n\n" + resetUrl + token);
        try {
            mail.send(message);
        } catch (MailException e) {
            LOG.warn("Password reset email could not be sent");
        }
    }

    @Transactional
    public void reset(String token, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds the supported length");
        }
        var user = users.lockByResetHash(hash(token)).orElseThrow(AccountService::invalidReset);
        if (user.getResetExpiresAt() == null || !user.getResetExpiresAt().isAfter(Instant.now())) throw invalidReset();
        user.setPassword(passwords.encode(password));
        user.setResetTokenHash(null);
        user.setResetExpiresAt(null);
        tokens.revokeByUserId(user.getId());
    }

    private User currentUser(String header) {
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                return authentication.validateToken(header.substring(7)).getUser();
            } catch (ExpiredTokenException | InvalidTokenException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A Bearer token is required");
    }

    private static ResponseStatusException invalidReset() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Profile(long id, String name, String email) {
        static Profile from(User user) {
            return new Profile(user.getId(), user.getName(), user.getEmail());
        }
    }
}

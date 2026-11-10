package com.example.UserAuthService.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.UserAuthService.exceptions.ExpiredTokenException;
import com.example.UserAuthService.exceptions.InvalidTokenException;
import com.example.UserAuthService.exceptions.PasswordMissMatchException;
import com.example.UserAuthService.exceptions.UserNotFoundException;
import com.example.UserAuthService.models.Token;
import com.example.UserAuthService.models.User;
import com.example.UserAuthService.repositories.TokenRepository;
import com.example.UserAuthService.repositories.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final TokenRepository tokenRepository;
    private final JwtTokens jwtTokens;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder,
            TokenRepository tokenRepository, JwtTokens jwtTokens) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.tokenRepository = tokenRepository;
        this.jwtTokens = jwtTokens;
    }

    @Override
    public User signup(String name, String email, String password) throws Exception {

        Optional<User> optionalUser = this.userRepository.findUserByEmail(email);

        if (optionalUser.isPresent()) {
            throw new Exception("User already present");
        }

        String encodedPassword = this.bCryptPasswordEncoder.encode(password);
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(encodedPassword);

        return this.userRepository.save(user); // upsert
    }

    @Override
    @Transactional
    public Token login(String email, String password) throws UserNotFoundException, PasswordMissMatchException {
        Optional<User> optionalUser = this.userRepository.lockByEmail(email);

        User user = optionalUser.orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean matches = this.bCryptPasswordEncoder.matches(password, user.getPassword());

        if (matches) {
            Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant expiresAt = issuedAt.plus(30, ChronoUnit.MINUTES);

            Token token = new Token();
            token.setUser(user);
            token.setValue(jwtTokens.issue(user.getId(), issuedAt, expiresAt));
            token.setExpiresAt(Date.from(expiresAt));
            token.setActive(true);
            return this.tokenRepository.save(token);
        } else {
            // throw exception
            throw new PasswordMissMatchException("Password is incorrect");
        }
    }

    @Override
    public Token validateToken(String tokenValue) throws InvalidTokenException, ExpiredTokenException {
        if (tokenValue == null || tokenValue.isBlank() || tokenValue.length() > 512) {
            throw new InvalidTokenException("Please use a valid token");
        }
        /*
         * 1. Fetch the token from db using value (select * from tokens where value =
         * {value})
         * 2. If token is not present in db, throw exception
         * 3. Else, check whether the token has expired or not
         * 4. If token is expired, then throw an Exception
         * 5. Else you are going to return the token
         */

        Optional<Token> tokenByValue = this.tokenRepository.findTokenByValue(tokenValue);

        Token token = tokenByValue.orElseThrow(() -> new InvalidTokenException("Please use a valid token"));

        Date expiresAt = token.getExpiresAt();
        Date now = new Date();
        // If now is greater than expires at
        if (!expiresAt.after(now) || !token.isActive()) {
            throw new ExpiredTokenException("The token has expired");
        }
        if (!jwtTokens.isValid(tokenValue)) {
            throw new InvalidTokenException("Please use a valid token");
        }

        return token;
    }

    @Override
    public void logout(String tokenValue) throws Exception {
        /*
         * 1. Fetch token from db
         * 2. If the token is not present, then return 400
         * 3. Else set the isActive to false and return
         */
        Optional<Token> tokenByValue = this.tokenRepository.findTokenByValue(tokenValue);

        Token token = tokenByValue.orElseThrow(() -> new InvalidTokenException("Please use a valid token"));

        if (token.isActive()) {
            token.setActive(false);
            this.tokenRepository.save(token);
        }

    }
}

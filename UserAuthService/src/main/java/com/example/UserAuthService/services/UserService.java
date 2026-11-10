package com.example.UserAuthService.services;

import com.example.UserAuthService.exceptions.ExpiredTokenException;
import com.example.UserAuthService.exceptions.InvalidTokenException;
import com.example.UserAuthService.exceptions.PasswordMissMatchException;
import com.example.UserAuthService.exceptions.UserNotFoundException;
import com.example.UserAuthService.models.Token;
import com.example.UserAuthService.models.User;

public interface UserService {

    public User signup(String name, String email, String password) throws Exception;

    public Token login(String email, String password) throws UserNotFoundException, PasswordMissMatchException;

    public Token validateToken(String tokenValue) throws InvalidTokenException, ExpiredTokenException;

    public void logout(String token) throws Exception;

}

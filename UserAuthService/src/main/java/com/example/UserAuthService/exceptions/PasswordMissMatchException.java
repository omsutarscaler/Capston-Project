package com.example.UserAuthService.exceptions;

public class PasswordMissMatchException extends Exception {
    public PasswordMissMatchException(String message) {
        super(message);
    }
}

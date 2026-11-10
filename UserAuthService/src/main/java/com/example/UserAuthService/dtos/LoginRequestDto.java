package com.example.UserAuthService.dtos;

import lombok.Data;

@Data
public class LoginRequestDto {

    private String email;
    private String password;
}

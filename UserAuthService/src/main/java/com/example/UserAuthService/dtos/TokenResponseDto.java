package com.example.UserAuthService.dtos;

import java.util.Date;

import lombok.Data;

@Data

public class TokenResponseDto {

    private int id;
    private String value;
    private Date expiresAt;
    private boolean active;
    private Long userId;
    private String email;

    public TokenResponseDto() {
    }

    public TokenResponseDto(int id, String value, Date expiresAt, boolean active, Long userId, String email) {
        this.id = id;
        this.value = value;
        this.expiresAt = expiresAt;
        this.active = active;
        this.userId = userId;
        this.email = email;
    }

}

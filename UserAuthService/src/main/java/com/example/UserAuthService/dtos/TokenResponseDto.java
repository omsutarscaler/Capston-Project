package com.example.UserAuthService.dtos;

import java.util.Date;

import lombok.Data;

@Data

public class TokenResponseDto {

    private int id;
    private String value;
    private Date expiresAt;
    private boolean active;

    public TokenResponseDto() {
    }

    public TokenResponseDto(int id, String value, Date expiresAt, boolean active) {
        this.id = id;
        this.value = value;
        this.expiresAt = expiresAt;
        this.active = active;
    }

}

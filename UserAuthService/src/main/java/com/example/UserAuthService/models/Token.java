package com.example.UserAuthService.models;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(length = 512)
    @ToString.Exclude
    private String value;
    @ManyToOne
    private User user;
    private Date expiresAt;
    private boolean isActive;
}

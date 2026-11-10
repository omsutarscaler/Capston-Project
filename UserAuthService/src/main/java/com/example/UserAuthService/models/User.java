package com.example.UserAuthService.models;

import java.util.List;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String email;
    @JsonIgnore
    @ToString.Exclude
    private String password;
    @JsonIgnore
    @ToString.Exclude
    private String resetTokenHash;
    @JsonIgnore
    private Instant resetExpiresAt;
    @ElementCollection
    private List<Role> roles;
}

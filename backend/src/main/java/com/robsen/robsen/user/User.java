package com.robsen.robsen.user;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    private String fullName;
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String role;
    private String username;
    private String phoneNumber;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private boolean active = true;
} 

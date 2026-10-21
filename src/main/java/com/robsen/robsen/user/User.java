package com.robsen.robsen.user;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.web.WebProperties.Resources.Chain.Strategy;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity 
@Data 
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    private String fullName;
    private String email;
    private String password;
    private String role;
    private String username;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private boolean isActive;
}

package com.robsen.robsen.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.robsen.robsen.security.AuthService.AuthResponse;
import com.robsen.robsen.security.AuthService.LoginRequest;
import com.robsen.robsen.security.AuthService.RegisterRequest;
import com.robsen.robsen.security.AuthService.UserProfile;
import com.robsen.robsen.security.AuthService.ProfileUpdateRequest;
import com.robsen.robsen.security.AuthService.ChangePasswordRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public UserProfile getCurrentUser(Authentication authentication) {
        return authService.getProfile(authentication.getName());
    }

    @PatchMapping("/profile")
    public AuthResponse updateCurrentUser(Authentication authentication, @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(authentication.getName(), request);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(Authentication authentication) {
        return authService.refresh(authentication.getName());
    }
}

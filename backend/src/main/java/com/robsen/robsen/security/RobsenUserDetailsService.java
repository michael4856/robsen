package com.robsen.robsen.security;

import java.util.Locale;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;

@Service
public class RobsenUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;

    public RobsenUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        User user = userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
        String role = user.getRole() == null || user.getRole().isBlank()
                ? "USER"
                : user.getRole().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail() == null ? user.getUsername() : user.getEmail())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority(authority))
                .disabled(!user.isActive())
                .build();
    }
}

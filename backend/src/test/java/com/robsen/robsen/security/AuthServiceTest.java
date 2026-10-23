package com.robsen.robsen.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;
import com.robsen.robsen.user.UserService;
import com.robsen.robsen.security.RolePermissionService;

class AuthServiceTest {
    @Test
    void loginReturnsBearerTokenForActiveUser() {
        UserRepo userRepo = mock(UserRepo.class);
        UserService userService = mock(UserService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        User user = user();
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase("user@example.com", "user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "encoded-password")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mock(UserDetails.class));
        when(jwtService.createToken(any(UserDetails.class))).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        AuthService authService = new AuthService(
                userRepo, userService, userDetailsService, passwordEncoder, jwtService,
                mock(RolePermissionService.class));

        AuthService.AuthResponse response =
                authService.login(new AuthService.LoginRequest("user@example.com", "password1"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("user@example.com", response.user().email());
    }

    @Test
    void loginRejectsInvalidCredentialsWithUnauthorizedStatus() {
        UserRepo userRepo = mock(UserRepo.class);
        AuthService authService = new AuthService(
                userRepo,
                mock(UserService.class),
                mock(UserDetailsService.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(RolePermissionService.class));
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase("missing", "missing"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new AuthService.LoginRequest("missing", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void registrationAlwaysAssignsUserRole() {
        UserRepo userRepo = mock(UserRepo.class);
        UserService userService = mock(UserService.class);
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        AuthService authService = new AuthService(
                userRepo,
                userService,
                mock(UserDetailsService.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(RolePermissionService.class));

        AuthService.UserProfile profile = authService.register(new AuthService.RegisterRequest(
                "Robsen Member", "member@example.com", "member", "password1", null));

        assertEquals("USER", profile.role());
        assertEquals(42L, profile.id());
    }

    @Test
    void profileUsesNavigationItemsFromAssignedActiveRole() {
        UserRepo userRepo = mock(UserRepo.class);
        RolePermissionService permissionService = mock(RolePermissionService.class);
        User user = user();
        when(permissionService.permissionsFor(user))
                .thenReturn(java.util.List.of("products:READ", "opportunities:READ"));
        when(permissionService.navigationFor(user)).thenReturn(java.util.List.of("products", "opportunities"));
        when(permissionService.isSuperAdmin(user)).thenReturn(false);
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase("user@example.com", "user@example.com"))
                .thenReturn(Optional.of(user));
        AuthService authService = new AuthService(
                userRepo,
                mock(UserService.class),
                mock(UserDetailsService.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                permissionService);

        AuthService.UserProfile profile = authService.getProfile("user@example.com");

        assertEquals(java.util.List.of("products", "opportunities"), profile.navigationItems());
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setFullName("Robsen User");
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setPassword("encoded-password");
        user.setRole("USER");
        user.setActive(true);
        return user;
    }
}

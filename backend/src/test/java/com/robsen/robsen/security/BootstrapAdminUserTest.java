package com.robsen.robsen.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;
import com.robsen.robsen.user.UserService;

class BootstrapAdminUserTest {
    private static final String PASSWORD = "safe-test-password-123";

    @Test
    void persistsSuperAdminWithEncodedPasswordWhenConfiguredAccountDoesNotExist() throws Exception {
        UserRepo userRepo = mock(UserRepo.class);
        UserService userService = new UserService(userRepo, new BCryptPasswordEncoder());
        when(userRepo.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userRepo.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepo.existsByUsername("admin")).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(42L);
            return savedUser;
        });
        BootstrapAdminUser bootstrap = new BootstrapAdminUser(
                userRepo, userService, "admin@example.com", "admin", PASSWORD, "Robsen Admin");

        bootstrap.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertEquals("Robsen Admin", captor.getValue().getFullName());
        assertEquals("admin@example.com", captor.getValue().getEmail());
        assertEquals("admin", captor.getValue().getUsername());
        assertEquals("SUPER_ADMIN", captor.getValue().getRole());
        assertEquals(42L, captor.getValue().getId());
        org.junit.jupiter.api.Assertions.assertTrue(
                new BCryptPasswordEncoder().matches(PASSWORD, captor.getValue().getPassword()));
    }

    @Test
    void doesNotChangeAnExistingAccount() throws Exception {
        UserRepo userRepo = mock(UserRepo.class);
        UserService userService = mock(UserService.class);
        User existing = new User();
        existing.setRole("SUPER_ADMIN");
        when(userRepo.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existing));
        BootstrapAdminUser bootstrap = new BootstrapAdminUser(
                userRepo, userService, "admin@example.com", "admin", PASSWORD, "Robsen Admin");

        bootstrap.run();

        verify(userService, never()).createUser(any(User.class));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void skipsBootstrapWhenNoCredentialsAreConfigured() throws Exception {
        UserRepo userRepo = mock(UserRepo.class);
        UserService userService = mock(UserService.class);
        BootstrapAdminUser bootstrap = new BootstrapAdminUser(
                userRepo, userService, "", "admin", "", "Robsen Admin");

        bootstrap.run();

        verify(userRepo, never()).findByEmailIgnoreCase(any());
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void refusesPartialOrWeakBootstrapCredentials() {
        UserRepo userRepo = mock(UserRepo.class);
        UserService userService = mock(UserService.class);
        BootstrapAdminUser partialBootstrap = new BootstrapAdminUser(
                userRepo, userService, "admin@example.com", "admin", "", "Robsen Admin");
        BootstrapAdminUser weakBootstrap = new BootstrapAdminUser(
                userRepo, userService, "admin@example.com", "admin", "short-pass", "Robsen Admin");

        assertThrows(IllegalStateException.class, partialBootstrap::run);
        assertThrows(IllegalStateException.class, weakBootstrap::run);
        verify(userRepo, never()).findByEmailIgnoreCase(any());
        verify(userService, never()).createUser(any(User.class));
    }
}

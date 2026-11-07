package com.robsen.robsen.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {
    @Test
    void createUserReportsDuplicateEmail() {
        UserRepo userRepo = mock(UserRepo.class);
        when(userRepo.existsByEmailIgnoreCase("used@example.com")).thenReturn(true);
        UserService userService = new UserService(userRepo, mock(PasswordEncoder.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(user("used@example.com", "new-user")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email is already registered.", exception.getReason());
    }

    @Test
    void createUserReportsDuplicateUsername() {
        UserRepo userRepo = mock(UserRepo.class);
        when(userRepo.existsByUsernameIgnoreCase("used-user")).thenReturn(true);
        UserService userService = new UserService(userRepo, mock(PasswordEncoder.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(user("new@example.com", "used-user")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username is already taken.", exception.getReason());
    }

    private User user(String email, String username) {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword("password");
        user.setRole("SUPER_ADMIN");
        return user;
    }
}

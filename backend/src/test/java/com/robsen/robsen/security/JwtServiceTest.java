package com.robsen.robsen.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.ExpiredJwtException;

class JwtServiceTest {
    private static final String SECRET = "robsen-test-secret-for-jwt-tests-only-32chars";

    @Test
    void createsAndValidatesTokenForUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserDetails user = User.withUsername("person@example.com")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.createToken(user);

        assertEquals("person@example.com", jwtService.getSubject(token));
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1_000);
        UserDetails user = User.withUsername("person@example.com")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();
        String token = jwtService.createToken(user);
        Thread.sleep(1_100);

        assertThrows(ExpiredJwtException.class, () -> jwtService.getSubject(token));
    }
}

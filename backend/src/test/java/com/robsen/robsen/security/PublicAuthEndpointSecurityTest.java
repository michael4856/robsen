package com.robsen.robsen.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.jwt.secret=test-secret-for-robsen-jwt-tests-only-32chars")
@AutoConfigureMockMvc
class PublicAuthEndpointSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationIsPublicEvenWhenAStaleBearerTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer expired-or-malformed-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonPostRequestsToRegistrationAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().isUnauthorized());
    }
}

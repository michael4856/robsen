package com.robsen.robsen.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> response.sendError(401, "Authentication required.");
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers(
                                "/api/users", "/api/users/**",
                                "/api/roles", "/api/roles/**",
                                "/api/tenants", "/api/tenants/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/**").access(dynamicApiAuthorizationManager())
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AuthorizationManager<RequestAuthorizationContext> dynamicApiAuthorizationManager() {
        return (authenticationSupplier, context) -> {
            var authentication = authenticationSupplier.get();
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                return new AuthorizationDecision(true);
            }

            String resource = resourceFor(context.getRequest().getRequestURI());
            String action = actionFor(context.getRequest().getMethod());
            if (resource == null || action == null) {
                return new AuthorizationDecision(false);
            }

            String permission = "PERMISSION:" + resource + ":" + action;
            boolean granted = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(permission));
            return new AuthorizationDecision(granted);
        };
    }

    private String resourceFor(String path) {
        if (path.startsWith("/api/business-memberships")) return "memberships";
        if (path.startsWith("/api/product-categories")) return "product-categories";
        if (path.startsWith("/api/product-listings")) return "product-listings";
        if (path.startsWith("/api/distribution-opportunities")) return "opportunities";
        if (path.startsWith("/api/partner-applications")) return "partner-applications";
        if (path.startsWith("/api/distribution-agreements")) return "agreements";
        if (path.startsWith("/api/order-items")) return "order-items";
        if (path.startsWith("/api/business")) return "businesses";
        if (path.startsWith("/api/products")) return "products";
        if (path.startsWith("/api/territories")) return "territories";
        if (path.startsWith("/api/orders")) return "orders";
        return null;
    }

    private String actionFor(String method) {
        return switch (method) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> null;
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:http://localhost:3000}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigin.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

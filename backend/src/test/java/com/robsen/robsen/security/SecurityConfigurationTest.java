package com.robsen.robsen.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

class SecurityConfigurationTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void businessMembershipAuthorizationIsDeferredToBusinessRoleChecks() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/business-memberships/42");
        AuthorizationManager<RequestAuthorizationContext> manager = configuration.dynamicApiAuthorizationManager();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com", null, java.util.List.of());

        AuthorizationDecision decision = (AuthorizationDecision) manager.authorize(
                () -> authentication, new RequestAuthorizationContext(request));

        assertTrue(decision.isGranted());
    }

    @Test
    void businessRolePermissionRoutesDeferAuthorizationToOwnerChecks() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PUT", "/api/business/42/roles/MANAGER/permissions");
        AuthorizationManager<RequestAuthorizationContext> manager = configuration.dynamicApiAuthorizationManager();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com", null, java.util.List.of());

        AuthorizationDecision decision = (AuthorizationDecision) manager.authorize(
                () -> authentication, new RequestAuthorizationContext(request));

        assertTrue(decision.isGranted());
    }

    @Test
    void unrelatedResourceStillUsesGlobalPermissionAuthorities() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/products/42");
        AuthorizationManager<RequestAuthorizationContext> manager = configuration.dynamicApiAuthorizationManager();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com", null, java.util.List.of());

        AuthorizationDecision decision = (AuthorizationDecision) manager.authorize(
                () -> authentication, new RequestAuthorizationContext(request));

        assertFalse(decision.isGranted());
    }
}

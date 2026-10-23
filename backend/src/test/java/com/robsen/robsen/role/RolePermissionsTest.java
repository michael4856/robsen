package com.robsen.robsen.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class RolePermissionsTest {
    @Test
    void normalizesAndDeduplicatesSupportedPermissions() {
        assertEquals(List.of("products:READ"), RolePermissions.normalize(
                List.of(" PRODUCTS:read ", "products:READ")));
    }

    @Test
    void rejectsUnknownResourcesAndActions() {
        assertThrows(IllegalArgumentException.class,
                () -> RolePermissions.normalize(List.of("users:READ")));
        assertThrows(IllegalArgumentException.class,
                () -> RolePermissions.normalize(List.of("products:EXECUTE")));
        assertThrows(IllegalArgumentException.class,
                () -> RolePermissions.normalize(List.of("products:UPDATE")));
    }

    @Test
    void seedsTheRequestedBusinessRolesWithOwnerOnlyRestrictions() {
        Map<String, Role> roles = DefaultRoles.roles().stream()
                .collect(Collectors.toMap(Role::getName, Function.identity()));

        assertEquals(11, roles.size());
        assertTrue(roles.keySet().containsAll(List.of(
                "OWNER", "ADMIN", "MANAGER", "SALES_MANAGER", "SALES_REPRESENTATIVE",
                "INVENTORY_MANAGER", "ORDER_MANAGER", "DISTRIBUTION_MANAGER",
                "FINANCE_MANAGER", "STAFF", "VIEWER")));
        assertTrue(roles.get("OWNER").getPermissions().contains("memberships:DELETE"));
        assertFalse(roles.get("ADMIN").getPermissions().contains("memberships:DELETE"));
        assertFalse(roles.get("ADMIN").getPermissions().contains("businesses:DELETE"));
        assertTrue(roles.get("INVENTORY_MANAGER").getPermissions().contains("product-listings:UPDATE"));
        assertFalse(roles.get("OWNER").getPermissions().contains("products:UPDATE"));
        assertFalse(roles.get("VIEWER").getPermissions().stream().anyMatch(permission ->
                permission.endsWith(":CREATE") || permission.endsWith(":UPDATE")
                        || permission.endsWith(":DELETE")));
    }
}

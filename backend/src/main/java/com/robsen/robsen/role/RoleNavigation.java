package com.robsen.robsen.role;

import java.util.List;
import java.util.Set;

public final class RoleNavigation {
    public static final List<String> ALL = List.of(
            "businesses",
            "users",
            "memberships",
            "roles",
            "product-categories",
            "products",
            "product-listings",
            "territories",
            "opportunities",
            "partner-applications",
            "agreements",
            "orders",
            "order-items",
            "tenants");
    public static final List<String> DEFAULT_USER = List.of(
            "businesses",
            "memberships",
            "product-categories",
            "products",
            "product-listings",
            "territories",
            "opportunities",
            "partner-applications",
            "agreements",
            "orders",
            "order-items");
    private static final Set<String> ALLOWED = Set.copyOf(ALL);

    private RoleNavigation() {
    }

    public static boolean containsOnlySupportedItems(List<String> navigationItems) {
        return navigationItems == null || ALLOWED.containsAll(navigationItems);
    }
}

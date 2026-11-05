package com.robsen.robsen.role;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DefaultRoles {
    private DefaultRoles() {
    }

    public static List<Role> roles() {
        return List.of(
                role("OWNER", "Full control of the business, membership management, and business settings.",
                        RolePermissions.RESOURCES, Set.of()),
                role("ADMIN", "Manage business operations and members, subject to owner-only restrictions.",
                        RolePermissions.RESOURCES, Set.of()),
                role("MANAGER", "Manage products, listings, orders, and assigned business operations.",
                        List.of("businesses", "product-categories", "products", "product-listings",
                                "opportunities", "partner-applications", "agreements", "orders", "order-items"),
                        List.of("product-categories", "products", "product-listings", "opportunities",
                                "partner-applications", "agreements", "orders", "order-items")),
                role("SALES_MANAGER", "Manage sales activity, wholesale pricing, and sales staff.",
                        List.of("product-listings", "partner-applications", "orders", "order-items", "memberships"),
                        List.of("product-listings", "orders", "order-items", "memberships")),
                role("SALES_REPRESENTATIVE", "Handle buyer enquiries, leads, and assigned customer orders.",
                        List.of("product-listings", "partner-applications", "orders", "order-items"),
                        List.of("partner-applications", "orders", "order-items")),
                role("INVENTORY_MANAGER", "Manage stock levels and product availability.",
                        List.of("products", "product-listings"),
                        List.of("product-listings")),
                role("ORDER_MANAGER", "Process, confirm, and fulfil wholesale orders.",
                        List.of("orders", "order-items"),
                        List.of("orders", "order-items")),
                role("DISTRIBUTION_MANAGER", "Manage distribution opportunities, territories, partner applications, and agreements.",
                        List.of("territories", "opportunities", "partner-applications", "agreements"),
                        List.of("territories", "opportunities", "partner-applications", "agreements")),
                role("FINANCE_MANAGER", "View permitted financial information and manage financial records.",
                        List.of("businesses", "agreements", "orders", "order-items"),
                        List.of("agreements")),
                role("STAFF", "Perform basic operational tasks granted by the business.",
                        List.of("product-listings", "orders", "order-items"),
                        List.of("orders", "order-items")),
                role("VIEWER", "Read permitted business information without changing it.",
                        RolePermissions.RESOURCES, Set.of()));
    }

    private static Role role(String name, String description, List<String> resources, Set<String> writableResources) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setActive(true);
        List<String> permissions = new ArrayList<>();
        if (name.equals("OWNER") || name.equals("ADMIN")) {
            resources = new ArrayList<>(resources);
            if (!resources.contains("memberships")) {
                resources.add("memberships");
            }
        }
        for (String resource : resources) {
            String readPermission = RolePermissions.permission(resource, "READ");
            if (RolePermissions.isSupported(readPermission)) {
                permissions.add(readPermission);
            }
            if (name.equals("OWNER") || name.equals("ADMIN") || writableResources.contains(resource)) {
                for (String action : List.of("CREATE", "UPDATE", "DELETE")) {
                    String permission = RolePermissions.permission(resource, action);
                    if (!RolePermissions.isSupported(permission)) {
                        continue;
                    }
                    if ((name.equals("OWNER") || name.equals("ADMIN"))
                            && resource.equals("businesses") && action.equals("CREATE")) {
                        continue;
                    }
                    if (name.equals("ADMIN") && resource.equals("memberships") && action.equals("DELETE")) {
                        continue;
                    }
                    if ((name.equals("OWNER") || name.equals("ADMIN"))
                            && resource.equals("businesses") && action.equals("DELETE")) {
                        continue;
                    }
                    permissions.add(permission);
                }
            }
        }
        role.setPermissions(permissions);
        role.setNavigationItems(resources);
        return role;
    }

    private static Role role(
            String name, String description, List<String> resources, List<String> writableResources) {
        return role(name, description, resources, Set.copyOf(writableResources));
    }
}

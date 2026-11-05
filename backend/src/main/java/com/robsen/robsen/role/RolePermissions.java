package com.robsen.robsen.role;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class RolePermissions {
    public static final List<String> ACTIONS = List.of("CREATE", "READ", "UPDATE", "DELETE");
    public static final List<String> RESOURCES = RoleNavigation.ALL.stream()
            .filter(resource -> !Set.of("users", "roles", "tenants").contains(resource))
            .toList();
    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9-]*:(CREATE|READ|UPDATE|DELETE)");

    private RolePermissions() {
    }

    public static List<String> catalog() {
        List<String> permissions = new ArrayList<>();
        for (String resource : RESOURCES) {
            for (String action : ACTIONS) {
                if (resource.equals("businesses") && Set.of("CREATE", "DELETE").contains(action)) {
                    continue;
                }
                permissions.add(permission(resource, action));
            }
        }
        return List.copyOf(permissions);
    }

    public static String permission(String resource, String action) {
        return resource + ":" + action;
    }

    public static boolean isSupported(String permission) {
        return permission != null && catalog().contains(permission);
    }

    public static List<String> normalize(List<String> permissions) {
        if (permissions == null) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission == null) {
                throw new IllegalArgumentException("Permissions cannot contain null values.");
            }
            String candidate = permission.trim().toUpperCase(Locale.ROOT);
            int separator = candidate.lastIndexOf(':');
            if (separator >= 0) {
                candidate = candidate.substring(0, separator).toLowerCase(Locale.ROOT)
                        + candidate.substring(separator);
            }
            if (!FORMAT.matcher(candidate).matches() || !isSupported(candidate)) {
                throw new IllegalArgumentException("Unsupported permission: " + permission);
            }
            normalized.add(candidate);
        }
        return List.copyOf(normalized);
    }

    public static List<String> allPlatformPermissions() {
        List<String> permissions = new ArrayList<>(catalog());
        for (String resource : List.of("users", "roles", "tenants")) {
            for (String action : ACTIONS) {
                permissions.add(permission(resource, action));
            }
        }
        return List.copyOf(permissions);
    }
}

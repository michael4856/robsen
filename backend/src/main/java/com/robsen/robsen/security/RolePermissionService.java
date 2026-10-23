package com.robsen.robsen.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.role.RoleNavigation;
import com.robsen.robsen.role.RolePermissions;
import com.robsen.robsen.user.User;

@Service
public class RolePermissionService {
    private final BusinessMembershipRepo membershipRepo;

    public RolePermissionService(BusinessMembershipRepo membershipRepo) {
        this.membershipRepo = membershipRepo;
    }

    @Transactional(readOnly = true)
    public List<String> permissionsFor(User user) {
        if (isSuperAdmin(user)) {
            return RolePermissions.allPlatformPermissions();
        }
        Set<String> permissions = new LinkedHashSet<>();
        membershipRepo.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                .stream()
                .map(membership -> membership.getRole())
                .filter(role -> role != null && role.isActive() && role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .forEach(permissions::add);
        return List.copyOf(permissions);
    }

    @Transactional(readOnly = true)
    public List<String> navigationFor(User user) {
        if (isSuperAdmin(user)) {
            return RoleNavigation.ALL;
        }
        Set<String> readable = new LinkedHashSet<>();
        for (String permission : permissionsFor(user)) {
            if (permission.endsWith(":READ")) {
                readable.add(permission.substring(0, permission.length() - ":READ".length()));
            }
        }
        return RoleNavigation.ALL.stream().filter(readable::contains).toList();
    }

    public boolean isSuperAdmin(User user) {
        return user.getRole() != null && user.getRole().equalsIgnoreCase("SUPER_ADMIN");
    }

    public boolean hasPermission(User user, String permission) {
        return isSuperAdmin(user) || permissionsFor(user).contains(permission);
    }
}

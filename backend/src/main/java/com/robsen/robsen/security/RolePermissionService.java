package com.robsen.robsen.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.businessMembership.BusinessMembership;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.role.RoleNavigation;
import com.robsen.robsen.role.RolePermissions;
import com.robsen.robsen.role.DefaultRoles;
import com.robsen.robsen.role.BusinessRolePermissionConfigurationRepo;
import com.robsen.robsen.role.BusinessRolePermissionService;
import com.robsen.robsen.user.User;

@Service
public class RolePermissionService {
    private static final Set<String> OWNER_PERMISSIONS = DefaultRoles.roles().stream()
            .filter(role -> "OWNER".equalsIgnoreCase(role.getName()))
            .findFirst()
            .map(role -> Set.copyOf(role.getPermissions()))
            .orElse(Set.of());

    private final BusinessMembershipRepo membershipRepo;
    private final BusinessRolePermissionConfigurationRepo businessRoleConfigurationRepo;

    public RolePermissionService(
            BusinessMembershipRepo membershipRepo,
            BusinessRolePermissionConfigurationRepo businessRoleConfigurationRepo) {
        this.membershipRepo = membershipRepo;
        this.businessRoleConfigurationRepo = businessRoleConfigurationRepo;
    }

    @Transactional(readOnly = true)
    public List<String> permissionsFor(User user) {
        if (isSuperAdmin(user)) {
            return RolePermissions.allPlatformPermissions();
        }
        Set<String> permissions = new LinkedHashSet<>();
        membershipRepo.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                .stream()
                .filter(membership -> membership.getRole() != null && membership.getRole().isActive())
                .flatMap(membership -> effectivePermissions(membership).stream())
                .forEach(permissions::add);
        return List.copyOf(permissions);
    }

    @Transactional(readOnly = true)
    public List<Long> businessIdsFor(User user) {
        return membershipRepo.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE).stream()
                .map(membership -> membership.getBusiness().getId())
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> navigationFor(User user) {
        if (isSuperAdmin(user)) {
            return RoleNavigation.ALL;
        }
        Set<String> permittedResources = new LinkedHashSet<>();
        for (String permission : permissionsFor(user)) {
            int separator = permission.indexOf(':');
            if (separator > 0) permittedResources.add(permission.substring(0, separator));
        }
        return RoleNavigation.ALL.stream().filter(permittedResources::contains).toList();
    }

    public boolean isSuperAdmin(User user) {
        return user.getRole() != null && user.getRole().equalsIgnoreCase("SUPER_ADMIN");
    }

    public boolean hasPermission(User user, String permission) {
        return isSuperAdmin(user) || permissionsFor(user).contains(permission);
    }

    public static boolean hasRolePermission(Role role, String permission) {
        return role != null && role.isActive() &&
                ((role.getPermissions() != null && role.getPermissions().contains(permission)) ||
                        ("OWNER".equalsIgnoreCase(role.getName()) && OWNER_PERMISSIONS.contains(permission)));
    }

    public static boolean hasMembershipPermission(BusinessMembership membership, String permission) {
        if (membership == null || membership.getStatus() != MembershipStatus.ACTIVE
                || membership.getRole() == null || !membership.getRole().isActive()) {
            return false;
        }
        return hasRolePermission(membership.getRole(), permission)
                || (membership.getPermissionGrants() != null
                        && membership.getPermissionGrants().contains(permission));
    }

    @Transactional(readOnly = true)
    public boolean hasEffectiveMembershipPermission(BusinessMembership membership, String permission) {
        return membership != null && membership.getStatus() == MembershipStatus.ACTIVE
                && membership.getRole() != null && membership.getRole().isActive()
                && effectivePermissions(membership).contains(permission);
    }

    private static Set<String> effectivePermissions(Role role) {
        Set<String> permissions = new LinkedHashSet<>();
        if (role.getPermissions() != null) permissions.addAll(role.getPermissions());
        if ("OWNER".equalsIgnoreCase(role.getName())) permissions.addAll(OWNER_PERMISSIONS);
        return permissions;
    }

    private Set<String> effectivePermissions(BusinessMembership membership) {
        Set<String> rolePermissions = effectivePermissions(membership.getRole());
        if (membership.getRole() != null && "MANAGER".equalsIgnoreCase(membership.getRole().getName())) {
            Set<String> configured = businessRoleConfigurationRepo
                    .findByBusinessIdAndRoleId(membership.getBusiness().getId(), membership.getRole().getId())
                    .map(configuration -> new LinkedHashSet<>(configuration.getEnabledPermissions()))
                    .orElseGet(() -> new LinkedHashSet<>(BusinessRolePermissionService.managerDefaults()));
            Set<String> defaultManagerPermissions = DefaultRoles.roles().stream()
                    .filter(role -> "MANAGER".equalsIgnoreCase(role.getName()))
                    .findFirst().map(role -> Set.copyOf(role.getPermissions())).orElse(Set.of());
            if (membership.getPermissionGrants() != null) {
                membership.getPermissionGrants().stream()
                        .filter(grant -> !defaultManagerPermissions.contains(grant) || configured.contains(grant))
                        .forEach(configured::add);
            }
            return configured;
        }
        Set<String> permissions = new LinkedHashSet<>(rolePermissions);
        if (membership.getPermissionGrants() != null) {
            permissions.addAll(membership.getPermissionGrants());
        }
        return permissions;
    }
}

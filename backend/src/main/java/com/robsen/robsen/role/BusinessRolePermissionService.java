package com.robsen.robsen.role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.businessMembership.BusinessMembership;
import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.security.BusinessAccessService;

@Service
@Order(2)
public class BusinessRolePermissionService implements CommandLineRunner {
    private static final Set<String> PROTECTED = Set.of(
            "businesses:UPDATE", "memberships:CREATE", "memberships:READ", "memberships:UPDATE", "memberships:DELETE",
            "products:DELETE", "product-categories:DELETE");
    private static final List<String> MANAGER_DEFAULTS = List.of(
            "businesses:READ",
            "product-categories:READ", "product-categories:CREATE", "product-categories:UPDATE",
            "products:READ", "products:CREATE", "products:UPDATE",
            "product-listings:READ", "product-listings:CREATE", "product-listings:UPDATE",
            "orders:READ", "orders:UPDATE", "order-items:READ", "order-items:UPDATE",
            "opportunities:READ", "opportunities:CREATE", "opportunities:UPDATE",
            "partner-applications:READ", "partner-applications:UPDATE",
            "agreements:READ", "agreements:CREATE", "agreements:UPDATE",
            "territories:READ");

    private final BusinessRolePermissionConfigurationRepo configurationRepo;
    private final BusinessRolePermissionAuditRepo auditRepo;
    private final BusinessMembershipRepo membershipRepo;
    private final BusinessRepo businessRepo;
    private final RoleRepo roleRepo;
    private final BusinessAccessService businessAccess;

    public BusinessRolePermissionService(
            BusinessRolePermissionConfigurationRepo configurationRepo,
            BusinessRolePermissionAuditRepo auditRepo,
            BusinessMembershipRepo membershipRepo,
            BusinessRepo businessRepo,
            RoleRepo roleRepo,
            BusinessAccessService businessAccess) {
        this.configurationRepo = configurationRepo;
        this.auditRepo = auditRepo;
        this.membershipRepo = membershipRepo;
        this.businessRepo = businessRepo;
        this.roleRepo = roleRepo;
        this.businessAccess = businessAccess;
    }

    public static Set<String> managerDefaults() {
        return Set.copyOf(MANAGER_DEFAULTS);
    }

    public static List<String> managerPermissionCatalog() {
        return RolePermissions.catalog().stream().filter(permission -> !PROTECTED.contains(permission)).toList();
    }

    @Transactional
    public void initializeBusiness(Long businessId) {
        Role managerRole = roleRepo.findByNameIgnoreCaseAndActiveTrue("MANAGER")
                .orElseThrow(() -> new IllegalStateException("The MANAGER business role is not configured."));
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found."));
        ensureConfiguration(business, managerRole);
    }

    @Transactional(readOnly = true)
    public Set<String> configuredPermissions(Long businessId, Long roleId) {
        return configurationRepo.findByBusinessIdAndRoleId(businessId, roleId)
                .map(configuration -> Set.copyOf(configuration.getEnabledPermissions()))
                .orElseGet(() -> "MANAGER".equalsIgnoreCase(roleRepo.findById(roleId)
                        .map(Role::getName).orElse("") ) ? managerDefaults() : Set.of());
    }

    @Transactional
    public ManagerPermissions getManagerPermissions(Long businessId) {
        requireOwner(businessId);
        BusinessRolePermissionConfiguration configuration = getOrInitialize(businessId);
        return response(configuration);
    }

    @Transactional
    public ManagerPermissions updateManagerPermissions(Long businessId, List<String> requestedPermissions) {
        requireOwner(businessId);
        if (requestedPermissions == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A complete permissions list is required.");
        }
        List<String> normalized;
        try {
            normalized = RolePermissions.normalize(requestedPermissions);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        Set<String> submitted = new LinkedHashSet<>(normalized);
        Set<String> allowed = Set.copyOf(managerPermissionCatalog());
        if (!allowed.containsAll(submitted)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more permissions are protected or unsupported.");
        }
        BusinessRolePermissionConfiguration configuration = getOrInitialize(businessId);
        recordChanges(configuration, submitted);
        configuration.setEnabledPermissions(submitted);
        configuration.setUpdatedAt(LocalDateTime.now());
        configuration.setUpdatedByUserId(businessAccess.currentUserId());
        return response(configurationRepo.save(configuration));
    }

    @Transactional
    public ManagerPermissions resetManagerPermissions(Long businessId) {
        requireOwner(businessId);
        BusinessRolePermissionConfiguration configuration = getOrInitialize(businessId);
        Set<String> defaults = managerDefaults();
        recordChanges(configuration, defaults);
        configuration.setEnabledPermissions(new LinkedHashSet<>(defaults));
        configuration.setDefaultsVersion(1);
        configuration.setUpdatedAt(LocalDateTime.now());
        configuration.setUpdatedByUserId(businessAccess.currentUserId());
        return response(configurationRepo.save(configuration));
    }

    @Override
    @Transactional
    public void run(String... args) {
        roleRepo.findByNameIgnoreCaseAndActiveTrue("MANAGER").ifPresent(managerRole ->
                businessRepo.findAll().forEach(business -> ensureConfiguration(business, managerRole)));
    }

    private BusinessRolePermissionConfiguration getOrInitialize(Long businessId) {
        Role managerRole = roleRepo.findByNameIgnoreCaseAndActiveTrue("MANAGER")
                .orElseThrow(() -> new IllegalStateException("The MANAGER business role is not configured."));
        return configurationRepo.findByBusinessIdAndRoleId(businessId, managerRole.getId()).orElseGet(() -> {
            Business business = businessRepo.findById(businessId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found."));
            return configurationRepo.save(newConfiguration(business, managerRole));
        });
    }

    private void ensureConfiguration(Business business, Role managerRole) {
        if (configurationRepo.findByBusinessIdAndRoleId(business.getId(), managerRole.getId()).isEmpty()) {
            configurationRepo.save(newConfiguration(business, managerRole));
        }
    }

    private BusinessRolePermissionConfiguration newConfiguration(Business business, Role managerRole) {
        BusinessRolePermissionConfiguration configuration = new BusinessRolePermissionConfiguration();
        configuration.setBusiness(business);
        configuration.setRole(managerRole);
        configuration.setEnabledPermissions(new LinkedHashSet<>(managerDefaults()));
        return configuration;
    }

    private void requireOwner(Long businessId) {
        if (businessId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business ID is required.");
        if (businessAccess.isSuperAdmin()) return;
        BusinessMembership membership = membershipRepo.findByUserIdAndBusinessIdAndStatus(
                        businessAccess.currentUserId(), businessId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("An active business owner is required."));
        if (membership.getRole() == null || !"OWNER".equalsIgnoreCase(membership.getRole().getName())) {
            throw new AccessDeniedException("Only the business owner can manage manager permissions.");
        }
    }

    private void recordChanges(BusinessRolePermissionConfiguration configuration, Set<String> nextPermissions) {
        Set<String> previous = configuration.getEnabledPermissions() == null
                ? Set.of() : configuration.getEnabledPermissions();
        List<BusinessRolePermissionAudit> changes = new ArrayList<>();
        for (String permission : managerPermissionCatalog()) {
            boolean wasEnabled = previous.contains(permission);
            boolean nowEnabled = nextPermissions.contains(permission);
            if (wasEnabled == nowEnabled) continue;
            BusinessRolePermissionAudit audit = new BusinessRolePermissionAudit();
            audit.setBusiness(configuration.getBusiness());
            audit.setRoleName("MANAGER");
            audit.setPermission(permission);
            audit.setPreviousEnabled(wasEnabled);
            audit.setNewEnabled(nowEnabled);
            audit.setActorUserId(businessAccess.currentUserId());
            changes.add(audit);
        }
        if (!changes.isEmpty()) auditRepo.saveAll(changes);
    }

    private ManagerPermissions response(BusinessRolePermissionConfiguration configuration) {
        Set<String> current = Set.copyOf(configuration.getEnabledPermissions());
        List<PermissionOption> options = managerPermissionCatalog().stream()
                .map(permission -> new PermissionOption(permission, permissionLabel(permission),
                        permissionDescription(permission), MANAGER_DEFAULTS.contains(permission), current.contains(permission)))
                .toList();
        return new ManagerPermissions(configuration.getBusiness().getId(), "MANAGER", MANAGER_DEFAULTS,
                List.copyOf(current), List.copyOf(current), options, configuration.getDefaultsVersion(),
                configuration.getUpdatedAt(), configuration.getUpdatedByUserId());
    }

    private String permissionLabel(String permission) {
        String[] parts = permission.split(":");
        return title(parts[0]) + " · " + title(parts[1]);
    }

    private String permissionDescription(String permission) {
        String[] parts = permission.split(":");
        String action = parts[1].toLowerCase();
        return switch (parts[0]) {
            case "businesses" -> "View business dashboard and reports.";
            case "products" -> title(action) + " business products.";
            case "product-categories" -> title(action) + " business product categories.";
            case "product-listings" -> title(action) + " business product listings.";
            case "orders", "order-items" -> title(action) + " wholesale orders.";
            case "opportunities" -> title(action) + " distribution opportunities.";
            case "partner-applications" -> title(action) + " partner applications.";
            case "agreements" -> title(action) + " distribution agreements.";
            case "territories" -> title(action) + " business territories.";
            default -> title(action) + " " + title(parts[0]) + ".";
        };
    }

    private String title(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').toLowerCase();
        return java.util.Arrays.stream(normalized.split(" "))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    public record PermissionOption(String key, String label, String description, boolean defaultEnabled, boolean enabled) {}
    public record ManagerPermissions(Long businessId, String roleName, List<String> recommendedDefaults,
            List<String> currentPermissions, List<String> effectivePermissions, List<PermissionOption> permissions,
            int defaultsVersion, LocalDateTime updatedAt, Long updatedByUserId) {}
}

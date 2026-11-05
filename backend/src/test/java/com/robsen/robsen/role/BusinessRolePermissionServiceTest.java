package com.robsen.robsen.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.businessMembership.BusinessMembership;
import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;

class BusinessRolePermissionServiceTest {
    private final BusinessRolePermissionConfigurationRepo configurationRepo = mock(BusinessRolePermissionConfigurationRepo.class);
    private final BusinessRolePermissionAuditRepo auditRepo = mock(BusinessRolePermissionAuditRepo.class);
    private final BusinessMembershipRepo membershipRepo = mock(BusinessMembershipRepo.class);
    private final BusinessRepo businessRepo = mock(BusinessRepo.class);
    private final RoleRepo roleRepo = mock(RoleRepo.class);
    private final UserRepo userRepo = mock(UserRepo.class);

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void recommendedDefaultsUseOnlyKnownNonProtectedPermissions() {
        assertTrue(BusinessRolePermissionService.managerPermissionCatalog().containsAll(
                BusinessRolePermissionService.managerDefaults()));
        assertTrue(BusinessRolePermissionService.managerDefaults().contains("products:CREATE"));
        assertTrue(BusinessRolePermissionService.managerDefaults().contains("orders:UPDATE"));
        assertFalse(BusinessRolePermissionService.managerDefaults().contains("memberships:UPDATE"));
        assertFalse(BusinessRolePermissionService.managerDefaults().contains("businesses:UPDATE"));
        assertFalse(BusinessRolePermissionService.managerDefaults().contains("agreements:DELETE"));
    }

    @Test
    void ownerCanReadUpdateAndResetOnlyTheirBusinessConfiguration() {
        setOwnerFor(10L);
        Role manager = managerRole();
        Business business = business(10L);
        when(roleRepo.findByNameIgnoreCaseAndActiveTrue("MANAGER")).thenReturn(Optional.of(manager));
        when(configurationRepo.findByBusinessIdAndRoleId(10L, manager.getId())).thenReturn(Optional.empty());
        when(businessRepo.findById(10L)).thenReturn(Optional.of(business));
        when(configurationRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        BusinessRolePermissionService service = service();

        var initial = service.getManagerPermissions(10L);
        assertTrue(initial.currentPermissions().contains("products:CREATE"));
        assertEquals(BusinessRolePermissionService.managerDefaults().size(), initial.recommendedDefaults().size());

        var updated = service.updateManagerPermissions(10L, List.of("orders:READ", "products:READ"));
        assertEquals(Set.of("orders:READ", "products:READ"), Set.copyOf(updated.effectivePermissions()));
        verify(auditRepo).saveAll(any());

        when(configurationRepo.findByBusinessIdAndRoleId(10L, manager.getId()))
                .thenReturn(Optional.of(configuration(business, manager, Set.of("orders:READ"))));
        var reset = service.resetManagerPermissions(10L);
        assertEquals(Set.copyOf(BusinessRolePermissionService.managerDefaults()), Set.copyOf(reset.currentPermissions()));
    }

    @Test
    void deniesManagersAndForeignBusinessUsersAndRejectsUnknownOrProtectedPermissions() {
        setMembershipFor(10L, "MANAGER");
        BusinessRolePermissionService service = service();
        assertThrows(AccessDeniedException.class, () -> service.getManagerPermissions(10L));
        assertThrows(AccessDeniedException.class, () -> service.updateManagerPermissions(11L, List.of("orders:READ")));

        setOwnerFor(10L);
        assertThrows(ResponseStatusException.class, () -> service.updateManagerPermissions(10L, List.of("unknown:READ")));
        assertThrows(ResponseStatusException.class, () -> service.updateManagerPermissions(10L, List.of("memberships:UPDATE")));
        verify(configurationRepo, never()).save(any());
    }

    @Test
    void existingBusinessInitializationDoesNotOverwriteCustomizedConfiguration() {
        Role manager = managerRole();
        Business business = business(10L);
        when(roleRepo.findByNameIgnoreCaseAndActiveTrue("MANAGER")).thenReturn(Optional.of(manager));
        when(businessRepo.findAll()).thenReturn(List.of(business));
        when(configurationRepo.findByBusinessIdAndRoleId(10L, manager.getId()))
                .thenReturn(Optional.of(configuration(business, manager, Set.of("orders:READ"))));

        service().run();

        verify(configurationRepo, never()).save(any());
    }

    @Test
    void configuredManagerPermissionsReplaceGlobalRolePermissionsWithinThatBusiness() {
        BusinessRolePermissionConfigurationRepo roleConfigRepo = mock(BusinessRolePermissionConfigurationRepo.class);
        Business business = business(10L);
        Role manager = managerRole();
        manager.setPermissions(List.of("products:READ", "products:UPDATE", "orders:UPDATE"));
        BusinessRolePermissionConfiguration configured = configuration(business, manager, Set.of("products:READ"));
        when(roleConfigRepo.findByBusinessIdAndRoleId(10L, manager.getId())).thenReturn(Optional.of(configured));
        var rolePermissions = new com.robsen.robsen.security.RolePermissionService(membershipRepo, roleConfigRepo);

        BusinessMembership membership = new BusinessMembership();
        membership.setBusiness(business);
        membership.setRole(manager);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setPermissionGrants(new java.util.LinkedHashSet<>(List.of("orders:UPDATE")));

        assertTrue(rolePermissions.hasEffectiveMembershipPermission(membership, "products:READ"));
        assertFalse(rolePermissions.hasEffectiveMembershipPermission(membership, "products:UPDATE"));
        assertFalse(rolePermissions.hasEffectiveMembershipPermission(membership, "orders:UPDATE"));
    }

    private BusinessRolePermissionService service() {
        return new BusinessRolePermissionService(configurationRepo, auditRepo, membershipRepo, businessRepo, roleRepo,
                new BusinessAccessService(membershipRepo, userRepo));
    }

    private void setOwnerFor(Long businessId) {
        User user = new User(); user.setId(5L); user.setEmail("owner@example.test");
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase("owner@example.test", "owner@example.test"))
                .thenReturn(Optional.of(user));
        Role role = new Role(); role.setName("OWNER"); role.setActive(true);
        BusinessMembership membership = new BusinessMembership();
        membership.setRole(role); membership.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepo.findByUserIdAndBusinessIdAndStatus(5L, businessId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), "token", List.of()));
    }

    private void setMembershipFor(Long businessId, String roleName) {
        User user = new User(); user.setId(5L); user.setEmail("owner@example.test");
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase("owner@example.test", "owner@example.test"))
                .thenReturn(Optional.of(user));
        Role role = new Role(); role.setName(roleName); role.setActive(true);
        BusinessMembership membership = new BusinessMembership();
        membership.setRole(role); membership.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepo.findByUserIdAndBusinessIdAndStatus(5L, businessId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), "token", List.of()));
    }

    private Role managerRole() { Role role = new Role(); role.setId(9L); role.setName("MANAGER"); role.setActive(true); return role; }
    private Business business(Long id) { Business business = new Business(); business.setId(id); business.setLegalName("Business " + id); return business; }
    private BusinessRolePermissionConfiguration configuration(Business business, Role role, Set<String> permissions) {
        BusinessRolePermissionConfiguration configuration = new BusinessRolePermissionConfiguration();
        configuration.setId(2L); configuration.setBusiness(business); configuration.setRole(role);
        configuration.setEnabledPermissions(permissions); return configuration;
    }
}

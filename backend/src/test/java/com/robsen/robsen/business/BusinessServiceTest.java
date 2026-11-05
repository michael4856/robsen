package com.robsen.robsen.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.robsen.robsen.businessMembership.BusinessMembership;
import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.role.RoleRepo;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.role.BusinessRolePermissionService;
import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;

class BusinessServiceTest {
    private final BusinessRepo businessRepo = mock(BusinessRepo.class);
    private final BusinessMembershipRepo membershipRepo = mock(BusinessMembershipRepo.class);
    private final UserRepo userRepo = mock(UserRepo.class);
    private final RoleRepo roleRepo = mock(RoleRepo.class);
    private final BusinessService businessService = new BusinessService(
            businessRepo,
            new BusinessAccessService(membershipRepo, userRepo),
            membershipRepo,
            roleRepo,
            mock(BusinessDashboardService.class));

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPendingBusinessAndMakesAuthenticatedCreatorItsOwner() {
        User creator = new User();
        creator.setId(7L);
        creator.setEmail("owner@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(creator.getEmail(), "token", List.of()));
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(creator.getEmail(), creator.getEmail()))
                .thenReturn(Optional.of(creator));

        Role ownerRole = new Role();
        ownerRole.setId(2L);
        ownerRole.setName("OWNER");
        ownerRole.setActive(true);
        when(roleRepo.findByNameIgnoreCaseAndActiveTrue("OWNER")).thenReturn(Optional.of(ownerRole));
        when(businessRepo.save(any(Business.class))).thenAnswer(invocation -> {
            Business saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        Business input = new Business();
        input.setLegalName("  Robsen Supply Ltd  ");
        input.setVerificationStatus(Status.APPROVED);
        input.setActive(false);

        BusinessRolePermissionService permissionConfiguration = mock(BusinessRolePermissionService.class);
        BusinessService service = new BusinessService(businessRepo,
                new BusinessAccessService(membershipRepo, userRepo), membershipRepo, roleRepo,
                mock(BusinessDashboardService.class), permissionConfiguration);
        Business created = service.createBusiness(input);

        assertEquals("Robsen Supply Ltd", created.getLegalName());
        assertEquals(Status.PENDING, created.getVerificationStatus());
        assertEquals(true, created.isActive());
        org.mockito.ArgumentCaptor<BusinessMembership> membership =
                org.mockito.ArgumentCaptor.forClass(BusinessMembership.class);
        verify(membershipRepo).save(membership.capture());
        assertEquals(creator, membership.getValue().getUser());
        assertEquals(15L, membership.getValue().getBusiness().getId());
        assertEquals(ownerRole, membership.getValue().getRole());
        assertEquals(MembershipStatus.ACTIVE, membership.getValue().getStatus());
        assertNotNull(membership.getValue().getJoinedAt());
        verify(permissionConfiguration).initializeBusiness(15L);
    }

    @Test
    void returnsOnlyTheAuthenticatedUsersActiveBusinessMemberships() {
        User creator = new User();
        creator.setId(7L);
        creator.setEmail("owner@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(creator.getEmail(), "token", List.of()));
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(creator.getEmail(), creator.getEmail()))
                .thenReturn(Optional.of(creator));

        Business business = new Business();
        business.setId(15L);
        business.setLegalName("Robsen Supply Ltd");
        business.setBusinessType("WHOLESALER");
        business.setVerificationStatus(Status.PENDING);
        business.setActive(true);
        Role ownerRole = new Role();
        ownerRole.setName("OWNER");
        BusinessMembership membership = new BusinessMembership();
        membership.setId(20L);
        membership.setBusiness(business);
        membership.setRole(ownerRole);
        membership.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepo.findByUserIdAndStatus(7L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));

        List<BusinessService.MyBusiness> result = businessService.getMyBusinesses();

        assertEquals(1, result.size());
        assertEquals(15L, result.get(0).id());
        assertEquals(20L, result.get(0).membershipId());
        assertEquals("OWNER", result.get(0).membershipRole());
        assertEquals(MembershipStatus.ACTIVE, result.get(0).membershipStatus());
        verify(membershipRepo).findByUserIdAndStatus(7L, MembershipStatus.ACTIVE);
    }
}

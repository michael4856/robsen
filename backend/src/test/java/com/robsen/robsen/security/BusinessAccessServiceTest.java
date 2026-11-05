package com.robsen.robsen.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.businessMembership.BusinessMembership;
import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;

class BusinessAccessServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checksPermissionAgainstTheActiveMembershipForThatBusiness() {
        BusinessMembershipRepo membershipRepo = mock(BusinessMembershipRepo.class);
        UserRepo userRepo = mock(UserRepo.class);
        User user = new User();
        user.setId(17L);
        Role role = new Role();
        role.setActive(true);
        role.setPermissions(List.of("orders:READ"));
        BusinessMembership membership = new BusinessMembership();
        membership.setUser(user);
        membership.setBusiness(new Business());
        membership.getBusiness().setId(25L);
        membership.setRole(role);
        when(userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase("person@example.com", "person@example.com"))
                .thenReturn(Optional.of(user));
        when(membershipRepo.findByUserIdAndBusinessIdAndStatus(17L, 25L, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(membershipRepo.findByUserIdAndBusinessIdAndStatus(17L, 26L, MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("person@example.com", "ignored", List.of()));

        BusinessAccessService access = new BusinessAccessService(membershipRepo, userRepo);

        assertTrue(access.canAccess(25L, "orders:READ"));
        assertFalse(access.canAccess(25L, "orders:UPDATE"));
        assertFalse(access.canAccess(26L, "orders:READ"));
    }
}

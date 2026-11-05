package com.robsen.robsen.security;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;

@Service
public class BusinessAccessService {
    private final BusinessMembershipRepo membershipRepo;
    private final UserRepo userRepo;
    private final RolePermissionService rolePermissionService;

    public BusinessAccessService(BusinessMembershipRepo membershipRepo, UserRepo userRepo) {
        this(membershipRepo, userRepo, null);
    }

    @Autowired
    public BusinessAccessService(
            BusinessMembershipRepo membershipRepo, UserRepo userRepo, RolePermissionService rolePermissionService) {
        this.membershipRepo = membershipRepo;
        this.userRepo = userRepo;
        this.rolePermissionService = rolePermissionService;
    }

    @Transactional(readOnly = true)
    public List<Long> accessibleBusinessIds() {
        if (isSuperAdmin()) {
            return List.of();
        }
        User user = currentUserEntity();
        return membershipRepo.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE).stream()
                .map(membership -> membership.getBusiness().getId())
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean canAccess(Long businessId) {
        if (businessId == null) {
            return false;
        }
        if (isSuperAdmin()) {
            return true;
        }
        User user = currentUserEntity();
        return membershipRepo.findByUserIdAndBusinessIdAndStatus(
                user.getId(), businessId, MembershipStatus.ACTIVE).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean canAccess(Long businessId, String permission) {
        if (isSuperAdmin()) return true;
        if (businessId == null) return false;
        return membershipRepo.findByUserIdAndBusinessIdAndStatus(
                        currentUserEntity().getId(), businessId, MembershipStatus.ACTIVE)
                .map(membership -> rolePermissionService == null
                        ? RolePermissionService.hasMembershipPermission(membership, permission)
                        : rolePermissionService.hasEffectiveMembershipPermission(membership, permission))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isBusinessOwner(Long businessId) {
        if (businessId == null || isSuperAdmin()) return false;
        return membershipRepo.findByUserIdAndBusinessIdAndStatus(
                        currentUserEntity().getId(), businessId, MembershipStatus.ACTIVE)
                .map(membership -> membership.getRole() != null
                        && "OWNER".equalsIgnoreCase(membership.getRole().getName()))
                .orElse(false);
    }

    public void requireAccess(Long businessId) {
        if (!canAccess(businessId)) {
            throw new AccessDeniedException("You do not have access to this business.");
        }
    }

    public void requireAccess(Long businessId, String permission) {
        if (!canAccess(businessId, permission)) {
            throw new AccessDeniedException("Your business role does not grant " + permission + ".");
        }
    }

    public boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    public Long currentUserId() {
        return currentUserEntity().getId();
    }

    public User currentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        String identifier = authentication.getName();
        return userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new AccessDeniedException("Account is not available."));
    }
}

package com.robsen.robsen.businessMembership;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;
import com.robsen.robsen.security.BusinessAccessService;

@Service 
public class BusinessMembershipService {
    private final BusinessMembershipRepo businessMembershipRepo;
    private final UserRepo userRepo;
    private final BusinessAccessService businessAccess;

    public BusinessMembershipService(
            BusinessMembershipRepo businessMembershipRepo, UserRepo userRepo, BusinessAccessService businessAccess){
        this.businessMembershipRepo = businessMembershipRepo;
        this.userRepo = userRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public BusinessMembership create(BusinessMembership membership) {
        membership.setId(null);
        Role actorRole = requireBusinessManager(membership.getBusiness().getId(), "memberships:CREATE");
        rejectOwnerRoleGrant(actorRole, membership.getRole());
        if (membership.getJoinedAt() == null) {
            membership.setJoinedAt(LocalDateTime.now());
        }
        return businessMembershipRepo.save(membership);
    }

    @Transactional(readOnly = true)
    public BusinessMembership get(Long id) {
        BusinessMembership membership = businessMembershipRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found: " + id));
        businessAccess.requireAccess(membership.getBusiness().getId(), "memberships:READ");
        return membership;
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getAll() {
        if (businessAccess.isSuperAdmin()) return businessMembershipRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return businessMembershipRepo.findByBusinessIdIn(businessIds).stream()
                .filter(membership -> businessAccess.canAccess(
                        membership.getBusiness().getId(), "memberships:READ"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getByUser(Long userId) {
        if (!businessAccess.isSuperAdmin() && !businessAccess.currentUserId().equals(userId)) {
            return getAll().stream()
                    .filter(membership -> membership.getUser().getId().equals(userId))
                    .toList();
        }
        if (businessAccess.isSuperAdmin()) return businessMembershipRepo.findByUserId(userId);
        return getAll().stream()
                .filter(membership -> membership.getUser().getId().equals(userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getByBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "memberships:READ");
        return businessMembershipRepo.findByBusinessId(businessId);
    }

    @Transactional
    public BusinessMembership update(Long id, BusinessMembership input) {
        BusinessMembership membership = get(id);
        Role actorRole = requireBusinessManager(membership.getBusiness().getId(), "memberships:UPDATE");
        if (isOwner(membership.getRole()) && actorRole != null && !isOwner(actorRole)) {
            throw new AccessDeniedException("Only a business owner can change the owner membership.");
        }
        Role targetActorRole = membership.getBusiness().getId().equals(input.getBusiness().getId())
                ? actorRole
                : requireBusinessManager(input.getBusiness().getId(), "memberships:UPDATE");
        rejectOwnerRoleGrant(targetActorRole, input.getRole());
        membership.setUser(input.getUser());
        membership.setBusiness(input.getBusiness());
        membership.setRole(input.getRole());
        membership.setStatus(input.getStatus());
        return businessMembershipRepo.save(membership);
    }

    @Transactional
    public void delete(Long id) {
        BusinessMembership membership = get(id);
        Role actorRole = requireBusinessManager(membership.getBusiness().getId(), "memberships:DELETE");
        if (isOwner(membership.getRole()) && actorRole != null && !isOwner(actorRole)) {
            throw new AccessDeniedException("Only a business owner can remove the owner membership.");
        }
        membership.setStatus(com.robsen.robsen.common.MembershipStatus.REMOVED);
        businessMembershipRepo.save(membership);
    }

    private Role requireBusinessManager(Long businessId, String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        String identifier = authentication.getName();
        User actor = userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new AccessDeniedException("Account is not available."));
        if ("SUPER_ADMIN".equalsIgnoreCase(actor.getRole())) {
            return null;
        }
        BusinessMembership membership = businessMembershipRepo
                .findByUserIdAndBusinessIdAndStatus(actor.getId(), businessId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("You are not an active member of this business."));
        Role role = membership.getRole();
        if (role == null || !role.isActive()) {
            throw new AccessDeniedException("An active business role is required.");
        }
        if (role.getPermissions() == null || !role.getPermissions().contains(permission)) {
            throw new AccessDeniedException("Your business role does not grant " + permission + ".");
        }
        return role;
    }

    private void rejectOwnerRoleGrant(Role actorRole, Role targetRole) {
        if (targetRole != null && isOwner(targetRole) && actorRole != null && !isOwner(actorRole)) {
            throw new AccessDeniedException("Only a business owner can assign the owner role.");
        }
    }

    private boolean isOwner(Role role) {
        return role != null && "OWNER".equalsIgnoreCase(role.getName());
    }
}

package com.robsen.robsen.businessMembership;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
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
import com.robsen.robsen.security.RolePermissionService;
import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.role.RoleRepo;
import com.robsen.robsen.role.RolePermissions;

@Service 
public class BusinessMembershipService {
    private final BusinessMembershipRepo businessMembershipRepo;
    private final UserRepo userRepo;
    private final BusinessAccessService businessAccess;
    private final BusinessRepo businessRepo;
    private final RoleRepo roleRepo;
    private final RolePermissionService rolePermissionService;

    public BusinessMembershipService(
            BusinessMembershipRepo businessMembershipRepo,
            UserRepo userRepo,
            BusinessAccessService businessAccess,
            BusinessRepo businessRepo,
            RoleRepo roleRepo,
            RolePermissionService rolePermissionService) {
        this.businessMembershipRepo = businessMembershipRepo;
        this.userRepo = userRepo;
        this.businessAccess = businessAccess;
        this.businessRepo = businessRepo;
        this.roleRepo = roleRepo;
        this.rolePermissionService = rolePermissionService;
    }

    @Transactional
    public BusinessMembership create(BusinessMembership membership) {
        if (membership.getBusiness() == null || membership.getBusiness().getId() == null
                || membership.getUser() == null || membership.getUser().getId() == null
                || membership.getRole() == null || membership.getRole().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business, user, and role are required.");
        }
        membership.setId(null);
        Long businessId = membership.getBusiness().getId();
        Role actorRole = requireBusinessManager(businessId, "memberships:CREATE");
        Business business = businessRepo.findByIdForUpdate(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found."));
        User invitedUser = userRepo.findById(membership.getUser().getId())
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active user not found."));
        Role assignedRole = roleRepo.findById(membership.getRole().getId())
                .filter(Role::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active business role not found."));
        assertRoleCanBeGranted(actorRole, assignedRole);
        if (businessMembershipRepo.existsByUserIdAndBusinessIdAndStatusIn(
                invitedUser.getId(), businessId, List.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user already belongs to the business or has a pending invitation.");
        }
        membership.setBusiness(business);
        membership.setUser(invitedUser);
        membership.setRole(assignedRole);
        membership.setStatus(MembershipStatus.INVITED);
        membership.setPermissionGrants(new LinkedHashSet<>());
        membership.setInvitedAt(LocalDateTime.now());
        membership.setExpiresAt(LocalDateTime.now().plus(14, ChronoUnit.DAYS));
        return businessMembershipRepo.save(membership);
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getMyInvitations() {
        LocalDateTime now = LocalDateTime.now();
        return businessMembershipRepo.findByUserIdAndStatus(businessAccess.currentUserId(), MembershipStatus.INVITED)
                .stream()
                .filter(invitation -> invitation.getExpiresAt() == null || invitation.getExpiresAt().isAfter(now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserOption> getEligibleUsers(Long businessId, String query) {
        businessAccess.requireAccess(businessId, "memberships:CREATE");
        String term = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        if (term.isEmpty()) return List.of();
        return userRepo.findByActiveTrue().stream()
                .filter(user -> (user.getFullName() != null && user.getFullName().toLowerCase().contains(term))
                        || (user.getEmail() != null && user.getEmail().toLowerCase().contains(term))
                        || (user.getUsername() != null && user.getUsername().toLowerCase().contains(term)))
                .filter(user -> !businessMembershipRepo.existsByUserIdAndBusinessIdAndStatusIn(
                        user.getId(), businessId, List.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED)))
                .limit(30)
                .map(user -> new UserOption(user.getId(), user.getFullName(), user.getEmail(), user.getUsername()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleOption> getAssignableRoles(Long businessId) {
        Role actorRole = requireBusinessManager(businessId, "memberships:CREATE");
        return roleRepo.findByActiveTrue().stream()
                .filter(role -> !isOwner(role) || actorRole == null)
                .filter(role -> !"ADMIN".equalsIgnoreCase(role.getName()) || actorRole == null || isOwner(actorRole))
                .filter(role -> actorRole == null || isOwner(actorRole)
                        || (role.getPermissions() != null && actorRole.getPermissions() != null
                                && actorRole.getPermissions().containsAll(role.getPermissions())))
                .map(role -> new RoleOption(role.getId(), role.getName()))
                .toList();
    }

    @Transactional
    public BusinessMembership acceptInvitation(Long id) {
        BusinessMembership invitation = businessMembershipRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found."));
        if (!invitation.getUser().getId().equals(businessAccess.currentUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("This invitation belongs to another user.");
        }
        if (invitation.getStatus() != MembershipStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This invitation is no longer pending.");
        }
        if (invitation.getExpiresAt() != null && !invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation has expired.");
        }
        invitation.setStatus(MembershipStatus.ACTIVE);
        invitation.setJoinedAt(LocalDateTime.now());
        return businessMembershipRepo.save(invitation);
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

    @Transactional(readOnly = true)
    public List<String> getPermissionCatalog(Long businessId) {
        requireOwner(businessId);
        return RolePermissions.catalog();
    }

    @Transactional
    public BusinessMembership setPermissionGrants(Long membershipId, List<String> permissions) {
        if (permissions == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A permissions list is required.");
        }
        BusinessMembership membership = businessMembershipRepo.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found."));
        requireOwner(membership.getBusiness().getId());
        if (isOwner(membership.getRole())) {
            throw new AccessDeniedException("The owner already has full business permissions.");
        }
        if (membership.getStatus() == MembershipStatus.SUSPENDED
                || membership.getStatus() == MembershipStatus.REMOVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Permissions can only be assigned to invited or active members.");
        }
        try {
            membership.setPermissionGrants(new LinkedHashSet<>(RolePermissions.normalize(permissions)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return businessMembershipRepo.save(membership);
    }

    @Transactional
    public BusinessMembership update(Long id, BusinessMembership input) {
        BusinessMembership membership = get(id);
        Role actorRole = requireBusinessManager(membership.getBusiness().getId(), "memberships:UPDATE");
        if (input.getRole() == null || input.getRole().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A business role is required.");
        }
        Role assignedRole = roleRepo.findById(input.getRole().getId())
                .filter(Role::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active business role not found."));
        if (isOwner(membership.getRole()) && actorRole != null) {
            throw new AccessDeniedException("The owner membership can only be changed through an ownership transfer.");
        }
        assertRoleCanBeGranted(actorRole, assignedRole);
        membership.setRole(assignedRole);
        if (input.getStatus() == MembershipStatus.SUSPENDED || input.getStatus() == MembershipStatus.REMOVED) {
            membership.setStatus(input.getStatus());
        } else if (input.getStatus() == MembershipStatus.ACTIVE
                && membership.getStatus() == MembershipStatus.SUSPENDED) {
            membership.setStatus(MembershipStatus.ACTIVE);
        }
        return businessMembershipRepo.save(membership);
    }

    @Transactional
    public void delete(Long id) {
        BusinessMembership membership = get(id);
        Role actorRole = requireBusinessManager(membership.getBusiness().getId(), "memberships:DELETE");
        if (isOwner(membership.getRole()) && actorRole != null && !isOwner(actorRole)) {
            throw new AccessDeniedException("Only a business owner can remove the owner membership.");
        }
        if (isOwner(membership.getRole()) && membership.getUser().getId().equals(businessAccess.currentUserId())) {
            throw new AccessDeniedException("Transfer business ownership before removing your owner membership.");
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
        if (!rolePermissionService.hasEffectiveMembershipPermission(membership, permission)) {
            throw new AccessDeniedException("Your business role does not grant " + permission + ".");
        }
        return role;
    }

    private void requireOwner(Long businessId) {
        BusinessMembership owner = businessMembershipRepo
                .findByUserIdAndBusinessIdAndStatus(
                        businessAccess.currentUserId(), businessId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("An active business owner is required."));
        if (!isOwner(owner.getRole())) {
            throw new AccessDeniedException("Only the business owner can manage individual permission grants.");
        }
    }

    private void assertRoleCanBeGranted(Role actorRole, Role targetRole) {
        if (targetRole == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A role is required.");
        if (isOwner(targetRole) && actorRole != null) {
            throw new AccessDeniedException("Ownership must be transferred through the owner transfer workflow.");
        }
        if ("ADMIN".equalsIgnoreCase(targetRole.getName())
                && actorRole != null && !isOwner(actorRole)) {
            throw new AccessDeniedException("Only a business owner can assign the ADMIN role.");
        }
        if (actorRole != null && !isOwner(actorRole)
                && (actorRole.getPermissions() == null || targetRole.getPermissions() == null
                        || !actorRole.getPermissions().containsAll(targetRole.getPermissions()))) {
            throw new AccessDeniedException("You cannot assign a role with broader permissions than your own.");
        }
    }

    private boolean isOwner(Role role) {
        return role != null && "OWNER".equalsIgnoreCase(role.getName());
    }

    public record UserOption(Long id, String fullName, String email, String username) {}

    public record RoleOption(Long id, String name) {}
}

package com.robsen.robsen.businessMembership;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-memberships")
public class BusinessMembershipController {
    private final BusinessMembershipService membershipService;

    public BusinessMembershipController(BusinessMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessMembership create(@RequestBody BusinessMembership membership) {
        return membershipService.create(membership);
    }

    @GetMapping("/{id}")
    public BusinessMembership get(@PathVariable Long id) {
        return membershipService.get(id);
    }

    @GetMapping
    public List<BusinessMembership> getAll() {
        return membershipService.getAll();
    }

    @GetMapping("/invitations")
    public List<BusinessMembership> getMyInvitations() {
        return membershipService.getMyInvitations();
    }

    @GetMapping("/available-users")
    public List<BusinessMembershipService.UserOption> getEligibleUsers(
            @RequestParam Long businessId, @RequestParam String query) {
        return membershipService.getEligibleUsers(businessId, query);
    }

    @GetMapping("/assignable-roles")
    public List<BusinessMembershipService.RoleOption> getAssignableRoles(@RequestParam Long businessId) {
        return membershipService.getAssignableRoles(businessId);
    }

    @PostMapping("/invitations/{id}/accept")
    public BusinessMembership acceptInvitation(@PathVariable Long id) {
        return membershipService.acceptInvitation(id);
    }

    @GetMapping("/user/{userId}")
    public List<BusinessMembership> getByUser(@PathVariable Long userId) {
        return membershipService.getByUser(userId);
    }

    @GetMapping("/business/{businessId}")
    public List<BusinessMembership> getByBusiness(@PathVariable Long businessId) {
        return membershipService.getByBusiness(businessId);
    }

    @GetMapping("/permission-catalog")
    public List<String> getPermissionCatalog(@RequestParam Long businessId) {
        return membershipService.getPermissionCatalog(businessId);
    }

    @PutMapping("/{id}/permissions")
    public BusinessMembership setPermissionGrants(
            @PathVariable Long id,
            @RequestBody PermissionGrantRequest request) {
        return membershipService.setPermissionGrants(
                id, request == null ? null : request.permissions());
    }

    @PutMapping("/{id}")
    public BusinessMembership update(@PathVariable Long id, @RequestBody BusinessMembership membership) {
        return membershipService.update(id, membership);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        membershipService.delete(id);
    }

    public record PermissionGrantRequest(List<String> permissions) {}
}

package com.robsen.robsen.role;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/business/{businessId}/roles/{roleName}/permissions")
public class BusinessRolePermissionController {
    private final BusinessRolePermissionService permissionService;

    public BusinessRolePermissionController(BusinessRolePermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public BusinessRolePermissionService.ManagerPermissions get(
            @PathVariable Long businessId, @PathVariable String roleName) {
        requireManagerRole(roleName);
        return permissionService.getManagerPermissions(businessId);
    }

    @PutMapping
    public BusinessRolePermissionService.ManagerPermissions update(
            @PathVariable Long businessId,
            @PathVariable String roleName,
            @Valid @RequestBody UpdateRequest request) {
        requireManagerRole(roleName);
        return permissionService.updateManagerPermissions(businessId, request.permissions());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.OK)
    public BusinessRolePermissionService.ManagerPermissions reset(
            @PathVariable Long businessId, @PathVariable String roleName) {
        requireManagerRole(roleName);
        return permissionService.resetManagerPermissions(businessId);
    }

    private void requireManagerRole(String roleName) {
        if (!"MANAGER".equalsIgnoreCase(roleName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business role configuration not found.");
        }
    }

    public record UpdateRequest(@NotNull List<String> permissions) {}
}

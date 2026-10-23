package com.robsen.robsen.role;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service 
public class RoleService {
    private final RoleRepo roleRepo;

    public RoleService(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }
    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + id));
    }
    @Transactional
    public Role createRole(Role role) {
        role.setId(null);
        validateRoleName(role.getName(), null);
        role.setName(role.getName().trim());
        role.setActive(true);
        role.setPermissions(validatePermissions(role.getPermissions()));
        role.setNavigationItems(navigationFor(role.getPermissions()));
        return roleRepo.save(role);
    }
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }
    @Transactional
    public Role updateRole(Long id, Role updatedRole) {
        Role role = getRoleById(id);
        validateRoleName(updatedRole.getName(), id);
        role.setName(updatedRole.getName().trim());
        role.setDescription(updatedRole.getDescription());
        role.setActive(updatedRole.isActive());
        role.setPermissions(validatePermissions(updatedRole.getPermissions()));
        role.setNavigationItems(navigationFor(role.getPermissions()));
        return roleRepo.save(role);
    }
    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        role.setActive(false);
        roleRepo.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllActiveRoles() {
        return roleRepo.findByActiveTrue();
    }

    public List<String> getPermissionCatalog() {
        return RolePermissions.catalog();
    }

    private void validateRoleName(String name, Long excludedId) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required.");
        }
        boolean exists = excludedId == null
                ? roleRepo.findByNameIgnoreCase(name.trim()).isPresent()
                : roleRepo.existsByNameIgnoreCaseAndIdNot(name.trim(), excludedId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A role with that name already exists.");
        }
    }

    private List<String> validatePermissions(List<String> permissions) {
        try {
            return RolePermissions.normalize(permissions);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private List<String> navigationFor(List<String> permissions) {
        return RolePermissions.RESOURCES.stream()
                .filter(resource -> permissions.contains(RolePermissions.permission(resource, "READ")))
                .toList();
    }

}

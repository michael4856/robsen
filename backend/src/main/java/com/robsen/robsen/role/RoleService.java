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
        role.setActive(true);
        validateNavigationItems(role.getNavigationItems());
        if (role.getNavigationItems() == null) {
            role.setNavigationItems(List.of());
        }
        return roleRepo.save(role);
    }
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }
    @Transactional
    public Role updateRole(Long id, Role updatedRole) {
        Role role = getRoleById(id);
        role.setName(updatedRole.getName());
        role.setDescription(updatedRole.getDescription());
        role.setActive(updatedRole.isActive());
        validateNavigationItems(updatedRole.getNavigationItems());
        if (updatedRole.getNavigationItems() != null) {
            role.setNavigationItems(updatedRole.getNavigationItems());
        }
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

    private void validateNavigationItems(List<String> navigationItems) {
        if (navigationItems == null) {
            return;
        }
        if (!RoleNavigation.containsOnlySupportedItems(navigationItems)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Role contains an unsupported navigation item.");
        }
    }
}

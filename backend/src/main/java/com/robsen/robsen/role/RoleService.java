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
}

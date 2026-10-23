package com.robsen.robsen.role;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleBootstrap implements CommandLineRunner {
    private final RoleRepo roleRepo;

    public RoleBootstrap(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (Role defaultRole : DefaultRoles.roles()) {
            var existing = roleRepo.findByNameIgnoreCase(defaultRole.getName());
            if (existing.isEmpty()) {
                roleRepo.save(defaultRole);
            } else {
                Role role = existing.get();
                if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
                    role.setPermissions(defaultRole.getPermissions());
                    role.setNavigationItems(defaultRole.getNavigationItems());
                    roleRepo.save(role);
                }
            }
        }
    }
}

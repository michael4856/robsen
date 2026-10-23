package com.robsen.robsen.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;

@Service
public class RobsenUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;
    private final RolePermissionService rolePermissionService;

    public RobsenUserDetailsService(UserRepo userRepo, RolePermissionService rolePermissionService) {
        this.userRepo = userRepo;
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        User user = userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
        if (rolePermissionService.isSuperAdmin(user)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        rolePermissionService.permissionsFor(user).stream()
                .map(permission -> new SimpleGrantedAuthority("PERMISSION:" + permission))
                .forEach(authorities::add);
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail() == null ? user.getUsername() : user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isActive())
                .build();
    }
}

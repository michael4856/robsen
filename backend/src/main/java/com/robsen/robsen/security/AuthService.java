package com.robsen.robsen.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;
import com.robsen.robsen.user.UserService;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.role.RoleNavigation;
import com.robsen.robsen.role.RoleRepo;

import java.util.List;

@Service
public class AuthService {
    private final UserRepo userRepo;
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepo roleRepo;

    public AuthService(
            UserRepo userRepo,
            UserService userService,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RoleRepo roleRepo) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.roleRepo = roleRepo;
    }

    @Transactional
    public UserProfile register(RegisterRequest request) {
        if (isBlank(request.fullName()) || isBlank(request.email())
                || isBlank(request.username()) || isBlank(request.password())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Full name, email, username, and password are required.");
        }
        if (request.password().length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setUsername(request.username().trim());
        user.setPassword(request.password());
        user.setPhoneNumber(request.phoneNumber());
        user.setRole("USER");
        user.setActive(true);
        return toProfile(userService.createUser(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (isBlank(request.identifier()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password.");
        }

        User user = userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                        request.identifier().trim(), request.identifier().trim())
                .filter(candidate -> candidate.isActive()
                        && candidate.getPassword() != null
                        && passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username/email or password."));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.identifier().trim());
        return new AuthResponse(
                jwtService.createToken(userDetails),
                "Bearer",
                jwtService.getExpirationSeconds(),
                toProfile(user));
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(String identifier) {
        User user = userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Account is no longer available."));
        return toProfile(user);
    }

    private UserProfile toProfile(User user) {
        String roleName = user.getRole() == null ? "" : user.getRole().trim();
        if (roleName.regionMatches(true, 0, "ROLE_", 0, 5)) {
            roleName = roleName.substring(5);
        }
        String assignedRoleName = roleName;
        List<String> navigationItems = roleRepo.findByNameIgnoreCaseAndActiveTrue(assignedRoleName)
                .map(Role::getNavigationItems)
                .filter(items -> items != null)
                .orElseGet(() -> "ADMIN".equalsIgnoreCase(assignedRoleName)
                        ? RoleNavigation.ALL
                        : RoleNavigation.DEFAULT_USER);
        return UserProfile.from(user, navigationItems);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LoginRequest(String identifier, String password) {
    }

    public record RegisterRequest(
            String fullName,
            String email,
            String username,
            String password,
            String phoneNumber) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {
    }

    public record UserProfile(
            Long id,
            String fullName,
            String email,
            String username,
            String role,
            String phoneNumber,
            boolean active,
            List<String> navigationItems) {
        static UserProfile from(User user, List<String> navigationItems) {
            return new UserProfile(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getRole(),
                    user.getPhoneNumber(),
                    user.isActive(),
                    navigationItems);
        }
    }
}

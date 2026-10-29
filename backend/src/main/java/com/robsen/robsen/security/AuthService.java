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

import java.util.List;

@Service
public class AuthService {
    private final UserRepo userRepo;
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RolePermissionService rolePermissionService;

    public AuthService(
            UserRepo userRepo,
            UserService userService,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolePermissionService rolePermissionService) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolePermissionService = rolePermissionService;
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
    public AuthResponse refresh(String identifier) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);
        if (!userDetails.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled.");
        }
        User user = userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Account is no longer available."));
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

    @Transactional
    public AuthResponse updateProfile(String identifier, ProfileUpdateRequest request) {
        User user = findActiveUser(identifier);
        if (isBlank(request.fullName()) || isBlank(request.email()) || isBlank(request.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email, and username are required.");
        }
        User candidate = new User();
        candidate.setEmail(request.email().trim());
        candidate.setUsername(request.username().trim());
        userService.ensureProfileIdentityAvailable(candidate, user.getId());
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim());
        user.setUsername(request.username().trim());
        user.setPhoneNumber(request.phoneNumber() == null || request.phoneNumber().isBlank()
                ? null : request.phoneNumber().trim());
        user.setProfilePicture(validateProfilePicture(request.profilePicture()));
        User savedUser = userRepo.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        return new AuthResponse(jwtService.createToken(userDetails), "Bearer", jwtService.getExpirationSeconds(), toProfile(savedUser));
    }

    @Transactional
    public void changePassword(String identifier, ChangePasswordRequest request) {
        User user = findActiveUser(identifier);
        if (isBlank(request.currentPassword()) || isBlank(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current and new passwords are required.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        if (request.newPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters long.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a password different from your current one.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);
    }

    private User findActiveUser(String identifier) {
        return userRepo.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is no longer available."));
    }

    private String validateProfilePicture(String picture) {
        if (picture == null || picture.isBlank()) return null;
        if (picture.length() > 2_800_000
                || !picture.matches("^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a PNG, JPEG, or WebP image up to 2 MB.");
        }
        return picture;
    }

    private UserProfile toProfile(User user) {
        List<String> navigationItems = rolePermissionService.navigationFor(user);
        List<String> permissions = rolePermissionService.permissionsFor(user);
        return UserProfile.from(user, navigationItems, permissions,
                rolePermissionService.isSuperAdmin(user), rolePermissionService.businessIdsFor(user));
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
            List<String> navigationItems,
            List<String> permissions,
            boolean superAdmin,
            List<Long> businessIds,
            String profilePicture) {
        static UserProfile from(
                User user,
                List<String> navigationItems,
                List<String> permissions,
                boolean superAdmin,
                List<Long> businessIds) {
            return new UserProfile(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getRole(),
                    user.getPhoneNumber(),
                    user.isActive(),
                    navigationItems,
                    permissions,
                    superAdmin,
                    businessIds,
                    user.getProfilePicture());
        }
    }

    public record ProfileUpdateRequest(
            String fullName,
            String email,
            String username,
            String phoneNumber,
            String profilePicture) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }
}

package com.robsen.robsen.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(User user){
        user.setId(null);
        ensureUnique(user, null);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }
        return userRepo.save(user);
    }
    @Transactional(readOnly = true)
    public ResponseEntity<User> getUserById(Long id){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return ResponseEntity.ok(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers(){
        return userRepo.findByActiveTrue();
    }

    @Transactional
    public ResponseEntity<User> updateUser(Long id, User updatedUser) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        protectLastSuperAdmin(user, updatedUser.getRole(), updatedUser.isActive());
        ensureUnique(updatedUser, id);
        user.setFullName(updatedUser.getFullName());
        user.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        user.setRole(updatedUser.getRole());
        user.setUsername(updatedUser.getUsername());
        user.setPhoneNumber(updatedUser.getPhoneNumber());
        user.setActive(updatedUser.isActive());

        User savedUser = userRepo.save(user);
        return ResponseEntity.ok(savedUser);
    }

    private void ensureUnique(User user, Long excludedId) {
        boolean emailExists = user.getEmail() != null && (excludedId == null
                ? userRepo.existsByEmail(user.getEmail())
                : userRepo.existsByEmailAndIdNot(user.getEmail(), excludedId));
        boolean usernameExists = user.getUsername() != null && (excludedId == null
                ? userRepo.existsByUsername(user.getUsername())
                : userRepo.existsByUsernameAndIdNot(user.getUsername(), excludedId));
        if (emailExists || usernameExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username is already registered");
        }
    }

    @Transactional
    public ResponseEntity<Void> deleteUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        protectLastSuperAdmin(user, user.getRole(), false);
        user.setActive(false);
        userRepo.save(user);
        return ResponseEntity.noContent().build();
    }

    private void protectLastSuperAdmin(User current, String nextRole, boolean nextActive) {
        if (current.isActive()
                && "SUPER_ADMIN".equalsIgnoreCase(current.getRole())
                && (!nextActive || !"SUPER_ADMIN".equalsIgnoreCase(nextRole))
                && userRepo.countByRoleIgnoreCaseAndActiveTrue("SUPER_ADMIN") <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The last active platform administrator cannot be removed.");
        }
    }

}

package com.robsen.robsen.user;

import org.springframework.http.ResponseEntity;

public class UserService {
    private final UserRepo userRepo;

    UserService(UserRepo userRepo){
        this.userRepo = userRepo;
    }

    public User creatUser(User user){
        return userRepo.save(user);
    }
    public ResponseEntity<User> getUserById(Long id){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return ResponseEntity.ok(user);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public ResponseEntity<User> updateUser(Long id, User updatedUser) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setFullName(updatedUser.getFullName());
        user.setEmail(updatedUser.getEmail());
        user.setPassword(updatedUser.getPassword());
        user.setRole(updatedUser.getRole());
        user.setUsername(updatedUser.getUsername());
        user.setPhoneNumber(updatedUser.getPhoneNumber());

        User savedUser = userRepo.save(user);
        return ResponseEntity.ok(savedUser);
    }

    public ResponseEntity<Void> deleteUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        userRepo.delete(user);
        return ResponseEntity.noContent().build();
    }

}

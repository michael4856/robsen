package com.robsen.robsen.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface UserRepo extends JpaRepository<User, Long> {
    java.util.List<User> findByActiveTrue();
    java.util.Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
    java.util.Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);
    long countByRoleIgnoreCaseAndActiveTrue(String role);
}

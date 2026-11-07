package com.robsen.robsen.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface UserRepo extends JpaRepository<User, Long> {
    java.util.List<User> findByActiveTrue();
    java.util.Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
    java.util.Optional<User> findByEmailIgnoreCase(String email);
    java.util.Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    long countByRoleIgnoreCaseAndActiveTrue(String role);
}

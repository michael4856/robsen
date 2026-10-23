package com.robsen.robsen.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.robsen.robsen.user.User;
import com.robsen.robsen.user.UserRepo;
import com.robsen.robsen.user.UserService;

@Component
public class BootstrapAdminUser implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapAdminUser.class);

    private final UserRepo userRepo;
    private final UserService userService;
    private final String email;
    private final String username;
    private final String password;
    private final String fullName;

    public BootstrapAdminUser(
            UserRepo userRepo,
            UserService userService,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.username:admin}") String username,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.full-name:Robsen Administrator}") String fullName) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.email = email;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Set BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_USERNAME, and BOOTSTRAP_ADMIN_PASSWORD together.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters long.");
        }

        if (userRepo.findByEmailIgnoreCase(email.trim()).isPresent()) {
            logger.info("Bootstrap admin account already exists; leaving it unchanged.");
            return;
        }

        User admin = new User();
        admin.setFullName(fullName.isBlank() ? "Robsen Administrator" : fullName.trim());
        admin.setEmail(email.trim());
        admin.setUsername(username.trim());
        admin.setPassword(password);
        admin.setRole("ADMIN");
        admin.setActive(true);
        userService.createUser(admin);
        logger.info("Created bootstrap administrator account for {}.", email.trim());
    }
}

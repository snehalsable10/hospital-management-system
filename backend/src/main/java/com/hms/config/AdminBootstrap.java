package com.hms.config;

import com.hms.entity.User;
import com.hms.repository.UserRepository;
import com.hms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator on a database that has none.
 *
 * Without this a fresh deployment is unusable: public signup deliberately
 * cannot choose a role - it always makes a PATIENT, which is what stopped
 * anyone registering themselves as an administrator - and promoting an account
 * requires an administrator to already exist. The only way out was to reach
 * the database directly, which a managed platform may not even allow.
 *
 * The credentials come from the environment, never from the repository, and
 * nothing happens unless both are supplied. It runs once: if any active
 * administrator already exists, this does nothing, so a restart or a redeploy
 * cannot quietly reinstate an account somebody deliberately removed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hms.bootstrap.admin.email:}")
    private String email;

    @Value("${hms.bootstrap.admin.password:}")
    private String password;

    @Value("${hms.bootstrap.admin.username:admin}")
    private String username;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }

        if (userRepository.countByRoleAndIsActiveTrue("ADMIN") > 0) {
            log.info("An administrator already exists; skipping bootstrap");
            return;
        }

        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            log.warn("Cannot bootstrap the administrator: {} or {} is already taken by a "
                    + "non-administrator account", email, username);
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setFirstName("Hospital");
        admin.setLastName("Administrator");
        admin.setRole("ADMIN");
        admin.setPassword(passwordEncoder.encode(password));
        admin.setIsActive(true);

        userRepository.save(admin);

        // The address, never the password.
        log.warn("Bootstrapped the first administrator: {}. Change this password now, and "
                + "clear HMS_ADMIN_PASSWORD from the environment.", email);
    }
}

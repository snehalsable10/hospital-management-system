package com.hms.repository;

import com.hms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     * @param email - user email
     * @return Optional<User> - user if exists, empty if not
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     * @param username - user username
     * @return Optional<User> - user if exists, empty if not
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if user exists by email
     * @param email - user email
     * @return true if exists, false if not
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by username
     * @param username - user username
     * @return true if exists, false if not
     */
    boolean existsByUsername(String username);

}
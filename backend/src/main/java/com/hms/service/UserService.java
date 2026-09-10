package com.hms.service;

import com.hms.dto.request.LoginRequest;
import com.hms.dto.request.SignupRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.AuthResponse;
import com.hms.entity.User;
import com.hms.repository.UserRepository;
import com.hms.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user (Signup)
     * @param signupRequest - user registration data
     * @return ApiResponse with success/error message
     */
    public ApiResponse signup(SignupRequest signupRequest) {
        try {
            // Check if email already exists
            if (userRepository.existsByEmail(signupRequest.getEmail())) {
                return new ApiResponse("Email already registered", false);
            }

            // Check if username already exists
            if (userRepository.existsByUsername(signupRequest.getUsername())) {
                return new ApiResponse("Username already taken", false);
            }

            // Create new user
            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setFirstName(signupRequest.getFirstName());
            user.setLastName(signupRequest.getLastName());
            user.setPhone(signupRequest.getPhone());
            user.setRole(signupRequest.getRole());
            
            // Hash password (bcrypt)
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setIsActive(true);

            // Save to database
            userRepository.save(user);

            return new ApiResponse("User registered successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Registration failed: " + e.getMessage(), false);
        }
    }

    /**
     * Authenticate user and generate JWT token (Login)
     * @param loginRequest - user credentials
     * @return AuthResponse with token and user details
     */
    public ApiResponse login(LoginRequest loginRequest) {
        try {
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

            if (!userOptional.isPresent()) {
                return new ApiResponse("Email not registered", false);
            }

            User user = userOptional.get();

            // Check if account is active
            if (!user.getIsActive()) {
                return new ApiResponse("Account is inactive", false);
            }

            // Verify password (compare with hashed password)
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return new ApiResponse("Invalid password", false);
            }

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

            // Build response with user details
            AuthResponse authResponse = AuthResponse.builder()
                    .message("Login successful")
                    .token(token)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole())
                    .success(true)
                    .build();

            return new ApiResponse("Login successful", authResponse, true);
        } catch (Exception e) {
            return new ApiResponse("Login failed: " + e.getMessage(), false);
        }
    }

    /**
     * Get user by ID
     * @param userId - user id
     * @return User object if found
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by email
     * @param email - user email
     * @return User object if found
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if user exists by email
     * @param email - user email
     * @return true if exists, false otherwise
     */
    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

}
package com.hms.service;

import com.hms.dto.request.CreateUserRequest;
import com.hms.dto.request.LoginRequest;
import com.hms.dto.request.SignupRequest;
import com.hms.dto.request.UpdateUserRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.AuthResponse;
import com.hms.dto.response.PageResponse;
import com.hms.dto.response.UserResponse;
import com.hms.entity.User;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.UserRepository;
import com.hms.security.JwtTokenProvider;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Public signup always creates a PATIENT. The caller does not get to choose:
     * accepting a client-supplied role let anyone register as an administrator.
     */
    public static final String DEFAULT_SIGNUP_ROLE = "PATIENT";

    /**
     * The only roles the system recognises. Anything outside this set would
     * create a user no @PreAuthorize rule can ever match.
     */
    public static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DOCTOR", "STAFF", "PATIENT");

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
            user.setRole(DEFAULT_SIGNUP_ROLE);

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
     * Create a user with an explicit role. Only reachable by an administrator:
     * public signup deliberately cannot choose a role.
     *
     * @throws DuplicateResourceException if the email or username is taken
     * @throws IllegalArgumentException   if the role is not one the system recognises
     */
    public ApiResponse createUser(CreateUserRequest request) {
        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    "Unknown role: " + request.getRole() + ". Allowed roles are " + ALLOWED_ROLES);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);

        User saved = userRepository.save(user);

        return new ApiResponse("User created successfully", saved.getId(), true);
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


    /**
     * List the users an administrator manages, newest last.
     */
    public PageResponse<UserResponse> getAllUsersPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Page<User> page = userRepository.findByIsActiveTrue(
                PaginationUtil.pageRequest(pageNumber, pageSize));
        return PaginationUtil.toPageResponse(page.map(UserResponse::from));
    }

    public PageResponse<UserResponse> getUsersByRolePaginated(String role, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Page<User> page = userRepository.findByRoleAndIsActiveTrue(
                normaliseRole(role), PaginationUtil.pageRequest(pageNumber, pageSize));
        return PaginationUtil.toPageResponse(page.map(UserResponse::from));
    }

    /**
     * Edit a user.
     *
     * @param actingUserId the administrator making the change, so they cannot
     *                     demote or deactivate themselves and lose their own
     *                     access on the next request
     */
    public ApiResponse updateUser(Long id, UpdateUserRequest request, Long actingUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String role = normaliseRole(request.getRole());
        boolean active = request.getIsActive() == null || request.getIsActive();
        boolean isSelf = actingUserId != null && actingUserId.equals(id);

        if (isSelf && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException(
                    "You cannot change your own role - ask another administrator");
        }
        if (isSelf && !active) {
            throw new IllegalArgumentException(
                    "You cannot deactivate your own account");
        }
        if ("ADMIN".equals(user.getRole()) && (!"ADMIN".equals(role) || !active)) {
            guardLastAdministrator();
        }

        userRepository.findByEmail(request.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(
                            "Email already registered: " + request.getEmail());
                });

        userRepository.findByUsername(request.getUsername())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(
                            "Username already taken: " + request.getUsername());
                });

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setIsActive(active);

        // Blank means "leave it alone" - an administrator editing a name should
        // not have to retype someone else's password.
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        return new ApiResponse("User updated successfully", true);
    }

    /**
     * Deactivate a user. The row stays so the audit trail still resolves.
     */
    public ApiResponse deactivateUser(Long id, Long actingUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (actingUserId != null && actingUserId.equals(id)) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }
        if ("ADMIN".equals(user.getRole())) {
            guardLastAdministrator();
        }

        user.setIsActive(false);
        userRepository.save(user);

        return new ApiResponse("User deactivated successfully", true);
    }

    public Optional<UserResponse> getUserResponseById(Long id) {
        return userRepository.findById(id).map(UserResponse::from);
    }

    private String normaliseRole(String role) {
        String normalised = role == null ? "" : role.trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(normalised)) {
            throw new IllegalArgumentException(
                    "Unknown role: " + role + ". Allowed roles are " + ALLOWED_ROLES);
        }
        return normalised;
    }

    /**
     * Removing the last administrator would leave nobody able to manage users,
     * and no way back in through the UI.
     */
    private void guardLastAdministrator() {
        if (userRepository.countByRoleAndIsActiveTrue("ADMIN") <= 1) {
            throw new IllegalArgumentException(
                    "This is the only administrator left - promote another one first");
        }
    }


}
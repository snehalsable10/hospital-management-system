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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
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

    /**
     * The only answer a failed sign-in ever gets, whatever was actually wrong.
     */
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    /** One answer for every refusal, for the same reason as the login message. */
    private static final String INVALID_REFRESH = "Session expired - please sign in again";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;

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
        String email = loginRequest.getEmail();

        // Refuse a locked account before touching the password, so a lockout
        // cannot itself be used to time whether a password was close.
        if (loginAttemptService.isLocked(email)) {
            return new ApiResponse(
                    "Too many failed attempts. Try again in "
                            + loginAttemptService.getLockoutMinutes() + " minutes.", false);
        }

        try {
            Optional<User> userOptional = userRepository.findByEmail(email);

            // One message for every rejection.
            //
            // This used to answer "Email not registered" or "Invalid password"
            // depending on which was wrong, which let anyone read off whether
            // an address had an account - useful for building a target list
            // and for confirming that a leaked address belongs to a patient
            // here. An inactive account is folded in for the same reason.
            if (userOptional.isEmpty()
                    || !userOptional.get().getIsActive()
                    || !passwordEncoder.matches(loginRequest.getPassword(),
                            userOptional.get().getPassword())) {
                loginAttemptService.recordFailure(email);
                return new ApiResponse(INVALID_CREDENTIALS, false);
            }

            User user = userOptional.get();
            loginAttemptService.reset(email);

            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(), user.getUsername(), user.getRole());

            AuthResponse authResponse = AuthResponse.builder()
                    .message("Login successful")
                    .token(token)
                    .refreshToken(refreshToken)
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
            // The reason stays in the log; the caller gets nothing it could
            // use to tell one failure apart from another.
            log.error("Login failed for {}", email, e);
            return new ApiResponse(INVALID_CREDENTIALS, false);
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



    /**
     * Exchange a refresh token for a fresh pair.
     *
     * The old refresh token is revoked as part of the exchange, so each one
     * works exactly once. If a stolen token is used, the legitimate holder's
     * next refresh fails and the theft surfaces, rather than both parties
     * quietly sharing a session for a week.
     *
     * @throws IllegalArgumentException if the token is missing, not a refresh
     *                                  token, expired, or already used
     */
    public ApiResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(INVALID_REFRESH);
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)
                || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException(INVALID_REFRESH);
        }
        // Already spent, or revoked by a logout.
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new IllegalArgumentException(INVALID_REFRESH);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new IllegalArgumentException(INVALID_REFRESH));

        // The caller's role may have changed, or been revoked, since the
        // refresh token was issued; the new access token reflects the account
        // as it is now rather than as it was.
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        String rotated = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername(), user.getRole());

        // Spend the old one. Each refresh token works exactly once.
        tokenBlacklistService.blacklistToken(refreshToken);

        AuthResponse response = AuthResponse.builder()
                .message("Token refreshed")
                .token(token)
                .refreshToken(rotated)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .success(true)
                .build();

        return new ApiResponse("Token refreshed", response, true);
    }

}
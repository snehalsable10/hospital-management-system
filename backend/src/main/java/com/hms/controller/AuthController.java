package com.hms.controller;

import com.hms.audit.AuditService;
import com.hms.dto.request.CreateUserRequest;
import com.hms.dto.request.LoginRequest;
import com.hms.dto.request.RefreshRequest;
import com.hms.dto.request.SignupRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.User;
import com.hms.security.JwtTokenProvider;
import com.hms.service.LoginAttemptService;
import com.hms.service.TokenBlacklistService;
import com.hms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    /**
     * POST /api/auth/signup - Register new user
     * Public endpoint (no authentication required)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest signupRequest,
                                              HttpServletRequest request) {
        ApiResponse response = userService.signup(signupRequest);
        if (response.getSuccess()) {
                        auditService.userRegistered(signupRequest.getEmail(), UserService.DEFAULT_SIGNUP_ROLE, clientIp(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * POST /api/auth/users - Create a user with a specific role
     * ADMIN only. Public signup cannot choose a role, so this is the only way
     * to create doctor, staff or administrator logins.
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody CreateUserRequest createUserRequest,
                                                  HttpServletRequest request) {
        ApiResponse response = userService.createUser(createUserRequest);
        auditService.userRegistered(createUserRequest.getEmail(),
                createUserRequest.getRole(), clientIp(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login - User login (get JWT token)
     * Public endpoint (no authentication required)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                             HttpServletRequest request) {
        String ip = clientIp(request);

        // Read the lock before attempting, so the audit trail distinguishes
        // "wrong password again" from "refused without being checked".
        boolean locked = loginAttemptService.isLocked(loginRequest.getEmail());

        ApiResponse response = userService.login(loginRequest);

        if (response.getSuccess()) {
            auditService.loginSucceeded(loginRequest.getEmail(), ip);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        if (locked) {
            auditService.loginBlocked(loginRequest.getEmail(), ip);
        } else {
            auditService.loginFailed(loginRequest.getEmail(), ip);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    /**
     * POST /api/auth/refresh - Exchange a refresh token for a new pair
     *
     * Public, because the caller's access token has expired by definition -
     * requiring a valid one would defeat the purpose. The refresh token is the
     * credential, and it is checked as one.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            return ResponseEntity.ok(userService.refresh(request.getRefreshToken()));
        } catch (IllegalArgumentException e) {
            // 401, not 400: the client's answer is to sign in again, which is
            // what its interceptor does with a 401.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage(), false));
        }
    }

    /**
     * POST /api/auth/logout - User logout (invalidate token)
     * Authenticated users only
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader,
                                              HttpServletRequest request) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Extract token from header
                String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

                if (token != null) {
                    // Add token to blacklist (logout invalidation)
                    tokenBlacklistService.blacklistToken(token);

                    auditService.logout(currentUser(), clientIp(request));

                    return ResponseEntity.ok(new ApiResponse("Logged out successfully", null, true));
                }
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid authorization header", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error during logout: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/auth/health - Health check endpoint
     * Public endpoint (no authentication required)
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(new ApiResponse("Auth service is running", true));
    }

    /**
     * Behind a proxy (Render) getRemoteAddr() returns the proxy's address, which would
     * make every audit entry point at the load balancer instead of the actual client.
     * X-Forwarded-For holds the original client first in a comma-separated chain.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * JwtAuthenticationFilter sets the numeric user id as the principal. Resolve it to the
     * email so login and logout entries in the audit trail share one identifier and a
     * session can actually be traced end to end.
     */
    private String currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof Long userId) {
            return userService.getUserById(userId)
                    .map(User::getEmail)
                    .orElse(authentication.getName());
        }
        return authentication.getName();
    }
}
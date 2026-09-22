package com.hms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.dto.response.ApiResponse;
import com.hms.security.JwtAuthenticationFilter;
import com.hms.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,      // Enable @PreAuthorize and @PostAuthorize
    securedEnabled = true,      // Enable @Secured
    jsr250Enabled = true        // Enable @RolesAllowed
)
@RequiredArgsConstructor
public class SecurityConfig {

    /** Comma-separated extra origins, for wherever the frontend is deployed. */
    @Value("${hms.security.cors.allowed-origins:}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper;

    /**
     * Configure Spring Security filter chain
     * IMPORTANT: Method-level security is now enabled via @EnableMethodSecurity
     * Use @PreAuthorize("hasRole(...)") on controller methods
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for stateless JWT)
                .csrf(csrf -> csrf.disable())

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Use stateless session (no session cookies needed with JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Without these, an unauthenticated or expired request falls through to
                // Spring's default 403. The client cannot then tell "log in again" from
                // "you are not allowed", so an expired session never triggers a re-login.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))

                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/signup").permitAll()
                        // The caller's access token has expired by definition;
                        // demanding a valid one would defeat the endpoint.
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/health").permitAll()

                        // Health probe must be reachable by the platform's health check.
                        // show-details=when-authorized keeps internals hidden from anonymous callers.
                        // /actuator/metrics is deliberately NOT public - it leaks operational data.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // API docs. Disabled entirely in prod via springdoc.*.enabled=false.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs").permitAll()

                        // All other endpoints require authentication
                        // Role-based authorization is checked at method level using @PreAuthorize
                        .anyRequest().authenticated()
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Ahead of the JWT filter: the endpoints it guards are the
                // unauthenticated ones, so there is no token to inspect and no
                // reason to do that work before turning the request away.
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 401 for anyone who is not authenticated at all (missing, expired or
     * blacklisted token). Distinct from 403, which means authenticated but
     * not permitted.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentication required - please log in again");
    }

    /**
     * 403 for an authenticated caller who lacks permission for this resource.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Access denied - insufficient permissions");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiResponse(message, false));
    }

    /**
     * Password encoder bean (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS configuration
     * Allow requests from React frontend (localhost:3000, localhost:3002)
     */
    @Bean
    /**
     * Which origins the browser may call this API from.
     *
     * The two localhost ports are the development frontend. A deployed
     * frontend lives on a domain nobody knows at build time, so the list is
     * extended from CORS_ALLOWED_ORIGINS - comma-separated - rather than
     * hardcoded. Without that, a deployed UI is blocked by the browser before
     * a single request is sent.
     *
     * Deliberately not "*": allowCredentials is true, and a wildcard origin
     * with credentials is both rejected by browsers and wrong in principle -
     * it would let any site on the internet make authenticated calls on a
     * logged-in user's behalf.
     */
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:3002"
        ));

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .forEach(origins::add);
        }

        configuration.setAllowedOrigins(origins);

        // Allow HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS",
            "PATCH"
        ));

        // Allow headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (JWT token in header)
        configuration.setAllowCredentials(true);

        // Cache CORS configuration for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
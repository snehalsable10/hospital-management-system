package com.hms.config;

import com.hms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,      // Enable @PreAuthorize and @PostAuthorize
    securedEnabled = true,      // Enable @Secured
    jsr250Enabled = true        // Enable @RolesAllowed
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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

                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/signup").permitAll()
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow requests from React frontend
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3002"
        ));

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
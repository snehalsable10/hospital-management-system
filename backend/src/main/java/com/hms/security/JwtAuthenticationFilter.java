package com.hms.security;

import com.hms.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {

        try {
            // Get Authorization header
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Extract token (remove "Bearer " prefix)
                String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

                if (token != null) {
                    // Check if token is blacklisted (logout invalidation)
                    if (tokenBlacklistService.isTokenBlacklisted(token)) {
                        log.warn("Attempt to use blacklisted token");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // A refresh token is long-lived and exists only to obtain
                    // an access token. Accepting one here would put a
                    // seven-day credential in every request header.
                    if (!jwtTokenProvider.isAccessToken(token)) {
                        log.warn("Refresh token presented as an access token");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Validate token (expiration, signature, etc)
                    if (jwtTokenProvider.validateToken(token)) {
                        // Extract user information from token
                        Long userId = jwtTokenProvider.getUserIdFromToken(token);
                        String username = jwtTokenProvider.getUsernameFromToken(token);
                        String role = jwtTokenProvider.getRoleFromToken(token);

                        // Create authority from role (Spring Security format: "ROLE_" prefix)
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                        // Create authentication token
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userId,                           // principal (user identifier)
                                null,                             // credentials (null, already authenticated)
                                Collections.singletonList(authority)  // authorities (roles)
                            );

                        // Set authentication in security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("JWT token validated for user: {}", username);
                    } else {
                        log.warn("JWT token validation failed for request from {}", request.getRemoteAddr());
                    }
                }
            }
        } catch (Exception ex) {
            // Log error but continue (let subsequent filters handle)
            log.error("Error in JWT authentication filter: {}", ex.getMessage(), ex);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
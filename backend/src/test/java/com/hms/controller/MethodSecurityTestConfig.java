package com.hms.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns @PreAuthorize back on for the slice tests.
 *
 * @WebMvcTest builds only the web layer, and the real SecurityConfig - which
 * is where @EnableMethodSecurity lives - is not part of it. Without this the
 * annotations would be inert and every one of these tests would pass while
 * proving nothing.
 *
 * The full SecurityConfig is deliberately not imported: it drags in the JWT
 * and rate-limit filters and their Redis-backed dependencies, none of which
 * these tests are about. Authentication is supplied directly by @WithMockUser;
 * what is under test is the authorisation rule on each method.
 */
@TestConfiguration
@EnableMethodSecurity
public class MethodSecurityTestConfig {
}

package com.hms.controller;

import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.DashboardSummary;
import com.hms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * One call for the whole dashboard.
 *
 * Open to every authenticated role: the service decides which sections to fill
 * from the caller's role, so a doctor gets patient and room figures but no
 * revenue, exactly as the list endpoints would answer.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Dashboard", description = "Aggregated figures for the landing page")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dashboard summary",
            description = "Totals, breakdowns and recent appointments, scoped to what the caller's role may see")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse> getSummary() {
        DashboardSummary summary = dashboardService.getSummary(currentRole());
        return ResponseEntity.ok(new ApiResponse("Dashboard summary retrieved successfully", summary, true));
    }

    /**
     * The caller's role, without the ROLE_ prefix the filter adds for Spring
     * Security's own matching.
     */
    private String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }
}

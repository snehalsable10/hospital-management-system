package com.hms.controller;

import com.hms.dto.request.DepartmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Department;
import com.hms.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Department Management", description = "APIs for managing hospital departments and their information")
@SecurityRequirement(name = "Bearer Authentication")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * GET /api/departments - List all departments
     * All authenticated users can view (public info)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get all departments", description = "Retrieve a list of all hospital departments")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Departments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(new ApiResponse("Departments retrieved successfully", departments, true));
    }

    /**
     * GET /api/departments/{id} - Get a specific department
     * All authenticated users can view (public info)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get department by ID", description = "Retrieve detailed information about a specific hospital department")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getDepartmentById(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id) {
        Optional<Department> department = departmentService.getDepartmentById(id);

        if (!department.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Department not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Department retrieved successfully", department.get(), true));
    }

    /**
     * POST /api/departments - Create a new department
     * ADMIN only (controlled department creation)
     *
     * Duplicate name -> DuplicateResourceException -> 409
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new department", description = "Create a new hospital department with name, description, and contact information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Department created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid department data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can create"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Department name already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createDepartment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Department data", required = true)
            @Valid @RequestBody DepartmentRequest request) {
        ApiResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/departments/{id} - Update a department
     * ADMIN only (controlled updates)
     *
     * Unknown id     -> ResourceNotFoundException  -> 404
     * Duplicate name -> DuplicateResourceException -> 409
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update department information", description = "Update existing department details (name, description, phone, status)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Department updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid department data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can update"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Department name already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateDepartment(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated department data", required = true)
            @Valid @RequestBody DepartmentRequest request) {
        ApiResponse response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/departments/{id} - Delete a department
     * ADMIN only
     *
     * Unknown id -> ResourceNotFoundException -> 404
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a department", description = "Soft delete a department (marks as inactive, data is retained)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Department deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can delete"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteDepartment(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = departmentService.deleteDepartment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/departments/paginated - Get all departments with pagination
     *
     * Invalid page params -> IllegalArgumentException -> 400 via GlobalExceptionHandler
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get all departments paginated", description = "Retrieve departments with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Departments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllDepartmentsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Department> response = departmentService.getAllDepartmentsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Departments retrieved successfully", response, true));
    }
}
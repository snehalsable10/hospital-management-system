package com.hms.controller;

import com.hms.dto.request.DoctorRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Doctor;
import com.hms.service.DoctorService;
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
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Doctor Management", description = "APIs for managing doctor profiles, specializations, and departments")
@SecurityRequirement(name = "Bearer Authentication")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * GET /api/doctors - List all doctors
     * ADMIN, STAFF, DOCTOR can view all doctors
     * PATIENT can also view (public info for appointment booking)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get all doctors", description = "Retrieve a list of all registered doctors in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", doctors, true));
    }

    /**
     * GET /api/doctors/{id} - Get a specific doctor
     * ADMIN, STAFF, DOCTOR can view any doctor
     * PATIENT can view any doctor (public info)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get doctor by ID", description = "Retrieve detailed information about a specific doctor")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctor retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Doctor not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getDoctorById(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long id) {
        Optional<Doctor> doctor = doctorService.getDoctorById(id);

        if (!doctor.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Doctor not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Doctor retrieved successfully", doctor.get(), true));
    }

    /**
     * GET /api/doctors/department/{departmentId} - Get all doctors in a department
     * ADMIN, STAFF, DOCTOR can view
     * PATIENT can view (for appointment booking)
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get doctors by department", description = "Retrieve all doctors working in a specific department")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getDoctorsByDepartment(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long departmentId) {
        List<Doctor> doctors = doctorService.getDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", doctors, true));
    }

    /**
     * GET /api/doctors/specialization/{specialization} - Get doctors by specialization
     * ADMIN, STAFF, DOCTOR can view
     * PATIENT can view (for appointment booking)
     */
    @GetMapping("/specialization/{specialization}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get doctors by specialization", description = "Retrieve all doctors with a specific medical specialization")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getDoctorsBySpecialization(
            @Parameter(description = "Specialization (e.g., Cardiology, Pediatrics)", required = true)
            @PathVariable String specialization) {
        List<Doctor> doctors = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", doctors, true));
    }

    /**
     * POST /api/doctors - Create a new doctor
     * ADMIN only (controlled doctor registration)
     *
     * Duplicate email/license -> DuplicateResourceException -> 409
     * Unknown departmentId    -> IllegalArgumentException   -> 400
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new doctor", description = "Register a new doctor in the system with credentials and specialization")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Doctor created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid doctor data or unknown department"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can create"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Email or license number already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createDoctor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Doctor data", required = true)
            @Valid @RequestBody DoctorRequest request) {
        ApiResponse response = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/doctors/{id} - Update a doctor
     * ADMIN can update any doctor
     * DOCTOR can update their own profile
     *
     * Unknown doctor id       -> ResourceNotFoundException  -> 404
     * Duplicate email/license -> DuplicateResourceException -> 409
     * Unknown departmentId    -> IllegalArgumentException   -> 400
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isOwnDoctor(#id)")
    @Operation(summary = "Update doctor information", description = "Update existing doctor details (ADMIN can update any doctor, doctors can update their own)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctor updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid doctor data or unknown department"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot update other doctor's data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Doctor not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Email or license number already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateDoctor(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated doctor data", required = true)
            @Valid @RequestBody DoctorRequest request) {
        ApiResponse response = doctorService.updateDoctor(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/doctors/{id} - Delete a doctor
     * ADMIN only
     *
     * Unknown id -> ResourceNotFoundException -> 404
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a doctor", description = "Soft delete a doctor (marks as inactive, data is retained)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctor deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can delete"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Doctor not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteDoctor(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = doctorService.deleteDoctor(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/doctors/paginated - Get all doctors with pagination
     * Optimized for large datasets
     *
     * Invalid page params -> IllegalArgumentException -> 400 via GlobalExceptionHandler
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get all doctors paginated", description = "Retrieve doctors with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllDoctorsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Doctor> response = doctorService.getAllDoctorsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", response, true));
    }

    /**
     * GET /api/doctors/department/{departmentId}/paginated - Get doctors by department with pagination
     */
    @GetMapping("/department/{departmentId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get doctors by department paginated", description = "Retrieve department doctors with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getDoctorsByDepartmentPaginated(
            @Parameter(description = "Department ID", required = true)
            @PathVariable Long departmentId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Doctor> response = doctorService.getDoctorsByDepartmentPaginated(departmentId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", response, true));
    }

    /**
     * GET /api/doctors/specialization/{specialization}/paginated - Get doctors by specialization with pagination
     */
    @GetMapping("/specialization/{specialization}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get doctors by specialization paginated", description = "Retrieve specialized doctors with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getDoctorsBySpecializationPaginated(
            @Parameter(description = "Specialization", required = true)
            @PathVariable String specialization,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Doctor> response = doctorService.getDoctorsBySpecializationPaginated(specialization, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", response, true));
    }
}
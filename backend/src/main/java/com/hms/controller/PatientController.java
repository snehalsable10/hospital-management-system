package com.hms.controller;

import com.hms.dto.request.PatientRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Patient;
import com.hms.service.PatientService;
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
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Patient Management", description = "APIs for managing patient information and records")
@SecurityRequirement(name = "Bearer Authentication")
public class PatientController {

    private final PatientService patientService;

    /**
     * GET /api/patients - List all patients
     * ADMIN, DOCTOR, STAFF can view all patients
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Get all patients", description = "Retrieve a list of all registered patients in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patients retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllPatients() {
        List<Patient> patients = patientService.getAllPatients();
        return ResponseEntity.ok(new ApiResponse("Patients retrieved successfully", patients, true));
    }

    /**
     * GET /api/patients/{id} - Get a specific patient
     * ADMIN, DOCTOR, STAFF can view any patient
     * PATIENT can only view their own data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF') or @authService.isOwnPatient(#id)")
    @Operation(summary = "Get patient by ID", description = "Retrieve detailed information about a specific patient")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patient retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot view other patient's data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patient not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPatientById(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long id) {
        Optional<Patient> patient = patientService.getPatientById(id);

        if (!patient.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Patient not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Patient retrieved successfully", patient.get(), true));
    }

    /**
     * GET /api/patients/search/firstname/{firstName} - Search by first name
     * ADMIN, DOCTOR, STAFF only
     */
    @GetMapping("/search/firstname/{firstName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Search patients by first name", description = "Search for patients using their first name (partial match supported)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByFirstName(
            @Parameter(description = "First name to search for", required = true)
            @PathVariable String firstName) {
        List<Patient> patients = patientService.searchByFirstName(firstName);
        return ResponseEntity.ok(new ApiResponse("Search results", patients, true));
    }

    /**
     * GET /api/patients/search/lastname/{lastName} - Search by last name
     * ADMIN, DOCTOR, STAFF only
     */
    @GetMapping("/search/lastname/{lastName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Search patients by last name", description = "Search for patients using their last name (partial match supported)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByLastName(
            @Parameter(description = "Last name to search for", required = true)
            @PathVariable String lastName) {
        List<Patient> patients = patientService.searchByLastName(lastName);
        return ResponseEntity.ok(new ApiResponse("Search results", patients, true));
    }

    /**
     * GET /api/patients/search/city/{city} - Search by city
     * ADMIN, DOCTOR, STAFF only
     */
    @GetMapping("/search/city/{city}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Search patients by city", description = "Search for patients by their city of residence")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByCity(
            @Parameter(description = "City to search for", required = true)
            @PathVariable String city) {
        List<Patient> patients = patientService.searchByCity(city);
        return ResponseEntity.ok(new ApiResponse("Search results", patients, true));
    }

    /**
     * POST /api/patients - Create a new patient
     * ADMIN, STAFF can create patients
     *
     * A duplicate email throws DuplicateResourceException -> 409 via GlobalExceptionHandler
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new patient", description = "Register a new patient in the system with personal and medical information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Patient created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid patient data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Email already registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createPatient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Patient data", required = true)
            @Valid @RequestBody PatientRequest request) {
        ApiResponse response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/patients/{id} - Update a patient
     * ADMIN, STAFF can update any patient
     * PATIENT can only update their own profile
     *
     * Unknown id throws ResourceNotFoundException -> 404
     * Duplicate email throws DuplicateResourceException -> 409
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#id)")
    @Operation(summary = "Update patient information", description = "Update existing patient details (ADMIN/STAFF can update any patient, patients can only update their own)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patient updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid patient data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot update other patient's data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patient not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Email already registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updatePatient(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated patient data", required = true)
            @Valid @RequestBody PatientRequest request) {
        ApiResponse response = patientService.updatePatient(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/patients/{id} - Delete a patient
     * ADMIN only
     *
     * Unknown id throws ResourceNotFoundException -> 404
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a patient", description = "Soft delete a patient (marks as inactive, data is retained)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patient deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can delete"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patient not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deletePatient(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = patientService.deletePatient(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/patients/paginated - List all patients with pagination
     * Optimized for large datasets - reduces database and memory load
     *
     * Invalid page params throw IllegalArgumentException -> 400 via GlobalExceptionHandler
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Get patients with pagination", description = "Retrieve patients in pages (10 per page default) - optimized for large datasets")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patients retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid page parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllPatientsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (1-100, default 10)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Patient> response = patientService.getAllPatientsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patients retrieved successfully", response, true));
    }

    /**
     * GET /api/patients/search/firstname/{firstName}/paginated - Search by first name with pagination
     */
    @GetMapping("/search/firstname/{firstName}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Search patients by first name (paginated)", description = "Search for patients using first name with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid page parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByFirstNamePaginated(
            @Parameter(description = "First name to search for", required = true)
            @PathVariable String firstName,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (1-100, default 10)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Patient> response = patientService.searchByFirstNamePaginated(firstName, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved", response, true));
    }

    /**
     * GET /api/patients/search/lastname/{lastName}/paginated - Search by last name with pagination
     */
    @GetMapping("/search/lastname/{lastName}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Search patients by last name (paginated)", description = "Search for patients using last name with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid page parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByLastNamePaginated(
            @Parameter(description = "Last name to search for", required = true)
            @PathVariable String lastName,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (1-100, default 10)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Patient> response = patientService.searchByLastNamePaginated(lastName, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved", response, true));
    }

    /**
     * GET /api/patients/search/city/{city}/paginated - Search by city with pagination
     */
    @GetMapping("/search/city/{city}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    @Operation(summary = "Search patients by city (paginated)", description = "Search for patients by city with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid page parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByCityPaginated(
            @Parameter(description = "City to search for", required = true)
            @PathVariable String city,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (1-100, default 10)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Patient> response = patientService.searchByCityPaginated(city, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved", response, true));
    }
}
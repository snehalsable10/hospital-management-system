package com.hms.controller;

import com.hms.dto.request.LaboratoryTestRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.LaboratoryTest;
import com.hms.service.LaboratoryTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/laboratory-tests")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Laboratory Test Management", description = "APIs for managing patient laboratory tests and results")
@SecurityRequirement(name = "Bearer Authentication")
public class LaboratoryTestController {

    private final LaboratoryTestService laboratoryTestService;

    /**
     * GET /api/laboratory-tests - List all laboratory tests
     * ADMIN, STAFF only
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all laboratory tests", description = "Retrieve a list of all laboratory tests in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN/STAFF can view all"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllLaboratoryTests() {
        List<LaboratoryTest> tests = laboratoryTestService.getAllLaboratoryTests();
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved successfully", tests, true));
    }

    /**
     * GET /api/laboratory-tests/{id} - Get a specific laboratory test
     * ADMIN, STAFF, Doctor, or patient
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR') or @authService.isLaboratoryTestOwner(#id)")
    @Operation(summary = "Get laboratory test by ID", description = "Retrieve detailed information about a specific laboratory test and results")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory test retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Laboratory test not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestById(
            @Parameter(description = "Laboratory Test ID", required = true)
            @PathVariable Long id) {
        Optional<LaboratoryTest> test = laboratoryTestService.getLaboratoryTestById(id);

        if (!test.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Laboratory test not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Laboratory test retrieved successfully", test.get(), true));
    }

    /**
     * GET /api/laboratory-tests/patient/{patientId} - Get tests for a patient
     * ADMIN, STAFF, Doctor or the patient themselves
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's laboratory tests", description = "Retrieve all laboratory tests conducted for a specific patient")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestsByPatient(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId) {
        List<LaboratoryTest> tests = laboratoryTestService.getLaboratoryTestsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Patient laboratory tests retrieved successfully", tests, true));
    }

    /**
     * GET /api/laboratory-tests/status/{status} - Get tests by status
     * ADMIN, STAFF only
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get laboratory tests by status", description = "Retrieve laboratory tests filtered by result status (NORMAL, ABNORMAL, PENDING, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestsByStatus(
            @Parameter(description = "Status (NORMAL, ABNORMAL, PENDING)", required = true)
            @PathVariable String status) {
        List<LaboratoryTest> tests = laboratoryTestService.getLaboratoryTestsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved by status successfully", tests, true));
    }

    /**
     * GET /api/laboratory-tests/search/{testName} - Search by test name
     * ADMIN, STAFF only
     */
    @GetMapping("/search/{testName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Search laboratory tests by name", description = "Search for laboratory tests by test name (partial match supported)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByTestName(
            @Parameter(description = "Test name to search for", required = true)
            @PathVariable String testName) {
        List<LaboratoryTest> tests = laboratoryTestService.searchByTestName(testName);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved successfully", tests, true));
    }

    /**
     * GET /api/laboratory-tests/daterange - Get tests in date range
     * ADMIN, STAFF only
     */
    @GetMapping("/daterange")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get laboratory tests by date range", description = "Retrieve laboratory tests conducted within a specific date range")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestsByDateRange(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<LaboratoryTest> tests = laboratoryTestService.getLaboratoryTestsByDateRange(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved by date range successfully", tests, true));
    }

    /**
     * POST /api/laboratory-tests - Create a new laboratory test
     * ADMIN, STAFF only
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new laboratory test", description = "Record a new laboratory test for a patient with test results")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Laboratory test created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid laboratory test data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createLaboratoryTest(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Laboratory test data", required = true)
            @Valid @RequestBody LaboratoryTestRequest request) {
        ApiResponse response = laboratoryTestService.createLaboratoryTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/laboratory-tests/{id} - Update a laboratory test
     * ADMIN, STAFF only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update laboratory test", description = "Update laboratory test results and status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory test updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid laboratory test data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Laboratory test not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateLaboratoryTest(
            @Parameter(description = "Laboratory Test ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated laboratory test data", required = true)
            @Valid @RequestBody LaboratoryTestRequest request) {

        ApiResponse response = laboratoryTestService.updateLaboratoryTest(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/laboratory-tests/{id} - Delete a laboratory test
     * ADMIN, STAFF only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete a laboratory test", description = "Soft delete a laboratory test record (marks as inactive)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory test deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Laboratory test not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteLaboratoryTest(
            @Parameter(description = "Laboratory Test ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = laboratoryTestService.deleteLaboratoryTest(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/laboratory-tests/paginated - Get all laboratory tests with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all laboratory tests paginated", description = "Retrieve laboratory tests with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllLaboratoryTestsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<LaboratoryTest> response = laboratoryTestService.getAllLaboratoryTestsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved successfully", response, true));
    }

    /**
     * GET /api/laboratory-tests/patient/{patientId}/paginated - Get patient tests with pagination
     */
    @GetMapping("/patient/{patientId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's laboratory tests paginated", description = "Retrieve patient laboratory tests with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestsByPatientPaginated(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<LaboratoryTest> response = laboratoryTestService.getLaboratoryTestsByPatientPaginated(patientId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patient laboratory tests retrieved successfully", response, true));
    }

    /**
     * GET /api/laboratory-tests/status/{status}/paginated - Get tests by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get laboratory tests by status paginated", description = "Retrieve laboratory tests filtered by status with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestsByStatusPaginated(
            @Parameter(description = "Status", required = true)
            @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<LaboratoryTest> response = laboratoryTestService.getLaboratoryTestsByStatusPaginated(status, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved by status successfully", response, true));
    }

    /**
     * GET /api/laboratory-tests/search/{testName}/paginated - Search by test name with pagination
     */
    @GetMapping("/search/{testName}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Search laboratory tests by name paginated", description = "Search for laboratory tests by test name with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByTestNamePaginated(
            @Parameter(description = "Test name to search for", required = true)
            @PathVariable String testName,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<LaboratoryTest> response = laboratoryTestService.searchByTestNamePaginated(testName, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved successfully", response, true));
    }

    /**
     * GET /api/laboratory-tests/daterange/paginated - Get tests by date range with pagination
     */
    @GetMapping("/daterange/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get laboratory tests by date range paginated", description = "Retrieve laboratory tests within date range with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Laboratory tests retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getLaboratoryTestsByDateRangePaginated(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<LaboratoryTest> response = laboratoryTestService.getLaboratoryTestsByDateRangePaginated(startDate, endDate, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved by date range successfully", response, true));
    }
}
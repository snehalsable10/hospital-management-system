package com.hms.controller;

import com.hms.dto.request.MedicalHistoryRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.MedicalHistory;
import com.hms.service.MedicalHistoryService;
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
@RequestMapping("/api/medical-histories")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Medical History Management", description = "APIs for managing patient medical history and past medical conditions")
@SecurityRequirement(name = "Bearer Authentication")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    /**
     * GET /api/medical-histories - List all medical histories
     * ADMIN, STAFF only
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all medical histories", description = "Retrieve a list of all medical history records in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical histories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN/STAFF can view all"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllMedicalHistories() {
        List<MedicalHistory> histories = medicalHistoryService.getAllMedicalHistories();
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved successfully", histories, true));
    }

    /**
     * GET /api/medical-histories/{id} - Get a specific medical history
     * ADMIN, STAFF, Doctor, or patient
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR') or @authService.isMedicalHistoryOwner(#id)")
    @Operation(summary = "Get medical history by ID", description = "Retrieve detailed information about a specific medical history record")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical history retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Medical history not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getMedicalHistoryById(
            @Parameter(description = "Medical History ID", required = true)
            @PathVariable Long id) {
        Optional<MedicalHistory> history = medicalHistoryService.getMedicalHistoryById(id);

        if (!history.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Medical history not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Medical history retrieved successfully", history.get(), true));
    }

    /**
     * GET /api/medical-histories/patient/{patientId} - Get medical histories for a patient
     * ADMIN, STAFF, Doctor or the patient themselves
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's medical histories", description = "Retrieve all past medical conditions and history for a specific patient")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical histories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getMedicalHistoriesByPatient(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId) {
        List<MedicalHistory> histories = medicalHistoryService.getMedicalHistoriesByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Patient medical histories retrieved successfully", histories, true));
    }

    /**
     * GET /api/medical-histories/status/{status} - Get medical histories by status
     * ADMIN, STAFF only
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get medical histories by status", description = "Retrieve medical histories filtered by status (ACTIVE, RESOLVED, ONGOING, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical histories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getMedicalHistoriesByStatus(
            @Parameter(description = "Status (ACTIVE, RESOLVED, ONGOING)", required = true)
            @PathVariable String status) {
        List<MedicalHistory> histories = medicalHistoryService.getMedicalHistoriesByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved by status successfully", histories, true));
    }

    /**
     * GET /api/medical-histories/search/{conditionName} - Search by condition name
     * ADMIN, STAFF only
     */
    @GetMapping("/search/{conditionName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Search medical histories by condition", description = "Search for medical histories by condition name (partial match supported)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByConditionName(
            @Parameter(description = "Condition name to search for", required = true)
            @PathVariable String conditionName) {
        List<MedicalHistory> histories = medicalHistoryService.searchByConditionName(conditionName);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved successfully", histories, true));
    }

    /**
     * POST /api/medical-histories - Create a new medical history entry
     * ADMIN, STAFF, DOCTOR only
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    @Operation(summary = "Create a new medical history entry", description = "Record a new past medical condition or history for a patient")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Medical history created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid medical history data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createMedicalHistory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Medical history data", required = true)
            @Valid @RequestBody MedicalHistoryRequest request) {
        ApiResponse response = medicalHistoryService.createMedicalHistory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/medical-histories/{id} - Update a medical history entry
     * ADMIN, STAFF, DOCTOR
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    @Operation(summary = "Update medical history entry", description = "Update existing medical history details (condition, treatment, status, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical history updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid medical history data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Medical history not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateMedicalHistory(
            @Parameter(description = "Medical History ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated medical history data", required = true)
            @Valid @RequestBody MedicalHistoryRequest request) {

        ApiResponse response = medicalHistoryService.updateMedicalHistory(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/medical-histories/{id} - Delete a medical history entry
     * ADMIN only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a medical history entry", description = "Soft delete a medical history record (marks as inactive)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical history deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can delete"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Medical history not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteMedicalHistory(
            @Parameter(description = "Medical History ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = medicalHistoryService.deleteMedicalHistory(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/medical-histories/paginated - Get all medical histories with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all medical histories paginated", description = "Retrieve medical histories with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical histories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllMedicalHistoriesPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<MedicalHistory> response = medicalHistoryService.getAllMedicalHistoriesPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved successfully", response, true));
    }

    /**
     * GET /api/medical-histories/patient/{patientId}/paginated - Get patient medical histories with pagination
     */
    @GetMapping("/patient/{patientId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's medical histories paginated", description = "Retrieve patient medical histories with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical histories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getMedicalHistoriesByPatientPaginated(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<MedicalHistory> response = medicalHistoryService.getMedicalHistoriesByPatientPaginated(patientId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patient medical histories retrieved successfully", response, true));
    }

    /**
     * GET /api/medical-histories/status/{status}/paginated - Get medical histories by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get medical histories by status paginated", description = "Retrieve medical histories filtered by status with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Medical histories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getMedicalHistoriesByStatusPaginated(
            @Parameter(description = "Status", required = true)
            @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<MedicalHistory> response = medicalHistoryService.getMedicalHistoriesByStatusPaginated(status, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved by status successfully", response, true));
    }

    /**
     * GET /api/medical-histories/search/{conditionName}/paginated - Search by condition name with pagination
     */
    @GetMapping("/search/{conditionName}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Search medical histories by condition paginated", description = "Search for medical histories by condition name with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> searchByConditionNamePaginated(
            @Parameter(description = "Condition name to search for", required = true)
            @PathVariable String conditionName,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<MedicalHistory> response = medicalHistoryService.searchByConditionNamePaginated(conditionName, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Search results retrieved successfully", response, true));
    }
}
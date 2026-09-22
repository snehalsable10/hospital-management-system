package com.hms.controller;

import com.hms.dto.request.PrescriptionRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PrescriptionResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Prescription;
import com.hms.service.PrescriptionService;
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
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Prescription Management", description = "APIs for managing patient prescriptions and medications")
@SecurityRequirement(name = "Bearer Authentication")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * GET /api/prescriptions - List all prescriptions
     * ADMIN, STAFF only (sensitive medical data)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all prescriptions", description = "Retrieve a list of all prescriptions in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN/STAFF can view all"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", PrescriptionResponse.from(prescriptions), true));
    }

    /**
     * GET /api/prescriptions/{id} - Get a specific prescription
     * ADMIN, STAFF, Doctor who created it, or patient
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isPrescriptionOwner(#id)")
    @Operation(summary = "Get prescription by ID", description = "Retrieve detailed information about a specific prescription")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescription retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Prescription not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionById(
            @Parameter(description = "Prescription ID", required = true)
            @PathVariable Long id) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(id);

        if (!prescription.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Prescription not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Prescription retrieved successfully", PrescriptionResponse.from(prescription.get()), true));
    }

    /**
     * GET /api/prescriptions/appointment/{appointmentId} - Get prescriptions for an appointment
     */
    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get prescriptions by appointment", description = "Retrieve all prescriptions issued for a specific appointment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable Long appointmentId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByAppointment(appointmentId);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", PrescriptionResponse.from(prescriptions), true));
    }

    /**
     * GET /api/prescriptions/patient/{patientId} - Get prescriptions for a patient
     * ADMIN, STAFF or the patient themselves
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's prescriptions", description = "Retrieve all prescriptions issued to a specific patient")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByPatient(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Patient prescriptions retrieved successfully", PrescriptionResponse.from(prescriptions), true));
    }

    /**
     * GET /api/prescriptions/doctor/{doctorId} - Get prescriptions written by a doctor
     * ADMIN, STAFF or the doctor themselves
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    @Operation(summary = "Get doctor's prescriptions", description = "Retrieve all prescriptions written by a specific doctor")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByDoctor(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long doctorId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse("Doctor prescriptions retrieved successfully", PrescriptionResponse.from(prescriptions), true));
    }

    /**
     * GET /api/prescriptions/status/{status} - Get prescriptions by status
     * ADMIN, STAFF only
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get prescriptions by status", description = "Retrieve prescriptions filtered by status (ACTIVE, EXPIRED, COMPLETED, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByStatus(
            @Parameter(description = "Status (ACTIVE, EXPIRED, COMPLETED)", required = true)
            @PathVariable String status) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved by status successfully", PrescriptionResponse.from(prescriptions), true));
    }

    /**
     * POST /api/prescriptions - Create a new prescription
     * ADMIN, STAFF, DOCTOR only
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    @Operation(summary = "Create a new prescription", description = "Issue a new prescription for a patient with medication details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Prescription created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid prescription data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createPrescription(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Prescription data", required = true)
            @Valid @RequestBody PrescriptionRequest request) {
        ApiResponse response = prescriptionService.createPrescription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/prescriptions/{id} - Update a prescription
     * ADMIN, STAFF, DOCTOR who created it
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isPrescriptionOwner(#id)")
    @Operation(summary = "Update prescription", description = "Update existing prescription details (medication, dosage, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescription updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid prescription data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Prescription not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updatePrescription(
            @Parameter(description = "Prescription ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated prescription data", required = true)
            @Valid @RequestBody PrescriptionRequest request) {

        ApiResponse response = prescriptionService.updatePrescription(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/prescriptions/{id} - Delete a prescription
     * ADMIN only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a prescription", description = "Soft delete a prescription (marks as inactive)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescription deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can delete"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Prescription not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deletePrescription(
            @Parameter(description = "Prescription ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = prescriptionService.deletePrescription(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/prescriptions/paginated - Get all prescriptions with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all prescriptions paginated", description = "Retrieve prescriptions with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllPrescriptionsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Prescription> response = prescriptionService.getAllPrescriptionsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", PrescriptionResponse.from(response), true));
    }

    /**
     * GET /api/prescriptions/appointment/{appointmentId}/paginated - Get appointment prescriptions with pagination
     */
    @GetMapping("/appointment/{appointmentId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get appointment prescriptions paginated", description = "Retrieve appointment prescriptions with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByAppointmentPaginated(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable Long appointmentId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Prescription> response = prescriptionService.getPrescriptionsByAppointmentPaginated(appointmentId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", PrescriptionResponse.from(response), true));
    }

    /**
     * GET /api/prescriptions/patient/{patientId}/paginated - Get patient prescriptions with pagination
     */
    @GetMapping("/patient/{patientId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's prescriptions paginated", description = "Retrieve patient prescriptions with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByPatientPaginated(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Prescription> response = prescriptionService.getPrescriptionsByPatientPaginated(patientId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patient prescriptions retrieved successfully", PrescriptionResponse.from(response), true));
    }

    /**
     * GET /api/prescriptions/doctor/{doctorId}/paginated - Get doctor prescriptions with pagination
     */
    @GetMapping("/doctor/{doctorId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    @Operation(summary = "Get doctor's prescriptions paginated", description = "Retrieve doctor prescriptions with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByDoctorPaginated(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long doctorId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Prescription> response = prescriptionService.getPrescriptionsByDoctorPaginated(doctorId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Doctor prescriptions retrieved successfully", PrescriptionResponse.from(response), true));
    }

    /**
     * GET /api/prescriptions/status/{status}/paginated - Get prescriptions by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get prescriptions by status paginated", description = "Retrieve prescriptions filtered by status with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prescriptions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPrescriptionsByStatusPaginated(
            @Parameter(description = "Status", required = true)
            @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Prescription> response = prescriptionService.getPrescriptionsByStatusPaginated(status, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved by status successfully", PrescriptionResponse.from(response), true));
    }
}
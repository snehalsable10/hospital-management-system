package com.hms.controller;

import com.hms.dto.request.AppointmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Appointment;
import com.hms.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Appointment Management", description = "APIs for managing patient appointments with doctors")
@SecurityRequirement(name = "Bearer Authentication")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * GET /api/appointments - List all appointments
     * ADMIN, STAFF only
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all appointments", description = "Retrieve a list of all scheduled appointments in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN/STAFF can view all"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllAppointments() {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/{id} - Get a specific appointment
     * ADMIN, STAFF can view any appointment
     * Patient/Doctor can view if they are a participant
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isAppointmentParticipant(#id)")
    @Operation(summary = "Get appointment by ID", description = "Retrieve detailed information about a specific appointment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointment retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not appointment participant"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Appointment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentById(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable Long id) {
        Optional<Appointment> appointment = appointmentService.getAppointmentById(id);

        if (!appointment.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Appointment not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Appointment retrieved successfully", appointment.get(), true));
    }

    /**
     * GET /api/appointments/patient/{patientId} - Get appointments for a patient
     * ADMIN, STAFF or the patient themselves
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's appointments", description = "Retrieve all appointments for a specific patient")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByPatient(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Patient appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/doctor/{doctorId} - Get appointments for a doctor
     * ADMIN, STAFF or the doctor themselves
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    @Operation(summary = "Get doctor's appointments", description = "Retrieve all appointments for a specific doctor")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByDoctor(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long doctorId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse("Doctor appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/status/{status} - Get appointments by status
     * ADMIN, STAFF only
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get appointments by status", description = "Retrieve appointments filtered by status (SCHEDULED, COMPLETED, CANCELLED, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByStatus(
            @Parameter(description = "Status (SCHEDULED, COMPLETED, CANCELLED)", required = true)
            @PathVariable String status) {
        List<Appointment> appointments = appointmentService.getAppointmentsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved by status successfully", appointments, true));
    }

    /**
     * GET /api/appointments/daterange - Get appointments in a date range
     * ADMIN, STAFF only
     */
    @GetMapping("/daterange")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get appointments by date range", description = "Retrieve appointments scheduled within a specific date range")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByDateRange(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Appointment> appointments = appointmentService.getAppointmentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved by date range successfully", appointments, true));
    }

    /**
     * POST /api/appointments - Create a new appointment
     * ADMIN, STAFF, PATIENT, DOCTOR can create
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'PATIENT', 'DOCTOR')")
    @Operation(summary = "Create a new appointment", description = "Schedule a new appointment between a patient and doctor")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Appointment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid appointment data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createAppointment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Appointment data", required = true)
            @Valid @RequestBody AppointmentRequest request) {
        ApiResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/appointments/{id} - Update an appointment
     * ADMIN, STAFF can update any appointment
     * Participant can update their own appointment
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isAppointmentParticipant(#id)")
    @Operation(summary = "Update appointment", description = "Update existing appointment details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointment updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid appointment data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Appointment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated appointment data", required = true)
            @Valid @RequestBody AppointmentRequest request) {

        ApiResponse response = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/appointments/{id} - Delete an appointment
     * ADMIN, STAFF only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete an appointment", description = "Soft delete an appointment (marks as inactive)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointment deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Appointment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = appointmentService.deleteAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/appointments/paginated - Get all appointments with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all appointments paginated", description = "Retrieve appointments with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllAppointmentsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Appointment> response = appointmentService.getAllAppointmentsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", response, true));
    }

    /**
     * GET /api/appointments/patient/{patientId}/paginated - Get patient appointments with pagination
     */
    @GetMapping("/patient/{patientId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's appointments paginated", description = "Retrieve patient appointments with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByPatientPaginated(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Appointment> response = appointmentService.getAppointmentsByPatientPaginated(patientId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patient appointments retrieved successfully", response, true));
    }

    /**
     * GET /api/appointments/doctor/{doctorId}/paginated - Get doctor appointments with pagination
     */
    @GetMapping("/doctor/{doctorId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    @Operation(summary = "Get doctor's appointments paginated", description = "Retrieve doctor appointments with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByDoctorPaginated(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long doctorId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Appointment> response = appointmentService.getAppointmentsByDoctorPaginated(doctorId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Doctor appointments retrieved successfully", response, true));
    }

    /**
     * GET /api/appointments/status/{status}/paginated - Get appointments by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get appointments by status paginated", description = "Retrieve appointments filtered by status with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByStatusPaginated(
            @Parameter(description = "Status", required = true)
            @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Appointment> response = appointmentService.getAppointmentsByStatusPaginated(status, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved by status successfully", response, true));
    }

    /**
     * GET /api/appointments/daterange/paginated - Get appointments by date range with pagination
     */
    @GetMapping("/daterange/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get appointments by date range paginated", description = "Retrieve appointments within date range with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAppointmentsByDateRangePaginated(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Appointment> response = appointmentService.getAppointmentsByDateRangePaginated(startDate, endDate, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved by date range successfully", response, true));
    }
}
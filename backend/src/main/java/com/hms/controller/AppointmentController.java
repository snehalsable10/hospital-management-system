package com.hms.controller;

import com.hms.dto.request.AppointmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Appointment;
import com.hms.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * GET /api/appointments - List all appointments
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllAppointments() {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/{id} - Get a specific appointment
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getAppointmentById(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentService.getAppointmentById(id);

        if (!appointment.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Appointment not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Appointment retrieved successfully", appointment.get(), true));
    }

    /**
     * GET /api/appointments/patient/{patientId} - Get all appointments for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse> getAppointmentsByPatient(@PathVariable Long patientId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/doctor/{doctorId} - Get all appointments for a doctor
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/status/{status} - Get appointments by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getAppointmentsByStatus(@PathVariable String status) {
        List<Appointment> appointments = appointmentService.getAppointmentsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", appointments, true));
    }

    /**
     * GET /api/appointments/daterange?startDate=2024-01-01&endDate=2024-01-31 - Get appointments in date range
     */
    @GetMapping("/daterange")
    public ResponseEntity<ApiResponse> getAppointmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Appointment> appointments = appointmentService.getAppointmentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse("Appointments retrieved successfully", appointments, true));
    }

    /**
     * POST /api/appointments - Create a new appointment
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        ApiResponse response = appointmentService.createAppointment(request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/appointments/{id} - Update an appointment
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {

        ApiResponse response = appointmentService.updateAppointment(id, request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/appointments/{id} - Delete an appointment
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteAppointment(@PathVariable Long id) {
        ApiResponse response = appointmentService.deleteAppointment(id);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
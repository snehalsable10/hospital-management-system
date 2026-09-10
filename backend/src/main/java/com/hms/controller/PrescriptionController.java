package com.hms.controller;

import com.hms.dto.request.PrescriptionRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Prescription;
import com.hms.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * GET /api/prescriptions - List all prescriptions
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", prescriptions, true));
    }

    /**
     * GET /api/prescriptions/{id} - Get a specific prescription
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getPrescriptionById(@PathVariable Long id) {
        Optional<Prescription> prescription = prescriptionService.getPrescriptionById(id);

        if (!prescription.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Prescription not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Prescription retrieved successfully", prescription.get(), true));
    }

    /**
     * GET /api/prescriptions/appointment/{appointmentId} - Get all prescriptions for an appointment
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse> getPrescriptionsByAppointment(@PathVariable Long appointmentId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByAppointment(appointmentId);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", prescriptions, true));
    }

    /**
     * GET /api/prescriptions/patient/{patientId} - Get all prescriptions for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse> getPrescriptionsByPatient(@PathVariable Long patientId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", prescriptions, true));
    }

    /**
     * GET /api/prescriptions/doctor/{doctorId} - Get all prescriptions written by a doctor
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse> getPrescriptionsByDoctor(@PathVariable Long doctorId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", prescriptions, true));
    }

    /**
     * GET /api/prescriptions/status/{status} - Get prescriptions by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getPrescriptionsByStatus(@PathVariable String status) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Prescriptions retrieved successfully", prescriptions, true));
    }

    /**
     * POST /api/prescriptions - Create a new prescription
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createPrescription(@Valid @RequestBody PrescriptionRequest request) {
        ApiResponse response = prescriptionService.createPrescription(request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/prescriptions/{id} - Update a prescription
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRequest request) {

        ApiResponse response = prescriptionService.updatePrescription(id, request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/prescriptions/{id} - Delete a prescription
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePrescription(@PathVariable Long id) {
        ApiResponse response = prescriptionService.deletePrescription(id);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
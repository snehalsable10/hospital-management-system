package com.hms.controller;

import com.hms.dto.request.PatientRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Patient;
import com.hms.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PatientController {

    private final PatientService patientService;

    /**
     * GET /api/patients - List all patients
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllPatients() {
        List<Patient> patients = patientService.getAllPatients();
        return ResponseEntity.ok(new ApiResponse("Patients retrieved successfully", patients, true));
    }

    /**
     * GET /api/patients/{id} - Get a specific patient
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getPatientById(@PathVariable Long id) {
        Optional<Patient> patient = patientService.getPatientById(id);

        if (!patient.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Patient not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Patient retrieved successfully", patient.get(), true));
    }

    /**
     * GET /api/patients/search/firstname/{firstName} - Search by first name
     */
    @GetMapping("/search/firstname/{firstName}")
    public ResponseEntity<ApiResponse> searchByFirstName(@PathVariable String firstName) {
        List<Patient> patients = patientService.searchByFirstName(firstName);
        return ResponseEntity.ok(new ApiResponse("Search results", patients, true));
    }

    /**
     * GET /api/patients/search/lastname/{lastName} - Search by last name
     */
    @GetMapping("/search/lastname/{lastName}")
    public ResponseEntity<ApiResponse> searchByLastName(@PathVariable String lastName) {
        List<Patient> patients = patientService.searchByLastName(lastName);
        return ResponseEntity.ok(new ApiResponse("Search results", patients, true));
    }

    /**
     * GET /api/patients/search/city/{city} - Search by city
     */
    @GetMapping("/search/city/{city}")
    public ResponseEntity<ApiResponse> searchByCity(@PathVariable String city) {
        List<Patient> patients = patientService.searchByCity(city);
        return ResponseEntity.ok(new ApiResponse("Search results", patients, true));
    }

    /**
     * POST /api/patients - Create a new patient
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        ApiResponse response = patientService.createPatient(request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/patients/{id} - Update a patient
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {

        ApiResponse response = patientService.updatePatient(id, request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/patients/{id} - Delete a patient
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePatient(@PathVariable Long id) {
        ApiResponse response = patientService.deletePatient(id);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
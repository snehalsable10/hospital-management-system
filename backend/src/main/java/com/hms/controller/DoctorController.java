package com.hms.controller;

import com.hms.dto.request.DoctorRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Doctor;
import com.hms.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * GET /api/doctors - List all doctors
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", doctors, true));
    }

    /**
     * GET /api/doctors/{id} - Get a specific doctor
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getDoctorById(@PathVariable Long id) {
        Optional<Doctor> doctor = doctorService.getDoctorById(id);

        if (!doctor.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Doctor not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Doctor retrieved successfully", doctor.get(), true));
    }

    /**
     * GET /api/doctors/department/{departmentId} - Get all doctors in a department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse> getDoctorsByDepartment(@PathVariable Long departmentId) {
        List<Doctor> doctors = doctorService.getDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", doctors, true));
    }

    /**
     * GET /api/doctors/specialization/{specialization} - Get doctors by specialization
     */
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<ApiResponse> getDoctorsBySpecialization(@PathVariable String specialization) {
        List<Doctor> doctors = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(new ApiResponse("Doctors retrieved successfully", doctors, true));
    }

    /**
     * POST /api/doctors - Create a new doctor
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createDoctor(@Valid @RequestBody DoctorRequest request) {
        ApiResponse response = doctorService.createDoctor(request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/doctors/{id} - Update a doctor
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request) {

        ApiResponse response = doctorService.updateDoctor(id, request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/doctors/{id} - Delete a doctor
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteDoctor(@PathVariable Long id) {
        ApiResponse response = doctorService.deleteDoctor(id);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
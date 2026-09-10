package com.hms.controller;

import com.hms.dto.request.MedicalHistoryRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.MedicalHistory;
import com.hms.service.MedicalHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medical-histories")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    /**
     * GET /api/medical-histories - List all medical histories
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllMedicalHistories() {
        List<MedicalHistory> medicalHistories = medicalHistoryService.getAllMedicalHistories();
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved successfully", medicalHistories, true));
    }

    /**
     * GET /api/medical-histories/{id} - Get a specific medical history
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getMedicalHistoryById(@PathVariable Long id) {
        Optional<MedicalHistory> medicalHistory = medicalHistoryService.getMedicalHistoryById(id);

        if (!medicalHistory.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Medical history not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Medical history retrieved successfully", medicalHistory.get(), true));
    }

    /**
     * GET /api/medical-histories/patient/{patientId} - Get all medical histories for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse> getMedicalHistoriesByPatient(@PathVariable Long patientId) {
        List<MedicalHistory> medicalHistories = medicalHistoryService.getMedicalHistoriesByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved successfully", medicalHistories, true));
    }

    /**
     * GET /api/medical-histories/status/{status} - Get medical histories by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getMedicalHistoriesByStatus(@PathVariable String status) {
        List<MedicalHistory> medicalHistories = medicalHistoryService.getMedicalHistoriesByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Medical histories retrieved successfully", medicalHistories, true));
    }

    /**
     * GET /api/medical-histories/search/{conditionName} - Search by condition name
     */
    @GetMapping("/search/{conditionName}")
    public ResponseEntity<ApiResponse> searchByConditionName(@PathVariable String conditionName) {
        List<MedicalHistory> medicalHistories = medicalHistoryService.searchByConditionName(conditionName);
        return ResponseEntity.ok(new ApiResponse("Search results", medicalHistories, true));
    }

    /**
     * POST /api/medical-histories - Create a new medical history entry
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createMedicalHistory(@Valid @RequestBody MedicalHistoryRequest request) {
        ApiResponse response = medicalHistoryService.createMedicalHistory(request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/medical-histories/{id} - Update a medical history entry
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateMedicalHistory(
            @PathVariable Long id,
            @Valid @RequestBody MedicalHistoryRequest request) {

        ApiResponse response = medicalHistoryService.updateMedicalHistory(id, request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/medical-histories/{id} - Delete a medical history entry
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteMedicalHistory(@PathVariable Long id) {
        ApiResponse response = medicalHistoryService.deleteMedicalHistory(id);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
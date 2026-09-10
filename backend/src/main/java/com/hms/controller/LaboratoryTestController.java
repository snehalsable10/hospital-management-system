package com.hms.controller;

import com.hms.dto.request.LaboratoryTestRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.LaboratoryTest;
import com.hms.service.LaboratoryTestService;
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
@RequestMapping("/api/laboratory-tests")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class LaboratoryTestController {

    private final LaboratoryTestService laboratoryTestService;

    /**
     * GET /api/laboratory-tests - List all laboratory tests
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllLaboratoryTests() {
        List<LaboratoryTest> laboratoryTests = laboratoryTestService.getAllLaboratoryTests();
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved successfully", laboratoryTests, true));
    }

    /**
     * GET /api/laboratory-tests/{id} - Get a specific laboratory test
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getLaboratoryTestById(@PathVariable Long id) {
        Optional<LaboratoryTest> laboratoryTest = laboratoryTestService.getLaboratoryTestById(id);

        if (!laboratoryTest.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Laboratory test not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Laboratory test retrieved successfully", laboratoryTest.get(), true));
    }

    /**
     * GET /api/laboratory-tests/patient/{patientId} - Get all laboratory tests for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse> getLaboratoryTestsByPatient(@PathVariable Long patientId) {
        List<LaboratoryTest> laboratoryTests = laboratoryTestService.getLaboratoryTestsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved successfully", laboratoryTests, true));
    }

    /**
     * GET /api/laboratory-tests/status/{status} - Get laboratory tests by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getLaboratoryTestsByStatus(@PathVariable String status) {
        List<LaboratoryTest> laboratoryTests = laboratoryTestService.getLaboratoryTestsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved successfully", laboratoryTests, true));
    }

    /**
     * GET /api/laboratory-tests/search/{testName} - Search by test name
     */
    @GetMapping("/search/{testName}")
    public ResponseEntity<ApiResponse> searchByTestName(@PathVariable String testName) {
        List<LaboratoryTest> laboratoryTests = laboratoryTestService.searchByTestName(testName);
        return ResponseEntity.ok(new ApiResponse("Search results", laboratoryTests, true));
    }

    /**
     * GET /api/laboratory-tests/daterange?startDate=2024-01-01&endDate=2024-12-31 - Get tests in date range
     */
    @GetMapping("/daterange")
    public ResponseEntity<ApiResponse> getLaboratoryTestsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<LaboratoryTest> laboratoryTests = laboratoryTestService.getLaboratoryTestsByDateRange(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse("Laboratory tests retrieved successfully", laboratoryTests, true));
    }

    /**
     * POST /api/laboratory-tests - Create a new laboratory test entry
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createLaboratoryTest(@Valid @RequestBody LaboratoryTestRequest request) {
        ApiResponse response = laboratoryTestService.createLaboratoryTest(request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/laboratory-tests/{id} - Update a laboratory test entry
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateLaboratoryTest(
            @PathVariable Long id,
            @Valid @RequestBody LaboratoryTestRequest request) {

        ApiResponse response = laboratoryTestService.updateLaboratoryTest(id, request);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/laboratory-tests/{id} - Delete a laboratory test entry
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteLaboratoryTest(@PathVariable Long id) {
        ApiResponse response = laboratoryTestService.deleteLaboratoryTest(id);

        if (!response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
package com.hms.service;

import com.hms.dto.request.LaboratoryTestRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.LaboratoryTest;
import com.hms.entity.Patient;
import com.hms.repository.LaboratoryTestRepository;
import com.hms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LaboratoryTestService {

    private final LaboratoryTestRepository laboratoryTestRepository;
    private final PatientRepository patientRepository;

    /**
     * Get all laboratory tests
     */
    public List<LaboratoryTest> getAllLaboratoryTests() {
        return laboratoryTestRepository.findAll();
    }

    /**
     * Get laboratory test by ID
     */
    public Optional<LaboratoryTest> getLaboratoryTestById(Long id) {
        return laboratoryTestRepository.findById(id);
    }

    /**
     * Get all laboratory tests for a patient
     */
    public List<LaboratoryTest> getLaboratoryTestsByPatient(Long patientId) {
        return laboratoryTestRepository.findByPatientId(patientId);
    }

    /**
     * Get laboratory tests by status
     */
    public List<LaboratoryTest> getLaboratoryTestsByStatus(String status) {
        return laboratoryTestRepository.findByStatus(status);
    }

    /**
     * Search laboratory tests by test name
     */
    public List<LaboratoryTest> searchByTestName(String testName) {
        return laboratoryTestRepository.findByTestNameIgnoreCaseContaining(testName);
    }

    /**
     * Get laboratory tests in a date range
     */
    public List<LaboratoryTest> getLaboratoryTestsByDateRange(LocalDate startDate, LocalDate endDate) {
        return laboratoryTestRepository.findByTestDateBetween(startDate, endDate);
    }

    /**
     * Create a new laboratory test entry
     */
    public ApiResponse createLaboratoryTest(LaboratoryTestRequest request) {
        try {
            // Validate patient exists
            Optional<Patient> patientOptional = patientRepository.findById(request.getPatientId());
            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            // Create new laboratory test
            LaboratoryTest laboratoryTest = new LaboratoryTest();
            laboratoryTest.setPatient(patientOptional.get());
            laboratoryTest.setTestName(request.getTestName());
            laboratoryTest.setTestDate(request.getTestDate());
            laboratoryTest.setResultValue(request.getResultValue());
            laboratoryTest.setResultUnit(request.getResultUnit());
            laboratoryTest.setReferenceMin(request.getReferenceMin());
            laboratoryTest.setReferenceMax(request.getReferenceMax());
            laboratoryTest.setStatus(request.getStatus() != null ? request.getStatus() : "NORMAL");
            laboratoryTest.setNotes(request.getNotes());
            laboratoryTest.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            laboratoryTestRepository.save(laboratoryTest);

            return new ApiResponse("Laboratory test created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create laboratory test: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing laboratory test entry
     */
    public ApiResponse updateLaboratoryTest(Long id, LaboratoryTestRequest request) {
        try {
            Optional<LaboratoryTest> laboratoryTestOptional = laboratoryTestRepository.findById(id);

            if (!laboratoryTestOptional.isPresent()) {
                return new ApiResponse("Laboratory test not found", false);
            }

            LaboratoryTest laboratoryTest = laboratoryTestOptional.get();

            // Validate patient exists
            Optional<Patient> patientOptional = patientRepository.findById(request.getPatientId());
            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            // Update fields
            laboratoryTest.setPatient(patientOptional.get());
            laboratoryTest.setTestName(request.getTestName());
            laboratoryTest.setTestDate(request.getTestDate());
            laboratoryTest.setResultValue(request.getResultValue());
            laboratoryTest.setResultUnit(request.getResultUnit());
            laboratoryTest.setReferenceMin(request.getReferenceMin());
            laboratoryTest.setReferenceMax(request.getReferenceMax());
            laboratoryTest.setStatus(request.getStatus() != null ? request.getStatus() : "NORMAL");
            laboratoryTest.setNotes(request.getNotes());
            if (request.getIsActive() != null) {
                laboratoryTest.setIsActive(request.getIsActive());
            }

            laboratoryTestRepository.save(laboratoryTest);

            return new ApiResponse("Laboratory test updated successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to update laboratory test: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a laboratory test entry (soft delete)
     */
    public ApiResponse deleteLaboratoryTest(Long id) {
        try {
            Optional<LaboratoryTest> laboratoryTestOptional = laboratoryTestRepository.findById(id);

            if (!laboratoryTestOptional.isPresent()) {
                return new ApiResponse("Laboratory test not found", false);
            }

            LaboratoryTest laboratoryTest = laboratoryTestOptional.get();
            laboratoryTest.setIsActive(false);
            laboratoryTestRepository.save(laboratoryTest);

            return new ApiResponse("Laboratory test deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete laboratory test: " + e.getMessage(), false);
        }
    }
}
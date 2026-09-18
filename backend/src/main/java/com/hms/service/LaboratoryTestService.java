package com.hms.service;

import com.hms.dto.request.LaboratoryTestRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.LaboratoryTest;
import com.hms.entity.Patient;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.LaboratoryTestRepository;
import com.hms.repository.PatientRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * Cached for 10 minutes
     */
    @Cacheable(value = "laboratoryTests", key = "'getAllLaboratoryTests'")
    public List<LaboratoryTest> getAllLaboratoryTests() {
        return laboratoryTestRepository.findByIsActiveTrue();
    }

    /**
     * Get laboratory test by ID
     * Cached for 10 minutes with key = laboratory test ID
     */
    @Cacheable(value = "laboratoryTest", key = "#id")
    public Optional<LaboratoryTest> getLaboratoryTestById(Long id) {
        return laboratoryTestRepository.findById(id);
    }

    /**
     * Get all laboratory tests for a patient
     * Cached for 10 minutes
     */
    @Cacheable(value = "laboratoryTests", key = "'getLaboratoryTestsByPatient:' + #patientId")
    public List<LaboratoryTest> getLaboratoryTestsByPatient(Long patientId) {
        return laboratoryTestRepository.findByPatientId(patientId);
    }

    /**
     * Get laboratory tests by status
     * Cached for 10 minutes
     */
    @Cacheable(value = "laboratoryTests", key = "'getLaboratoryTestsByStatus:' + #status")
    public List<LaboratoryTest> getLaboratoryTestsByStatus(String status) {
        return laboratoryTestRepository.findByStatus(status);
    }

    /**
     * Search laboratory tests by test name
     * Cached for 10 minutes
     */
    @Cacheable(value = "laboratoryTests", key = "'searchByTestName:' + #testName")
    public List<LaboratoryTest> searchByTestName(String testName) {
        return laboratoryTestRepository.findByTestNameIgnoreCaseContaining(testName);
    }

    /**
     * Get laboratory tests in a date range
     * Cached for 10 minutes
     */
    @Cacheable(value = "laboratoryTests", key = "'getLaboratoryTestsByDateRange:' + #startDate + ':' + #endDate")
    public List<LaboratoryTest> getLaboratoryTestsByDateRange(LocalDate startDate, LocalDate endDate) {
        return laboratoryTestRepository.findByTestDateBetween(startDate, endDate);
    }

    /**
     * Create a new laboratory test entry
     * Clears all laboratory test caches on create
     *
     * @throws IllegalArgumentException if the referenced patient does not exist
     */
    @CacheEvict(value = {"laboratoryTests", "laboratoryTest"}, allEntries = true)
    public ApiResponse createLaboratoryTest(LaboratoryTestRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        LaboratoryTest laboratoryTest = new LaboratoryTest();
        laboratoryTest.setPatient(patient);
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
    }

    /**
     * Update an existing laboratory test entry
     * Clears all laboratory test caches on update
     *
     * @throws ResourceNotFoundException if no laboratory test exists with the given id
     * @throws IllegalArgumentException  if the referenced patient does not exist
     */
    @CacheEvict(value = {"laboratoryTests", "laboratoryTest"}, allEntries = true)
    public ApiResponse updateLaboratoryTest(Long id, LaboratoryTestRequest request) {
        LaboratoryTest laboratoryTest = laboratoryTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratory test not found with id: " + id));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        laboratoryTest.setPatient(patient);
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
    }

    /**
     * Delete a laboratory test entry (soft delete)
     * Clears all laboratory test caches on delete
     *
     * @throws ResourceNotFoundException if no laboratory test exists with the given id
     */
    @CacheEvict(value = {"laboratoryTests", "laboratoryTest"}, allEntries = true)
    public ApiResponse deleteLaboratoryTest(Long id) {
        LaboratoryTest laboratoryTest = laboratoryTestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratory test not found with id: " + id));

        laboratoryTest.setIsActive(false);
        laboratoryTestRepository.save(laboratoryTest);

        return new ApiResponse("Laboratory test deleted successfully", true);
    }

    /**
     * Get all laboratory tests with pagination
     */
    public PageResponse<LaboratoryTest> getAllLaboratoryTestsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<LaboratoryTest> page = laboratoryTestRepository.findByIsActiveTrue(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get laboratory tests by patient with pagination
     */
    public PageResponse<LaboratoryTest> getLaboratoryTestsByPatientPaginated(Long patientId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<LaboratoryTest> page = laboratoryTestRepository.findByPatientId(patientId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get laboratory tests by status with pagination
     */
    public PageResponse<LaboratoryTest> getLaboratoryTestsByStatusPaginated(String status, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<LaboratoryTest> page = laboratoryTestRepository.findByStatus(status, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Search laboratory tests by test name with pagination
     */
    public PageResponse<LaboratoryTest> searchByTestNamePaginated(String testName, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<LaboratoryTest> page = laboratoryTestRepository.findByTestNameIgnoreCaseContaining(testName, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get laboratory tests by date range with pagination
     */
    public PageResponse<LaboratoryTest> getLaboratoryTestsByDateRangePaginated(LocalDate startDate, LocalDate endDate, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<LaboratoryTest> page = laboratoryTestRepository.findByTestDateBetween(startDate, endDate, pageable);
        return PaginationUtil.toPageResponse(page);
    }
}
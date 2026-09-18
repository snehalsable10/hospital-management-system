package com.hms.service;

import com.hms.dto.request.MedicalHistoryRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.MedicalHistory;
import com.hms.entity.Patient;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.MedicalHistoryRepository;
import com.hms.repository.PatientRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PatientRepository patientRepository;

    /**
     * Get all medical histories
     * Cached for 10 minutes
     */
    @Cacheable(value = "medicalHistories", key = "'getAllMedicalHistories'")
    public List<MedicalHistory> getAllMedicalHistories() {
        return medicalHistoryRepository.findAll();
    }

    /**
     * Get medical history by ID
     * Cached for 10 minutes with key = medical history ID
     */
    @Cacheable(value = "medicalHistory", key = "#id")
    public Optional<MedicalHistory> getMedicalHistoryById(Long id) {
        return medicalHistoryRepository.findById(id);
    }

    /**
     * Get all medical histories for a patient
     * Cached for 10 minutes
     */
    @Cacheable(value = "medicalHistories", key = "'getMedicalHistoriesByPatient:' + #patientId")
    public List<MedicalHistory> getMedicalHistoriesByPatient(Long patientId) {
        return medicalHistoryRepository.findByPatientId(patientId);
    }

    /**
     * Get medical histories by status
     * Cached for 10 minutes
     */
    @Cacheable(value = "medicalHistories", key = "'getMedicalHistoriesByStatus:' + #status")
    public List<MedicalHistory> getMedicalHistoriesByStatus(String status) {
        return medicalHistoryRepository.findByStatus(status);
    }

    /**
     * Search medical histories by condition name
     * Cached for 10 minutes
     */
    @Cacheable(value = "medicalHistories", key = "'searchByConditionName:' + #conditionName")
    public List<MedicalHistory> searchByConditionName(String conditionName) {
        return medicalHistoryRepository.findByConditionNameIgnoreCaseContaining(conditionName);
    }

    /**
     * Create a new medical history entry
     * Clears all medical history caches on create
     *
     * @throws IllegalArgumentException if the referenced patient does not exist
     */
    @CacheEvict(value = {"medicalHistories", "medicalHistory"}, allEntries = true)
    public ApiResponse createMedicalHistory(MedicalHistoryRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        MedicalHistory medicalHistory = new MedicalHistory();
        medicalHistory.setPatient(patient);
        medicalHistory.setConditionName(request.getConditionName());
        medicalHistory.setDiagnosisDate(request.getDiagnosisDate());
        medicalHistory.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        medicalHistory.setDescription(request.getDescription());
        medicalHistory.setTreatment(request.getTreatment());
        medicalHistory.setDoctorNotes(request.getDoctorNotes());
        medicalHistory.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        medicalHistoryRepository.save(medicalHistory);

        return new ApiResponse("Medical history created successfully", true);
    }

    /**
     * Update an existing medical history entry
     * Clears all medical history caches on update
     *
     * @throws ResourceNotFoundException if no medical history exists with the given id
     * @throws IllegalArgumentException  if the referenced patient does not exist
     */
    @CacheEvict(value = {"medicalHistories", "medicalHistory"}, allEntries = true)
    public ApiResponse updateMedicalHistory(Long id, MedicalHistoryRequest request) {
        MedicalHistory medicalHistory = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical history not found with id: " + id));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        medicalHistory.setPatient(patient);
        medicalHistory.setConditionName(request.getConditionName());
        medicalHistory.setDiagnosisDate(request.getDiagnosisDate());
        medicalHistory.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        medicalHistory.setDescription(request.getDescription());
        medicalHistory.setTreatment(request.getTreatment());
        medicalHistory.setDoctorNotes(request.getDoctorNotes());
        if (request.getIsActive() != null) {
            medicalHistory.setIsActive(request.getIsActive());
        }

        medicalHistoryRepository.save(medicalHistory);

        return new ApiResponse("Medical history updated successfully", true);
    }

    /**
     * Delete a medical history entry (soft delete)
     * Clears all medical history caches on delete
     *
     * @throws ResourceNotFoundException if no medical history exists with the given id
     */
    @CacheEvict(value = {"medicalHistories", "medicalHistory"}, allEntries = true)
    public ApiResponse deleteMedicalHistory(Long id) {
        MedicalHistory medicalHistory = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical history not found with id: " + id));

        medicalHistory.setIsActive(false);
        medicalHistoryRepository.save(medicalHistory);

        return new ApiResponse("Medical history deleted successfully", true);
    }

    /**
     * Get all medical histories with pagination
     */
    public PageResponse<MedicalHistory> getAllMedicalHistoriesPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<MedicalHistory> page = medicalHistoryRepository.findAll(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get medical histories by patient with pagination
     */
    public PageResponse<MedicalHistory> getMedicalHistoriesByPatientPaginated(Long patientId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<MedicalHistory> page = medicalHistoryRepository.findByPatientId(patientId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get medical histories by status with pagination
     */
    public PageResponse<MedicalHistory> getMedicalHistoriesByStatusPaginated(String status, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<MedicalHistory> page = medicalHistoryRepository.findByStatus(status, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Search medical histories by condition name with pagination
     */
    public PageResponse<MedicalHistory> searchByConditionNamePaginated(String conditionName, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<MedicalHistory> page = medicalHistoryRepository.findByConditionNameIgnoreCaseContaining(conditionName, pageable);
        return PaginationUtil.toPageResponse(page);
    }
}
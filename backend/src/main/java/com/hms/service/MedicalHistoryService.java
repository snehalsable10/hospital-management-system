package com.hms.service;

import com.hms.dto.request.MedicalHistoryRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.MedicalHistory;
import com.hms.entity.Patient;
import com.hms.repository.MedicalHistoryRepository;
import com.hms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
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
     */
    public List<MedicalHistory> getAllMedicalHistories() {
        return medicalHistoryRepository.findAll();
    }

    /**
     * Get medical history by ID
     */
    public Optional<MedicalHistory> getMedicalHistoryById(Long id) {
        return medicalHistoryRepository.findById(id);
    }

    /**
     * Get all medical histories for a patient
     */
    public List<MedicalHistory> getMedicalHistoriesByPatient(Long patientId) {
        return medicalHistoryRepository.findByPatientId(patientId);
    }

    /**
     * Get medical histories by status
     */
    public List<MedicalHistory> getMedicalHistoriesByStatus(String status) {
        return medicalHistoryRepository.findByStatus(status);
    }

    /**
     * Search medical histories by condition name
     */
    public List<MedicalHistory> searchByConditionName(String conditionName) {
        return medicalHistoryRepository.findByConditionNameIgnoreCaseContaining(conditionName);
    }

    /**
     * Create a new medical history entry
     */
    public ApiResponse createMedicalHistory(MedicalHistoryRequest request) {
        try {
            // Validate patient exists
            Optional<Patient> patientOptional = patientRepository.findById(request.getPatientId());
            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            // Create new medical history
            MedicalHistory medicalHistory = new MedicalHistory();
            medicalHistory.setPatient(patientOptional.get());
            medicalHistory.setConditionName(request.getConditionName());
            medicalHistory.setDiagnosisDate(request.getDiagnosisDate());
            medicalHistory.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
            medicalHistory.setDescription(request.getDescription());
            medicalHistory.setTreatment(request.getTreatment());
            medicalHistory.setDoctorNotes(request.getDoctorNotes());
            medicalHistory.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            medicalHistoryRepository.save(medicalHistory);

            return new ApiResponse("Medical history created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create medical history: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing medical history entry
     */
    public ApiResponse updateMedicalHistory(Long id, MedicalHistoryRequest request) {
        try {
            Optional<MedicalHistory> medicalHistoryOptional = medicalHistoryRepository.findById(id);

            if (!medicalHistoryOptional.isPresent()) {
                return new ApiResponse("Medical history not found", false);
            }

            MedicalHistory medicalHistory = medicalHistoryOptional.get();

            // Validate patient exists
            Optional<Patient> patientOptional = patientRepository.findById(request.getPatientId());
            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            // Update fields
            medicalHistory.setPatient(patientOptional.get());
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
        } catch (Exception e) {
            return new ApiResponse("Failed to update medical history: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a medical history entry (soft delete)
     */
    public ApiResponse deleteMedicalHistory(Long id) {
        try {
            Optional<MedicalHistory> medicalHistoryOptional = medicalHistoryRepository.findById(id);

            if (!medicalHistoryOptional.isPresent()) {
                return new ApiResponse("Medical history not found", false);
            }

            MedicalHistory medicalHistory = medicalHistoryOptional.get();
            medicalHistory.setIsActive(false);
            medicalHistoryRepository.save(medicalHistory);

            return new ApiResponse("Medical history deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete medical history: " + e.getMessage(), false);
        }
    }
}
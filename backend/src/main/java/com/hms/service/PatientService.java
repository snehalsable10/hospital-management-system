package com.hms.service;

import com.hms.dto.request.PatientRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Patient;
import com.hms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    /**
     * Get all patients
     */
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    /**
     * Get patient by ID
     */
    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    /**
     * Search patients by first name
     */
    public List<Patient> searchByFirstName(String firstName) {
        return patientRepository.findByFirstNameIgnoreCaseContaining(firstName);
    }

    /**
     * Search patients by last name
     */
    public List<Patient> searchByLastName(String lastName) {
        return patientRepository.findByLastNameIgnoreCaseContaining(lastName);
    }

    /**
     * Search patients by city
     */
    public List<Patient> searchByCity(String city) {
        return patientRepository.findByCity(city);
    }

    /**
     * Create a new patient
     */
    public ApiResponse createPatient(PatientRequest request) {
        try {
            // Check if email already exists (only if email is provided)
            if (request.getEmail() != null && !request.getEmail().isEmpty() &&
                patientRepository.existsByEmail(request.getEmail())) {
                return new ApiResponse("Email already registered", false);
            }

            // Create new patient
            Patient patient = new Patient();
            patient.setFirstName(request.getFirstName());
            patient.setLastName(request.getLastName());
            patient.setEmail(request.getEmail());
            patient.setPhone(request.getPhone());
            patient.setDateOfBirth(request.getDateOfBirth());
            patient.setGender(request.getGender());
            patient.setAddress(request.getAddress());
            patient.setCity(request.getCity());
            patient.setState(request.getState());
            patient.setZipCode(request.getZipCode());
            patient.setBloodGroup(request.getBloodGroup());
            patient.setEmergencyContact(request.getEmergencyContact());
            patient.setEmergencyPhone(request.getEmergencyPhone());
            patient.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            patientRepository.save(patient);

            return new ApiResponse("Patient created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create patient: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing patient
     */
    public ApiResponse updatePatient(Long id, PatientRequest request) {
        try {
            Optional<Patient> patientOptional = patientRepository.findById(id);

            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            Patient patient = patientOptional.get();

            // Check if new email already exists (excluding current patient)
            if (request.getEmail() != null && !request.getEmail().isEmpty() &&
                !patient.getEmail().equals(request.getEmail()) &&
                patientRepository.existsByEmail(request.getEmail())) {
                return new ApiResponse("Email already registered", false);
            }

            // Update fields
            patient.setFirstName(request.getFirstName());
            patient.setLastName(request.getLastName());
            patient.setEmail(request.getEmail());
            patient.setPhone(request.getPhone());
            patient.setDateOfBirth(request.getDateOfBirth());
            patient.setGender(request.getGender());
            patient.setAddress(request.getAddress());
            patient.setCity(request.getCity());
            patient.setState(request.getState());
            patient.setZipCode(request.getZipCode());
            patient.setBloodGroup(request.getBloodGroup());
            patient.setEmergencyContact(request.getEmergencyContact());
            patient.setEmergencyPhone(request.getEmergencyPhone());
            if (request.getIsActive() != null) {
                patient.setIsActive(request.getIsActive());
            }

            patientRepository.save(patient);

            return new ApiResponse("Patient updated successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to update patient: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a patient (soft delete)
     */
    public ApiResponse deletePatient(Long id) {
        try {
            Optional<Patient> patientOptional = patientRepository.findById(id);

            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            Patient patient = patientOptional.get();
            patient.setIsActive(false);
            patientRepository.save(patient);

            return new ApiResponse("Patient deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete patient: " + e.getMessage(), false);
        }
    }
}
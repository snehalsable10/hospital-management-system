package com.hms.service;

import com.hms.dto.request.PatientRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Patient;
import com.hms.entity.User;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.PatientRepository;
import com.hms.repository.UserRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    /**
     * Get all patients
     * Cached for 15 minutes
     * WARNING: Returns all patients - use paginated version for large datasets
     */
    @Cacheable(value = "patients", key = "'getAllPatients'")
    public List<Patient> getAllPatients() {
        return patientRepository.findByIsActiveTrue();
    }

    /**
     * Get all patients with pagination
     * Not cached - pagination results vary by page
     * Reduces database load and memory usage
     */
    public PageResponse<Patient> getAllPatientsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Patient> page = patientRepository.findByIsActiveTrue(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get patient by ID
     * Cached for 15 minutes with key = patient ID
     */
    @Cacheable(value = "patient", key = "#id")
    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    /**
     * Search patients by first name
     * Cached for 15 minutes
     * WARNING: Returns all matching patients - use paginated version for large datasets
     */
    @Cacheable(value = "patients", key = "'searchByFirstName:' + #firstName")
    public List<Patient> searchByFirstName(String firstName) {
        return patientRepository.findByFirstNameIgnoreCaseContainingAndIsActiveTrue(firstName);
    }

    /**
     * Search patients by first name with pagination
     * Not cached - pagination results vary by page
     */
    public PageResponse<Patient> searchByFirstNamePaginated(String firstName, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Patient> page = patientRepository.findByFirstNameIgnoreCaseContainingAndIsActiveTrue(firstName, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Search patients by last name
     * Cached for 15 minutes
     * WARNING: Returns all matching patients - use paginated version for large datasets
     */
    @Cacheable(value = "patients", key = "'searchByLastName:' + #lastName")
    public List<Patient> searchByLastName(String lastName) {
        return patientRepository.findByLastNameIgnoreCaseContainingAndIsActiveTrue(lastName);
    }

    /**
     * Search patients by last name with pagination
     * Not cached - pagination results vary by page
     */
    public PageResponse<Patient> searchByLastNamePaginated(String lastName, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Patient> page = patientRepository.findByLastNameIgnoreCaseContainingAndIsActiveTrue(lastName, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Search patients by city
     * Cached for 15 minutes
     * WARNING: Returns all matching patients - use paginated version for large datasets
     */
    @Cacheable(value = "patients", key = "'searchByCity:' + #city")
    public List<Patient> searchByCity(String city) {
        return patientRepository.findByCityAndIsActiveTrue(city);
    }

    /**
     * Search patients by city with pagination
     * Not cached - pagination results vary by page
     */
    public PageResponse<Patient> searchByCityPaginated(String city, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Patient> page = patientRepository.findByCityAndIsActiveTrue(city, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Create a new patient
     * Clears all patient caches on create
     *
     * @throws DuplicateResourceException if the email is already registered,
     *                                    or the login account is already linked
     *                                    to another patient
     * @throws IllegalArgumentException   if the referenced user does not exist
     */
    @CacheEvict(value = {"patients", "patient"}, allEntries = true)
    public ApiResponse createPatient(PatientRequest request) {
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
            patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

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

        applyUserLink(patient, request.getUserId(), null);

        patientRepository.save(patient);

        return new ApiResponse("Patient created successfully", true);
    }

    /**
     * Update an existing patient
     * Clears patient caches on update
     *
     * @throws ResourceNotFoundException  if no patient exists with the given id
     * @throws DuplicateResourceException if the new email belongs to another patient,
     *                                    or the login account is already linked
     *                                    to another patient
     * @throws IllegalArgumentException   if the referenced user does not exist
     */
    @CacheEvict(value = {"patients", "patient"}, allEntries = true)
    public ApiResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        // Objects.equals is null-safe: a patient may legitimately have no email yet
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
            !Objects.equals(patient.getEmail(), request.getEmail()) &&
            patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

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

        applyUserLink(patient, request.getUserId(), id);

        patientRepository.save(patient);

        return new ApiResponse("Patient updated successfully", true);
    }

    /**
     * Delete a patient (soft delete)
     * Clears patient caches on delete
     *
     * @throws ResourceNotFoundException if no patient exists with the given id
     */
    @CacheEvict(value = {"patients", "patient"}, allEntries = true)
    public ApiResponse deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        patient.setIsActive(false);
        patientRepository.save(patient);

        return new ApiResponse("Patient deleted successfully", true);
    }

    /**
     * Point this patient record at a login account, so ownership checks can
     * recognise the patient as its owner.
     *
     * A null userId leaves any existing link untouched rather than clearing it:
     * the patient edit form does not send this field, and treating "absent" as
     * "unlink" would silently revoke a patient's access on every edit.
     *
     * @param currentPatientId id of the patient being updated, or null on create,
     *                         so re-saving a patient with its own link is not
     *                         mistaken for a conflict
     */
    private void applyUserLink(Patient patient, Long userId, Long currentPatientId) {
        if (userId == null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        patientRepository.findByUserId(userId).ifPresent(existing -> {
            if (!existing.getId().equals(currentPatientId)) {
                throw new DuplicateResourceException(
                        "User " + userId + " is already linked to patient " + existing.getId());
            }
        });

        patient.setUser(user);
    }
}
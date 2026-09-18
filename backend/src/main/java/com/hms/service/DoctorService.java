package com.hms.service;

import com.hms.dto.request.DoctorRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Department;
import com.hms.entity.Doctor;
import com.hms.entity.User;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.DepartmentRepository;
import com.hms.repository.DoctorRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    /**
     * Get all doctors
     * Cached for 20 minutes
     */
    @Cacheable(value = "doctors", key = "'getAllDoctors'")
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Get doctor by ID
     * Cached for 20 minutes with key = doctor ID
     */
    @Cacheable(value = "doctor", key = "#id")
    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    /**
     * Get all doctors in a department
     * Cached for 20 minutes
     */
    @Cacheable(value = "doctors", key = "'getDoctorsByDepartment:' + #departmentId")
    public List<Doctor> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartmentId(departmentId);
    }

    /**
     * Get all doctors with a specific specialization
     * Cached for 20 minutes
     */
    @Cacheable(value = "doctors", key = "'getDoctorsBySpecialization:' + #specialization")
    public List<Doctor> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization);
    }

    /**
     * Create a new doctor
     * Clears all doctor caches on create
     *
     * @throws DuplicateResourceException if the email or license number is taken,
     *                                    or the login account is already linked
     *                                    to another doctor
     * @throws IllegalArgumentException   if the referenced department or user does not exist
     */
    @CacheEvict(value = {"doctors", "doctor"}, allEntries = true)
    public ApiResponse createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("License number already exists: " + request.getLicenseNumber());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department not found with id: " + request.getDepartmentId()));

        Doctor doctor = new Doctor();
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setEmail(request.getEmail());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setDepartment(department);
        doctor.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        applyUserLink(doctor, request.getUserId(), null);

        doctorRepository.save(doctor);

        return new ApiResponse("Doctor created successfully", true);
    }

    /**
     * Update an existing doctor
     * Clears all doctor caches on update
     *
     * @throws ResourceNotFoundException  if no doctor exists with the given id
     * @throws DuplicateResourceException if the new email or license belongs to another
     *                                    doctor, or the login account is already linked
     *                                    to another doctor
     * @throws IllegalArgumentException   if the referenced department or user does not exist
     */
    @CacheEvict(value = {"doctors", "doctor"}, allEntries = true)
    public ApiResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));

        if (!doctor.getEmail().equals(request.getEmail()) &&
            doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        if (!doctor.getLicenseNumber().equals(request.getLicenseNumber()) &&
            doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("License number already exists: " + request.getLicenseNumber());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department not found with id: " + request.getDepartmentId()));

        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setEmail(request.getEmail());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setDepartment(department);
        if (request.getIsActive() != null) {
            doctor.setIsActive(request.getIsActive());
        }

        applyUserLink(doctor, request.getUserId(), id);

        doctorRepository.save(doctor);

        return new ApiResponse("Doctor updated successfully", true);
    }

    /**
     * Delete a doctor (soft delete)
     * Clears all doctor caches on delete
     *
     * @throws ResourceNotFoundException if no doctor exists with the given id
     */
    @CacheEvict(value = {"doctors", "doctor"}, allEntries = true)
    public ApiResponse deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));

        doctor.setIsActive(false);
        doctorRepository.save(doctor);

        return new ApiResponse("Doctor deleted successfully", true);
    }

    /**
     * Get all doctors with pagination
     */
    public PageResponse<Doctor> getAllDoctorsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Doctor> page = doctorRepository.findAll(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get doctors by department with pagination
     */
    public PageResponse<Doctor> getDoctorsByDepartmentPaginated(Long departmentId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Doctor> page = doctorRepository.findByDepartmentId(departmentId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get doctors by specialization with pagination
     */
    public PageResponse<Doctor> getDoctorsBySpecializationPaginated(String specialization, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Doctor> page = doctorRepository.findBySpecialization(specialization, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Point this doctor record at a login account, so ownership checks can
     * recognise the doctor as its owner.
     *
     * A null userId leaves any existing link untouched rather than clearing it:
     * the doctor edit form does not send this field, and treating "absent" as
     * "unlink" would silently revoke a doctor's access on every edit.
     *
     * @param currentDoctorId id of the doctor being updated, or null on create,
     *                        so re-saving a doctor with its own link is not
     *                        mistaken for a conflict
     */
    private void applyUserLink(Doctor doctor, Long userId, Long currentDoctorId) {
        if (userId == null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        doctorRepository.findByUserId(userId).ifPresent(existing -> {
            if (!existing.getId().equals(currentDoctorId)) {
                throw new DuplicateResourceException(
                        "User " + userId + " is already linked to doctor " + existing.getId());
            }
        });

        doctor.setUser(user);
    }
}
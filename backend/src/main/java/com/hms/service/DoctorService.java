package com.hms.service;

import com.hms.dto.request.DoctorRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Department;
import com.hms.entity.Doctor;
import com.hms.repository.DepartmentRepository;
import com.hms.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Get all doctors
     */
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Get doctor by ID
     */
    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    /**
     * Get all doctors in a department
     */
    public List<Doctor> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartmentId(departmentId);
    }

    /**
     * Get all doctors with a specific specialization
     */
    public List<Doctor> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization);
    }

    /**
     * Create a new doctor
     */
    public ApiResponse createDoctor(DoctorRequest request) {
        try {
            // Check if email already exists
            if (doctorRepository.existsByEmail(request.getEmail())) {
                return new ApiResponse("Email already registered", false);
            }

            // Check if license number already exists
            if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                return new ApiResponse("License number already exists", false);
            }

            // Check if department exists
            Optional<Department> departmentOptional = departmentRepository.findById(request.getDepartmentId());
            if (!departmentOptional.isPresent()) {
                return new ApiResponse("Department not found", false);
            }

            // Create new doctor
            Doctor doctor = new Doctor();
            doctor.setFirstName(request.getFirstName());
            doctor.setLastName(request.getLastName());
            doctor.setEmail(request.getEmail());
            doctor.setPhone(request.getPhone());
            doctor.setSpecialization(request.getSpecialization());
            doctor.setLicenseNumber(request.getLicenseNumber());
            doctor.setDepartment(departmentOptional.get());
            doctor.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            doctorRepository.save(doctor);

            return new ApiResponse("Doctor created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create doctor: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing doctor
     */
    public ApiResponse updateDoctor(Long id, DoctorRequest request) {
        try {
            Optional<Doctor> doctorOptional = doctorRepository.findById(id);

            if (!doctorOptional.isPresent()) {
                return new ApiResponse("Doctor not found", false);
            }

            Doctor doctor = doctorOptional.get();

            // Check if new email already exists (excluding current doctor)
            if (!doctor.getEmail().equals(request.getEmail()) &&
                doctorRepository.existsByEmail(request.getEmail())) {
                return new ApiResponse("Email already registered", false);
            }

            // Check if new license number already exists (excluding current doctor)
            if (!doctor.getLicenseNumber().equals(request.getLicenseNumber()) &&
                doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                return new ApiResponse("License number already exists", false);
            }

            // Check if department exists
            Optional<Department> departmentOptional = departmentRepository.findById(request.getDepartmentId());
            if (!departmentOptional.isPresent()) {
                return new ApiResponse("Department not found", false);
            }

            // Update fields
            doctor.setFirstName(request.getFirstName());
            doctor.setLastName(request.getLastName());
            doctor.setEmail(request.getEmail());
            doctor.setPhone(request.getPhone());
            doctor.setSpecialization(request.getSpecialization());
            doctor.setLicenseNumber(request.getLicenseNumber());
            doctor.setDepartment(departmentOptional.get());
            if (request.getIsActive() != null) {
                doctor.setIsActive(request.getIsActive());
            }

            doctorRepository.save(doctor);

            return new ApiResponse("Doctor updated successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to update doctor: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a doctor (soft delete)
     */
    public ApiResponse deleteDoctor(Long id) {
        try {
            Optional<Doctor> doctorOptional = doctorRepository.findById(id);

            if (!doctorOptional.isPresent()) {
                return new ApiResponse("Doctor not found", false);
            }

            Doctor doctor = doctorOptional.get();
            doctor.setIsActive(false);
            doctorRepository.save(doctor);

            return new ApiResponse("Doctor deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete doctor: " + e.getMessage(), false);
        }
    }
}
package com.hms.service;

import com.hms.dto.request.DepartmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Department;
import com.hms.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Get all departments
     */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /**
     * Get department by ID
     */
    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    /**
     * Create a new department
     */
    public ApiResponse createDepartment(DepartmentRequest request) {
        try {
            // Check if department name already exists
            if (departmentRepository.existsByName(request.getName())) {
                return new ApiResponse("Department name already exists", false);
            }

            // Create new department
            Department department = new Department();
            department.setName(request.getName());
            department.setDescription(request.getDescription());
            department.setPhone(request.getPhone());
            department.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            departmentRepository.save(department);

            return new ApiResponse("Department created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create department: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing department
     */
    public ApiResponse updateDepartment(Long id, DepartmentRequest request) {
        try {
            Optional<Department> departmentOptional = departmentRepository.findById(id);

            if (!departmentOptional.isPresent()) {
                return new ApiResponse("Department not found", false);
            }

            Department department = departmentOptional.get();

            // Check if new name already exists (excluding current department)
            if (!department.getName().equals(request.getName()) && 
                departmentRepository.existsByName(request.getName())) {
                return new ApiResponse("Department name already exists", false);
            }

            // Update fields
            department.setName(request.getName());
            department.setDescription(request.getDescription());
            department.setPhone(request.getPhone());
            if (request.getIsActive() != null) {
                department.setIsActive(request.getIsActive());
            }

            departmentRepository.save(department);

            return new ApiResponse("Department updated successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to update department: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a department (soft delete - just mark as inactive)
     */
    public ApiResponse deleteDepartment(Long id) {
        try {
            Optional<Department> departmentOptional = departmentRepository.findById(id);

            if (!departmentOptional.isPresent()) {
                return new ApiResponse("Department not found", false);
            }

            Department department = departmentOptional.get();
            department.setIsActive(false);
            departmentRepository.save(department);

            return new ApiResponse("Department deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete department: " + e.getMessage(), false);
        }
    }
}
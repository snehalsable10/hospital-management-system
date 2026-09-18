package com.hms.service;

import com.hms.dto.request.DepartmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Department;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.DepartmentRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Get all departments
     * Cached for 30 minutes
     */
    @Cacheable(value = "departments", key = "'getAllDepartments'")
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    /**
     * Get department by ID
     * Cached for 30 minutes with key = department ID
     */
    @Cacheable(value = "department", key = "#id")
    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    /**
     * Create a new department
     * Clears all department caches on create
     *
     * @throws DuplicateResourceException if the department name is taken
     */
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    public ApiResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department name already exists: " + request.getName());
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setPhone(request.getPhone());
        department.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        departmentRepository.save(department);

        return new ApiResponse("Department created successfully", true);
    }

    /**
     * Update an existing department
     * Clears all department caches on update
     *
     * @throws ResourceNotFoundException  if no department exists with the given id
     * @throws DuplicateResourceException if the new name belongs to another department
     */
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    public ApiResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!department.getName().equals(request.getName()) &&
            departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department name already exists: " + request.getName());
        }

        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setPhone(request.getPhone());
        if (request.getIsActive() != null) {
            department.setIsActive(request.getIsActive());
        }

        departmentRepository.save(department);

        return new ApiResponse("Department updated successfully", true);
    }

    /**
     * Delete a department (soft delete - just mark as inactive)
     * Clears all department caches on delete
     *
     * @throws ResourceNotFoundException if no department exists with the given id
     */
    @CacheEvict(value = {"departments", "department"}, allEntries = true)
    public ApiResponse deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        department.setIsActive(false);
        departmentRepository.save(department);

        return new ApiResponse("Department deleted successfully", true);
    }

    /**
     * Get all departments with pagination
     */
    public PageResponse<Department> getAllDepartmentsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Department> page = departmentRepository.findAll(pageable);
        return PaginationUtil.toPageResponse(page);
    }
}
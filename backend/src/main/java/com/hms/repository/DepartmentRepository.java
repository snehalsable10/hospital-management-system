package com.hms.repository;

import com.hms.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    boolean existsByName(String name);

    // Soft delete: lists must not show records flagged inactive
    List<Department> findByIsActiveTrue();
    Page<Department> findByIsActiveTrue(Pageable pageable);
}

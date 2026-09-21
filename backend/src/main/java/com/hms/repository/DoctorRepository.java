package com.hms.repository;

import com.hms.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByLicenseNumber(String licenseNumber);
    List<Doctor> findBySpecializationAndIsActiveTrue(String specialization);
    List<Doctor> findByDepartmentIdAndIsActiveTrue(Long departmentId);
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);

    // A login account may own at most one doctor record
    Optional<Doctor> findByUserId(Long userId);

    // Paginated methods for performance optimization
    Page<Doctor> findBySpecializationAndIsActiveTrue(String specialization, Pageable pageable);
    Page<Doctor> findByDepartmentIdAndIsActiveTrue(Long departmentId, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Doctor> findByIsActiveTrue();
    Page<Doctor> findByIsActiveTrue(Pageable pageable);

    // --- dashboard aggregates ---

    long countByIsActiveTrue();

    /**
     * Doctor headcount per department. Left join so a doctor with no
     * department still appears, grouped under a null name the service
     * relabels.
     */
    @Query("select dep.name, count(d) from Doctor d left join d.department dep "
            + "where d.isActive = true group by dep.name")
    List<Object[]> countGroupedByDepartment();

}
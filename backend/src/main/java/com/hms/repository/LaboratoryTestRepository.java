package com.hms.repository;

import com.hms.entity.LaboratoryTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LaboratoryTestRepository extends JpaRepository<LaboratoryTest, Long> {
    List<LaboratoryTest> findByPatientIdAndIsActiveTrue(Long patientId);
    List<LaboratoryTest> findByTestNameIgnoreCaseContainingAndIsActiveTrue(String testName);
    List<LaboratoryTest> findByStatusAndIsActiveTrue(String status);
    List<LaboratoryTest> findByTestDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate);
    List<LaboratoryTest> findByPatientIdAndStatusAndIsActiveTrue(Long patientId, String status);
    List<LaboratoryTest> findByPatientIdAndTestDateBetweenAndIsActiveTrue(Long patientId, LocalDate startDate, LocalDate endDate);

    // Paginated methods for performance optimization
    Page<LaboratoryTest> findByPatientIdAndIsActiveTrue(Long patientId, Pageable pageable);
    Page<LaboratoryTest> findByTestNameIgnoreCaseContainingAndIsActiveTrue(String testName, Pageable pageable);
    Page<LaboratoryTest> findByStatusAndIsActiveTrue(String status, Pageable pageable);
    Page<LaboratoryTest> findByTestDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<LaboratoryTest> findByIsActiveTrue();
    Page<LaboratoryTest> findByIsActiveTrue(Pageable pageable);
}
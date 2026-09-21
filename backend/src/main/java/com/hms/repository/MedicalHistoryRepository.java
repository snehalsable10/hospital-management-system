package com.hms.repository;

import com.hms.entity.MedicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatientIdAndIsActiveTrue(Long patientId);
    List<MedicalHistory> findByStatusAndIsActiveTrue(String status);
    List<MedicalHistory> findByConditionNameIgnoreCaseContainingAndIsActiveTrue(String conditionName);
    List<MedicalHistory> findByPatientIdAndStatusAndIsActiveTrue(Long patientId, String status);

    // Paginated methods for performance optimization
    Page<MedicalHistory> findByPatientIdAndIsActiveTrue(Long patientId, Pageable pageable);
    Page<MedicalHistory> findByStatusAndIsActiveTrue(String status, Pageable pageable);
    Page<MedicalHistory> findByConditionNameIgnoreCaseContainingAndIsActiveTrue(String conditionName, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<MedicalHistory> findByIsActiveTrue();
    Page<MedicalHistory> findByIsActiveTrue(Pageable pageable);
}
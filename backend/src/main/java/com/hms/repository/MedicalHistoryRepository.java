package com.hms.repository;

import com.hms.entity.MedicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatientId(Long patientId);
    List<MedicalHistory> findByStatus(String status);
    List<MedicalHistory> findByConditionNameIgnoreCaseContaining(String conditionName);
    List<MedicalHistory> findByPatientIdAndStatus(Long patientId, String status);

    // Paginated methods for performance optimization
    Page<MedicalHistory> findByPatientId(Long patientId, Pageable pageable);
    Page<MedicalHistory> findByStatus(String status, Pageable pageable);
    Page<MedicalHistory> findByConditionNameIgnoreCaseContaining(String conditionName, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<MedicalHistory> findByIsActiveTrue();
    Page<MedicalHistory> findByIsActiveTrue(Pageable pageable);
}
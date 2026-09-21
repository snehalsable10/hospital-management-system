package com.hms.repository;

import com.hms.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPatientIdAndIsActiveTrue(Long patientId);
    List<Bill> findByDoctorIdAndIsActiveTrue(Long doctorId);
    List<Bill> findByStatusAndIsActiveTrue(String status);
    List<Bill> findByBillDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate);
    List<Bill> findByPatientIdAndStatusAndIsActiveTrue(Long patientId, String status);
    List<Bill> findByDoctorIdAndStatusAndIsActiveTrue(Long doctorId, String status);

    // Paginated methods for performance optimization
    Page<Bill> findByPatientIdAndIsActiveTrue(Long patientId, Pageable pageable);
    Page<Bill> findByDoctorIdAndIsActiveTrue(Long doctorId, Pageable pageable);
    Page<Bill> findByStatusAndIsActiveTrue(String status, Pageable pageable);
    Page<Bill> findByBillDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<Bill> findByPatientIdAndStatusAndIsActiveTrue(Long patientId, String status, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Bill> findByIsActiveTrue();
    Page<Bill> findByIsActiveTrue(Pageable pageable);
}
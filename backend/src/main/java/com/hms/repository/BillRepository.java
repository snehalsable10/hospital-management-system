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
    List<Bill> findByPatientId(Long patientId);
    List<Bill> findByDoctorId(Long doctorId);
    List<Bill> findByStatus(String status);
    List<Bill> findByBillDateBetween(LocalDate startDate, LocalDate endDate);
    List<Bill> findByPatientIdAndStatus(Long patientId, String status);
    List<Bill> findByDoctorIdAndStatus(Long doctorId, String status);

    // Paginated methods for performance optimization
    Page<Bill> findByPatientId(Long patientId, Pageable pageable);
    Page<Bill> findByDoctorId(Long doctorId, Pageable pageable);
    Page<Bill> findByStatus(String status, Pageable pageable);
    Page<Bill> findByBillDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<Bill> findByPatientIdAndStatus(Long patientId, String status, Pageable pageable);
}
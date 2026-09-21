package com.hms.repository;

import com.hms.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByAppointmentIdAndIsActiveTrue(Long appointmentId);
    List<Prescription> findByPatientIdAndIsActiveTrue(Long patientId);
    List<Prescription> findByDoctorIdAndIsActiveTrue(Long doctorId);
    List<Prescription> findByStatusAndIsActiveTrue(String status);
    List<Prescription> findByPatientIdAndStatusAndIsActiveTrue(Long patientId, String status);
    List<Prescription> findByDoctorIdAndStatusAndIsActiveTrue(Long doctorId, String status);

    // Paginated methods for performance optimization
    Page<Prescription> findByAppointmentIdAndIsActiveTrue(Long appointmentId, Pageable pageable);
    Page<Prescription> findByPatientIdAndIsActiveTrue(Long patientId, Pageable pageable);
    Page<Prescription> findByDoctorIdAndIsActiveTrue(Long doctorId, Pageable pageable);
    Page<Prescription> findByStatusAndIsActiveTrue(String status, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Prescription> findByIsActiveTrue();
    Page<Prescription> findByIsActiveTrue(Pageable pageable);
}
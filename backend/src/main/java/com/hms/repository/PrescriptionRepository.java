package com.hms.repository;

import com.hms.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByAppointmentId(Long appointmentId);
    List<Prescription> findByPatientId(Long patientId);
    List<Prescription> findByDoctorId(Long doctorId);
    List<Prescription> findByStatus(String status);
    List<Prescription> findByPatientIdAndStatus(Long patientId, String status);
    List<Prescription> findByDoctorIdAndStatus(Long doctorId, String status);

    // Paginated methods for performance optimization
    Page<Prescription> findByAppointmentId(Long appointmentId, Pageable pageable);
    Page<Prescription> findByPatientId(Long patientId, Pageable pageable);
    Page<Prescription> findByDoctorId(Long doctorId, Pageable pageable);
    Page<Prescription> findByStatus(String status, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Prescription> findByIsActiveTrue();
    Page<Prescription> findByIsActiveTrue(Pageable pageable);
}
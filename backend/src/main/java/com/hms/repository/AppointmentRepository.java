package com.hms.repository;

import com.hms.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdAndIsActiveTrue(Long patientId);
    List<Appointment> findByDoctorIdAndIsActiveTrue(Long doctorId);
    List<Appointment> findByStatusAndIsActiveTrue(String status);
    List<Appointment> findByAppointmentDateAndIsActiveTrue(LocalDate appointmentDate);
    List<Appointment> findByAppointmentDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate);
    List<Appointment> findByPatientIdAndStatusAndIsActiveTrue(Long patientId, String status);
    List<Appointment> findByDoctorIdAndStatusAndIsActiveTrue(Long doctorId, String status);

    // Paginated methods for performance optimization
    Page<Appointment> findByPatientIdAndIsActiveTrue(Long patientId, Pageable pageable);
    Page<Appointment> findByDoctorIdAndIsActiveTrue(Long doctorId, Pageable pageable);
    Page<Appointment> findByStatusAndIsActiveTrue(String status, Pageable pageable);
    Page<Appointment> findByAppointmentDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Appointment> findByIsActiveTrue();
    Page<Appointment> findByIsActiveTrue(Pageable pageable);

    // --- dashboard aggregates ---

    long countByIsActiveTrue();

    @Query("select a.status, count(a) from Appointment a where a.isActive = true group by a.status")
    List<Object[]> countGroupedByStatus();

    List<Appointment> findTop5ByIsActiveTrueOrderByAppointmentDateDescAppointmentTimeDesc();

}
package com.hms.repository;

import com.hms.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByStatus(String status);
    List<Appointment> findByAppointmentDate(LocalDate appointmentDate);
    List<Appointment> findByAppointmentDateBetween(LocalDate startDate, LocalDate endDate);
    List<Appointment> findByPatientIdAndStatus(Long patientId, String status);
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, String status);

    // Paginated methods for performance optimization
    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);
    Page<Appointment> findByStatus(String status, Pageable pageable);
    Page<Appointment> findByAppointmentDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
}
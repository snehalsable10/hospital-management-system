package com.hms.service;

import com.hms.dto.request.AppointmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Appointment;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.repository.AppointmentRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Get all appointments
     */
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    /**
     * Get appointment by ID
     */
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Get all appointments for a patient
     */
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    /**
     * Get all appointments for a doctor
     */
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    /**
     * Get appointments by status
     */
    public List<Appointment> getAppointmentsByStatus(String status) {
        return appointmentRepository.findByStatus(status);
    }

    /**
     * Get appointments in a date range
     */
    public List<Appointment> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByAppointmentDateBetween(startDate, endDate);
    }

    /**
     * Create a new appointment
     */
    public ApiResponse createAppointment(AppointmentRequest request) {
        try {
            // Validate patient exists
            Optional<Patient> patientOptional = patientRepository.findById(request.getPatientId());
            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            // Validate doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(request.getDoctorId());
            if (!doctorOptional.isPresent()) {
                return new ApiResponse("Doctor not found", false);
            }

            // Create new appointment
            Appointment appointment = new Appointment();
            appointment.setPatient(patientOptional.get());
            appointment.setDoctor(doctorOptional.get());
            appointment.setAppointmentDate(request.getAppointmentDate());
            appointment.setAppointmentTime(request.getAppointmentTime());
            appointment.setStatus(request.getStatus() != null ? request.getStatus() : "SCHEDULED");
            appointment.setReason(request.getReason());
            appointment.setNotes(request.getNotes());
            appointment.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            appointmentRepository.save(appointment);

            return new ApiResponse("Appointment created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create appointment: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing appointment
     */
    public ApiResponse updateAppointment(Long id, AppointmentRequest request) {
        try {
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(id);

            if (!appointmentOptional.isPresent()) {
                return new ApiResponse("Appointment not found", false);
            }

            Appointment appointment = appointmentOptional.get();

            // Validate patient exists
            Optional<Patient> patientOptional = patientRepository.findById(request.getPatientId());
            if (!patientOptional.isPresent()) {
                return new ApiResponse("Patient not found", false);
            }

            // Validate doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(request.getDoctorId());
            if (!doctorOptional.isPresent()) {
                return new ApiResponse("Doctor not found", false);
            }

            // Update fields
            appointment.setPatient(patientOptional.get());
            appointment.setDoctor(doctorOptional.get());
            appointment.setAppointmentDate(request.getAppointmentDate());
            appointment.setAppointmentTime(request.getAppointmentTime());
            appointment.setStatus(request.getStatus() != null ? request.getStatus() : "SCHEDULED");
            appointment.setReason(request.getReason());
            appointment.setNotes(request.getNotes());
            if (request.getIsActive() != null) {
                appointment.setIsActive(request.getIsActive());
            }

            appointmentRepository.save(appointment);

            return new ApiResponse("Appointment updated successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to update appointment: " + e.getMessage(), false);
        }
    }

    /**
     * Delete an appointment (soft delete)
     */
    public ApiResponse deleteAppointment(Long id) {
        try {
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(id);

            if (!appointmentOptional.isPresent()) {
                return new ApiResponse("Appointment not found", false);
            }

            Appointment appointment = appointmentOptional.get();
            appointment.setIsActive(false);
            appointmentRepository.save(appointment);

            return new ApiResponse("Appointment deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete appointment: " + e.getMessage(), false);
        }
    }
}
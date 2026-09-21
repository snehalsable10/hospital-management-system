package com.hms.service;

import com.hms.dto.request.AppointmentRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Appointment;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.AppointmentRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Cached for 5 minutes
     */
    @Cacheable(value = "appointments", key = "'getAllAppointments'")
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findByIsActiveTrue();
    }

    /**
     * Get appointment by ID
     * Cached for 5 minutes with key = appointment ID
     */
    @Cacheable(value = "appointment", key = "#id")
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Get all appointments for a patient
     * Cached for 5 minutes
     */
    @Cacheable(value = "appointments", key = "'getAppointmentsByPatient:' + #patientId")
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdAndIsActiveTrue(patientId);
    }

    /**
     * Get all appointments for a doctor
     * Cached for 5 minutes
     */
    @Cacheable(value = "appointments", key = "'getAppointmentsByDoctor:' + #doctorId")
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorIdAndIsActiveTrue(doctorId);
    }

    /**
     * Get appointments by status
     * Cached for 5 minutes
     */
    @Cacheable(value = "appointments", key = "'getAppointmentsByStatus:' + #status")
    public List<Appointment> getAppointmentsByStatus(String status) {
        return appointmentRepository.findByStatusAndIsActiveTrue(status);
    }

    /**
     * Get appointments in a date range
     * Cached for 5 minutes
     */
    @Cacheable(value = "appointments", key = "'getAppointmentsByDateRange:' + #startDate + ':' + #endDate")
    public List<Appointment> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByAppointmentDateBetweenAndIsActiveTrue(startDate, endDate);
    }

    /**
     * Create a new appointment
     * Clears all appointment caches on create
     *
     * @throws IllegalArgumentException if the referenced patient or doctor does not exist
     */
    @CacheEvict(value = {"appointments", "appointment"}, allEntries = true)
    public ApiResponse createAppointment(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + request.getDoctorId()));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(request.getStatus() != null ? request.getStatus() : "SCHEDULED");
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());
        appointment.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        appointmentRepository.save(appointment);

        return new ApiResponse("Appointment created successfully", true);
    }

    /**
     * Update an existing appointment
     * Clears all appointment caches on update
     *
     * @throws ResourceNotFoundException if no appointment exists with the given id
     * @throws IllegalArgumentException  if the referenced patient or doctor does not exist
     */
    @CacheEvict(value = {"appointments", "appointment"}, allEntries = true)
    public ApiResponse updateAppointment(Long id, AppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + request.getDoctorId()));

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
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
    }

    /**
     * Delete an appointment (soft delete)
     * Clears all appointment caches on delete
     *
     * @throws ResourceNotFoundException if no appointment exists with the given id
     */
    @CacheEvict(value = {"appointments", "appointment"}, allEntries = true)
    public ApiResponse deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setIsActive(false);
        appointmentRepository.save(appointment);

        return new ApiResponse("Appointment deleted successfully", true);
    }

    /**
     * Get all appointments with pagination
     */
    public PageResponse<Appointment> getAllAppointmentsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Appointment> page = appointmentRepository.findByIsActiveTrue(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get appointments by patient with pagination
     */
    public PageResponse<Appointment> getAppointmentsByPatientPaginated(Long patientId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Appointment> page = appointmentRepository.findByPatientIdAndIsActiveTrue(patientId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get appointments by doctor with pagination
     */
    public PageResponse<Appointment> getAppointmentsByDoctorPaginated(Long doctorId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Appointment> page = appointmentRepository.findByDoctorIdAndIsActiveTrue(doctorId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get appointments by status with pagination
     */
    public PageResponse<Appointment> getAppointmentsByStatusPaginated(String status, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Appointment> page = appointmentRepository.findByStatusAndIsActiveTrue(status, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get appointments by date range with pagination
     */
    public PageResponse<Appointment> getAppointmentsByDateRangePaginated(LocalDate startDate, LocalDate endDate, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PaginationUtil.pageRequest(pageNumber, pageSize);
        Page<Appointment> page = appointmentRepository.findByAppointmentDateBetweenAndIsActiveTrue(startDate, endDate, pageable);
        return PaginationUtil.toPageResponse(page);
    }
}
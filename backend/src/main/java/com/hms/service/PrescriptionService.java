package com.hms.service;

import com.hms.dto.request.PrescriptionRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Appointment;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.entity.Prescription;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.AppointmentRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import com.hms.repository.PrescriptionRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Get all prescriptions
     * Cached for 10 minutes
     */
    @Cacheable(value = "prescriptions", key = "'getAllPrescriptions'")
    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findByIsActiveTrue();
    }

    /**
     * Get prescription by ID
     * Cached for 10 minutes with key = prescription ID
     */
    @Cacheable(value = "prescription", key = "#id")
    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    /**
     * Get all prescriptions for an appointment
     * Cached for 10 minutes
     */
    @Cacheable(value = "prescriptions", key = "'getPrescriptionsByAppointment:' + #appointmentId")
    public List<Prescription> getPrescriptionsByAppointment(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId);
    }

    /**
     * Get all prescriptions for a patient
     * Cached for 10 minutes
     */
    @Cacheable(value = "prescriptions", key = "'getPrescriptionsByPatient:' + #patientId")
    public List<Prescription> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }

    /**
     * Get all prescriptions written by a doctor
     * Cached for 10 minutes
     */
    @Cacheable(value = "prescriptions", key = "'getPrescriptionsByDoctor:' + #doctorId")
    public List<Prescription> getPrescriptionsByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId);
    }

    /**
     * Get prescriptions by status
     * Cached for 10 minutes
     */
    @Cacheable(value = "prescriptions", key = "'getPrescriptionsByStatus:' + #status")
    public List<Prescription> getPrescriptionsByStatus(String status) {
        return prescriptionRepository.findByStatus(status);
    }

    /**
     * Create a new prescription
     * Clears all prescription caches on create
     *
     * @throws IllegalArgumentException if the referenced appointment, patient or doctor does not exist
     */
    @CacheEvict(value = {"prescriptions", "prescription"}, allEntries = true)
    public ApiResponse createPrescription(PrescriptionRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + request.getAppointmentId()));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + request.getDoctorId()));

        Prescription prescription = new Prescription();
        prescription.setAppointment(appointment);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setMedicineName(request.getMedicineName());
        prescription.setDosage(request.getDosage());
        prescription.setFrequency(request.getFrequency());
        prescription.setDuration(request.getDuration());
        prescription.setInstructions(request.getInstructions());
        prescription.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        prescription.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        prescriptionRepository.save(prescription);

        return new ApiResponse("Prescription created successfully", true);
    }

    /**
     * Update an existing prescription
     * Clears all prescription caches on update
     *
     * @throws ResourceNotFoundException if no prescription exists with the given id
     * @throws IllegalArgumentException  if the referenced appointment, patient or doctor does not exist
     */
    @CacheEvict(value = {"prescriptions", "prescription"}, allEntries = true)
    public ApiResponse updatePrescription(Long id, PrescriptionRequest request) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + request.getAppointmentId()));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + request.getDoctorId()));

        prescription.setAppointment(appointment);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setMedicineName(request.getMedicineName());
        prescription.setDosage(request.getDosage());
        prescription.setFrequency(request.getFrequency());
        prescription.setDuration(request.getDuration());
        prescription.setInstructions(request.getInstructions());
        prescription.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        if (request.getIsActive() != null) {
            prescription.setIsActive(request.getIsActive());
        }

        prescriptionRepository.save(prescription);

        return new ApiResponse("Prescription updated successfully", true);
    }

    /**
     * Delete a prescription (soft delete)
     * Clears all prescription caches on delete
     *
     * @throws ResourceNotFoundException if no prescription exists with the given id
     */
    @CacheEvict(value = {"prescriptions", "prescription"}, allEntries = true)
    public ApiResponse deletePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));

        prescription.setIsActive(false);
        prescriptionRepository.save(prescription);

        return new ApiResponse("Prescription deleted successfully", true);
    }

    /**
     * Get all prescriptions with pagination
     */
    public PageResponse<Prescription> getAllPrescriptionsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Prescription> page = prescriptionRepository.findByIsActiveTrue(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get prescriptions by appointment with pagination
     */
    public PageResponse<Prescription> getPrescriptionsByAppointmentPaginated(Long appointmentId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Prescription> page = prescriptionRepository.findByAppointmentId(appointmentId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get prescriptions by patient with pagination
     */
    public PageResponse<Prescription> getPrescriptionsByPatientPaginated(Long patientId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Prescription> page = prescriptionRepository.findByPatientId(patientId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get prescriptions by doctor with pagination
     */
    public PageResponse<Prescription> getPrescriptionsByDoctorPaginated(Long doctorId, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Prescription> page = prescriptionRepository.findByDoctorId(doctorId, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get prescriptions by status with pagination
     */
    public PageResponse<Prescription> getPrescriptionsByStatusPaginated(String status, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Prescription> page = prescriptionRepository.findByStatus(status, pageable);
        return PaginationUtil.toPageResponse(page);
    }
}
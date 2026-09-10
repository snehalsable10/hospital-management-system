package com.hms.service;

import com.hms.dto.request.PrescriptionRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Appointment;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import com.hms.entity.Prescription;
import com.hms.repository.AppointmentRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import com.hms.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
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
     */
    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    /**
     * Get prescription by ID
     */
    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    /**
     * Get all prescriptions for an appointment
     */
    public List<Prescription> getPrescriptionsByAppointment(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId);
    }

    /**
     * Get all prescriptions for a patient
     */
    public List<Prescription> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }

    /**
     * Get all prescriptions written by a doctor
     */
    public List<Prescription> getPrescriptionsByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId);
    }

    /**
     * Get prescriptions by status
     */
    public List<Prescription> getPrescriptionsByStatus(String status) {
        return prescriptionRepository.findByStatus(status);
    }

    /**
     * Create a new prescription
     */
    public ApiResponse createPrescription(PrescriptionRequest request) {
        try {
            // Validate appointment exists
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(request.getAppointmentId());
            if (!appointmentOptional.isPresent()) {
                return new ApiResponse("Appointment not found", false);
            }

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

            // Create new prescription
            Prescription prescription = new Prescription();
            prescription.setAppointment(appointmentOptional.get());
            prescription.setPatient(patientOptional.get());
            prescription.setDoctor(doctorOptional.get());
            prescription.setMedicineName(request.getMedicineName());
            prescription.setDosage(request.getDosage());
            prescription.setFrequency(request.getFrequency());
            prescription.setDuration(request.getDuration());
            prescription.setInstructions(request.getInstructions());
            prescription.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
            prescription.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

            prescriptionRepository.save(prescription);

            return new ApiResponse("Prescription created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create prescription: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing prescription
     */
    public ApiResponse updatePrescription(Long id, PrescriptionRequest request) {
        try {
            Optional<Prescription> prescriptionOptional = prescriptionRepository.findById(id);

            if (!prescriptionOptional.isPresent()) {
                return new ApiResponse("Prescription not found", false);
            }

            Prescription prescription = prescriptionOptional.get();

            // Validate appointment exists
            Optional<Appointment> appointmentOptional = appointmentRepository.findById(request.getAppointmentId());
            if (!appointmentOptional.isPresent()) {
                return new ApiResponse("Appointment not found", false);
            }

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
            prescription.setAppointment(appointmentOptional.get());
            prescription.setPatient(patientOptional.get());
            prescription.setDoctor(doctorOptional.get());
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
        } catch (Exception e) {
            return new ApiResponse("Failed to update prescription: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a prescription (soft delete)
     */
    public ApiResponse deletePrescription(Long id) {
        try {
            Optional<Prescription> prescriptionOptional = prescriptionRepository.findById(id);

            if (!prescriptionOptional.isPresent()) {
                return new ApiResponse("Prescription not found", false);
            }

            Prescription prescription = prescriptionOptional.get();
            prescription.setIsActive(false);
            prescriptionRepository.save(prescription);

            return new ApiResponse("Prescription deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete prescription: " + e.getMessage(), false);
        }
    }
}
package com.hms.security;

import com.hms.repository.AppointmentRepository;
import com.hms.repository.BillRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.LaboratoryTestRepository;
import com.hms.repository.MedicalHistoryRepository;
import com.hms.repository.PatientRepository;
import com.hms.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Ownership checks referenced from @PreAuthorize as @authService.
 *
 * Role checks (hasAnyRole) answer "may this kind of user touch this kind of
 * record"; these answer "is this particular record theirs". Every method fails
 * closed: if the caller is anonymous, the record is missing, or the record has
 * no linked login account, access is denied.
 *
 * JwtAuthenticationFilter puts the numeric user id in the principal, which is
 * what these compare against.
 */
@Service("authService")
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final LaboratoryTestRepository laboratoryTestRepository;

    /** True when the patient record belongs to the calling user. */
    public boolean isOwnPatient(Long patientId) {
        Long userId = currentUserId();
        if (userId == null || patientId == null) {
            return false;
        }
        return patientRepository.findById(patientId)
                .map(patient -> ownedBy(userId, patient.getUser()))
                .orElse(false);
    }

    /** True when the doctor record belongs to the calling user. */
    public boolean isOwnDoctor(Long doctorId) {
        Long userId = currentUserId();
        if (userId == null || doctorId == null) {
            return false;
        }
        return doctorRepository.findById(doctorId)
                .map(doctor -> ownedBy(userId, doctor.getUser()))
                .orElse(false);
    }

    /** True when the caller is either the patient or the doctor on the appointment. */
    public boolean isAppointmentParticipant(Long appointmentId) {
        Long userId = currentUserId();
        if (userId == null || appointmentId == null) {
            return false;
        }
        return appointmentRepository.findById(appointmentId)
                .map(appointment ->
                        ownedBy(userId, appointment.getPatient() == null ? null : appointment.getPatient().getUser())
                     || ownedBy(userId, appointment.getDoctor() == null ? null : appointment.getDoctor().getUser()))
                .orElse(false);
    }

    /** True when the bill belongs to the calling patient. */
    public boolean isOwnBill(Long billId) {
        Long userId = currentUserId();
        if (userId == null || billId == null) {
            return false;
        }
        return billRepository.findById(billId)
                .map(bill -> ownedBy(userId, bill.getPatient() == null ? null : bill.getPatient().getUser()))
                .orElse(false);
    }

    /** True when the caller is the prescribing doctor or the patient it was written for. */
    public boolean isPrescriptionOwner(Long prescriptionId) {
        Long userId = currentUserId();
        if (userId == null || prescriptionId == null) {
            return false;
        }
        return prescriptionRepository.findById(prescriptionId)
                .map(prescription ->
                        ownedBy(userId, prescription.getPatient() == null ? null : prescription.getPatient().getUser())
                     || ownedBy(userId, prescription.getDoctor() == null ? null : prescription.getDoctor().getUser()))
                .orElse(false);
    }

    /** True when the medical history entry belongs to the calling patient. */
    public boolean isMedicalHistoryOwner(Long medicalHistoryId) {
        Long userId = currentUserId();
        if (userId == null || medicalHistoryId == null) {
            return false;
        }
        return medicalHistoryRepository.findById(medicalHistoryId)
                .map(history -> ownedBy(userId, history.getPatient() == null ? null : history.getPatient().getUser()))
                .orElse(false);
    }

    /** True when the laboratory test belongs to the calling patient. */
    public boolean isLaboratoryTestOwner(Long laboratoryTestId) {
        Long userId = currentUserId();
        if (userId == null || laboratoryTestId == null) {
            return false;
        }
        return laboratoryTestRepository.findById(laboratoryTestId)
                .map(test -> ownedBy(userId, test.getPatient() == null ? null : test.getPatient().getUser()))
                .orElse(false);
    }

    private boolean ownedBy(Long userId, com.hms.entity.User owner) {
        return owner != null && userId.equals(owner.getId());
    }

    /**
     * The authenticated user's id, or null when the request is anonymous.
     * Returns null rather than throwing so a failed lookup denies access
     * instead of turning into a 500.
     */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        log.debug("Unexpected principal type for ownership check: {}",
                principal == null ? "null" : principal.getClass().getName());
        return null;
    }
}
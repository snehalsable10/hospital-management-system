package com.hms.dto.response;

import com.hms.entity.Appointment;
import com.hms.entity.Doctor;
import com.hms.entity.Patient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The small shapes a record uses to refer to another one.
 *
 * An appointment needs to say who it is for; it does not need to carry that
 * patient's home address, date of birth and emergency contacts, which is what
 * serialising the entity did - nineteen fields where four will do, handed to
 * anyone allowed to list appointments. Whoever needs the rest can ask
 * /api/patients/{id}, where the ownership rules are applied.
 */
public final class Summaries {

    private Summaries() {
    }

    /** Enough of a patient to name them and tell two apart. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;

        public static PatientSummary from(Patient patient) {
            return patient == null ? null : new PatientSummary(
                    patient.getId(), patient.getFirstName(),
                    patient.getLastName(), patient.getEmail());
        }
    }

    /** Enough of a doctor to name them, plus what they practise. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String specialization;

        public static DoctorSummary from(Doctor doctor) {
            return doctor == null ? null : new DoctorSummary(
                    doctor.getId(), doctor.getFirstName(), doctor.getLastName(),
                    doctor.getEmail(), doctor.getSpecialization());
        }
    }

    /**
     * Enough of an appointment to identify the visit a prescription was
     * written at, without repeating its patient and doctor - the prescription
     * already carries those itself.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentSummary {
        private Long id;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String status;

        public static AppointmentSummary from(Appointment appointment) {
            return appointment == null ? null : new AppointmentSummary(
                    appointment.getId(), appointment.getAppointmentDate(),
                    appointment.getAppointmentTime(), appointment.getStatus());
        }
    }
}

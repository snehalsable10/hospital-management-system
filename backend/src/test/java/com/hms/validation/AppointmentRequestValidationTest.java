package com.hms.validation;

import com.hms.dto.request.AppointmentRequest;
import com.hms.dto.request.OnCreate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The appointment date constraint has to fire on create and stay quiet on
 * update.
 *
 * It used to apply to both, which meant that the moment an appointment's date
 * passed, nobody could edit it - including to mark it COMPLETED, the single
 * most routine thing anyone does to an appointment.
 */
class AppointmentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private AppointmentRequest requestDated(LocalDate date) {
        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(1L);
        request.setDoctorId(2L);
        request.setAppointmentDate(date);
        request.setAppointmentTime(LocalTime.of(10, 30));
        request.setStatus("SCHEDULED");
        request.setReason("Follow-up on blood pressure");
        return request;
    }

    private Set<String> violatedFields(AppointmentRequest request, Class<?>... groups) {
        Set<ConstraintViolation<AppointmentRequest>> violations = validator.validate(request, groups);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("booking in the past is rejected on create")
    void pastDateRejectedOnCreate() {
        assertThat(violatedFields(requestDated(LocalDate.now().minusDays(1)),
                Default.class, OnCreate.class))
                .contains("appointmentDate");
    }

    @Test
    @DisplayName("editing a past appointment is allowed - the date check is create-only")
    void pastDateAllowedOnUpdate() {
        assertThat(violatedFields(requestDated(LocalDate.now().minusDays(1)), Default.class))
                .doesNotContain("appointmentDate");
    }

    @Test
    @DisplayName("today and future dates are fine on create")
    void presentAndFutureAcceptedOnCreate() {
        assertThat(violatedFields(requestDated(LocalDate.now()), Default.class, OnCreate.class))
                .isEmpty();
        assertThat(violatedFields(requestDated(LocalDate.now().plusMonths(1)),
                Default.class, OnCreate.class))
                .isEmpty();
    }

    @Test
    @DisplayName("the other field rules still apply on update, only the date is exempt")
    void otherConstraintsStillApplyOnUpdate() {
        AppointmentRequest request = requestDated(LocalDate.now().minusYears(1));
        request.setReason("ab");
        request.setPatientId(null);

        assertThat(violatedFields(request, Default.class))
                .contains("reason", "patientId")
                .doesNotContain("appointmentDate");
    }

    @Test
    @DisplayName("the other field rules also still apply on create")
    void otherConstraintsApplyOnCreate() {
        AppointmentRequest request = requestDated(LocalDate.now().minusYears(1));
        request.setReason("ab");

        assertThat(violatedFields(request, Default.class, OnCreate.class))
                .contains("reason", "appointmentDate");
    }
}

package com.hms.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotBlank(message = "Medicine name is required")
    @Size(min = 2, max = 100, message = "Medicine name must be between 2 and 100 characters")
    private String medicineName;

    @NotBlank(message = "Dosage is required")
    @Size(min = 2, max = 50, message = "Dosage must be between 2 and 50 characters")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    @Size(min = 3, max = 50, message = "Frequency must be between 3 and 50 characters")
    private String frequency;

    @NotBlank(message = "Duration is required")
    @Size(min = 2, max = 50, message = "Duration must be between 2 and 50 characters")
    private String duration;

    @Size(max = 255, message = "Instructions must not exceed 255 characters")
    private String instructions;

    @NotBlank(message = "Status is required")
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status = "ACTIVE";

    private Boolean isActive = true;
}
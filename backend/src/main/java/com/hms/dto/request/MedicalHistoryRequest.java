package com.hms.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Condition name is required")
    @Size(min = 2, max = 100, message = "Condition name must be between 2 and 100 characters")
    private String conditionName;

    @NotNull(message = "Diagnosis date is required")
    @PastOrPresent(message = "Diagnosis date cannot be in the future")
    private LocalDate diagnosisDate;

    @NotBlank(message = "Status is required")
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status = "ACTIVE";

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Treatment must not exceed 500 characters")
    private String treatment;

    @Size(max = 500, message = "Doctor notes must not exceed 500 characters")
    private String doctorNotes;

    private Boolean isActive = true;
}
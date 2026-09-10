package com.hms.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratoryTestRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Test name is required")
    @Size(min = 2, max = 100, message = "Test name must be between 2 and 100 characters")
    private String testName;

    @NotNull(message = "Test date is required")
    @PastOrPresent(message = "Test date cannot be in the future")
    private LocalDate testDate;

    @NotBlank(message = "Result value is required")
    @Size(min = 1, max = 50, message = "Result value must not exceed 50 characters")
    private String resultValue;

    @NotBlank(message = "Result unit is required")
    @Size(min = 1, max = 50, message = "Result unit must not exceed 50 characters")
    private String resultUnit;

    @Size(max = 50, message = "Reference min must not exceed 50 characters")
    private String referenceMin;

    @Size(max = 50, message = "Reference max must not exceed 50 characters")
    private String referenceMax;

    @NotBlank(message = "Status is required")
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status = "NORMAL";

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private Boolean isActive = true;
}
package com.hms.dto.response;

import com.hms.entity.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A prescription as the API presents it.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {

    private Long id;
    private Summaries.AppointmentSummary appointment;
    private Summaries.PatientSummary patient;
    private Summaries.DoctorSummary doctor;
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private String status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PrescriptionResponse from(Prescription source) {
        if (source == null) {
            return null;
        }
        return new PrescriptionResponse(
                source.getId(),
                Summaries.AppointmentSummary.from(source.getAppointment()),
                Summaries.PatientSummary.from(source.getPatient()),
                Summaries.DoctorSummary.from(source.getDoctor()),
                source.getMedicineName(),
                source.getDosage(),
                source.getFrequency(),
                source.getDuration(),
                source.getInstructions(),
                source.getStatus(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<PrescriptionResponse> from(List<Prescription> sources) {
        return sources == null ? List.of()
                : sources.stream().map(PrescriptionResponse::from).toList();
    }

    public static PageResponse<PrescriptionResponse> from(PageResponse<Prescription> page) {
        return new PageResponse<>(
                from(page.getContent()),
                page.getPageNumber(),
                page.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isHasNext(),
                page.isHasPrevious());
    }
}

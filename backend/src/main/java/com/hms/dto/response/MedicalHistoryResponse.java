package com.hms.dto.response;

import com.hms.entity.MedicalHistory;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A diagnosed condition as the API presents it.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryResponse {

    private Long id;
    private Summaries.PatientSummary patient;
    private String conditionName;
    private LocalDate diagnosisDate;
    private String status;
    private String description;
    private String treatment;
    private String doctorNotes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MedicalHistoryResponse from(MedicalHistory source) {
        if (source == null) {
            return null;
        }
        return new MedicalHistoryResponse(
                source.getId(),
                Summaries.PatientSummary.from(source.getPatient()),
                source.getConditionName(),
                source.getDiagnosisDate(),
                source.getStatus(),
                source.getDescription(),
                source.getTreatment(),
                source.getDoctorNotes(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<MedicalHistoryResponse> from(List<MedicalHistory> sources) {
        return sources == null ? List.of()
                : sources.stream().map(MedicalHistoryResponse::from).toList();
    }

    public static PageResponse<MedicalHistoryResponse> from(PageResponse<MedicalHistory> page) {
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

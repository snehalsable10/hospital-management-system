package com.hms.dto.response;

import com.hms.entity.LaboratoryTest;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A laboratory result as the API presents it.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratoryTestResponse {

    private Long id;
    private Summaries.PatientSummary patient;
    private String testName;
    private LocalDate testDate;
    private String resultValue;
    private String resultUnit;
    private String referenceMin;
    private String referenceMax;
    private String status;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LaboratoryTestResponse from(LaboratoryTest source) {
        if (source == null) {
            return null;
        }
        return new LaboratoryTestResponse(
                source.getId(),
                Summaries.PatientSummary.from(source.getPatient()),
                source.getTestName(),
                source.getTestDate(),
                source.getResultValue(),
                source.getResultUnit(),
                source.getReferenceMin(),
                source.getReferenceMax(),
                source.getStatus(),
                source.getNotes(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<LaboratoryTestResponse> from(List<LaboratoryTest> sources) {
        return sources == null ? List.of()
                : sources.stream().map(LaboratoryTestResponse::from).toList();
    }

    public static PageResponse<LaboratoryTestResponse> from(PageResponse<LaboratoryTest> page) {
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

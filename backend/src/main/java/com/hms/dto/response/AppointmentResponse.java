package com.hms.dto.response;

import com.hms.entity.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An appointment as the API presents it.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private Summaries.PatientSummary patient;
    private Summaries.DoctorSummary doctor;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private String reason;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AppointmentResponse from(Appointment source) {
        if (source == null) {
            return null;
        }
        return new AppointmentResponse(
                source.getId(),
                Summaries.PatientSummary.from(source.getPatient()),
                Summaries.DoctorSummary.from(source.getDoctor()),
                source.getAppointmentDate(),
                source.getAppointmentTime(),
                source.getStatus(),
                source.getReason(),
                source.getNotes(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<AppointmentResponse> from(List<Appointment> sources) {
        return sources == null ? List.of()
                : sources.stream().map(AppointmentResponse::from).toList();
    }

    public static PageResponse<AppointmentResponse> from(PageResponse<Appointment> page) {
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

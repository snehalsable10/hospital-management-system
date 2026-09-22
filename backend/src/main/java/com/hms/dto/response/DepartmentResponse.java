package com.hms.dto.response;

import com.hms.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A department as the API presents it.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String description;
    private String phone;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DepartmentResponse from(Department source) {
        if (source == null) {
            return null;
        }
        return new DepartmentResponse(
                source.getId(),
                source.getName(),
                source.getDescription(),
                source.getPhone(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<DepartmentResponse> from(List<Department> sources) {
        return sources == null ? List.of()
                : sources.stream().map(DepartmentResponse::from).toList();
    }

    public static PageResponse<DepartmentResponse> from(PageResponse<Department> page) {
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

package com.hms.dto.response;

import com.hms.entity.Department;
import com.hms.entity.Doctor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A doctor as the API presents it. See PatientResponse for why the entity is
 * no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String specialization;
    private String licenseNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** The linked login's id, never the login itself - it holds the hash. */
    private Long userId;

    private DepartmentSummary department;

    /** Enough of the department to name it on a doctor record. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private Long id;
        private String name;

        static DepartmentSummary from(Department department) {
            return department == null ? null
                    : new DepartmentSummary(department.getId(), department.getName());
        }
    }

    public static DoctorResponse from(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        return new DoctorResponse(
                doctor.getId(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getEmail(),
                doctor.getPhone(),
                doctor.getSpecialization(),
                doctor.getLicenseNumber(),
                doctor.getIsActive(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt(),
                doctor.linkedUserId(),
                DepartmentSummary.from(doctor.getDepartment()));
    }

    public static List<DoctorResponse> from(List<Doctor> doctors) {
        return doctors == null ? List.of() : doctors.stream().map(DoctorResponse::from).toList();
    }

    public static PageResponse<DoctorResponse> from(PageResponse<Doctor> page) {
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

package com.hms.dto.response;

import com.hms.entity.Patient;
import com.hms.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A patient as the API presents it.
 *
 * The entity was being serialised straight onto responses, which made the JPA
 * model the public contract: a field added for persistence appeared in the API
 * the moment it was declared, and keeping the password hash and the lazy
 * collections out depended on remembering a @JsonIgnore in the right place.
 * That is a rule enforced by vigilance, and it only has to be forgotten once.
 *
 * Listing the fields here inverts that. Nothing reaches a client unless it is
 * named below, so adding a column to the entity changes nothing until somebody
 * decides it should.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String bloodGroup;
    private String emergencyContact;
    private String emergencyPhone;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** The linked login's id, never the login itself - it holds the hash. */
    private Long userId;

    private RoomSummary room;

    /**
     * Enough of the room to identify it on a patient record. The full room,
     * with its bed counts and cost, belongs to the rooms endpoints.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummary {
        private Long id;
        private String roomNumber;
        private String roomType;
        private String ward;

        static RoomSummary from(Room room) {
            return room == null ? null : new RoomSummary(
                    room.getId(), room.getRoomNumber(), room.getRoomType(), room.getWard());
        }
    }

    public static PatientResponse from(Patient patient) {
        if (patient == null) {
            return null;
        }
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getAddress(),
                patient.getCity(),
                patient.getState(),
                patient.getZipCode(),
                patient.getBloodGroup(),
                patient.getEmergencyContact(),
                patient.getEmergencyPhone(),
                patient.getIsActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.linkedUserId(),
                RoomSummary.from(patient.getRoom()));
    }

    public static List<PatientResponse> from(List<Patient> patients) {
        return patients == null ? List.of() : patients.stream().map(PatientResponse::from).toList();
    }

    public static PageResponse<PatientResponse> from(PageResponse<Patient> page) {
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

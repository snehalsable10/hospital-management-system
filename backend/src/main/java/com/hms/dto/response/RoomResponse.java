package com.hms.dto.response;

import com.hms.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A room as the API presents it. The patients occupying it are not listed here - that is a clinical question for the patients endpoints.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private String roomType;
    private String ward;
    private Integer capacity;
    private Integer occupiedBeds;
    private String status;
    private String description;
    private String amenities;
    private Double costPerDay;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoomResponse from(Room source) {
        if (source == null) {
            return null;
        }
        return new RoomResponse(
                source.getId(),
                source.getRoomNumber(),
                source.getRoomType(),
                source.getWard(),
                source.getCapacity(),
                source.getOccupiedBeds(),
                source.getStatus(),
                source.getDescription(),
                source.getAmenities(),
                source.getCostPerDay(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<RoomResponse> from(List<Room> sources) {
        return sources == null ? List.of()
                : sources.stream().map(RoomResponse::from).toList();
    }

    public static PageResponse<RoomResponse> from(PageResponse<Room> page) {
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

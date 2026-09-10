package com.hms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(min = 1, max = 20, message = "Room number must be between 1 and 20 characters")
    private String roomNumber;

    @NotBlank(message = "Room type is required")
    @Size(min = 2, max = 50, message = "Room type must be between 2 and 50 characters")
    private String roomType;

    @NotBlank(message = "Ward is required")
    @Size(min = 2, max = 50, message = "Ward must be between 2 and 50 characters")
    private String ward;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;

    @NotNull(message = "Cost per day is required")
    @Positive(message = "Cost per day must be positive")
    private Double costPerDay;

    @NotBlank(message = "Status is required")
    private String status;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Amenities must not exceed 100 characters")
    private String amenities;

}
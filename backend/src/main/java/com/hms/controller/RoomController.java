package com.hms.controller;

import com.hms.dto.request.RoomRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Room;
import com.hms.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Room Management", description = "APIs for managing hospital rooms and bed occupancy")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomController {

    private final RoomService roomService;

    /**
     * GET /api/rooms - List all rooms
     * All authenticated users can view (patients need to see available rooms)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get all rooms", description = "Retrieve a list of all hospital rooms")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved successfully", rooms, true));
    }

    /**
     * GET /api/rooms/{id} - Get a specific room
     * All authenticated users can view
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getRoomById(@PathVariable Long id) {
        Optional<Room> room = roomService.getRoomById(id);
        if (room.isPresent()) {
            return ResponseEntity.ok(new ApiResponse("Room retrieved successfully", room.get(), true));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Room not found", false));
        }
    }

    /**
     * GET /api/rooms/type/{roomType} - Get rooms by type
     * All authenticated users can view
     */
    @GetMapping("/type/{roomType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getRoomsByType(@PathVariable String roomType) {
        List<Room> rooms = roomService.getRoomsByType(roomType);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved by type successfully", rooms, true));
    }

    /**
     * GET /api/rooms/ward/{ward} - Get rooms by ward
     * All authenticated users can view
     */
    @GetMapping("/ward/{ward}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getRoomsByWard(@PathVariable String ward) {
        List<Room> rooms = roomService.getRoomsByWard(ward);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved by ward successfully", rooms, true));
    }

    /**
     * GET /api/rooms/status/{status} - Get rooms by status
     * All authenticated users can view
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getRoomsByStatus(@PathVariable String status) {
        List<Room> rooms = roomService.getRoomsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved by status successfully", rooms, true));
    }

    /**
     * GET /api/rooms/available - Get available rooms
     * All authenticated users can view
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getAvailableRooms() {
        List<Room> rooms = roomService.getAvailableRooms();
        return ResponseEntity.ok(new ApiResponse("Available rooms retrieved successfully", rooms, true));
    }

    /**
     * GET /api/rooms/available/type/{roomType} - Get available rooms by type
     * All authenticated users can view
     */
    @GetMapping("/available/type/{roomType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getAvailableRoomsByType(@PathVariable String roomType) {
        List<Room> rooms = roomService.getAvailableRoomsByType(roomType);
        return ResponseEntity.ok(new ApiResponse("Available rooms retrieved by type successfully", rooms, true));
    }

    /**
     * GET /api/rooms/available/ward/{ward} - Get available rooms by ward
     * All authenticated users can view
     */
    @GetMapping("/available/ward/{ward}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse> getAvailableRoomsByWard(@PathVariable String ward) {
        List<Room> rooms = roomService.getAvailableRoomsByWard(ward);
        return ResponseEntity.ok(new ApiResponse("Available rooms retrieved by ward successfully", rooms, true));
    }

    /**
     * POST /api/rooms - Create a new room
     * ADMIN, STAFF only (room management)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new room", description = "Add a new room to the hospital")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Room created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid room data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        ApiResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/rooms/{id} - Update a room
     * ADMIN, STAFF only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update room", description = "Update room details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid room data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        ApiResponse response = roomService.updateRoom(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/rooms/{id} - Delete a room
     * ADMIN only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a room", description = "Soft delete a room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteRoom(@PathVariable Long id) {
        ApiResponse response = roomService.deleteRoom(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/rooms/{id}/occupy - Mark a bed as occupied
     * ADMIN, STAFF only (managing admissions)
     */
    @PostMapping("/{id}/occupy")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Occupy a bed", description = "Mark a bed as occupied in a room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bed occupied successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Room is full"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> occupyBed(@PathVariable Long id) {
        ApiResponse response = roomService.occupyBed(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/rooms/{id}/vacate - Mark a bed as vacant
     * ADMIN, STAFF only (managing discharge)
     */
    @PostMapping("/{id}/vacate")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Vacate a bed", description = "Mark a bed as vacant in a room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bed vacated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No occupied beds to vacate"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> vacateBed(@PathVariable Long id) {
        ApiResponse response = roomService.vacateBed(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rooms/paginated - Get all rooms with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get all rooms paginated", description = "Retrieve rooms with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllRoomsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getAllRoomsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved successfully", response, true));
    }

    /**
     * GET /api/rooms/type/{roomType}/paginated - Get rooms by type with pagination
     */
    @GetMapping("/type/{roomType}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get rooms by type paginated", description = "Retrieve rooms filtered by type with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getRoomsByTypePaginated(
            @Parameter(description = "Room Type", required = true)
            @PathVariable String roomType,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getRoomsByTypePaginated(roomType, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved by type successfully", response, true));
    }

    /**
     * GET /api/rooms/ward/{ward}/paginated - Get rooms by ward with pagination
     */
    @GetMapping("/ward/{ward}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get rooms by ward paginated", description = "Retrieve rooms filtered by ward with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getRoomsByWardPaginated(
            @Parameter(description = "Ward", required = true)
            @PathVariable String ward,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getRoomsByWardPaginated(ward, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved by ward successfully", response, true));
    }

    /**
     * GET /api/rooms/status/{status}/paginated - Get rooms by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get rooms by status paginated", description = "Retrieve rooms filtered by status with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getRoomsByStatusPaginated(
            @Parameter(description = "Status", required = true)
            @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getRoomsByStatusPaginated(status, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Rooms retrieved by status successfully", response, true));
    }

    /**
     * GET /api/rooms/available/paginated - Get available rooms with pagination
     */
    @GetMapping("/available/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get available rooms paginated", description = "Retrieve available rooms with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAvailableRoomsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getAvailableRoomsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Available rooms retrieved successfully", response, true));
    }

    /**
     * GET /api/rooms/available/type/{roomType}/paginated - Get available rooms by type with pagination
     */
    @GetMapping("/available/type/{roomType}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get available rooms by type paginated", description = "Retrieve available rooms filtered by type with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAvailableRoomsByTypePaginated(
            @Parameter(description = "Room Type", required = true)
            @PathVariable String roomType,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getAvailableRoomsByTypePaginated(roomType, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Available rooms retrieved by type successfully", response, true));
    }

    /**
     * GET /api/rooms/available/ward/{ward}/paginated - Get available rooms by ward with pagination
     */
    @GetMapping("/available/ward/{ward}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get available rooms by ward paginated", description = "Retrieve available rooms filtered by ward with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAvailableRoomsByWardPaginated(
            @Parameter(description = "Ward", required = true)
            @PathVariable String ward,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Room> response = roomService.getAvailableRoomsByWardPaginated(ward, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Available rooms retrieved by ward successfully", response, true));
    }
}
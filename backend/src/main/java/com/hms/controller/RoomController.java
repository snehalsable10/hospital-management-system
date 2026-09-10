package com.hms.controller;

import com.hms.dto.request.RoomRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Room;
import com.hms.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllRooms() {
        try {
            List<Room> rooms = roomService.getAllRooms();
            return ResponseEntity.ok(new ApiResponse("Rooms retrieved successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving rooms: " + e.getMessage(), false));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRoomById(@PathVariable Long id) {
        try {
            Optional<Room> room = roomService.getRoomById(id);
            if (room.isPresent()) {
                return ResponseEntity.ok(new ApiResponse("Room retrieved successfully", room.get(), true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Room not found", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving room: " + e.getMessage(), false));
        }
    }

    @GetMapping("/type/{roomType}")
    public ResponseEntity<ApiResponse> getRoomsByType(@PathVariable String roomType) {
        try {
            List<Room> rooms = roomService.getRoomsByType(roomType);
            return ResponseEntity.ok(new ApiResponse("Rooms retrieved by type successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving rooms: " + e.getMessage(), false));
        }
    }

    @GetMapping("/ward/{ward}")
    public ResponseEntity<ApiResponse> getRoomsByWard(@PathVariable String ward) {
        try {
            List<Room> rooms = roomService.getRoomsByWard(ward);
            return ResponseEntity.ok(new ApiResponse("Rooms retrieved by ward successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving rooms: " + e.getMessage(), false));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getRoomsByStatus(@PathVariable String status) {
        try {
            List<Room> rooms = roomService.getRoomsByStatus(status);
            return ResponseEntity.ok(new ApiResponse("Rooms retrieved by status successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving rooms: " + e.getMessage(), false));
        }
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse> getAvailableRooms() {
        try {
            List<Room> rooms = roomService.getAvailableRooms();
            return ResponseEntity.ok(new ApiResponse("Available rooms retrieved successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving available rooms: " + e.getMessage(), false));
        }
    }

    @GetMapping("/available/type/{roomType}")
    public ResponseEntity<ApiResponse> getAvailableRoomsByType(@PathVariable String roomType) {
        try {
            List<Room> rooms = roomService.getAvailableRoomsByType(roomType);
            return ResponseEntity.ok(new ApiResponse("Available rooms retrieved by type successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving available rooms: " + e.getMessage(), false));
        }
    }

    @GetMapping("/available/ward/{ward}")
    public ResponseEntity<ApiResponse> getAvailableRoomsByWard(@PathVariable String ward) {
        try {
            List<Room> rooms = roomService.getAvailableRoomsByWard(ward);
            return ResponseEntity.ok(new ApiResponse("Available rooms retrieved by ward successfully", rooms, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving available rooms: " + e.getMessage(), false));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        try {
            Room room = roomService.createRoom(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Room created successfully", room, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error creating room: " + e.getMessage(), false));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        try {
            Room room = roomService.updateRoom(id, request);
            return ResponseEntity.ok(new ApiResponse("Room updated successfully", room, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error updating room: " + e.getMessage(), false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok(new ApiResponse("Room deleted successfully", null, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error deleting room: " + e.getMessage(), false));
        }
    }

    @PostMapping("/{id}/occupy")
    public ResponseEntity<ApiResponse> occupyBed(@PathVariable Long id) {
        try {
            roomService.occupyBed(id);
            return ResponseEntity.ok(new ApiResponse("Bed occupied successfully", null, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error occupying bed: " + e.getMessage(), false));
        }
    }

    @PostMapping("/{id}/vacate")
    public ResponseEntity<ApiResponse> vacateBed(@PathVariable Long id) {
        try {
            roomService.vacateBed(id);
            return ResponseEntity.ok(new ApiResponse("Bed vacated successfully", null, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error vacating bed: " + e.getMessage(), false));
        }
    }

}
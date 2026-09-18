package com.hms.service;

import com.hms.dto.request.RoomRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Room;
import com.hms.repository.RoomRepository;
import com.hms.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    /**
     * Get all rooms
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getAllRooms'")
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Get room by ID
     * Cached for 10 minutes with key = room ID
     */
    @Cacheable(value = "room", key = "#id")
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    /**
     * Get rooms by type
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getRoomsByType:' + #roomType")
    public List<Room> getRoomsByType(String roomType) {
        return roomRepository.findByRoomType(roomType);
    }

    /**
     * Get rooms by ward
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getRoomsByWard:' + #ward")
    public List<Room> getRoomsByWard(String ward) {
        return roomRepository.findByWard(ward);
    }

    /**
     * Get rooms by status
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getRoomsByStatus:' + #status")
    public List<Room> getRoomsByStatus(String status) {
        return roomRepository.findByStatus(status);
    }

    /**
     * Get all available rooms
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getAvailableRooms'")
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus("AVAILABLE");
    }

    /**
     * Get available rooms by type
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getAvailableRoomsByType:' + #roomType")
    public List<Room> getAvailableRoomsByType(String roomType) {
        return roomRepository.findByRoomTypeAndStatus(roomType, "AVAILABLE");
    }

    /**
     * Get available rooms by ward
     * Cached for 10 minutes
     */
    @Cacheable(value = "rooms", key = "'getAvailableRoomsByWard:' + #ward")
    public List<Room> getAvailableRoomsByWard(String ward) {
        return roomRepository.findByWardAndStatus(ward, "AVAILABLE");
    }

    /**
     * Create a new room
     * Clears all room caches on create
     */
    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    public ApiResponse createRoom(RoomRequest request) {
        try {
            if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
                return new ApiResponse("Room number already exists", false);
            }

            Room room = new Room();
            room.setRoomNumber(request.getRoomNumber());
            room.setRoomType(request.getRoomType());
            room.setWard(request.getWard());
            room.setCapacity(request.getCapacity());
            room.setCostPerDay(request.getCostPerDay());
            room.setStatus(request.getStatus());
            room.setDescription(request.getDescription());
            room.setAmenities(request.getAmenities());
            room.setOccupiedBeds(0);
            room.setIsActive(true);

            roomRepository.save(room);

            return new ApiResponse("Room created successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to create room: " + e.getMessage(), false);
        }
    }

    /**
     * Update an existing room
     * Clears all room caches on update
     */
    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    public ApiResponse updateRoom(Long id, RoomRequest request) {
        try {
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            if (!room.getRoomNumber().equals(request.getRoomNumber()) &&
                roomRepository.existsByRoomNumber(request.getRoomNumber())) {
                return new ApiResponse("Room number already exists", false);
            }

            room.setRoomNumber(request.getRoomNumber());
            room.setRoomType(request.getRoomType());
            room.setWard(request.getWard());
            room.setCapacity(request.getCapacity());
            room.setCostPerDay(request.getCostPerDay());
            room.setStatus(request.getStatus());
            room.setDescription(request.getDescription());
            room.setAmenities(request.getAmenities());

            roomRepository.save(room);

            return new ApiResponse("Room updated successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to update room: " + e.getMessage(), false);
        }
    }

    /**
     * Delete a room (soft delete)
     * Clears all room caches on delete
     */
    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    public ApiResponse deleteRoom(Long id) {
        try {
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room not found"));
            room.setIsActive(false);
            roomRepository.save(room);

            return new ApiResponse("Room deleted successfully", true);
        } catch (Exception e) {
            return new ApiResponse("Failed to delete room: " + e.getMessage(), false);
        }
    }

    /**
     * Occupy a bed in a room
     * Clears all room caches (modifies room state)
     */
    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    public ApiResponse occupyBed(Long roomId) {
        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            if (room.getOccupiedBeds() < room.getCapacity()) {
                room.setOccupiedBeds(room.getOccupiedBeds() + 1);

                if (room.getOccupiedBeds() >= room.getCapacity()) {
                    room.setStatus("FULL");
                } else {
                    room.setStatus("AVAILABLE");
                }

                roomRepository.save(room);
                return new ApiResponse("Bed occupied successfully", true);
            } else {
                return new ApiResponse("Room is already full", false);
            }
        } catch (Exception e) {
            return new ApiResponse("Failed to occupy bed: " + e.getMessage(), false);
        }
    }

    /**
     * Vacate a bed in a room
     * Clears all room caches (modifies room state)
     */
    @CacheEvict(value = {"rooms", "room"}, allEntries = true)
    public ApiResponse vacateBed(Long roomId) {
        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            if (room.getOccupiedBeds() > 0) {
                room.setOccupiedBeds(room.getOccupiedBeds() - 1);
                room.setStatus("AVAILABLE");
                roomRepository.save(room);
                return new ApiResponse("Bed vacated successfully", true);
            } else {
                return new ApiResponse("No occupied beds to vacate", false);
            }
        } catch (Exception e) {
            return new ApiResponse("Failed to vacate bed: " + e.getMessage(), false);
        }
    }

    /**
     * Get all rooms with pagination
     */
    public PageResponse<Room> getAllRoomsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findAll(pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get rooms by type with pagination
     */
    public PageResponse<Room> getRoomsByTypePaginated(String roomType, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findByRoomType(roomType, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get rooms by ward with pagination
     */
    public PageResponse<Room> getRoomsByWardPaginated(String ward, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findByWard(ward, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get rooms by status with pagination
     */
    public PageResponse<Room> getRoomsByStatusPaginated(String status, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findByStatus(status, pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get available rooms with pagination
     */
    public PageResponse<Room> getAvailableRoomsPaginated(int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findByStatus("AVAILABLE", pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get available rooms by type with pagination
     */
    public PageResponse<Room> getAvailableRoomsByTypePaginated(String roomType, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findByRoomTypeAndStatus(roomType, "AVAILABLE", pageable);
        return PaginationUtil.toPageResponse(page);
    }

    /**
     * Get available rooms by ward with pagination
     */
    public PageResponse<Room> getAvailableRoomsByWardPaginated(String ward, int pageNumber, int pageSize) {
        PaginationUtil.validatePaginationParams(pageNumber, pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Room> page = roomRepository.findByWardAndStatus(ward, "AVAILABLE", pageable);
        return PaginationUtil.toPageResponse(page);
    }
}
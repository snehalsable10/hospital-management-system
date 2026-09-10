package com.hms.service;

import com.hms.dto.request.RoomRequest;
import com.hms.entity.Room;
import com.hms.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public List<Room> getRoomsByType(String roomType) {
        return roomRepository.findByRoomType(roomType);
    }

    public List<Room> getRoomsByWard(String ward) {
        return roomRepository.findByWard(ward);
    }

    public List<Room> getRoomsByStatus(String status) {
        return roomRepository.findByStatus(status);
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus("AVAILABLE");
    }

    public List<Room> getAvailableRoomsByType(String roomType) {
        return roomRepository.findByRoomTypeAndStatus(roomType, "AVAILABLE");
    }

    public List<Room> getAvailableRoomsByWard(String ward) {
        return roomRepository.findByWardAndStatus(ward, "AVAILABLE");
    }

    public Room createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new RuntimeException("Room number already exists");
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

        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getRoomNumber().equals(request.getRoomNumber()) && 
            roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new RuntimeException("Room number already exists");
        }

        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(request.getRoomType());
        room.setWard(request.getWard());
        room.setCapacity(request.getCapacity());
        room.setCostPerDay(request.getCostPerDay());
        room.setStatus(request.getStatus());
        room.setDescription(request.getDescription());
        room.setAmenities(request.getAmenities());

        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.setIsActive(false);
        roomRepository.save(room);
    }

    public void occupyBed(Long roomId) {
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
        } else {
            throw new RuntimeException("Room is already full");
        }
    }

    public void vacateBed(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        
        if (room.getOccupiedBeds() > 0) {
            room.setOccupiedBeds(room.getOccupiedBeds() - 1);
            room.setStatus("AVAILABLE");
            roomRepository.save(room);
        } else {
            throw new RuntimeException("No occupied beds to vacate");
        }
    }

}
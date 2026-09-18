package com.hms.repository;

import com.hms.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByRoomType(String roomType);
    List<Room> findByWard(String ward);
    List<Room> findByStatus(String status);
    List<Room> findByRoomTypeAndStatus(String roomType, String status);
    List<Room> findByWardAndStatus(String ward, String status);
    boolean existsByRoomNumber(String roomNumber);

    // Paginated methods for performance optimization
    Page<Room> findByRoomType(String roomType, Pageable pageable);
    Page<Room> findByWard(String ward, Pageable pageable);
    Page<Room> findByStatus(String status, Pageable pageable);
    Page<Room> findByRoomTypeAndStatus(String roomType, String status, Pageable pageable);
    Page<Room> findByWardAndStatus(String ward, String status, Pageable pageable);
}
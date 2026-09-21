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
    List<Room> findByRoomTypeAndIsActiveTrue(String roomType);
    List<Room> findByWardAndIsActiveTrue(String ward);
    List<Room> findByStatusAndIsActiveTrue(String status);
    List<Room> findByRoomTypeAndStatusAndIsActiveTrue(String roomType, String status);
    List<Room> findByWardAndStatusAndIsActiveTrue(String ward, String status);
    boolean existsByRoomNumber(String roomNumber);

    // Paginated methods for performance optimization
    Page<Room> findByRoomTypeAndIsActiveTrue(String roomType, Pageable pageable);
    Page<Room> findByWardAndIsActiveTrue(String ward, Pageable pageable);
    Page<Room> findByStatusAndIsActiveTrue(String status, Pageable pageable);
    Page<Room> findByRoomTypeAndStatusAndIsActiveTrue(String roomType, String status, Pageable pageable);
    Page<Room> findByWardAndStatusAndIsActiveTrue(String ward, String status, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Room> findByIsActiveTrue();
    Page<Room> findByIsActiveTrue(Pageable pageable);
}
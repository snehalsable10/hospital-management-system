package com.hms.repository;

import com.hms.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);
    List<Patient> findByPhone(String phone);
    List<Patient> findByFirstNameIgnoreCaseContainingAndIsActiveTrue(String firstName);
    List<Patient> findByLastNameIgnoreCaseContainingAndIsActiveTrue(String lastName);
    List<Patient> findByCityAndIsActiveTrue(String city);
    boolean existsByEmail(String email);

    // A login account may own at most one patient record
    Optional<Patient> findByUserId(Long userId);

    // Paginated methods for performance optimization
    Page<Patient> findByFirstNameIgnoreCaseContainingAndIsActiveTrue(String firstName, Pageable pageable);
    Page<Patient> findByLastNameIgnoreCaseContainingAndIsActiveTrue(String lastName, Pageable pageable);
    Page<Patient> findByCityAndIsActiveTrue(String city, Pageable pageable);

    // Soft delete: lists must not show records flagged inactive
    List<Patient> findByIsActiveTrue();
    Page<Patient> findByIsActiveTrue(Pageable pageable);
}
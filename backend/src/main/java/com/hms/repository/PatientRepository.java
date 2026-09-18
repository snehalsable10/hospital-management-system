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
    List<Patient> findByFirstNameIgnoreCaseContaining(String firstName);
    List<Patient> findByLastNameIgnoreCaseContaining(String lastName);
    List<Patient> findByCity(String city);
    boolean existsByEmail(String email);

    // Paginated methods for performance optimization
    Page<Patient> findByFirstNameIgnoreCaseContaining(String firstName, Pageable pageable);
    Page<Patient> findByLastNameIgnoreCaseContaining(String lastName, Pageable pageable);
    Page<Patient> findByCity(String city, Pageable pageable);
}
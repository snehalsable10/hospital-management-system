package com.hms.repository;

import com.hms.entity.LaboratoryTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LaboratoryTestRepository extends JpaRepository<LaboratoryTest, Long> {
    List<LaboratoryTest> findByPatientId(Long patientId);
    List<LaboratoryTest> findByTestNameIgnoreCaseContaining(String testName);
    List<LaboratoryTest> findByStatus(String status);
    List<LaboratoryTest> findByTestDateBetween(LocalDate startDate, LocalDate endDate);
    List<LaboratoryTest> findByPatientIdAndStatus(Long patientId, String status);
    List<LaboratoryTest> findByPatientIdAndTestDateBetween(Long patientId, LocalDate startDate, LocalDate endDate);
}
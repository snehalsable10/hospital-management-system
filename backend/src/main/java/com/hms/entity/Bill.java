package com.hms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills", indexes = {
    @Index(name = "idx_bill_patient_id", columnList = "patient_id"),
    @Index(name = "idx_bill_doctor_id", columnList = "doctor_id"),
    @Index(name = "idx_bill_status", columnList = "status"),
    @Index(name = "idx_bill_date", columnList = "bill_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal testsFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal medicationsFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal otherCharges;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    private String status = "UNPAID";

    @Column(nullable = false, length = 50)
    private String paymentMethod;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "UNPAID";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
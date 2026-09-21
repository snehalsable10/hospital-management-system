package com.hms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "doctors", indexes = {
    @Index(name = "idx_doctor_email", columnList = "email", unique = true),
    @Index(name = "idx_doctor_department_id", columnList = "department_id"),
    @Index(name = "idx_doctor_is_active", columnList = "is_active"),
    @Index(name = "idx_doctor_specialization", columnList = "specialization"),
    @Index(name = "idx_doctor_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String specialization;

    @Column(unique = true, nullable = false, length = 50)
    private String licenseNumber;

    /**
     * The login account this doctor record belongs to, if any.
     * Ownership checks (@authService.isOwnDoctor) compare against this.
     * JSON-ignored: User carries the bcrypt password hash.
     */
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Excluded from JSON, toString and equals to break the parent/child cycle.
    // Use /api/appointments/doctor/{id} and /api/prescriptions/doctor/{id}.

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> prescriptions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * The id of the linked login, without the login itself.
     *
     * The User object is @JsonIgnore'd because it holds the bcrypt hash, but
     * the administration screens still need to know whether this record is
     * claimed and by whom. Read-only: the link is set through the request DTO's
     * userId, never by posting this back.
     *
     * Deliberately NOT named getUserId: a bean property called "userId" makes
     * Spring Data resolve findByUserId as a single path segment rather than a
     * traversal into user.id, and the context then fails to start because the
     * JPA metamodel has no such attribute.
     */
    @JsonProperty(value = "userId", access = JsonProperty.Access.READ_ONLY)
    public Long linkedUserId() {
        return user == null ? null : user.getId();
    }

}
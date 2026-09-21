package com.hms.service;

import com.hms.dto.response.DashboardSummary;
import com.hms.entity.Appointment;
import com.hms.repository.AppointmentRepository;
import com.hms.repository.BillRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import com.hms.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * The dashboard's numbers, counted in the database rather than in the browser.
 *
 * Which sections get filled follows the same rules as the module list
 * endpoints, so the dashboard can never show a caller a total it would be
 * refused if it asked for the underlying list.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final RoomRepository roomRepository;

    // Mirrors the @PreAuthorize on each module's list endpoint.
    private static final Set<String> PATIENT_READERS = Set.of("ADMIN", "STAFF", "DOCTOR");
    private static final Set<String> DOCTOR_READERS = Set.of("ADMIN", "STAFF", "DOCTOR", "PATIENT");
    private static final Set<String> APPOINTMENT_READERS = Set.of("ADMIN", "STAFF");
    private static final Set<String> BILL_READERS = Set.of("ADMIN", "STAFF");
    private static final Set<String> ROOM_READERS = Set.of("ADMIN", "STAFF", "DOCTOR", "PATIENT");

    /**
     * Cached per role, not per user: every caller with the same role sees the
     * same figures, and caching per user would multiply identical entries.
     */
    @Cacheable(value = "dashboard", key = "'summary:' + #role")
    public DashboardSummary getSummary(String role) {
        String normalised = role == null ? "" : role.trim().toUpperCase();
        DashboardSummary summary = new DashboardSummary();

        boolean patients = PATIENT_READERS.contains(normalised);
        boolean doctors = DOCTOR_READERS.contains(normalised);
        boolean appointments = APPOINTMENT_READERS.contains(normalised);
        boolean bills = BILL_READERS.contains(normalised);
        boolean rooms = ROOM_READERS.contains(normalised);

        summary.getSections().put("patients", patients);
        summary.getSections().put("doctors", doctors);
        summary.getSections().put("appointments", appointments);
        summary.getSections().put("bills", bills);
        summary.getSections().put("rooms", rooms);

        if (patients) {
            summary.setTotalPatients(patientRepository.countByIsActiveTrue());
        }

        if (doctors) {
            summary.setTotalDoctors(doctorRepository.countByIsActiveTrue());
            summary.setDoctorsByDepartment(
                    doctorRepository.countGroupedByDepartment().stream()
                            .map(row -> new DashboardSummary.NamedCount(
                                    row[0] == null ? "Unassigned" : (String) row[0],
                                    toLong(row[1])))
                            .toList());
        }

        if (appointments) {
            summary.setTotalAppointments(appointmentRepository.countByIsActiveTrue());
            summary.setAppointmentsByStatus(
                    appointmentRepository.countGroupedByStatus().stream()
                            .map(row -> new DashboardSummary.NamedCount(
                                    (String) row[0], toLong(row[1])))
                            .toList());
            summary.setRecentAppointments(
                    appointmentRepository
                            .findTop5ByIsActiveTrueOrderByAppointmentDateDescAppointmentTimeDesc()
                            .stream()
                            .map(this::toRecent)
                            .toList());
        }

        if (bills) {
            summary.setTotalRevenue(orZero(billRepository.sumTotalAmount()));
            summary.setOutstanding(orZero(billRepository.sumOutstanding()));

            long paid = 0;
            long unpaid = 0;
            for (Object[] row : billRepository.countGroupedByStatus()) {
                if ("PAID".equals(row[0])) {
                    paid = toLong(row[1]);
                } else if ("UNPAID".equals(row[0])) {
                    unpaid = toLong(row[1]);
                }
            }
            summary.setPaidBills(paid);
            summary.setUnpaidBills(unpaid);
        }

        if (rooms) {
            summary.setTotalRooms(roomRepository.countByIsActiveTrue());

            List<DashboardSummary.Occupancy> byType =
                    roomRepository.bedsGroupedByType().stream()
                            .map(row -> new DashboardSummary.Occupancy(
                                    row[0] == null ? "Other" : (String) row[0],
                                    toLong(row[1]),
                                    toLong(row[2])))
                            .toList();

            summary.setOccupancyByRoomType(byType);
            // The hospital-wide totals are these rows added up, so there is no
            // second query for them.
            summary.setOccupiedBeds(byType.stream().mapToLong(DashboardSummary.Occupancy::getOccupied).sum());
            summary.setTotalBeds(byType.stream().mapToLong(DashboardSummary.Occupancy::getTotal).sum());
        }

        return summary;
    }

    private DashboardSummary.RecentAppointment toRecent(Appointment appointment) {
        return new DashboardSummary.RecentAppointment(
                appointment.getId(),
                name(appointment.getPatient() == null ? null : appointment.getPatient().getFirstName(),
                        appointment.getPatient() == null ? null : appointment.getPatient().getLastName()),
                name(appointment.getDoctor() == null ? null : appointment.getDoctor().getFirstName(),
                        appointment.getDoctor() == null ? null : appointment.getDoctor().getLastName()),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getStatus());
    }

    private String name(String first, String last) {
        String joined = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return joined.isEmpty() ? null : joined;
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

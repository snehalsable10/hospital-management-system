package com.hms.service;

import com.hms.dto.response.DashboardSummary;
import com.hms.repository.AppointmentRepository;
import com.hms.repository.BillRepository;
import com.hms.repository.DoctorRepository;
import com.hms.repository.PatientRepository;
import com.hms.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The dashboard must never show a caller a figure it would be refused if it
 * asked for the underlying list, and its bed totals must agree with the
 * per-type rows they are derived from.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private BillRepository billRepository;
    @Mock private RoomRepository roomRepository;

    @InjectMocks private DashboardService dashboardService;

    private void stubEverything() {
        lenient().when(patientRepository.countByIsActiveTrue()).thenReturn(15L);
        lenient().when(doctorRepository.countByIsActiveTrue()).thenReturn(3L);
        lenient().when(doctorRepository.countGroupedByDepartment())
                .thenReturn(List.<Object[]>of(new Object[]{"Cardiology", 3L}));
        lenient().when(appointmentRepository.countByIsActiveTrue()).thenReturn(12L);
        lenient().when(appointmentRepository.countGroupedByStatus())
                .thenReturn(List.<Object[]>of(new Object[]{"SCHEDULED", 8L}, new Object[]{"COMPLETED", 3L}));
        lenient().when(appointmentRepository
                        .findTop5ByIsActiveTrueOrderByAppointmentDateDescAppointmentTimeDesc())
                .thenReturn(List.of());
        lenient().when(billRepository.sumTotalAmount()).thenReturn(new BigDecimal("26750.00"));
        lenient().when(billRepository.sumOutstanding()).thenReturn(new BigDecimal("17400.00"));
        lenient().when(billRepository.countGroupedByStatus())
                .thenReturn(List.<Object[]>of(new Object[]{"PAID", 5L}, new Object[]{"UNPAID", 5L},
                        new Object[]{"CANCELLED", 1L}));
        lenient().when(roomRepository.countByIsActiveTrue()).thenReturn(11L);
        lenient().when(roomRepository.bedsGroupedByType())
                .thenReturn(List.<Object[]>of(
                        new Object[]{"Private", 1L, 3L},
                        new Object[]{"Maternity", 0L, 6L},
                        new Object[]{"ICU", 8L, 10L},
                        new Object[]{"General", 4L, 18L}));
    }

    // --- role scoping ---

    @ParameterizedTest(name = "{0} may see every section")
    @ValueSource(strings = {"ADMIN", "STAFF"})
    @DisplayName("administrators and staff see everything")
    void adminAndStaffSeeEverything(String role) {
        stubEverything();

        DashboardSummary summary = dashboardService.getSummary(role);

        assertThat(summary.getSections()).containsOnly(
                org.assertj.core.api.Assertions.entry("patients", true),
                org.assertj.core.api.Assertions.entry("doctors", true),
                org.assertj.core.api.Assertions.entry("appointments", true),
                org.assertj.core.api.Assertions.entry("bills", true),
                org.assertj.core.api.Assertions.entry("rooms", true));
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo("26750.00");
    }

    @Test
    @DisplayName("a doctor gets patients, doctors and rooms but no appointments or money")
    void doctorIsScoped() {
        stubEverything();

        DashboardSummary summary = dashboardService.getSummary("DOCTOR");

        assertThat(summary.getSections())
                .containsEntry("patients", true)
                .containsEntry("doctors", true)
                .containsEntry("rooms", true)
                .containsEntry("appointments", false)
                .containsEntry("bills", false);

        assertThat(summary.getTotalPatients()).isEqualTo(15L);
        assertThat(summary.getTotalRooms()).isEqualTo(11L);

        // Null, not zero: "not yours" and "none yet" must stay distinguishable.
        assertThat(summary.getTotalAppointments()).isNull();
        assertThat(summary.getTotalRevenue()).isNull();
        assertThat(summary.getOutstanding()).isNull();
        assertThat(summary.getPaidBills()).isNull();

        verify(billRepository, never()).sumTotalAmount();
        verify(appointmentRepository, never()).countByIsActiveTrue();
    }

    @Test
    @DisplayName("a patient sees neither the patient roll nor any money")
    void patientIsScoped() {
        stubEverything();

        DashboardSummary summary = dashboardService.getSummary("PATIENT");

        assertThat(summary.getSections())
                .containsEntry("patients", false)
                .containsEntry("doctors", true)
                .containsEntry("rooms", true)
                .containsEntry("appointments", false)
                .containsEntry("bills", false);

        assertThat(summary.getTotalPatients()).isNull();
        verify(patientRepository, never()).countByIsActiveTrue();
    }

    @Test
    @DisplayName("an unrecognised or missing role gets nothing rather than everything")
    void unknownRoleSeesNothing() {
        DashboardSummary summary = dashboardService.getSummary(null);

        assertThat(summary.getSections()).containsOnlyKeys(
                "patients", "doctors", "appointments", "bills", "rooms");
        assertThat(summary.getSections().values()).containsOnly(false);
        assertThat(summary.getTotalPatients()).isNull();
        assertThat(summary.getTotalDoctors()).isNull();
    }

    @Test
    @DisplayName("role matching ignores case and surrounding space")
    void roleIsNormalised() {
        stubEverything();

        assertThat(dashboardService.getSummary(" admin ").getSections())
                .containsEntry("bills", true);
    }

    // --- arithmetic ---

    @Test
    @DisplayName("hospital-wide bed totals are the per-type rows added up")
    void bedTotalsMatchTheirRows() {
        stubEverything();

        DashboardSummary summary = dashboardService.getSummary("ADMIN");

        assertThat(summary.getOccupiedBeds()).isEqualTo(1 + 0 + 8 + 4);
        assertThat(summary.getTotalBeds()).isEqualTo(3 + 6 + 10 + 18);

        long occupiedFromRows = summary.getOccupancyByRoomType().stream()
                .mapToLong(DashboardSummary.Occupancy::getOccupied).sum();
        assertThat(summary.getOccupiedBeds()).isEqualTo(occupiedFromRows);
    }

    @Test
    @DisplayName("only PAID and UNPAID feed the bill counters; other statuses are ignored")
    void billCountersIgnoreOtherStatuses() {
        stubEverything();

        DashboardSummary summary = dashboardService.getSummary("ADMIN");

        assertThat(summary.getPaidBills()).isEqualTo(5L);
        assertThat(summary.getUnpaidBills()).isEqualTo(5L);
    }

    @Test
    @DisplayName("a doctor with no department is labelled rather than dropped")
    void nullDepartmentBecomesUnassigned() {
        stubEverything();
        lenient().when(doctorRepository.countGroupedByDepartment())
                .thenReturn(List.<Object[]>of(new Object[]{"Cardiology", 3L}, new Object[]{null, 2L}));

        DashboardSummary summary = dashboardService.getSummary("ADMIN");

        assertThat(summary.getDoctorsByDepartment())
                .extracting(DashboardSummary.NamedCount::getName)
                .containsExactly("Cardiology", "Unassigned");
    }

    @Test
    @DisplayName("no bills at all reads as zero, not as a null that would crash the page")
    void emptyMoneySumsAreZero() {
        stubEverything();
        lenient().when(billRepository.sumTotalAmount()).thenReturn(null);
        lenient().when(billRepository.sumOutstanding()).thenReturn(null);

        DashboardSummary summary = dashboardService.getSummary("ADMIN");

        assertThat(summary.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(summary.getOutstanding()).isEqualByComparingTo("0");
    }
}

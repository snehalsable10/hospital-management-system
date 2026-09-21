package com.hms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the dashboard draws, in one response.
 *
 * It used to fetch five full lists and count them in the browser, which meant
 * five round trips and every row of every table on the wire to render a
 * handful of numbers.
 *
 * `sections` says which parts the caller was allowed to see. A section the
 * caller may not read is absent rather than zero, so the page can hide the
 * panel instead of showing it empty - "not yours" and "none yet" are different
 * things.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private Map<String, Boolean> sections = new LinkedHashMap<>();

    private Long totalPatients;
    private Long totalDoctors;
    private Long totalAppointments;
    private Long totalRooms;

    private Long occupiedBeds;
    private Long totalBeds;

    private BigDecimal totalRevenue;
    private BigDecimal outstanding;
    private Long paidBills;
    private Long unpaidBills;

    private List<NamedCount> appointmentsByStatus = new ArrayList<>();
    private List<NamedCount> doctorsByDepartment = new ArrayList<>();
    private List<Occupancy> occupancyByRoomType = new ArrayList<>();
    private List<RecentAppointment> recentAppointments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NamedCount {
        private String name;
        private Long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Occupancy {
        private String type;
        private Long occupied;
        private Long total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAppointment {
        private Long id;
        private String patientName;
        private String doctorName;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String status;
    }
}

package com.hms.controller;

import com.hms.dto.request.BillRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.BillResponse;
import com.hms.dto.response.PageResponse;
import com.hms.entity.Bill;
import com.hms.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"})
@Tag(name = "Bill Management", description = "APIs for managing patient bills and financial transactions")
@SecurityRequirement(name = "Bearer Authentication")
public class BillController {

    private final BillService billService;

    /**
     * GET /api/bills - List all bills
     * ADMIN, STAFF only (sensitive financial data)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all bills", description = "Retrieve a list of all patient bills in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN/STAFF can view all"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllBills() {
        List<Bill> bills = billService.getAllBills();
        return ResponseEntity.ok(new ApiResponse("Bills retrieved successfully", BillResponse.from(bills), true));
    }

    /**
     * GET /api/bills/{id} - Get a specific bill
     * ADMIN, STAFF, or patient of this bill
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnBill(#id)")
    @Operation(summary = "Get bill by ID", description = "Retrieve details of a specific patient bill")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bill retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bill not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getBillById(
            @Parameter(description = "Bill ID", required = true)
            @PathVariable Long id) {
        Optional<Bill> bill = billService.getBillById(id);

        if (!bill.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Bill not found", false));
        }

        return ResponseEntity.ok(new ApiResponse("Bill retrieved successfully", BillResponse.from(bill.get()), true));
    }

    /**
     * GET /api/bills/patient/{patientId} - Get all bills for a patient
     * ADMIN, STAFF, or the patient themselves
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    public ResponseEntity<ApiResponse> getBillsByPatient(@PathVariable Long patientId) {
        List<Bill> bills = billService.getBillsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse("Patient bills retrieved successfully", BillResponse.from(bills), true));
    }

    /**
     * GET /api/bills/doctor/{doctorId} - Get all bills for a doctor
     * ADMIN, STAFF, or the doctor themselves
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    public ResponseEntity<ApiResponse> getBillsByDoctor(@PathVariable Long doctorId) {
        List<Bill> bills = billService.getBillsByDoctor(doctorId);
        return ResponseEntity.ok(new ApiResponse("Doctor bills retrieved successfully", BillResponse.from(bills), true));
    }

    /**
     * GET /api/bills/status/{status} - Get bills by status
     * ADMIN, STAFF only
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse> getBillsByStatus(@PathVariable String status) {
        List<Bill> bills = billService.getBillsByStatus(status);
        return ResponseEntity.ok(new ApiResponse("Bills retrieved by status successfully", BillResponse.from(bills), true));
    }

    /**
     * GET /api/bills/daterange - Get bills in date range
     * ADMIN, STAFF only
     */
    @GetMapping("/daterange")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse> getBillsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Bill> bills = billService.getBillsByDateRange(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse("Bills retrieved by date range successfully", BillResponse.from(bills), true));
    }

    /**
     * GET /api/bills/patient/{patientId}/unpaid - Get unpaid bills for a patient
     * ADMIN, STAFF, or the patient themselves
     */
    @GetMapping("/patient/{patientId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    public ResponseEntity<ApiResponse> getPatientUnpaidBills(@PathVariable Long patientId) {
        List<Bill> bills = billService.getPatientUnpaidBills(patientId);
        return ResponseEntity.ok(new ApiResponse("Patient unpaid bills retrieved successfully", BillResponse.from(bills), true));
    }

    /**
     * GET /api/bills/doctor/{doctorId}/paid - Get paid bills for a doctor
     * ADMIN, STAFF, or the doctor themselves
     */
    @GetMapping("/doctor/{doctorId}/paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    public ResponseEntity<ApiResponse> getDoctorPaidBills(@PathVariable Long doctorId) {
        List<Bill> bills = billService.getDoctorPaidBills(doctorId);
        return ResponseEntity.ok(new ApiResponse("Doctor paid bills retrieved successfully", BillResponse.from(bills), true));
    }

    /**
     * POST /api/bills - Create a new bill
     * ADMIN, STAFF only (financial control)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new bill", description = "Create a new patient bill with charges")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bill created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid bill data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> createBill(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bill data", required = true)
            @Valid @RequestBody BillRequest request) {
        ApiResponse response = billService.createBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/bills/{id} - Update a bill
     * ADMIN, STAFF only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update bill", description = "Update an existing bill")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bill updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid bill data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> updateBill(
            @Parameter(description = "Bill ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated bill data", required = true)
            @Valid @RequestBody BillRequest request) {
        ApiResponse response = billService.updateBill(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/bills/{id} - Delete a bill
     * ADMIN only (financial audit trail)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a bill", description = "Soft delete a bill (marks as inactive)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bill deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bill not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> deleteBill(
            @Parameter(description = "Bill ID", required = true)
            @PathVariable Long id) {
        ApiResponse response = billService.deleteBill(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/bills/paginated - Get all bills with pagination
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all bills paginated", description = "Retrieve bills with pagination (optimized for large datasets)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getAllBillsPaginated(
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Bill> response = billService.getAllBillsPaginated(pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Bills retrieved successfully", BillResponse.from(response), true));
    }

    /**
     * GET /api/bills/patient/{patientId}/paginated - Get patient bills with pagination
     */
    @GetMapping("/patient/{patientId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's bills paginated", description = "Retrieve patient bills with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getBillsByPatientPaginated(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Bill> response = billService.getBillsByPatientPaginated(patientId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patient bills retrieved successfully", BillResponse.from(response), true));
    }

    /**
     * GET /api/bills/doctor/{doctorId}/paginated - Get doctor bills with pagination
     */
    @GetMapping("/doctor/{doctorId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnDoctor(#doctorId)")
    @Operation(summary = "Get doctor's bills paginated", description = "Retrieve doctor bills with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getBillsByDoctorPaginated(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable Long doctorId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Bill> response = billService.getBillsByDoctorPaginated(doctorId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Doctor bills retrieved successfully", BillResponse.from(response), true));
    }

    /**
     * GET /api/bills/status/{status}/paginated - Get bills by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get bills by status paginated", description = "Retrieve bills filtered by status with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getBillsByStatusPaginated(
            @Parameter(description = "Status", required = true)
            @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Bill> response = billService.getBillsByStatusPaginated(status, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Bills retrieved by status successfully", BillResponse.from(response), true));
    }

    /**
     * GET /api/bills/daterange/paginated - Get bills by date range with pagination
     */
    @GetMapping("/daterange/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get bills by date range paginated", description = "Retrieve bills within date range with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getBillsByDateRangePaginated(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Bill> response = billService.getBillsByDateRangePaginated(startDate, endDate, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Bills retrieved by date range successfully", BillResponse.from(response), true));
    }

    /**
     * GET /api/bills/patient/{patientId}/unpaid/paginated - Get unpaid bills with pagination
     */
    @GetMapping("/patient/{patientId}/unpaid/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or @authService.isOwnPatient(#patientId)")
    @Operation(summary = "Get patient's unpaid bills paginated", description = "Retrieve unpaid bills with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bills retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse> getPatientUnpaidBillsPaginated(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable Long patientId,
            @Parameter(description = "Page number (0-indexed)", required = false)
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Page size (max 100)", required = false)
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<Bill> response = billService.getPatientUnpaidBillsPaginated(patientId, pageNumber, pageSize);
        return ResponseEntity.ok(new ApiResponse("Patient unpaid bills retrieved successfully", BillResponse.from(response), true));
    }
}
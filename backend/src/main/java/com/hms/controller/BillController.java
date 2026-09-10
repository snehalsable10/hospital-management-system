package com.hms.controller;

import com.hms.dto.request.BillRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Bill;
import com.hms.service.BillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class BillController {

    @Autowired
    private BillService billService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllBills() {
        try {
            List<Bill> bills = billService.getAllBills();
            return ResponseEntity.ok(new ApiResponse("Bills retrieved successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving bills: " + e.getMessage(), false));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getBillById(@PathVariable Long id) {
        try {
            Optional<Bill> bill = billService.getBillById(id);
            if (bill.isPresent()) {
                return ResponseEntity.ok(new ApiResponse("Bill retrieved successfully", bill.get(), true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Bill not found", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving bill: " + e.getMessage(), false));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse> getBillsByPatient(@PathVariable Long patientId) {
        try {
            List<Bill> bills = billService.getBillsByPatient(patientId);
            return ResponseEntity.ok(new ApiResponse("Patient bills retrieved successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), false));
        }
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse> getBillsByDoctor(@PathVariable Long doctorId) {
        try {
            List<Bill> bills = billService.getBillsByDoctor(doctorId);
            return ResponseEntity.ok(new ApiResponse("Doctor bills retrieved successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), false));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getBillsByStatus(@PathVariable String status) {
        try {
            List<Bill> bills = billService.getBillsByStatus(status);
            return ResponseEntity.ok(new ApiResponse("Bills retrieved by status successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving bills: " + e.getMessage(), false));
        }
    }

    @GetMapping("/daterange")
    public ResponseEntity<ApiResponse> getBillsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Bill> bills = billService.getBillsByDateRange(startDate, endDate);
            return ResponseEntity.ok(new ApiResponse("Bills retrieved by date range successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving bills: " + e.getMessage(), false));
        }
    }

    @GetMapping("/patient/{patientId}/unpaid")
    public ResponseEntity<ApiResponse> getPatientUnpaidBills(@PathVariable Long patientId) {
        try {
            List<Bill> bills = billService.getPatientUnpaidBills(patientId);
            return ResponseEntity.ok(new ApiResponse("Patient unpaid bills retrieved successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), false));
        }
    }

    @GetMapping("/doctor/{doctorId}/paid")
    public ResponseEntity<ApiResponse> getDoctorPaidBills(@PathVariable Long doctorId) {
        try {
            List<Bill> bills = billService.getDoctorPaidBills(doctorId);
            return ResponseEntity.ok(new ApiResponse("Doctor paid bills retrieved successfully", bills, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage(), false));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createBill(@Valid @RequestBody BillRequest request) {
        try {
            Bill bill = billService.createBill(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Bill created successfully", bill, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error creating bill: " + e.getMessage(), false));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateBill(@PathVariable Long id, @Valid @RequestBody BillRequest request) {
        try {
            Bill bill = billService.updateBill(id, request);
            return ResponseEntity.ok(new ApiResponse("Bill updated successfully", bill, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error updating bill: " + e.getMessage(), false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBill(@PathVariable Long id) {
        try {
            billService.deleteBill(id);
            return ResponseEntity.ok(new ApiResponse("Bill deleted successfully", null, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Error deleting bill: " + e.getMessage(), false));
        }
    }

}
package com.hms.dto.response;

import com.hms.entity.Bill;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A bill as the API presents it, with the fees that make up its total.
 *
 * See PatientResponse for why entities are no longer serialised directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private Long id;
    private Summaries.PatientSummary patient;
    private Summaries.DoctorSummary doctor;
    private LocalDate billDate;
    private BigDecimal consultationFee;
    private BigDecimal testsFee;
    private BigDecimal medicationsFee;
    private BigDecimal otherCharges;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BillResponse from(Bill source) {
        if (source == null) {
            return null;
        }
        return new BillResponse(
                source.getId(),
                Summaries.PatientSummary.from(source.getPatient()),
                Summaries.DoctorSummary.from(source.getDoctor()),
                source.getBillDate(),
                source.getConsultationFee(),
                source.getTestsFee(),
                source.getMedicationsFee(),
                source.getOtherCharges(),
                source.getTotalAmount(),
                source.getStatus(),
                source.getPaymentMethod(),
                source.getDescription(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }

    public static List<BillResponse> from(List<Bill> sources) {
        return sources == null ? List.of()
                : sources.stream().map(BillResponse::from).toList();
    }

    public static PageResponse<BillResponse> from(PageResponse<Bill> page) {
        return new PageResponse<>(
                from(page.getContent()),
                page.getPageNumber(),
                page.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isHasNext(),
                page.isHasPrevious());
    }
}
